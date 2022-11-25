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
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.StreamingResult;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.Variable;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_Service;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.pac4j.core.profile.CommonProfile;

import javax.ws.rs.core.Response;
import java.security.PrivilegedActionException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

import static org.finos.legend.pure.generated.core_legend_service_validation.Root_meta_legend_service_validation_extractAssertMessage_FunctionDefinition_1__String_1_;
import static org.finos.legend.pure.generated.core_legend_service_validation.Root_meta_legend_service_validation_generateValidationQuery_FunctionDefinition_1__FunctionDefinition_1__FunctionDefinition_1_;

public class LegendServicePostValidationRunner extends ServicePostValidationRunner
{
    public LegendServicePostValidationRunner(PureModel pureModel, Root_meta_legend_service_metamodel_Service pureService, List<Variable> rawParams, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, MutableList<PlanTransformer> transformers, String pureVersion, MutableList<CommonProfile> profiles, SerializationFormat format)
    {
        super(pureModel, pureService, rawParams, extensions, transformers, pureVersion, profiles, format);
    }

    protected MutableMap<String, Result> evaluateParameters(RichIterable<?> parameters)
    {
        List<Result> evaluatedParams = FastList.newList();

        for (Object parameter : parameters)
        {
            if (parameter instanceof LambdaFunction<?>)
            {
                Object innerParam = ((LambdaFunction<?>) parameter)._expressionSequence().getAny();

                if (innerParam instanceof SimpleFunctionExpression)
                {
                    SingleExecutionPlan sep = PlanGenerator.generateExecutionPlan((LambdaFunction<?>) parameter, this.mapping, this.runtime, null, this.pureModel, this.pureVersion, PlanPlatform.JAVA, null, this.extensions, this.transformers);

                    try
                    {
                        Result paramResult = executePlan(sep, new HashMap<>());

                        if (paramResult instanceof RelationalResult)
                        {
                            ResultSet resultSet = ((RelationalResult) paramResult).resultSet;
                            resultSet.next();
                            evaluatedParams.add(new ConstantResult(resultSet.getObject(1)));
                        }
                        else
                        {
                            evaluatedParams.add(paramResult);
                        }
                    }
                    catch (PrivilegedActionException | SQLException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
                else if (innerParam instanceof InstanceValue)
                {
                    evaluatedParams.add(new ConstantResult(((InstanceValue) innerParam)._values().getAny()));
                }
            }
            else
            {
                throw new UnsupportedOperationException("Not supported");
            }
        }

        return ListIterate.zip(this.rawParams, evaluatedParams).toMap(p -> p.getOne().name, Pair::getTwo);
    }

    protected Response executeValidationAssertion(String assertionId, Pair<RichIterable<?>, LambdaFunction<?>> paramsWithAssertion)
    {
        RichIterable<?> params = paramsWithAssertion.getOne();
        LambdaFunction<?> assertion = paramsWithAssertion.getTwo();

        FunctionDefinition<?> assertQuery = Root_meta_legend_service_validation_generateValidationQuery_FunctionDefinition_1__FunctionDefinition_1__FunctionDefinition_1_(this.queryFunc, assertion, this.pureModel.getExecutionSupport());
        String assertMessage = Root_meta_legend_service_validation_extractAssertMessage_FunctionDefinition_1__String_1_(assertion, this.pureModel.getExecutionSupport());

        MutableMap<String, Result> evaluatedParams = evaluateParameters(params);

        SingleExecutionPlan sep = PlanGenerator.generateExecutionPlan((LambdaFunction<?>) assertQuery, this.mapping, this.runtime,  null, this.pureModel, this.pureVersion, PlanPlatform.JAVA, null, this.extensions, this.transformers);

        try
        {
            Result queryResult = executePlan(sep, evaluatedParams);

            if (queryResult instanceof StreamingResult)
            {
                return Response.ok(new PostValidationAssertionStreamingOutput(assertionId, assertMessage, (StreamingResult) queryResult, this.format)).build();
            }
            else
            {
                return Response.serverError().build();
            }
        }
        catch (PrivilegedActionException e)
        {
            throw new RuntimeException(e);
        }
    }
}
