// Copyright 2026 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.language.pure.dsl.generation.model;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModelProcessParameter;
import org.finos.legend.engine.language.pure.dsl.generation.compiler.toPureGraph.GenerationCompilerExtension;
import org.finos.legend.engine.language.pure.dsl.generation.compiler.toPureGraph.HelperGenerationSpecificationBuilder;
import org.finos.legend.engine.language.pure.dsl.generation.extension.ModelGenerationExtension;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.generationSpecification.GenerationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.generationSpecification.GenerationTreeNode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.generationSpecification.GenerationTreeNodeHelper;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.function.Function;

/**
 * Runs the model generations of a {@link GenerationSpecification}.
 * <p>
 * Nodes run in declared order and each node is a barrier: the generation elements of a node are
 * generated against the same compiled graph - so they cannot consume each other's output - and the
 * graph is recompiled once the whole node is done, which is what makes the next node able to
 * reference what this one produced. A node holding several elements therefore recompiles once
 * instead of once per element.
 * <p>
 * The run ends by compiling, since nothing else proves that what was generated compiles.
 * <p>
 * Given a {@link ForkJoinPool}, the elements of a node are generated in it; without one they are
 * generated one after another. Either way the order they are merged in is not a guarantee.
 */
public class ModelGenerationRunner
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ModelGenerationRunner.class);

    private final GenerationSpecification generationSpecification;
    private final MutableMap<String, PackageableElement> coreModelElementIndex;
    private final MutableList<ModelGenerationExtension> extensions;

    private final PureModelContextData.Builder fullModelBuilder;
    private final PureModelContextData.Builder generatedModelBuilder;
    private final MutableMap<String, String> generatedElementSources = Maps.mutable.empty();
    private PureModel pureModel;
    private boolean recompileNeeded;

    private Function<PureModelContextData, PureModel> compiler = this::compile;
    private String clientVersion = PureClientVersions.production;
    private ForkJoinPool forkJoinPool;

    private ModelGenerationRunner(GenerationSpecification generationSpecification, PureModelContextData coreModel, PureModel pureModel)
    {
        this.generationSpecification = generationSpecification;
        this.coreModelElementIndex = Iterate.groupByUniqueKey(coreModel.getElements(), PackageableElement::getPath);
        this.extensions = Iterate.addAllTo(ServiceLoader.load(ModelGenerationExtension.class), Lists.mutable.empty());
        this.fullModelBuilder = PureModelContextData.newBuilder().withPureModelContextData(coreModel);
        this.generatedModelBuilder = PureModelContextData.newBuilder();
        this.pureModel = pureModel;
    }

    public static ModelGenerationRunner newRunner(GenerationSpecification generationSpecification, PureModelContextData coreModel, PureModel pureModel)
    {
        return new ModelGenerationRunner(generationSpecification, coreModel, pureModel);
    }

    public static ModelGenerationRunner newRunner(GenerationSpecification generationSpecification, PureModelContextData coreModel)
    {
        return new ModelGenerationRunner(generationSpecification, coreModel, null);
    }

    /**
     * Overrides how the graph is compiled, for callers that need their own deployment mode,
     * class loader or error handling.
     */
    public ModelGenerationRunner withCompiler(Function<PureModelContextData, PureModel> compiler)
    {
        this.compiler = compiler;
        return this;
    }

    public ModelGenerationRunner withClientVersion(String clientVersion)
    {
        this.clientVersion = clientVersion;
        return this;
    }

    /**
     * Generates the elements of a node in the given pool, at whatever parallelism it has. Without
     * one, a node's elements are generated one after another.
     * <p>
     * The pool is the caller's: it is never shut down here. It is also handed to the compilations
     * between nodes, since {@link PureModel} indexes concurrently only when it was compiled with
     * one - {@code getType_safe} caches into its type index on the read path - and a caller
     * supplying its own compiler has to do the same. Generators reaching into the Pure graph are
     * not otherwise known to be thread safe, so pass a pool only for generators verified under
     * contention.
     */
    public ModelGenerationRunner withForkJoinPool(ForkJoinPool forkJoinPool)
    {
        this.forkJoinPool = forkJoinPool;
        return this;
    }

    private PureModel compile(PureModelContextData model)
    {
        return new PureModel(model, null, DeploymentMode.PROD,
                PureModelProcessParameter.newBuilder().withForkJoinPool(this.forkJoinPool).build(), null);
    }

    public Result run()
    {
        if ((this.generationSpecification._package == null) || this.generationSpecification._package.isEmpty())
        {
            throw new EngineException("Invalid generation specification, missing path '" + this.generationSpecification.name + "'", this.generationSpecification.sourceInformation, EngineErrorType.COMPILATION);
        }
        LOGGER.info("Running generation specification '{}'", this.generationSpecification.getPath());
        int generationCount = 0;
        for (GenerationTreeNode node : this.generationSpecification.generationNodes)
        {
            List<String> generationElements = GenerationTreeNodeHelper.generationElements(node);
            generationCount += generationElements.size();
            CompileContext context = currentPureModel().getContext();
            List<PureModelContextData> generated = ((generationElements.size() > 1) && (this.forkJoinPool != null))
                    ? generateConcurrently(node, generationElements, context)
                    : ListIterate.collect(generationElements, generationElement -> generate(node, generationElement, context));
            mergeNodeOutput(generationElements, generated);
        }
        // the last node's output would otherwise never be compiled, and compiling is the only check that what was generated is valid
        currentPureModel();
        PureModelContextData generatedModel = this.generatedModelBuilder.build();
        LOGGER.info("Finished running generation specification '{}'. Generated {} elements", this.generationSpecification.getPath(), generatedModel.getElements().size());
        return new Result(this.fullModelBuilder.build(), generatedModel, generationCount);
    }

    /**
     * Merges a model a host generated itself, outside of the specification's nodes, as one step:
     * it may not override a core element and the graph recompiles before the next read.
     */
    public ModelGenerationRunner addGeneratedModel(String generationElement, PureModelContextData generated)
    {
        merge(generationElement, generated, Maps.mutable.empty());
        normalize();
        return this;
    }

    /**
     * The core elements together with everything generated so far.
     */
    public PureModelContextData getFullModel()
    {
        return this.fullModelBuilder.build();
    }

    /**
     * Only the elements generated so far.
     */
    public PureModelContextData getGeneratedModel()
    {
        return this.generatedModelBuilder.build();
    }

    /**
     * The graph holding the core elements and everything generated so far, compiling first if a
     * generation has landed since the last compilation.
     */
    public PureModel getPureModel()
    {
        return currentPureModel();
    }

    private PureModel currentPureModel()
    {
        if ((this.pureModel == null) || this.recompileNeeded)
        {
            LOGGER.info("Compiling graph with generated elements");
            this.pureModel = this.compiler.apply(this.fullModelBuilder.build());
            this.recompileNeeded = false;
            LOGGER.info("Finished compiling graph");
        }
        return this.pureModel;
    }

    private PureModelContextData generate(GenerationTreeNode node, String generationElementPath, CompileContext context)
    {
        LOGGER.info("Start generating generation model element '{}'", generationElementPath);
        try
        {
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement generationElement = LazyIterate
                    .flatCollect(HelperGenerationSpecificationBuilder.getGenerationCompilerExtensions(context), GenerationCompilerExtension::getExtraModelGenerationSpecificationResolvers)
                    .collect(resolver -> resolver.value(generationElementPath, node.sourceInformation, context))
                    .detect(Objects::nonNull);
            if (generationElement == null)
            {
                throw new EngineException("Can't find generation element '" + generationElementPath + "'", node.sourceInformation, EngineErrorType.COMPILATION);
            }
            PureModelContextData generated = LazyIterate.flatCollect(this.extensions, ModelGenerationExtension::getPureModelContextDataGenerators)
                    .collect(generator -> generator.value(generationElement, context, this.clientVersion))
                    .detect(Objects::nonNull);
            if (generated == null)
            {
                throw new EngineException("No model generator found for generation element '" + generationElementPath + "'", node.sourceInformation, EngineErrorType.COMPILATION);
            }
            LOGGER.info("Finished generating model for '{}', {} elements generated", generationElementPath, generated.getElements().size());
            return generated;
        }
        catch (Throwable t)
        {
            LOGGER.info("Error generating element '{}'", generationElementPath, t);
            throw t;
        }
    }

    private List<PureModelContextData> generateConcurrently(GenerationTreeNode node, List<String> generationElements, CompileContext context)
    {
        MutableList<Future<PureModelContextData>> futures = ListIterate.collect(generationElements, generationElement -> this.forkJoinPool.submit(() -> generate(node, generationElement, context)));
        return futures.collect(future -> get(future, node));
    }

    private PureModelContextData get(Future<PureModelContextData> future, GenerationTreeNode node)
    {
        try
        {
            return future.get();
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new EngineException("Interrupted generating generation node '" + node.id + "'", node.sourceInformation, EngineErrorType.COMPILATION);
        }
        catch (ExecutionException e)
        {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException)
            {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error)
            {
                throw (Error) cause;
            }
            throw new EngineException("Error generating generation node '" + node.id + "': " + cause.getMessage(), node.sourceInformation, EngineErrorType.COMPILATION);
        }
    }

    private void mergeNodeOutput(List<String> generationElements, List<PureModelContextData> generated)
    {
        MutableMap<String, String> elementSources = Maps.mutable.empty();
        for (int i = 0; i < generated.size(); i++)
        {
            merge(generationElements.get(i), generated.get(i), elementSources);
        }
        normalize();
    }

    /**
     * Dedups and sorts both accumulators. Both walk and sort everything accumulated so far rather
     * than what was just generated, so this runs once a node is done instead of once per element.
     */
    private void normalize()
    {
        this.fullModelBuilder.distinct().sorted();
        this.generatedModelBuilder.distinct().sorted();
    }

    private void merge(String generationElement, PureModelContextData generated, MutableMap<String, String> nodeElementSources)
    {
        if (generated.getElements().isEmpty())
        {
            return;
        }
        generated.getElements().forEach(element ->
        {
            String path = element.getPath();
            PackageableElement coreElement = this.coreModelElementIndex.get(path);
            if (coreElement != null)
            {
                throw new EngineException("Generated element '" + path + "' of type " + element.getClass().getSimpleName() + " can't override existing element of type " + coreElement.getClass().getSimpleName(), EngineErrorType.COMPILATION);
            }
            // PureModelContextData.Builder.distinct() keeps the first of two elements sharing a path and reports nothing, so a path
            // generated twice has to be caught here. Only a collision between two elements of one node fails: that needs the new
            // group syntax, so no specification that exists today can hit it. The other two cases have always been deduped
            // silently and projects build on that, so they warn, and a follow-up will turn them into failures.
            String sameNodeGenerationElement = nodeElementSources.put(path, generationElement);
            if (sameNodeGenerationElement != null)
            {
                if (!sameNodeGenerationElement.equals(generationElement))
                {
                    throw new EngineException("Generation elements '" + sameNodeGenerationElement + "' and '" + generationElement + "' of the same generation node both generated element '" + path + "'", EngineErrorType.COMPILATION);
                }
                LOGGER.warn("Generation element '{}' generated element '{}' more than once, keeping the first", generationElement, path);
                return;
            }
            String earlierGenerationElement = this.generatedElementSources.put(path, generationElement);
            if (earlierGenerationElement != null)
            {
                LOGGER.warn("Generation element '{}' generated element '{}', which generation element '{}' already generated in an earlier node, keeping the first", generationElement, path, earlierGenerationElement);
            }
        });
        this.fullModelBuilder.withPureModelContextData(generated);
        this.generatedModelBuilder.withPureModelContextData(generated);
        this.recompileNeeded = true;
    }

    public static final class Result
    {
        private final PureModelContextData fullModel;
        private final PureModelContextData generatedModel;
        private final int generationCount;

        private Result(PureModelContextData fullModel, PureModelContextData generatedModel, int generationCount)
        {
            this.fullModel = fullModel;
            this.generatedModel = generatedModel;
            this.generationCount = generationCount;
        }

        /**
         * The core elements together with everything generated.
         */
        public PureModelContextData getFullModel()
        {
            return this.fullModel;
        }

        /**
         * Only the generated elements.
         */
        public PureModelContextData getGeneratedModel()
        {
            return this.generatedModel;
        }

        /**
         * Number of generation elements run, across all nodes.
         */
        public int getGenerationCount()
        {
            return this.generationCount;
        }
    }
}
