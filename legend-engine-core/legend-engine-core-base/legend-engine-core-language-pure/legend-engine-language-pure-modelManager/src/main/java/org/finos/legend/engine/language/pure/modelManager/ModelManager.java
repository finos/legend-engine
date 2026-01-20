// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.language.pure.modelManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.opentracing.Scope;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModelProcessParameter;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextCombination;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextConcrete;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextText;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointerCombination;
import org.finos.legend.engine.protocol.pure.v1.model.context.AlloySDLC;
import org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

public class ModelManager
{
    // ------------------------------------------------------------------------------------------------
    // Since we use ServiceLoader to load various extensions for PURE protocol, it might not be wise
    // to expose mutable instance of object mapper like this. We do this because we potentially modify
    // object mapper here to account for other types of PureModelContextData in legacy code, but we should
    // aim to use extensions even there and hide this mutable singleton here. Also there's the potential
    // issue with `static` and `ServiceLoader` which we need to investigate
    //
    // As of now, make sure we don't use this object mapper in Legend
    // TODO: consider renaming this to UNSAFE/DEPRECATED_objectMapper
    //-------------------------------------------------------------------------------------------------
    public static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    public final Cache<PureModelContext, PureModel> pureModelCache = CacheBuilder.newBuilder().recordStats().softValues().expireAfterAccess(30, TimeUnit.MINUTES).build();
    public final Cache<PureModelContext, PureModelContextData> pureModelContextCache = CacheBuilder.newBuilder().recordStats().softValues().expireAfterAccess(30, TimeUnit.MINUTES).build();
    private final DeploymentMode deploymentMode;
    private final MutableList<ModelLoader> modelLoaders;
    private final Tracer tracer;
    private final ForkJoinPool forkJoinPool;

    public ModelManager(DeploymentMode mode, ModelLoader... modelLoaders)
    {
        this(mode, null, GlobalTracer.get(), modelLoaders);
    }

    public ModelManager(DeploymentMode mode, ForkJoinPool forkJoinPool, ModelLoader... modelLoaders)
    {
        this(mode, forkJoinPool, GlobalTracer.get(), modelLoaders);
    }

    public ModelManager(DeploymentMode mode, ForkJoinPool forkJoinPool, Tracer tracer, ModelLoader... modelLoaders)
    {
        this.tracer = tracer;
        this.modelLoaders = Lists.mutable.of(modelLoaders);
        this.modelLoaders.forEach((Procedure<ModelLoader>) loader -> loader.setModelManager(this));
        this.deploymentMode = mode;
        this.forkJoinPool = forkJoinPool;
    }

    // Remove clientVersion
    public PureModel loadModel(PureModelContext context, String clientVersion, Identity identity, String packageOffset)
    {
        PureModelProcessParameter modelProcessParameter = PureModelProcessParameter.newBuilder().withPackagePrefix(packageOffset).withForkJoinPool(this.forkJoinPool).build();
        return loadModelOrData(context, clientVersion, identity, pureModelCache, p -> Compiler.compile(p, this.deploymentMode, identity.getName(), null, modelProcessParameter));
    }

    // Remove clientVersion
    public PureModelContextData loadData(PureModelContext context, String clientVersion, Identity identity)
    {
        try (Scope scope = tracer.buildSpan("Load Model").startActive(true))
        {
            scope.span().setTag("context", context.getClass().getSimpleName());
            return loadModelOrData(context, clientVersion, identity, pureModelContextCache, p -> p);
        }
    }

    // Remove clientVersion
    public Pair<PureModelContextData, PureModel> loadModelAndData(PureModelContext context, String clientVersion, Identity identity, String packageOffset)
    {
        PureModelContextData data = this.loadData(context, clientVersion, identity);
        return Tuples.pair(data, loadModel(data, clientVersion, identity, packageOffset));
    }

    // Remove clientVersion
    public String getLambdaReturnType(LambdaFunction lambda, PureModelContext context, String clientVersion, Identity identity)
    {
        PureModel result = this.loadModel(context, clientVersion, identity, null);
        return Compiler.getLambdaReturnType(lambda, result);
    }


    private <T> T loadModelOrData(PureModelContext context, String clientVersion, Identity identity, Cache<PureModelContext, T> pointerCache, Function<PureModelContextData, T> mayCompileFunction)
    {
        if (context instanceof PureModelContextCombination)
        {
            Pair<MutableList<PureModelContextData>, MutableList<PureModelContextPointer>> concreteVsPointer = recursivelyDiscriminateDataAndPointersLeaves((PureModelContextCombination) context);
            MutableList<PureModelContextData> concretes = concreteVsPointer.getOne();
            MutableList<PureModelContextPointer> pointers = concreteVsPointer.getTwo();
            PureModelContextData globalContext;
            if (!pointers.isEmpty())
            {
                PureModelContextPointerCombination pureModelContextPointerCombination = new PureModelContextPointerCombination();
                pureModelContextPointerCombination.pointers = pointers;
                PureModelContextData aggregated = (PureModelContextData) loadModelOrData(pureModelContextPointerCombination, clientVersion, identity, pointerCache, mayCompileFunction);
                globalContext = concretes.injectInto(aggregated, (a, b) -> a.combine(b));
            }
            else if (!concretes.isEmpty())
            {
                globalContext = concretes.subList(1, concretes.size()).injectInto(concretes.get(0), (a, b) -> a.combine(b));
            }
            else
            {
                throw new RuntimeException("No content to process");
            }
            return mayCompileFunction.apply(globalContext);
        }
        else if (context instanceof PureModelContextConcrete)
        {
            return mayCompileFunction.apply(transformToData((PureModelContextConcrete) context));
        }
        else if (context instanceof PureModelContextPointerCombination)
        {
            List<PureModelContextPointer> pointers = new ArrayList<>(((PureModelContextPointerCombination) context).pointers);
            List<PureModelContextPointer> alloyPointers = pointers.stream().filter(p -> p.sdlcInfo instanceof AlloySDLC).collect(Collectors.toList());
            if (alloyPointers.size() > 1)
            {
                pointers.removeAll(alloyPointers);
            }
            else
            {
                // PS: if only one alloy present, go through the normal flow
                alloyPointers = new ArrayList<>();
            }
            PureModelContextData globalContext = null;
            if (!pointers.isEmpty())
            {
                globalContext = pointers.stream().map(p -> resolvePointerAndCache(p, identity, pureModelContextCache, cacheKey -> loadModelDataFromStorage(cacheKey, clientVersion, identity))).reduce((a,b) -> a.combine(b)).get();
            }
            if (!alloyPointers.isEmpty())
            {
                PureModelContextPointerCombination pureModelContextPointerCombination = new PureModelContextPointerCombination();
                pureModelContextPointerCombination.pointers = alloyPointers;
                PureModelContextData alloyContext = resolvePointerAndCache(pureModelContextPointerCombination, identity, pureModelContextCache, cacheKey -> loadModelDataFromStorage(cacheKey, clientVersion, identity));
                globalContext = globalContext == null ? alloyContext : globalContext.combine(alloyContext);
            }
            return mayCompileFunction.apply(globalContext);
        }
        else
        {
            return resolvePointerAndCache(context, identity, pointerCache, cacheKey -> mayCompileFunction.apply(this.loadModelDataFromStorage(cacheKey, clientVersion, identity)));
        }
    }


    private PureModelContextData loadModelDataFromStorage(PureModelContext context, String clientVersion, Identity identity)
    {
        try (Scope scope = tracer.buildSpan("Load Model").startActive(true))
        {
            scope.span().setTag("context", context.getClass().getSimpleName());
            return this.modelLoaderForContext(context).load(identity, context, clientVersion, scope.span());
        }
    }

    private PureModelContextData transformToData(PureModelContextConcrete context)
    {
        if (context instanceof PureModelContextData)
        {
            return (PureModelContextData) context;
        }
        else if (context instanceof PureModelContextText)
        {
            return PureGrammarParser.newInstance().parseModel(((PureModelContextText) context).code);
        }
        else
        {
            throw new EngineException(context.getClass().getSimpleName() + " is not supported yet");
        }
    }

    private Pair<MutableList<PureModelContextData>, MutableList<PureModelContextPointer>> recursivelyDiscriminateDataAndPointersLeaves(PureModelContextCombination context)
    {
        MutableList<PureModelContextData> concrete = Lists.mutable.empty();
        MutableList<PureModelContextPointer> pointers = Lists.mutable.empty();
        context.contexts.forEach(x ->
        {
            if (x instanceof PureModelContextConcrete)
            {
                concrete.add(transformToData((PureModelContextConcrete) x));
            }
            else if (x instanceof PureModelContextPointer)
            {
                pointers.add((PureModelContextPointer) x);
            }
            else if (x instanceof PureModelContextCombination)
            {
                Pair<MutableList<PureModelContextData>, MutableList<PureModelContextPointer>> res = recursivelyDiscriminateDataAndPointersLeaves((PureModelContextCombination) x);
                concrete.addAll(res.getOne());
                pointers.addAll(res.getTwo());
            }
            else
            {
                throw new EngineException(x.getClass().getSimpleName() + " is not supported yet");
            }
        });
        return Tuples.pair(concrete, pointers);
    }

    private <Z> Z resolvePointerAndCache(PureModelContext context, Identity identity, Cache<PureModelContext, Z> cache, Function<PureModelContext, Z> resolver)
    {
        ModelLoader loader = this.modelLoaderForContext(context);
        if (loader.shouldCache(context))
        {
            PureModelContext cacheKey = loader.cacheKey(context, identity);
            try
            {
                return cache.get(cacheKey, () -> resolver.apply(context));
            }
            catch (ExecutionException e)
            {
                throw new EngineException("Engine was not able to cache", e);
            }
        }
        return resolver.apply(context);
    }

    private ModelLoader modelLoaderForContext(PureModelContext context)
    {
        MutableList<ModelLoader> loaders = modelLoaders.select(loader -> loader.supports(context));
        Assert.assertTrue(loaders.size() == 1, () -> "Didn't find a model loader for " + context.getClass().getSimpleName());
        return loaders.get(0);
    }
}