// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.language.pure.dsl.service.execution;

import com.google.common.cache.CacheBuilder;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.finos.legend.engine.authentication.H2TestDatabaseAuthenticationFlowProvider;
import org.finos.legend.engine.authentication.H2TestDatabaseAuthenticationFlowProviderConfiguration;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.plan.execution.cache.ExecutionCache;
import org.finos.legend.engine.plan.execution.cache.ExecutionCacheBuilder;
import org.finos.legend.engine.plan.execution.cache.graphFetch.GraphFetchCacheKey;
import org.finos.legend.engine.plan.execution.cache.graphFetch.GraphFetchCrossAssociationKeys;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.JavaHelper;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.execution.stores.relational.config.RelationalExecutionConfiguration;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactoryProvider;
import org.finos.legend.engine.shared.javaCompiler.EngineJavaCompiler;
import org.finos.legend.engine.shared.javaCompiler.JavaCompileException;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;
import org.junit.internal.matchers.ThrowableMessageMatcher;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.finos.legend.pure.generated.core_relational_relational_extensions_extension.Root_meta_relational_extension_relationalExtensions__Extension_MANY_;

public class TestServiceRunner
{
    private void testOptionalParameter(String fetchFunction, String argName, Object optionalParameter, String expectedResultWithParameter, String expectedResultWithoutParameter)
    {
        SimpleOptionalParameterServiceRunner simpleOptionalParameterServiceRunner = new SimpleOptionalParameterServiceRunner(fetchFunction, argName);
        ServiceRunnerInput serviceRunnerInputWithOptionalParameter = ServiceRunnerInput
                .newInstance()
                .withArgs(Collections.singletonList(optionalParameter))
                .withSerializationFormat(SerializationFormat.PURE);
        String result1 = simpleOptionalParameterServiceRunner.run(serviceRunnerInputWithOptionalParameter);
        Assert.assertEquals("Result when optional parameter has value", expectedResultWithParameter, result1);

        ServiceRunnerInput serviceRunnerInputWithEmptyListParameter = ServiceRunnerInput
                .newInstance()
                .withArgs(Collections.singletonList(Collections.emptyList()))
                .withSerializationFormat(SerializationFormat.PURE);
        String result2 = simpleOptionalParameterServiceRunner.run(serviceRunnerInputWithEmptyListParameter);
        Assert.assertEquals("Result when optional parameter is an empty list", expectedResultWithoutParameter, result2);

        ServiceRunnerInput serviceRunnerInputWithNullParameter = ServiceRunnerInput
                .newInstance()
                .withArgs(Collections.singletonList(null))
                .withSerializationFormat(SerializationFormat.PURE);
        String result3 = simpleOptionalParameterServiceRunner.run(serviceRunnerInputWithNullParameter);
        Assert.assertEquals("Result when optional parameter is null", expectedResultWithoutParameter, result3);
    }

    @Test
    public void testSimpleServiceForOptionalString()
    {
        this.testOptionalParameter("test::fetchOptionalCity_String_$0_1$__Any_MANY_", "optionalCity", "New York", "{\"firstName\":\"Peter\",\"lastName\":\"Smith\",\"city\":\"New York\"}", "{\"firstName\":\"John\",\"lastName\":\"Johnson\",\"city\":null}");
    }

    @Test
    public void testSimpleServiceForOptionalInteger()
    {
        this.testOptionalParameter("test::fetchOptionalAge_Integer_$0_1$__Any_MANY_", "optionalAge", 35, "{\"firstName\":\"John\",\"lastName\":\"Johnson\",\"age\":35}", "{\"firstName\":\"Peter\",\"lastName\":\"Smith\",\"age\":null}");
    }

    @Test
    public void testSimpleServiceForOptionalFloat()
    {
        this.testOptionalParameter("test::fetchOptionalSalary_Float_$0_1$__Any_MANY_", "optionalSalary", 80000.75, "{\"firstName\":\"John\",\"lastName\":\"Johnson\",\"salary\":80000.75}", "{\"firstName\":\"Peter\",\"lastName\":\"Smith\",\"salary\":null}");
    }

    @Test
    public void testSimpleServiceForOptionalDate()
    {
        this.testOptionalParameter("test::fetchOptionalDateOfBirth_Date_$0_1$__Any_MANY_", "optionalDate", "1982-01-20", "{\"firstName\":\"Peter\",\"lastName\":\"Smith\",\"dob\":\"1982-01-20\"}", "{\"firstName\":\"John\",\"lastName\":\"Johnson\",\"dob\":null}");
    }

    @Test
    public void testSimpleServiceForOptionalDateTimeWithNoTimeZone()
    {
        this.testOptionalParameter("test::fetchOptionalEmploymentDateTime_DateTime_$0_1$__Any_MANY_", "optionalDateTime", "2005-03-15T18:47:52", "{\"firstName\":\"John\",\"lastName\":\"Johnson\",\"employmentDateTime\":\"2005-03-15T18:47:52.000000000\"}", "{\"firstName\":\"Bob\",\"lastName\":\"Stevens\",\"employmentDateTime\":null}");
    }

    @Test
    public void testSimpleServiceForOptionalDateTimeWithTimeZone()
    {
        this.testOptionalParameter("test::fetchOptionalEmploymentDateTimeWithTZ_DateTime_$0_1$__Any_MANY_", "optionalDateTimeWithTZ", "2012-05-20T03:10:52.501", "{\"firstName\":\"Peter\",\"lastName\":\"Smith\",\"employmentDateTime\":\"2012-05-20T13:10:52.501000000\"}", "{\"firstName\":\"Bob\",\"lastName\":\"Stevens\",\"employmentDateTime\":null}");
    }

    @Test
    public void testSimpleServiceForOptionalBoolean()
    {
        this.testOptionalParameter("test::fetchOptionalActiveEmployment_Boolean_$0_1$__Any_MANY_", "optionalActiveEmployment", true, "{\"firstName\":\"Bob\",\"lastName\":\"Stevens\"}", "[]");
        this.testOptionalParameter("test::fetchOptionalActiveEmployment_Boolean_$0_1$__Any_MANY_", "optionalActiveEmployment", false, "[{\"firstName\":\"Peter\",\"lastName\":\"Smith\"},{\"firstName\":\"John\",\"lastName\":\"Johnson\"}]", "[]");
    }

    @Test
    public void testSimpleServiceForOptionalString_Many()
    {
        this.testOptionalParameter("test::fetchOptionalCityMany_String_MANY__Any_MANY_", "optionalCity", Arrays.asList("New York","Dallas"), "[{\"firstName\":\"Peter\",\"lastName\":\"Smith\"},{\"firstName\":\"Bob\",\"lastName\":\"Stevens\"}]", "[]");
    }

    @Test
    public void testSimpleServiceForOptionalInteger_Many()
    {
        this.testOptionalParameter("test::fetchOptionalAgeMany_Integer_MANY__Any_MANY_", "optionalAge", Arrays.asList(25,35), "[{\"firstName\":\"John\",\"lastName\":\"Johnson\"},{\"firstName\":\"Bob\",\"lastName\":\"Stevens\"}]", "[]");
    }

    @Test
    public void testSimpleServiceForOptionalFloat_Many()
    {
        this.testOptionalParameter("test::fetchOptionalSalaryMany_Float_MANY__Any_MANY_", "optionalSalary", Arrays.asList(80000.75,75000.75), "[{\"firstName\":\"John\",\"lastName\":\"Johnson\",\"salary\":80000.75},{\"firstName\":\"Bob\",\"lastName\":\"Stevens\",\"salary\":75000.75}]", "[]");
    }

    @Test
    public void testSimpleServiceForOptionalDate_Many()
    {
        this.testOptionalParameter("test::fetchOptionalDobMany_Date_MANY__Any_MANY_", "optionalDate", Arrays.asList("1982-01-20","1997-12-16"), "[{\"firstName\":\"Peter\",\"lastName\":\"Smith\",\"dob\":\"1982-01-20\"},{\"firstName\":\"Bob\",\"lastName\":\"Stevens\",\"dob\":\"1997-12-16\"}]", "[]");
    }

    @Test
    public void testSimpleServiceForOptionalDateTimeWithNoTZ_Many()
    {
        this.testOptionalParameter("test::fetchOptionalDateTimeWithNoTZMany_DateTime_MANY__Any_MANY_", "optionalDateTime", Arrays.asList("2005-03-15T18:47:52", "2012-05-20T13:10:52.501"), "[{\"firstName\":\"Peter\",\"lastName\":\"Smith\",\"employmentDateTime\":\"2012-05-20T13:10:52.501000000\"},{\"firstName\":\"John\",\"lastName\":\"Johnson\",\"employmentDateTime\":\"2005-03-15T18:47:52.000000000\"}]", "[]");
    }

    @Test
    public void testSimpleServiceForOptionalDateTimeWithTZ_Many()
    {
        this.testOptionalParameter("test::fetchOptionalDateTimeWithTZMany_DateTime_MANY__Any_MANY_", "optionalDateTime", Arrays.asList("2005-03-15T08:47:52", "2012-05-20T03:10:52.501"), "[{\"firstName\":\"Peter\",\"lastName\":\"Smith\",\"employmentDateTime\":\"2012-05-20T13:10:52.501000000\"},{\"firstName\":\"John\",\"lastName\":\"Johnson\",\"employmentDateTime\":\"2005-03-15T18:47:52.000000000\"}]", "[]");
    }

    @Test
    public void testSimpleM2MServiceExecution()
    {
        SimpleM2MServiceRunner simpleM2MServiceRunner = new SimpleM2MServiceRunner();
        ServiceRunnerInput serviceRunnerInput = ServiceRunnerInput
                .newInstance()
                .withArgs(Collections.singletonList("{\"fullName\": \"Peter Smith\"}"));

        String result = simpleM2MServiceRunner.run(serviceRunnerInput);
        Assert.assertEquals("{\"builder\":{\"_type\":\"json\"},\"values\":{\"firstName\":\"Peter\",\"lastName\":\"Smith\"}}", result);
    }

    @Test
    public void testNullParameterFailIfRequired()
    {
        SimpleM2MServiceRunner simpleM2MServiceRunner = new SimpleM2MServiceRunner();
        ServiceRunnerInput serviceRunnerInput = ServiceRunnerInput
                .newInstance()
                .withArgs(Collections.singletonList(null));
        IllegalArgumentException exception = Assert.assertThrows(IllegalArgumentException.class, () -> simpleM2MServiceRunner.run(serviceRunnerInput));
        MatcherAssert.assertThat(exception, ThrowableMessageMatcher.hasMessage(CoreMatchers.startsWith("Missing external parameter(s): input:String[1]")));
    }

    @Test
    public void testSimpleM2MServiceExecutionWithOutputStream()
    {
        SimpleM2MServiceRunner simpleM2MServiceRunner = new SimpleM2MServiceRunner();
        ServiceRunnerInput serviceRunnerInput = ServiceRunnerInput
                .newInstance()
                .withArgs(Collections.singletonList("{\"fullName\": \"Peter Smith\"}"));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        simpleM2MServiceRunner.run(serviceRunnerInput, outputStream);
        String result = outputStream.toString();
        Assert.assertEquals("{\"builder\":{\"_type\":\"json\"},\"values\":{\"firstName\":\"Peter\",\"lastName\":\"Smith\"}}", result);
    }

    @Test
    public void testSimpleM2MServiceExecutionWithSerializationFormat()
    {
        SimpleM2MServiceRunner simpleM2MServiceRunner = new SimpleM2MServiceRunner();
        ServiceRunnerInput serviceRunnerInput = ServiceRunnerInput
                .newInstance()
                .withArgs(Collections.singletonList("[{\"fullName\": \"Peter Smith\"},{\"fullName\": \"John Johnson\"}]"))
                .withSerializationFormat(SerializationFormat.PURE);

        String result = simpleM2MServiceRunner.run(serviceRunnerInput);
        Assert.assertEquals("[{\"firstName\":\"Peter\",\"lastName\":\"Smith\"},{\"firstName\":\"John\",\"lastName\":\"Johnson\"}]", result);
    }

    @Test
    public void testMultiParameterM2MServiceExecution()
    {
        MultiParameterM2MServiceRunner multiParameterM2MServiceRunner = new MultiParameterM2MServiceRunner();
        ServiceRunnerInput serviceRunnerInput = ServiceRunnerInput
                .newInstance()
                .withArgs(Arrays.asList("{\"fullName\": \"Peter Smith\"}", "{\"fullName\": \"John Johnson\"}"))
                .withSerializationFormat(SerializationFormat.PURE);

        String result = multiParameterM2MServiceRunner.run(serviceRunnerInput);
        Assert.assertEquals("[{\"firstName\":\"Peter\",\"lastName\":\"Smith\"},{\"firstName\":\"John\",\"lastName\":\"Johnson\"}]", result);
    }


    @Test
    public void testMultiParameterM2MServiceExecutionMerge()
    {
        MultiParameterM2MServiceRunnerWithMerge multiParameterM2MServiceRunner = new MultiParameterM2MServiceRunnerWithMerge();
        ServiceRunnerInput serviceRunnerInput = ServiceRunnerInput
                .newInstance()
                .withArgs(Arrays.asList("{\"firstName\": \"Peter\"}", "{\"lastName\": \"Smith\"}"))
                .withSerializationFormat(SerializationFormat.PURE);

        String result = multiParameterM2MServiceRunner.run(serviceRunnerInput);
        Assert.assertEquals("{\"firstName\":\"Peter\",\"lastName\":\"Smith\"}", result);
    }

    private static class SimpleServiceRunnerWithLetVariablePureExpression extends AbstractServicePlanExecutor
    {
        SimpleServiceRunnerWithLetVariablePureExpression()
        {
            super("test::Service", buildPlanForFetchFunction("/org/finos/legend/engine/pure/dsl/service/execution/test/simpleRelationalService.pure", "test::testMultiExpressionQueryWithMandatoryTemporalDate_StrictDate_$0_1$__String_1_"), true);
        }

        @Override
        public List<ServiceVariable> getServiceVariables()
        {
            return Collections.singletonList(new ServiceVariable("businessDate", Date.class, Multiplicity.PURE_ONE));
        }
    }

    @Test
    public void testSimpleServiceRunnerWithLetVariablePureExpressionWithParam()
    {
        SimpleServiceRunnerWithLetVariablePureExpression serviceRunnerWithLetVariablePureExpression = new SimpleServiceRunnerWithLetVariablePureExpression();
        ServiceRunnerInput serviceRunnerInput = ServiceRunnerInput
                .newInstance()
                .withArgs(Arrays.asList("1982-01-20"))
                .withSerializationFormat(SerializationFormat.PURE);

        String result = serviceRunnerWithLetVariablePureExpression.run(serviceRunnerInput);
        Assert.assertEquals("[{\"firstName\":\"Peter\",\"lastName\":\"Smith\"},{\"firstName\":\"John\",\"lastName\":\"Johnson\"},{\"firstName\":\"Bob\",\"lastName\":\"Stevens\"}]", result);
    }

    @Test
    public void testSimpleServiceRunnerWithLetVariablePureExpressionWithoutParam()
    {
        SimpleServiceRunnerWithLetVariablePureExpression serviceRunnerWithLetVariablePureExpression = new SimpleServiceRunnerWithLetVariablePureExpression();
        ServiceRunnerInput serviceRunnerInput = ServiceRunnerInput
                .newInstance()
                .withArgs(Collections.singletonList(null))
                .withSerializationFormat(SerializationFormat.PURE);

        String result = serviceRunnerWithLetVariablePureExpression.run(serviceRunnerInput);
        Assert.assertEquals("[{\"firstName\":\"Peter\",\"lastName\":\"Smith\"},{\"firstName\":\"John\",\"lastName\":\"Johnson\"},{\"firstName\":\"Bob\",\"lastName\":\"Stevens\"}]", result);
    }

    @Test
    public void testSimpleServiceWithMultiplePureExpressionsHavingPropertyWithPath()
    {
        SimpleServiceWithMultiplePureExpressions simpleServiceWithMultiplePureExpressions = new SimpleServiceWithMultiplePureExpressions();
        ServiceRunnerInput serviceRunnerInput = ServiceRunnerInput
                .newInstance()
                .withSerializationFormat(SerializationFormat.PURE);

        String result = simpleServiceWithMultiplePureExpressions.run(serviceRunnerInput);
        Assert.assertEquals("{\"firstName\":\"Peter\",\"lastName\":\"Smith\"}",result);
    }

    @Test
    public void testSimpleRelationalServiceExecution()
    {
        RelationalExecutionConfiguration relationalExecutionConfiguration = RelationalExecutionConfiguration.newInstance()
                .withDatabaseAuthenticationFlowProvider(H2TestDatabaseAuthenticationFlowProvider.class, new H2TestDatabaseAuthenticationFlowProviderConfiguration())
                .build();

        SimpleRelationalServiceRunner simpleRelationalServiceRunner = (SimpleRelationalServiceRunner) ServiceRunnerBuilder.newInstance()
                .withServiceRunnerClass(SimpleRelationalServiceRunner.class.getCanonicalName())
                .withAllowJavaCompilation(false)
                .withStoreExecutorConfigurations(relationalExecutionConfiguration)
                .build();

        ServiceRunnerInput serviceRunnerInput1 = ServiceRunnerInput
                .newInstance()
                .withArgs(Collections.singletonList(Collections.singletonList("John")))
                .withSerializationFormat(SerializationFormat.PURE);
        String result1 = simpleRelationalServiceRunner.run(serviceRunnerInput1);
        Assert.assertEquals("{\"firstName\":\"John\",\"lastName\":\"Johnson\"}", result1);

        ServiceRunnerInput serviceRunnerInput2 = ServiceRunnerInput
                .newInstance()
                .withArgs(Collections.singletonList(Arrays.asList("John", "Peter")))
                .withSerializationFormat(SerializationFormat.PURE);
        String result2 = simpleRelationalServiceRunner.run(serviceRunnerInput2);
        Assert.assertEquals("[{\"firstName\":\"Peter\",\"lastName\":\"Smith\"},{\"firstName\":\"John\",\"lastName\":\"Johnson\"}]", result2);
    }

    @Test
    public void testSimpleRelationalServiceExecutionWithTDSResult()
    {
        RelationalExecutionConfiguration relationalExecutionConfiguration = RelationalExecutionConfiguration.newInstance()
                .withDatabaseAuthenticationFlowProvider(H2TestDatabaseAuthenticationFlowProvider.class, new H2TestDatabaseAuthenticationFlowProviderConfiguration())
                .build();

        SimpleRelationalServiceRunnerTDS simpleRelationalServiceRunner = (SimpleRelationalServiceRunnerTDS) ServiceRunnerBuilder.newInstance()
                .withServiceRunnerClass(SimpleRelationalServiceRunnerTDS.class.getCanonicalName())
                .withAllowJavaCompilation(false)
                .withStoreExecutorConfigurations(relationalExecutionConfiguration)
                .build();

        ServiceRunnerInput serviceRunnerInput1 = ServiceRunnerInput
                .newInstance()
                .withArgs(Collections.singletonList(Collections.singletonList("John")))
                .withSerializationFormat(SerializationFormat.PURE);
        String result1 = simpleRelationalServiceRunner.run(serviceRunnerInput1);
        Assert.assertEquals("{\"columns\":[{\"name\":\"Age\",\"type\":\"Integer\"},{\"name\":\"First Name\",\"type\":\"String\"},{\"name\":\"Last Name\",\"type\":\"String\"}],\"rows\":[{\"values\":[1,\"f1\",\"l1\"]}]}", result1);
    }

    private static class SimpleM2MServiceRunnerForRecursive extends AbstractServicePlanExecutor
    {
        SimpleM2MServiceRunnerForRecursive()
        {
            super("test::Service", buildPlanForFetchFunction("/org/finos/legend/engine/pure/dsl/service/execution/test/simpleM2MService.pure", "test::recursiveTestingForM2M_Integer_1__String_1_"), true);
        }

        @Override
        public List<ServiceVariable> getServiceVariables()
        {
            return Collections.singletonList(new ServiceVariable("input",Integer.class,Multiplicity.PURE_ONE));
        }
    }

    @Test
    public void SimpleM2MServiceRunnerForRecursiveExecution()
    {
        SimpleM2MServiceRunnerForRecursive simpleM2MServiceRunnerForRecursive = new SimpleM2MServiceRunnerForRecursive();
        ServiceRunnerInput serviceRunnerInput = ServiceRunnerInput
                .newInstance()
                .withArgs(Collections.singletonList(1))
                .withSerializationFormat(SerializationFormat.PURE);

        String result = simpleM2MServiceRunnerForRecursive.run(serviceRunnerInput);
        Assert.assertEquals("{\"wheels\":55}", result);
    }

    private static class EnumParamServiceRunner extends AbstractServicePlanExecutor
    {
        private String argName;

        EnumParamServiceRunner(String fetchFunction, String argName)
        {
            super("test::Service", buildPlanForFetchFunction("/org/finos/legend/engine/pure/dsl/service/execution/test/enumServiceParameter.pure", fetchFunction), true);
            this.argName = argName;
        }

        @Override
        public void run(ServiceRunnerInput serviceRunnerInput, OutputStream outputStream)
        {
            newExecutionBuilder()
                    .withParameter(this.argName, serviceRunnerInput.getArgs().get(0))
                    .withServiceRunnerInput(serviceRunnerInput)
                    .executeToStream(outputStream);
        }
    }

    private void testServiceExecutionWithEnumParam(String fetchFunction, String argName, Object parameter, String expectedResult)
    {
        EnumParamServiceRunner enumParamServiceRunner = new EnumParamServiceRunner(fetchFunction, argName);
        ServiceRunnerInput serviceRunnerInputWithEnumParameter = ServiceRunnerInput
                .newInstance()
                .withArgs(Collections.singletonList(parameter))
                .withSerializationFormat(SerializationFormat.PURE);
        String result = enumParamServiceRunner.run(serviceRunnerInputWithEnumParameter);
        Assert.assertEquals(expectedResult, result);
    }

    private void testServiceExecutionWithEnumParamException(String fetchFunction, String argName, Object parameter, String expectedResult)
    {
        EnumParamServiceRunner enumParamServiceRunner = new EnumParamServiceRunner(fetchFunction, argName);
        ServiceRunnerInput serviceRunnerInputWithEnumParameter = ServiceRunnerInput
                .newInstance()
                .withArgs(Collections.singletonList(parameter))
                .withSerializationFormat(SerializationFormat.PURE);
        Exception e = Assert.assertThrows(RuntimeException.class, () -> enumParamServiceRunner.run(serviceRunnerInputWithEnumParameter));
        Assert.assertEquals(expectedResult, e.getMessage());
    }

    @Test
    public void testServiceWithEnumParamEqualOpFilter()
    {
        this.testServiceExecutionWithEnumParam("test::EnumParamEqualOpFilter_EmployeeType_1__TabularDataSet_1_", "eType", "FULL_TIME", "{\"columns\":[{\"name\":\"ID\",\"type\":\"Integer\"},{\"name\":\"Name\",\"type\":\"String\"},{\"name\":\"Employee Type\",\"type\":\"test::EmployeeType\"}],\"rows\":[{\"values\":[102,\"Bob\",\"FULL_TIME\"]}]}");
    }

    @Test
    public void testServiceWithEnumParamInOpFilter()
    {
        this.testServiceExecutionWithEnumParam("test::EnumParamInOpFilter_EmployeeType_1__TabularDataSet_1_", "eType", "CONTRACT", "{\"columns\":[{\"name\":\"ID\",\"type\":\"Integer\"},{\"name\":\"Name\",\"type\":\"String\"},{\"name\":\"Employee Type\",\"type\":\"test::EmployeeType\"}],\"rows\":[{\"values\":[101,\"Alice\",\"CONTRACT\"]},{\"values\":[103,\"Curtis\",\"CONTRACT\"]}]}");
    }

    @Test
    public void testServiceWithEnumParamNotEqualOpFilter()
    {
        this.testServiceExecutionWithEnumParam("test::EnumParamNotEqualOpFilter_EmployeeType_1__TabularDataSet_1_", "eType", "CONTRACT", "{\"columns\":[{\"name\":\"ID\",\"type\":\"Integer\"},{\"name\":\"Name\",\"type\":\"String\"},{\"name\":\"Employee Type\",\"type\":\"test::EmployeeType\"}],\"rows\":[{\"values\":[102,\"Bob\",\"FULL_TIME\"]},{\"values\":[104,\"Bob\",null]}]}");
    }

    @Test
    public void testServiceWithEnumParamNotInOpFilter()
    {
        this.testServiceExecutionWithEnumParam("test::EnumParamNotInOpFilter_EmployeeType_1__TabularDataSet_1_", "eType", "FULL_TIME", "{\"columns\":[{\"name\":\"ID\",\"type\":\"Integer\"},{\"name\":\"Name\",\"type\":\"String\"},{\"name\":\"Employee Type\",\"type\":\"test::EmployeeType\"}],\"rows\":[{\"values\":[101,\"Alice\",\"CONTRACT\"]},{\"values\":[103,\"Curtis\",\"CONTRACT\"]},{\"values\":[104,\"Bob\",null]}]}");
    }

    @Test
    public void testServiceWithInvalidEnumParam()
    {
        this.testServiceExecutionWithEnumParamException("test::EnumParamEqualOpFilter_EmployeeType_1__TabularDataSet_1_", "eType", "CONTRCT", "Invalid provided parameter(s): [Invalid enum value CONTRCT for test::EmployeeType, valid enum values: [CONTRACT, FULL_TIME]]");
    }

    @Test
    public void testServiceWithEnumParamIfOpFilter()
    {
        this.testServiceExecutionWithEnumParam("test::EnumParamIfOpFilter_YesNo_1__TabularDataSet_1_", "yesOrNo", "NO", "{\"columns\":[{\"name\":\"ID\",\"type\":\"Integer\"},{\"name\":\"Name\",\"type\":\"String\"}],\"rows\":[{\"values\":[101,\"InActive User\"]},{\"values\":[102,\"InActive User\"]},{\"values\":[103,\"InActive User\"]},{\"values\":[104,\"InActive User\"]}]}");
    }

    @Test
    public void testServiceWithEnumParamIfOpFilterWithClassProp()
    {
        this.testServiceExecutionWithEnumParam("test::EnumParamIfOpFilterWithClassProp_YesNo_1__TabularDataSet_1_", "yesOrNo", "YES", "{\"columns\":[{\"name\":\"ID\",\"type\":\"Integer\"},{\"name\":\"Name\",\"type\":\"String\"}],\"rows\":[{\"values\":[101,\"InActive User\"]},{\"values\":[102,\"Bob\"]},{\"values\":[103,\"InActive User\"]},{\"values\":[104,\"Bob\"]}]}");
    }

    @Test
    public void testServiceWithIfOpFilterEnumValueWithClassProp()
    {
        this.testServiceExecutionWithEnumParam("test::IfOpFilterEnumValueWithClassProp_String_1__TabularDataSet_1_", "str", "random", "{\"columns\":[{\"name\":\"ID\",\"type\":\"Integer\"},{\"name\":\"Name\",\"type\":\"String\"}],\"rows\":[{\"values\":[101,\"InActive User\"]},{\"values\":[102,\"Bob\"]},{\"values\":[103,\"InActive User\"]},{\"values\":[104,\"Bob\"]}]}");
    }

    @Test
    public void testServiceWithOptionalEnumParam()
    {
        this.testServiceExecutionWithEnumParam("test::OptionalEnumParam_YesNo_$0_1$__TabularDataSet_1_", "yesOrNo", "NO", "{\"columns\":[{\"name\":\"ID\",\"type\":\"Integer\"},{\"name\":\"Status\",\"type\":\"String\"}],\"rows\":[{\"values\":[101,\"InActive User\"]},{\"values\":[102,\"InActive User\"]},{\"values\":[103,\"InActive User\"]},{\"values\":[104,\"InActive User\"]}]}");
        this.testServiceExecutionWithEnumParam("test::OptionalEnumParam_YesNo_$0_1$__TabularDataSet_1_", "yesOrNo", null, "{\"columns\":[{\"name\":\"ID\",\"type\":\"Integer\"},{\"name\":\"Status\",\"type\":\"String\"}],\"rows\":[{\"values\":[101,\"Active User\"]},{\"values\":[102,\"Active User\"]},{\"values\":[103,\"Active User\"]},{\"values\":[104,\"Active User\"]}]}");
        this.testServiceExecutionWithEnumParam("test::OptionalEnumParamClassProp_YesNo_$0_1$__TabularDataSet_1_", "yesOrNo", "NO", "{\"columns\":[{\"name\":\"ID\",\"type\":\"Integer\"},{\"name\":\"Status\",\"type\":\"String\"}],\"rows\":[{\"values\":[101,\"InActive User\"]},{\"values\":[102,\"Active User\"]},{\"values\":[103,\"InActive User\"]},{\"values\":[104,\"Active User\"]}]}");
        this.testServiceExecutionWithEnumParam("test::OptionalEnumParamClassProp_YesNo_$0_1$__TabularDataSet_1_", "yesOrNo", null, "{\"columns\":[{\"name\":\"ID\",\"type\":\"Integer\"},{\"name\":\"Status\",\"type\":\"String\"}],\"rows\":[{\"values\":[101,\"Active User\"]},{\"values\":[102,\"Active User\"]},{\"values\":[103,\"Active User\"]},{\"values\":[104,\"Active User\"]}]}");
    }

    @Test
    public void testServiceWithCollectionEnumParam()
    {
        Exception e = Assert.assertThrows(RuntimeException.class, () -> buildPlanForFetchFunction("/org/finos/legend/engine/pure/dsl/service/execution/test/enumServiceParameter.pure", "test::CollectionEnumParam_EmployeeType_MANY__TabularDataSet_1_"));
        e.getMessage().contains("Collection of Enums is not supported as service parameter [eType]");
    }

    private static class EnumMultipleParamServiceRunner extends AbstractServicePlanExecutor
    {
        private String argName1;
        private String argName2;

        EnumMultipleParamServiceRunner(String fetchFunction, String argName1, String argName2)
        {
            super("test::Service", buildPlanForFetchFunction("/org/finos/legend/engine/pure/dsl/service/execution/test/enumServiceParameter.pure", fetchFunction), true);
            this.argName1 = argName1;
            this.argName2 = argName2;
        }

        @Override
        public void run(ServiceRunnerInput serviceRunnerInput, OutputStream outputStream)
        {
            List<String> args = (List<String>) serviceRunnerInput.getArgs().get(0);
            newExecutionBuilder()
                    .withParameter(this.argName1, args.get(0))
                    .withParameter(this.argName2, args.get(1))
                    .withServiceRunnerInput(serviceRunnerInput)
                    .executeToStream(outputStream);
        }
    }

    private void testServiceExecutionWithMultipleEnumParam(String fetchFunction, String argName1, String argName2, Object parameter, String expectedResult)
    {
        EnumMultipleParamServiceRunner enumMultipleParamServiceRunner = new EnumMultipleParamServiceRunner(fetchFunction, argName1, argName2);
        ServiceRunnerInput serviceRunnerInputWithEnumParameter = ServiceRunnerInput
                .newInstance()
                .withArgs(Collections.singletonList(parameter))
                .withSerializationFormat(SerializationFormat.PURE);
        String result = enumMultipleParamServiceRunner.run(serviceRunnerInputWithEnumParameter);
        Assert.assertEquals(expectedResult, result);
    }

    @Test
    public void testServiceWithEnumParamStringParamFilters()
    {
        this.testServiceExecutionWithMultipleEnumParam("test::EnumParamStringParamFilters_EmployeeType_1__String_1__TabularDataSet_1_", "eType", "eName", Arrays.asList("CONTRACT", "Alice"), "{\"columns\":[{\"name\":\"ID\",\"type\":\"Integer\"},{\"name\":\"Name\",\"type\":\"String\"},{\"name\":\"Employee Type\",\"type\":\"test::EmployeeType\"}],\"rows\":[{\"values\":[101,\"Alice\",\"CONTRACT\"]}]}");
    }

    @Test
    public void testServiceWithMultipleEnumParamsInOPEqualOpFilter()
    {
        this.testServiceExecutionWithMultipleEnumParam("test::MultipleEnumParamsInOPEqualOpFilter_EmployeeType_1__YesNo_1__TabularDataSet_1_", "eType", "yesOrNo", Arrays.asList("FULL_TIME", "NO"), "{\"columns\":[{\"name\":\"ID\",\"type\":\"Integer\"}],\"rows\":[]}");
    }

    @Test
    public void testServiceWithMultipleEnumParamsNotInOPEqualOpFilter()
    {
        this.testServiceExecutionWithMultipleEnumParam("test::MultipleEnumParamsNotInOPEqualOpFilter_EmployeeType_1__YesNo_1__TabularDataSet_1_", "eType", "yesOrNo", Arrays.asList("FULL_TIME", "YES"), "{\"columns\":[{\"name\":\"ID\",\"type\":\"Integer\"}],\"rows\":[{\"values\":[104]}]}");
    }

    @Test
    public void testServiceWithMultipleEnumParamsNotInOPNotEqualOpFilter()
    {
        this.testServiceExecutionWithMultipleEnumParam("test::MultipleEnumParamsNotInOPNotEqualOpFilter_EmployeeType_1__YesNo_1__TabularDataSet_1_", "eType", "yesOrNo", Arrays.asList("FULL_TIME", "YES"), "{\"columns\":[{\"name\":\"ID\",\"type\":\"Integer\"}],\"rows\":[{\"values\":[101]},{\"values\":[103]}]}");
    }

    @Test
    public void testXStoreServiceExecutionWithNoCrossPropertyAccess() throws JavaCompileException
    {
        XStoreServiceRunnerWithNoCrossPropertyAccess xStoreServiceRunner = new XStoreServiceRunnerWithNoCrossPropertyAccess();
        Assert.assertEquals(Collections.emptyList(), xStoreServiceRunner.getGraphFetchCrossAssociationKeys());
        String result = xStoreServiceRunner.run(ServiceRunnerInput.newInstance().withSerializationFormat(SerializationFormat.PURE));
        Assert.assertEquals("[{\"fullName\":\"P1\"},{\"fullName\":\"P2\"},{\"fullName\":\"P3\"},{\"fullName\":\"P4\"},{\"fullName\":\"P5\"}]", result);
    }

    @Test
    public void testXStoreServiceExecutionWithSingleCrossPropertyAccessCache() throws JavaCompileException
    {
        XStoreServiceRunnerWithSingleCrossPropertyAccess xStoreServiceRunner = new XStoreServiceRunnerWithSingleCrossPropertyAccess();
        Assert.assertEquals(1, xStoreServiceRunner.getGraphFetchCrossAssociationKeys().size());
        Assert.assertEquals("<default, root.firm>", xStoreServiceRunner.getGraphFetchCrossAssociationKeys().get(0).getName());

        ExecutionCache<GraphFetchCacheKey, List<Object>> firmCache = ExecutionCacheBuilder.buildExecutionCacheFromGuavaCache(CacheBuilder.newBuilder().recordStats().build());
        OperationalContext operationalContext = OperationalContext
                .newInstance()
                .withGraphFetchCrossAssociationKeysCacheConfig(Maps.mutable.of(xStoreServiceRunner.getGraphFetchCrossAssociationKeys().get(0), firmCache));

        String expectedRes = "[" +
                "{\"fullName\":\"P1\",\"firm\":{\"name\":\"F1\"}}," +
                "{\"fullName\":\"P2\",\"firm\":{\"name\":\"F2\"}}," +
                "{\"fullName\":\"P3\",\"firm\":null}," +
                "{\"fullName\":\"P4\",\"firm\":null}," +
                "{\"fullName\":\"P5\",\"firm\":{\"name\":\"F1\"}}" +
                "]";

        Assert.assertEquals(expectedRes, xStoreServiceRunner.run(ServiceRunnerInput.newInstance().withOperationalContext(operationalContext).withSerializationFormat(SerializationFormat.PURE)));
        assertCacheStats(firmCache, 3, 5, 2, 3);
        Assert.assertEquals(expectedRes, xStoreServiceRunner.run(ServiceRunnerInput.newInstance().withOperationalContext(operationalContext).withSerializationFormat(SerializationFormat.PURE)));
        assertCacheStats(firmCache, 3, 10, 7, 3);
    }

    @Test
    public void testXStoreServiceExecutionWithToManyCrossPropertyAccessCache() throws JavaCompileException
    {
        XStoreServiceRunnerWithToManyCrossPropertyAccess xStoreServiceRunner = new XStoreServiceRunnerWithToManyCrossPropertyAccess();
        Assert.assertEquals(1, xStoreServiceRunner.getGraphFetchCrossAssociationKeys().size());
        Assert.assertEquals("<default, root.persons>", xStoreServiceRunner.getGraphFetchCrossAssociationKeys().get(0).getName());

        ExecutionCache<GraphFetchCacheKey, List<Object>> personCache = ExecutionCacheBuilder.buildExecutionCacheFromGuavaCache(CacheBuilder.newBuilder().recordStats().build());
        OperationalContext operationalContext = OperationalContext
                .newInstance()
                .withGraphFetchCrossAssociationKeysCacheConfig(Maps.mutable.of(xStoreServiceRunner.getGraphFetchCrossAssociationKeys().get(0), personCache));

        String expectedRes = "[" +
                "{\"name\":\"A1\",\"persons\":[{\"fullName\":\"P1\"},{\"fullName\":\"P5\"}]}," +
                "{\"name\":\"A2\",\"persons\":[{\"fullName\":\"P2\"}]}," +
                "{\"name\":\"A3\",\"persons\":[{\"fullName\":\"P4\"}]}," +
                "{\"name\":\"A4\",\"persons\":[]}," +
                "{\"name\":\"A5\",\"persons\":[]}" +
                "]";

        Assert.assertEquals(expectedRes, xStoreServiceRunner.run(ServiceRunnerInput.newInstance().withOperationalContext(operationalContext).withSerializationFormat(SerializationFormat.PURE)));
        assertCacheStats(personCache, 5, 5, 0, 5);
        Assert.assertEquals(expectedRes, xStoreServiceRunner.run(ServiceRunnerInput.newInstance().withOperationalContext(operationalContext).withSerializationFormat(SerializationFormat.PURE)));
        assertCacheStats(personCache, 5, 10, 5, 5);
    }

    @Test
    public void testXStoreServiceExecutionWithMultiCrossPropertyAccessCaches() throws JavaCompileException
    {
        XStoreServiceRunnerWithMultiCrossPropertyAccess xStoreServiceRunner = new XStoreServiceRunnerWithMultiCrossPropertyAccess();
        Assert.assertEquals(2, xStoreServiceRunner.getGraphFetchCrossAssociationKeys().size());
        Assert.assertEquals(Sets.mutable.of("<default, root.firm>", "<default, root.address>"), xStoreServiceRunner.getGraphFetchCrossAssociationKeys().stream().map(GraphFetchCrossAssociationKeys::getName).collect(Collectors.toSet()));

        ExecutionCache<GraphFetchCacheKey, List<Object>> firmCache = ExecutionCacheBuilder.buildExecutionCacheFromGuavaCache(CacheBuilder.newBuilder().recordStats().build());
        ExecutionCache<GraphFetchCacheKey, List<Object>> addressCache = ExecutionCacheBuilder.buildExecutionCacheFromGuavaCache(CacheBuilder.newBuilder().recordStats().build());
        OperationalContext operationalContext = OperationalContext
                .newInstance()
                .withGraphFetchCrossAssociationKeysCacheConfig(Maps.mutable.of(
                        xStoreServiceRunner.getGraphFetchCrossAssociationKeys().stream().filter(x -> "<default, root.firm>".equals(x.getName())).findFirst().orElse(null), firmCache,
                        xStoreServiceRunner.getGraphFetchCrossAssociationKeys().stream().filter(x -> "<default, root.address>".equals(x.getName())).findFirst().orElse(null), addressCache
                ));

        String expectedRes = "[" +
                "{\"fullName\":\"P1\",\"firm\":{\"name\":\"F1\"},\"address\":{\"name\":\"A1\"}}," +
                "{\"fullName\":\"P2\",\"firm\":{\"name\":\"F2\"},\"address\":{\"name\":\"A2\"}}," +
                "{\"fullName\":\"P3\",\"firm\":null,\"address\":null}," +
                "{\"fullName\":\"P4\",\"firm\":null,\"address\":{\"name\":\"A3\"}}," +
                "{\"fullName\":\"P5\",\"firm\":{\"name\":\"F1\"},\"address\":{\"name\":\"A1\"}}" +
                "]";

        Assert.assertEquals(expectedRes, xStoreServiceRunner.run(ServiceRunnerInput.newInstance().withOperationalContext(operationalContext).withSerializationFormat(SerializationFormat.PURE)));
        assertCacheStats(firmCache, 3, 5, 2, 3);
        assertCacheStats(addressCache, 4, 5, 1, 4);
        Assert.assertEquals(expectedRes, xStoreServiceRunner.run(ServiceRunnerInput.newInstance().withOperationalContext(operationalContext).withSerializationFormat(SerializationFormat.PURE)));
        assertCacheStats(firmCache, 3, 10, 7, 3);
        assertCacheStats(addressCache, 4, 10, 6, 4);
    }

    @Test
    public void testXStoreServiceExecutionWithDeepCrossPropertyAccessSharedCaches() throws JavaCompileException
    {
        XStoreServiceRunnerWithDeepCrossPropertyAccess xStoreServiceRunner = new XStoreServiceRunnerWithDeepCrossPropertyAccess();
        Assert.assertEquals(3, xStoreServiceRunner.getGraphFetchCrossAssociationKeys().size());
        Assert.assertEquals(Sets.mutable.of("<default, root.firm>", "<default, root.address>", "<default, root.firm.address>"), xStoreServiceRunner.getGraphFetchCrossAssociationKeys().stream().map(GraphFetchCrossAssociationKeys::getName).collect(Collectors.toSet()));

        ExecutionCache<GraphFetchCacheKey, List<Object>> firmCache = ExecutionCacheBuilder.buildExecutionCacheFromGuavaCache(CacheBuilder.newBuilder().recordStats().build());
        ExecutionCache<GraphFetchCacheKey, List<Object>> addressCache = ExecutionCacheBuilder.buildExecutionCacheFromGuavaCache(CacheBuilder.newBuilder().recordStats().build());
        OperationalContext operationalContext = OperationalContext
                .newInstance()
                .withGraphFetchCrossAssociationKeysCacheConfig(Maps.mutable.of(
                        xStoreServiceRunner.getGraphFetchCrossAssociationKeys().stream().filter(x -> "<default, root.firm>".equals(x.getName())).findFirst().orElse(null), firmCache,
                        xStoreServiceRunner.getGraphFetchCrossAssociationKeys().stream().filter(x -> "<default, root.address>".equals(x.getName())).findFirst().orElse(null), addressCache,
                        xStoreServiceRunner.getGraphFetchCrossAssociationKeys().stream().filter(x -> "<default, root.firm.address>".equals(x.getName())).findFirst().orElse(null), addressCache
                ));

        String expectedRes = "[" +
                "{\"fullName\":\"P1\",\"firm\":{\"name\":\"F1\",\"address\":{\"name\":\"A4\"}},\"address\":{\"name\":\"A1\"}}," +
                "{\"fullName\":\"P2\",\"firm\":{\"name\":\"F2\",\"address\":{\"name\":\"A3\"}},\"address\":{\"name\":\"A2\"}}," +
                "{\"fullName\":\"P3\",\"firm\":null,\"address\":null}," +
                "{\"fullName\":\"P4\",\"firm\":null,\"address\":{\"name\":\"A3\"}}," +
                "{\"fullName\":\"P5\",\"firm\":{\"name\":\"F1\",\"address\":{\"name\":\"A4\"}},\"address\":{\"name\":\"A1\"}}" +
                "]";

        Assert.assertEquals(expectedRes, xStoreServiceRunner.run(ServiceRunnerInput.newInstance().withOperationalContext(operationalContext).withSerializationFormat(SerializationFormat.PURE)));
        assertCacheStats(firmCache, 3, 5, 2, 3);
        assertCacheStats(addressCache, 5, 7, 2, 5);
        Assert.assertEquals(expectedRes, xStoreServiceRunner.run(ServiceRunnerInput.newInstance().withOperationalContext(operationalContext).withSerializationFormat(SerializationFormat.PURE)));
        assertCacheStats(firmCache, 3, 10, 7, 3);
        assertCacheStats(addressCache, 5, 12, 7, 5);
    }

    @Test
    public void testServiceRunnerGraphFetchBatchMemoryLimit()
    {
        ServiceRunnerInput serviceRunnerInput1 = ServiceRunnerInput
                .newInstance()
                .withArgs(Collections.singletonList("{\"fullName\": \"Peter Smith\"}"))
                .withSerializationFormat(SerializationFormat.PURE);

        ServiceRunnerInput serviceRunnerInput2 = ServiceRunnerInput
                .newInstance()
                .withArgs(Collections.singletonList("[{\"fullName\": \"Peter Smith\"}, {\"fullName\": \"John Hill\"}]"))
                .withSerializationFormat(SerializationFormat.PURE);


        // Service with default batch size (1 in case of M2M)
        SimpleM2MServiceRunner simpleM2MServiceRunner = new SimpleM2MServiceRunner();

        simpleM2MServiceRunner.setGraphFetchBatchMemoryLimit(1);
        Exception e1 = Assert.assertThrows(RuntimeException.class, () -> simpleM2MServiceRunner.run(serviceRunnerInput1));
        Assert.assertEquals("Maximum memory reached when processing the graphFetch. Try reducing batch size of graphFetch fetch operation.", e1.getMessage());

        simpleM2MServiceRunner.setGraphFetchBatchMemoryLimit(200);
        Assert.assertEquals("{\"firstName\":\"Peter\",\"lastName\":\"Smith\"}", simpleM2MServiceRunner.run(serviceRunnerInput1));
        Assert.assertEquals("[{\"firstName\":\"Peter\",\"lastName\":\"Smith\"},{\"firstName\":\"John\",\"lastName\":\"Hill\"}]", simpleM2MServiceRunner.run(serviceRunnerInput2));


        // Service which has graph fetch batch size set
        SimpleM2MServiceRunnerWthGraphFetchBatchSize simpleM2MServiceRunnerWthGraphFetchBatchSize = new SimpleM2MServiceRunnerWthGraphFetchBatchSize();

        simpleM2MServiceRunnerWthGraphFetchBatchSize.setGraphFetchBatchMemoryLimit(1);
        Exception e2 = Assert.assertThrows(RuntimeException.class, () -> simpleM2MServiceRunnerWthGraphFetchBatchSize.run(serviceRunnerInput1));
        Assert.assertEquals("Maximum memory reached when processing the graphFetch. Try reducing batch size of graphFetch fetch operation.", e2.getMessage());

        simpleM2MServiceRunnerWthGraphFetchBatchSize.setGraphFetchBatchMemoryLimit(200);
        Assert.assertEquals("{\"firstName\":\"Peter\",\"lastName\":\"Smith\"}", simpleM2MServiceRunnerWthGraphFetchBatchSize.run(serviceRunnerInput1));

        Exception e3 = Assert.assertThrows(RuntimeException.class, () -> simpleM2MServiceRunnerWthGraphFetchBatchSize.run(serviceRunnerInput2));
        Assert.assertEquals("Maximum memory reached when processing the graphFetch. Try reducing batch size of graphFetch fetch operation.", e3.getMessage());

        simpleM2MServiceRunnerWthGraphFetchBatchSize.setGraphFetchBatchMemoryLimit(400);
        Assert.assertEquals("[{\"firstName\":\"Peter\",\"lastName\":\"Smith\"},{\"firstName\":\"John\",\"lastName\":\"Hill\"}]", simpleM2MServiceRunnerWthGraphFetchBatchSize.run(serviceRunnerInput2));
    }

    @Test
    public void testSimpleRelationalServiceWithUserId()
    {
        SimpleRelationalServiceWithUserRunner simpleRelationalServiceWithUserRunner = new SimpleRelationalServiceWithUserRunner();
        Set<KerberosPrincipal> principals = new HashSet<>();
        principals.add(new KerberosPrincipal("peter@test.com"));

        ServiceRunnerInput serviceRunnerInput = ServiceRunnerInput
                .newInstance()
                .withIdentity(IdentityFactoryProvider.getInstance().makeIdentity(new Subject(false, principals, Sets.fixedSize.empty(), Sets.fixedSize.empty())))
                .withSerializationFormat(SerializationFormat.PURE);
        String result = simpleRelationalServiceWithUserRunner.run(serviceRunnerInput);
        Assert.assertEquals("{\"firstName\":\"Peter\",\"lastName\":\"Smith\"}", result);
    }

    private static class SimpleOptionalParameterServiceRunner extends AbstractServicePlanExecutor
    {
        private String argName;

        SimpleOptionalParameterServiceRunner(String fetchFunction, String argName)
        {
            super("test::Service", buildPlanForFetchFunction("/org/finos/legend/engine/pure/dsl/service/execution/test/simpleRelationalService.pure", fetchFunction), true);
            this.argName = argName;
        }

        @Override
        public void run(ServiceRunnerInput serviceRunnerInput, OutputStream outputStream)
        {
            newExecutionBuilder()
                    .withParameter(this.argName, serviceRunnerInput.getArgs().get(0))
                    .withServiceRunnerInput(serviceRunnerInput)
                    .executeToStream(outputStream);
        }
    }

    private static class SimpleRelationalServiceWithUserRunner extends AbstractServicePlanExecutor
    {
        SimpleRelationalServiceWithUserRunner()
        {
            super("test::Service", buildPlanForFetchFunction("/org/finos/legend/engine/pure/dsl/service/execution/test/simpleRelationalService.pure", "test::fetchWithUserId__String_1_"), true);
        }

        @Override
        public void run(ServiceRunnerInput serviceRunnerInput, OutputStream outputStream)
        {
            newExecutionBuilder()
                    .withServiceRunnerInput(serviceRunnerInput)
                    .executeToStream(outputStream);
        }
    }

    private static class SimpleServiceWithMultiplePureExpressions extends AbstractServicePlanExecutor
    {
        SimpleServiceWithMultiplePureExpressions()
        {
            super("test::Service", buildPlanForFetchFunction("/org/finos/legend/engine/pure/dsl/service/execution/test/simpleRelationalService.pure", "test::testMultiExpressionQueryWithPropertyPath__Any_MANY_"), true);
        }

        @Override
        public void run(ServiceRunnerInput serviceRunnerInput, OutputStream outputStream)
        {
            newExecutionBuilder()
                    .withServiceRunnerInput(serviceRunnerInput)
                    .executeToStream(outputStream);
        }
    }

    private static class SimpleM2MServiceRunner extends AbstractServicePlanExecutor
    {
        SimpleM2MServiceRunner()
        {
            super("test::Service", buildPlanForFetchFunction("/org/finos/legend/engine/pure/dsl/service/execution/test/simpleM2MService.pure", "test::function_String_1__String_1_"), true);
        }

        @Override
        public List<ServiceVariable> getServiceVariables()
        {
            return Collections.singletonList(new ServiceVariable("input", String.class, Multiplicity.PURE_ONE));
        }
    }

    private static class MultiParameterM2MServiceRunner extends AbstractServicePlanExecutor
    {
        MultiParameterM2MServiceRunner()
        {
            super("test::Service", buildPlanForFetchFunction("/org/finos/legend/engine/pure/dsl/service/execution/test/multiParamM2MService.pure", "test::function_String_1__String_1__String_1_"), true);
        }

        @Override
        public List<ServiceVariable> getServiceVariables()
        {
            return Lists.mutable.of(
                    new ServiceVariable("input1", String.class, Multiplicity.PURE_ONE),
                    new ServiceVariable("input2", String.class, Multiplicity.PURE_ONE)
            );
        }
    }

    private static class MultiParameterM2MServiceRunnerWithMerge extends AbstractServicePlanExecutor
    {
        MultiParameterM2MServiceRunnerWithMerge()
        {
            super("test::Service", buildPlanForFetchFunction("/org/finos/legend/engine/pure/dsl/service/execution/test/multiParamM2MServiceMerge.pure", "test::function_String_1__String_1__String_1_"), true);
        }

        @Override
        public List<ServiceVariable> getServiceVariables()
        {
            return Lists.mutable.of(
                    new ServiceVariable("input1", String.class, Multiplicity.PURE_ONE),
                    new ServiceVariable("input2", String.class, Multiplicity.PURE_ONE)
            );
        }
    }

    private static class SimpleM2MServiceRunnerWthGraphFetchBatchSize extends AbstractServicePlanExecutor
    {
        SimpleM2MServiceRunnerWthGraphFetchBatchSize()
        {
            super("test::Service", buildPlanForFetchFunction("/org/finos/legend/engine/pure/dsl/service/execution/test/simpleM2MService.pure", "test::functionWithBatchSize_String_1__String_1_"), true);
        }

        @Override
        public List<ServiceVariable> getServiceVariables()
        {
            return Collections.singletonList(new ServiceVariable("input", String.class, Multiplicity.PURE_ONE));
        }
    }

    private abstract static class AbstractXStoreServiceRunner extends AbstractServicePlanExecutor
    {
        private final EngineJavaCompiler compiler;

        AbstractXStoreServiceRunner(String servicePath, SingleExecutionPlan plan) throws JavaCompileException
        {
            super(servicePath, plan, false);
            compiler = JavaHelper.compilePlan(plan, null);
        }

        @Override
        public List<ServiceVariable> getServiceVariables()
        {
            return Collections.emptyList();
        }

        @Override
        public void run(ServiceRunnerInput serviceRunnerInput, OutputStream outputStream)
        {
            try (JavaHelper.ThreadContextClassLoaderScope ignored = JavaHelper.withCurrentThreadContextClassLoader(compiler.getClassLoader()))
            {
                super.run(serviceRunnerInput, outputStream);
            }
        }
    }

    private static class XStoreServiceRunnerWithNoCrossPropertyAccess extends AbstractXStoreServiceRunner
    {
        private static final SingleExecutionPlan plan = buildPlanForFetchFunction("/org/finos/legend/engine/pure/dsl/service/execution/test/xStorePropertyAccessServices.pure", "test::fetch1__String_1_");

        XStoreServiceRunnerWithNoCrossPropertyAccess() throws JavaCompileException
        {
            super("test::Service", plan);
        }
    }

    private static class XStoreServiceRunnerWithSingleCrossPropertyAccess extends AbstractXStoreServiceRunner
    {
        private static final SingleExecutionPlan plan = buildPlanForFetchFunction("/org/finos/legend/engine/pure/dsl/service/execution/test/xStorePropertyAccessServices.pure", "test::fetch2__String_1_");

        XStoreServiceRunnerWithSingleCrossPropertyAccess() throws JavaCompileException
        {
            super("test::Service", plan);
        }
    }

    private static class XStoreServiceRunnerWithToManyCrossPropertyAccess extends AbstractXStoreServiceRunner
    {
        private static final SingleExecutionPlan plan = buildPlanForFetchFunction("/org/finos/legend/engine/pure/dsl/service/execution/test/xStorePropertyAccessServices.pure", "test::fetch3__String_1_");

        XStoreServiceRunnerWithToManyCrossPropertyAccess() throws JavaCompileException
        {
            super("test::Service", plan);
        }
    }

    private static class XStoreServiceRunnerWithMultiCrossPropertyAccess extends AbstractXStoreServiceRunner
    {
        private static final SingleExecutionPlan plan = buildPlanForFetchFunction("/org/finos/legend/engine/pure/dsl/service/execution/test/xStorePropertyAccessServices.pure", "test::fetch4__String_1_");

        XStoreServiceRunnerWithMultiCrossPropertyAccess() throws JavaCompileException
        {
            super("test::Service", plan);
        }
    }

    private static class XStoreServiceRunnerWithDeepCrossPropertyAccess extends AbstractXStoreServiceRunner
    {
        private static final SingleExecutionPlan plan = buildPlanForFetchFunction("/org/finos/legend/engine/pure/dsl/service/execution/test/xStorePropertyAccessServices.pure", "test::fetch5__String_1_");

        XStoreServiceRunnerWithDeepCrossPropertyAccess() throws JavaCompileException
        {
            super("test::Service", plan);
        }
    }

    public static SingleExecutionPlan buildPlanForFetchFunction(String modelCodeResource, String fetchFunctionName)
    {
        try
        {
            InputStreamReader inputStreamReader = new InputStreamReader(Objects.requireNonNull(TestServiceRunner.class.getResourceAsStream(modelCodeResource)));
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            PureModelContextData contextData = PureGrammarParser.newInstance().parseModel(bufferedReader.lines().collect(Collectors.joining("\n")));
            PureModel pureModel = Compiler.compile(contextData, null, null);

            Function fetchFunction = contextData.getElementsOfType(Function.class).stream().filter(x -> fetchFunctionName.equals(x._package + "::" + x.name)).findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown function"));

            return PlanGenerator.generateExecutionPlan(
                    HelperValueSpecificationBuilder.buildLambda(fetchFunction.body, fetchFunction.parameters, new CompileContext.Builder(pureModel).build()),
                    pureModel.getMapping("test::Map"),
                    pureModel.getRuntime("test::Runtime"),
                    null,
                    pureModel,
                    "vX_X_X",
                    PlanPlatform.JAVA,
                    null,
                    Root_meta_relational_extension_relationalExtensions__Extension_MANY_(pureModel.getExecutionSupport()),
                    LegendPlanTransformers.transformers
            );
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static void assertCacheStats(ExecutionCache<?, ?> cache, int estimatedSize, int requestCount, int hitCount, int missCount)
    {
        Assert.assertEquals(estimatedSize, cache.estimatedSize());
        Assert.assertEquals(requestCount, cache.stats().requestCount());
        Assert.assertEquals(hitCount, cache.stats().hitCount());
        Assert.assertEquals(missCount, cache.stats().missCount());
    }
}
