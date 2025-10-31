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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

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

        if (context instanceof PureModelContextPointer)
        {
            return manage((PureModelContextPointer) context, identity, pureModelCache, cacheKey -> Compiler.compile(this.loadData(cacheKey, clientVersion, identity), this.deploymentMode, identity.getName(), null, modelProcessParameter));
        }
        else if (context instanceof PureModelContextCombination)
        {
            Pair<MutableList<PureModelContextData>, MutableList<PureModelContextPointer>> concreteVsPointer = discriminate((PureModelContextCombination) context);
            MutableList<PureModelContextData> concretes = concreteVsPointer.getOne();
            MutableList<PureModelContextPointer> pointers = concreteVsPointer.getTwo();
            PureModelContextData globalContext;
            if (!pointers.isEmpty())
            {
                PureModelContextData initial = manage(pointers.get(0), identity, pureModelContextCache, cacheKey -> loadData(cacheKey, clientVersion, identity));
                PureModelContextData aggregated = pointers.subList(1, pointers.size()).injectInto(initial, (a, b) -> a.combine(manage(b, identity, pureModelContextCache, cacheKey -> loadData(cacheKey, clientVersion, identity))));
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
            return Compiler.compile(globalContext, this.deploymentMode, identity.getName(), null, modelProcessParameter);
        }
        else if (context instanceof PureModelContextConcrete)
        {
            return Compiler.compile(this.loadData(context, clientVersion, identity), this.deploymentMode, identity.getName(), null, modelProcessParameter);
        }
        else
        {
            throw new EngineException(context.getClass().getSimpleName() + " is not supported yet");
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

    // Remove clientVersion
    public PureModelContextData loadData(PureModelContext context, String clientVersion, Identity identity)
    {
        try (Scope scope = tracer.buildSpan("Load Model").startActive(true))
        {
            scope.span().setTag("context", context.getClass().getSimpleName());
            if (context instanceof PureModelContextConcrete)
            {
                return resolve((PureModelContextConcrete) context);
            }
            else
            {
                ModelLoader loader = this.modelLoaderForContext(context);
                return loader.load(identity, context, clientVersion, scope.span());
            }
        }
    }

    public PureModelContextData resolve(PureModelContextConcrete context)
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

    public ModelLoader modelLoaderForContext(PureModelContext context)
    {
        MutableList<ModelLoader> loaders = modelLoaders.select(loader -> loader.supports(context));
        Assert.assertTrue(loaders.size() == 1, () -> "Didn't find a model loader for " + context.getClass().getSimpleName());
        return loaders.get(0);
    }

    private Pair<MutableList<PureModelContextData>, MutableList<PureModelContextPointer>> discriminate(PureModelContextCombination context)
    {
        MutableList<PureModelContextData> concrete = Lists.mutable.empty();
        MutableList<PureModelContextPointer> pointers = Lists.mutable.empty();
        context.contexts.forEach(x ->
        {
            if (x instanceof PureModelContextConcrete)
            {
                concrete.add(resolve((PureModelContextConcrete) x));
            }
            else if (x instanceof PureModelContextPointer)
            {
                pointers.add((PureModelContextPointer) x);
            }
            else if (x instanceof PureModelContextCombination)
            {
                Pair<MutableList<PureModelContextData>, MutableList<PureModelContextPointer>> res = discriminate((PureModelContextCombination) x);
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

    private <T> T manage(PureModelContextPointer context, Identity identity, Cache<PureModelContext, T> cache, Function<PureModelContext, T> resolver)
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

}