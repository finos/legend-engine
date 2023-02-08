/*
 *  Copyright 2022 Goldman Sachs
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.finos.legend.engine.service.post.validation.runner;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.execution.stores.inMemory.plugin.InMemory;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.Relational;
import org.finos.legend.engine.plan.execution.stores.service.plugin.ServiceStore;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.Variable;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.prometheus.MetricsHandler;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.pac4j.core.profile.CommonProfile;

import javax.security.auth.Subject;
import javax.ws.rs.core.Response;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract class ServicePostValidationRunner
{
    private final MutableList<CommonProfile> profiles;

    protected static final PlanExecutor planExecutor = PlanExecutor.newPlanExecutor(Relational.build(), ServiceStore.build(), InMemory.build());
    protected final PureModel pureModel;
    protected final Root_meta_legend_service_metamodel_Service pureService;
    protected final List<Variable> rawParams;
    protected final RichIterable<? extends Root_meta_pure_extension_Extension> extensions;
    protected final MutableList<PlanTransformer> transformers;
    protected final String pureVersion;
    protected final SerializationFormat format;
    protected LambdaFunction<?> queryFunc;
    protected Mapping mapping;
    protected Root_meta_pure_runtime_Runtime runtime;

    public ServicePostValidationRunner(PureModel pureModel, Root_meta_legend_service_metamodel_Service pureService, List<Variable> rawParams, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, MutableList<PlanTransformer> transformers, String pureVersion, MutableList<CommonProfile> profiles, SerializationFormat format)
    {
        this.pureModel = pureModel;
        this.pureService = pureService;
        this.rawParams = rawParams;
        this.extensions = extensions;
        this.transformers = transformers;
        this.pureVersion = pureVersion;
        this.profiles = profiles;
        this.format = format;
        MetricsHandler.createMetrics(this.getClass());
    }

    public Response runValidationAssertion(String assertionId)
    {
        Pair<RichIterable<?>, LambdaFunction<?>> paramsWithAssertion = findParamsWithAssertion(assertionId);
        instantiateQueryMappingAndRuntime(paramsWithAssertion);

        return executeValidationAssertion(assertionId, paramsWithAssertion);
    }

    private void instantiateQueryMappingAndRuntime(Pair<RichIterable<?>, LambdaFunction<?>> paramsWithAssertion)
    {
        if (pureService._execution() instanceof Root_meta_legend_service_metamodel_PureSingleExecution_Impl)
        {
            Root_meta_legend_service_metamodel_PureSingleExecution singleExecution = (Root_meta_legend_service_metamodel_PureSingleExecution) pureService._execution();
            this.queryFunc = (LambdaFunction<?>) singleExecution._func();
            this.mapping = singleExecution._mapping();
            this.runtime = singleExecution._runtime();
        }
        else if (pureService._execution() instanceof Root_meta_legend_service_metamodel_PureMultiExecution_Impl)
        {
            Root_meta_legend_service_metamodel_PureMultiExecution multiExecution = (Root_meta_legend_service_metamodel_PureMultiExecution) pureService._execution();
            // Find index in service path of execution key param
            int keyIndex = findExecutionKeyIndex(multiExecution);

            // Find value of execution key param
            RichIterable<?> params = paramsWithAssertion.getOne();
            Object rawParam = params.toList().get(keyIndex);
            String executionParamValue = (String) ((InstanceValue) ((LambdaFunction<?>) rawParam)._expressionSequence().getAny())._values().getAny();

            // Find execution that matches the param, then extract and instantiate query/mapping/runtime from execution
            for (Root_meta_legend_service_metamodel_KeyedExecutionParameter keyedParam : multiExecution._executionParameters())
            {
                if (keyedParam._key().equals(executionParamValue))
                {
                    this.queryFunc = (LambdaFunction<?>) multiExecution._func();
                    this.mapping = keyedParam._mapping();
                    this.runtime = keyedParam._runtime();
                }
            }

            // Throw exception if no execution matches the param
            if (this.mapping == null)
            {
                throw new NoSuchElementException("No execution parameter with key '" + executionParamValue + "'");
            }
        }
        else
        {
            throw new UnsupportedOperationException("Execution type unsupported");
        }
    }

    private int findExecutionKeyIndex(Root_meta_legend_service_metamodel_PureMultiExecution multiExecution)
    {
        String servicePattern = this.pureService._pattern();
        Matcher m = Pattern.compile("\\{(\\w*)\\}").matcher(servicePattern);
        List<String> paramGroups = FastList.newList();
        while (m.find())
        {
            paramGroups.add(m.group(1));
        }
        int keyIndex = paramGroups.indexOf(multiExecution._executionKey());
        if (keyIndex == -1)
        {
            throw new NoSuchElementException("No param matching key found in service pattern");
        }

        return keyIndex;
    }

    private Pair<RichIterable<?>, LambdaFunction<?>> findParamsWithAssertion(String assertionId)
    {
        List<String> locatedAssertionIds = FastList.newList();

        for (Root_meta_legend_service_metamodel_PostValidation<?> postValidation : pureService._postValidations())
        {
            for (Root_meta_legend_service_metamodel_PostValidationAssertion<?> assertion : postValidation._assertions())
            {
                locatedAssertionIds.add(assertion._id());

                if (assertion._id().equals(assertionId))
                {
                    return Tuples.pair(postValidation._parameters(), (LambdaFunction<?>) assertion._assertion());
                }
            }
        }

        throw new NoSuchElementException("Assertion " + assertionId + " not found, expected one of " + locatedAssertionIds);
    }

    protected Result executePlan(SingleExecutionPlan plan, Map<String, Result> params) throws PrivilegedActionException
    {
        return Subject.doAs(ProfileManagerHelper.extractSubject(profiles), (PrivilegedExceptionAction<Result>) () -> planExecutor.execute(plan, params, null, profiles));
    }

    protected abstract MutableMap<String, Result> evaluateParameters(RichIterable<?> parameters);

    protected abstract Response executeValidationAssertion(String assertionId, Pair<RichIterable<?>, LambdaFunction<?>> paramsWithAssertion);
}
