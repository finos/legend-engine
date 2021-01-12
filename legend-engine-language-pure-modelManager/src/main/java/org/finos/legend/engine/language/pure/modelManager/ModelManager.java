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
import io.opentracing.util.GlobalTracer;
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
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextText;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.slf4j.Logger;

import javax.security.auth.Subject;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

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
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");
    public final Cache<PureModelContext, PureModel> pureModelCache = CacheBuilder.newBuilder().softValues().expireAfterWrite(24, TimeUnit.HOURS).build();
    private final DeploymentMode deploymentMode;
    private final MutableList<ModelLoader> modelLoaders;

    public ModelManager(DeploymentMode mode, ModelLoader... modelLoaders)
    {
        this.modelLoaders = Lists.mutable.of(modelLoaders);
        this.modelLoaders.forEach((Procedure<ModelLoader>) loader -> loader.setModelManager(this));
        this.deploymentMode = mode;
    }

    // Remove clientVersion
    public PureModel loadModel(PureModelContext context, String clientVersion, Subject subject, String packageOffset)
    {
        if(context instanceof PureModelContextData || context instanceof PureModelContextText)
        {
            return Compiler.compile(this.loadData(context, clientVersion, subject), this.deploymentMode, subject, packageOffset);
        }
        else
        {
            ModelLoader loader = this.modelLoaderForContext(context);
            if (loader.shouldCache(context))
            {
                PureModelContext cacheKey = loader.cacheKey(context, subject);
                try
                {
                    return this.pureModelCache.get(cacheKey, () -> Compiler.compile(this.loadData(cacheKey, clientVersion, subject), this.deploymentMode, subject, packageOffset));
                }
                catch (ExecutionException e)
                {
                    throw new EngineException("Engine was not able to cache", e);
                }
            }
            return Compiler.compile(this.loadData(context, clientVersion, subject), this.deploymentMode, subject, packageOffset);
        }
    }

    // Remove clientVersion
    public Pair<PureModelContextData, PureModel> loadModelAndData(PureModelContext context, String clientVersion, Subject subject, String packageOffset)
    {
            PureModelContextData data = this.loadData(context, clientVersion, subject);
            return Tuples.pair(data, loadModel(data, clientVersion, subject, packageOffset));
    }

    // Remove clientVersion
    public String getLambdaReturnType(Lambda lambda, PureModelContext context, String clientVersion, Subject subject)
    {
        PureModel result = this.loadModel(context, clientVersion, subject, null);
        return Compiler.getLambdaReturnType(lambda, result);
    }

    // Remove clientVersion
    public PureModelContextData loadData(PureModelContext context, String clientVersion, Subject subject)
    {
        try (Scope scope = GlobalTracer.get().buildSpan("Load Model").startActive(true))
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
                return loader.load(subject, context, clientVersion, scope.span());
            }
        }
    }

    private ModelLoader modelLoaderForContext(PureModelContext context)
    {
        MutableList<ModelLoader> loaders = modelLoaders.select(loader -> loader.supports(context));
        Assert.assertTrue(loaders.size() == 1, () -> "Didn't find a model loader for " + context.getClass().getSimpleName());
        return loaders.get(0);
    }

}