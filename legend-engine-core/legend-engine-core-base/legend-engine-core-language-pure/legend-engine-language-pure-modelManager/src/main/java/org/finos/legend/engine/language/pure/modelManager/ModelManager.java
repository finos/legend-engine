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

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextText;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
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
    private final DeploymentMode deploymentMode;
    private final MutableList<ModelLoader> modelLoaders;
    private final Tracer tracer;

    public ModelManager(DeploymentMode mode, ModelLoader... modelLoaders)
    {
        this(mode, GlobalTracer.get(), modelLoaders);
    }

    public ModelManager(DeploymentMode mode, Tracer tracer, ModelLoader... modelLoaders)
    {
        this.tracer = tracer;
        this.modelLoaders = Lists.mutable.of(modelLoaders);
        this.modelLoaders.forEach((Procedure<ModelLoader>) loader -> loader.setModelManager(this));
        this.deploymentMode = mode;
    }

    // Remove clientVersion
    public PureModel loadModel(PureModelContext context, String clientVersion, Identity identity, String packageOffset)
    {
        if (!(context instanceof PureModelContextData) && !(context instanceof PureModelContextText))
        {
            ModelLoader loader = this.modelLoaderForContext(context);
            if (loader.shouldCache(context))
            {
                PureModelContext cacheKey = loader.cacheKey(context, identity);
                try
                {
                    return this.pureModelCache.get(cacheKey, () -> Compiler.compile(this.loadData(cacheKey, clientVersion, identity), this.deploymentMode, identity.getName(), packageOffset));
                }
                catch (ExecutionException e)
                {
                    throw new EngineException("Engine was not able to cache", e);
                }
            }
        }
        return Compiler.compile(this.loadData(context, clientVersion, identity), this.deploymentMode, identity.getName(), packageOffset);
    }

    // Remove clientVersion
    public Pair<PureModelContextData, PureModel> loadModelAndData(PureModelContext context, String clientVersion, Identity identity, String packageOffset)
    {
        PureModelContextData data = this.loadData(context, clientVersion, identity);
        return Tuples.pair(data, loadModel(data, clientVersion, identity, packageOffset));
    }

    // Remove clientVersion
    public String getLambdaReturnType(Lambda lambda, PureModelContext context, String clientVersion, Identity identity)
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
                ModelLoader loader = this.modelLoaderForContext(context);
                return loader.load(identity, context, clientVersion, scope.span());
            }
        }
    }

    public PureModelContextData loadData(List<PureModelContext> pureModelContextList, String clientVersion, Identity identity)
    {
        try (Scope scope = tracer.buildSpan("Load Model").startActive(true))
        {
            scope.span().setTag("context", pureModelContextList.get(0).getClass().getSimpleName());
            if (pureModelContextList.stream().allMatch(pureModelContext -> pureModelContext instanceof PureModelContextPointer))
            {
                ModelLoader loader = this.modelLoaderForContext(pureModelContextList.get(0));
                return loader.load(identity, pureModelContextList, clientVersion, scope.span());
            }
            else
            {
                throw new UnsupportedOperationException("Invalid arguments - PureModelContextList should have all elements of type PureModelContextPointer");
            }
        }
    }

    public ModelLoader modelLoaderForContext(PureModelContext context)
    {
        MutableList<ModelLoader> loaders = modelLoaders.select(loader -> loader.supports(context));
        Assert.assertTrue(loaders.size() == 1, () -> "Didn't find a model loader for " + context.getClass().getSimpleName());
        return loaders.get(0);
    }

}