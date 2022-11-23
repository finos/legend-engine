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
import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextCollection;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextText;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.pac4j.core.profile.CommonProfile;

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
    public final Cache<PureModelContext, Pair<PureModelContextData, PureModel>> pureModelCache = CacheBuilder.newBuilder().recordStats().softValues().expireAfterAccess(30, TimeUnit.MINUTES).build();
    public final Cache<PureModelContext, PureModelContextData> pointerToPmcdCache = CacheBuilder.newBuilder().recordStats().softValues().expireAfterAccess(30, TimeUnit.MINUTES).build();
    private final DeploymentMode deploymentMode;
    private final MutableList<ModelLoader> modelLoaders;

    public ModelManager(DeploymentMode mode, ModelLoader... modelLoaders)
    {
        this.modelLoaders = Lists.mutable.of(modelLoaders);
        this.modelLoaders.forEach((Procedure<ModelLoader>) loader -> loader.setModelManager(this));
        this.deploymentMode = mode;
    }

    // Remove clientVersion
    public PureModel loadModel(PureModelContext context, String clientVersion, MutableList<CommonProfile> pm, String packageOffset)
    {
        return this.loadModelAndData(context, clientVersion, pm, packageOffset).getTwo();
    }

    // Remove clientVersion
    public Pair<PureModelContextData, PureModel> loadModelAndData(PureModelContext context, String clientVersion, MutableList<CommonProfile> pm, String packageOffset)
    {
        Supplier<Pair<PureModelContextData, PureModel>> resolver = () ->
        {
            PureModelContextData pmcd = this.loadData(context, clientVersion, pm);
            return Tuples.pair(pmcd, Compiler.compile(pmcd, this.deploymentMode, pm, packageOffset));
        };

        if (context instanceof PureModelContextPointer)
        {
            ModelLoader loader = this.modelLoaderForContext(context);
            return this.resolveAndCacheIfAllowed(this.pureModelCache, loader, (PureModelContextPointer) context, pm, resolver);
        }
        else
        {
            return resolver.get();
        }
    }

    // Remove clientVersion
    public String getLambdaReturnType(Lambda lambda, PureModelContext context, String clientVersion, MutableList<CommonProfile> pm)
    {
        PureModel result = this.loadModel(context, clientVersion, pm, null);
        return Compiler.getLambdaReturnType(lambda, result);
    }

    // Remove clientVersion
    public PureModelContextData loadData(PureModelContext context, String clientVersion, MutableList<CommonProfile> pm)
    {
        Span span = GlobalTracer.get().buildSpan("Load Model").start();
        try (Scope ignored =  GlobalTracer.get().activateSpan(span))
        {
            span.setTag("context", context.getClass().getSimpleName());

            PureModelContextVisitor<PureModelContextData> visitor = new PureModelContextVisitor<PureModelContextData>()
            {
                @Override
                public PureModelContextData visit(PureModelContextData data)
                {
                    return data;
                }

                @Override
                public PureModelContextData visit(PureModelContextText text)
                {
                    return PureGrammarParser.newInstance().parseModel(text.code);
                }

                @Override
                public PureModelContextData visit(PureModelContextPointer pointer)
                {
                    ModelLoader loader = modelLoaderForContext(context);
                    Supplier<PureModelContextData> resolver = () -> loader.load(pm, pointer, clientVersion, span);
                    return resolveAndCacheIfAllowed(pointerToPmcdCache, loader, pointer, pm, resolver);
                }

                @Override
                public PureModelContextData visit(PureModelContextCollection collection)
                {
                    PureModelContextData.Builder builder = PureModelContextData.newBuilder();
                    collection.contextCollection.stream().map(x -> x.accept(this)).forEach(builder::addPureModelContextData);
                    return builder.distinct().sorted().build();
                }
            };

            return context.accept(visitor);
        }
        finally
        {
            span.finish();
        }
    }

    private ModelLoader modelLoaderForContext(PureModelContext context)
    {
        MutableList<ModelLoader> loaders = modelLoaders.select(loader -> loader.supports(context));
        Assert.assertTrue(loaders.size() == 1, () -> "Didn't find a model loader for " + context.getClass().getSimpleName());
        return loaders.get(0);
    }

    private <V> V resolveAndCacheIfAllowed(Cache<PureModelContext, V> cache, ModelLoader loader, PureModelContextPointer context, MutableList<CommonProfile> pm, Supplier<V> resolver)
    {
        if (loader.shouldCache(context))
        {
            PureModelContext cacheKey = loader.cacheKey(context, pm);
            try
            {
                return cache.get(cacheKey, resolver::get);
            }
            catch (ExecutionException e)
            {
                throw new EngineException("Engine was not able to cache", e);
            }
        }
        else
        {
            return resolver.get();
        }
    }
}