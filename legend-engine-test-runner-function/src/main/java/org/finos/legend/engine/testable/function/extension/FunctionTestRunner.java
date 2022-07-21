// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.testable.function.extension;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.StreamingResult;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.functionTest.FunctionTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.functionTest.FunctionTestParameter;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.functionTest.FunctionTestParameterComplexValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.functionTest.FunctionTestParameterPrimitiveValue;
import org.finos.legend.engine.protocol.pure.v1.model.test.AtomicTestId;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.EqualToJson;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertFail;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertionStatus;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestError;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestFailed;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestPassed;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult;
import org.finos.legend.engine.testable.extension.TestRunner;
import org.finos.legend.engine.testable.function.assertion.FunctionTestAssertionEvaluator;
import org.finos.legend.engine.testable.function.helper.PrimitiveValueSpecificationToObjectVisitor;
import org.finos.legend.pure.generated.Root_meta_legend_function_metamodel_FunctionTest_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_Mapping_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_Runtime_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.generated.Root_meta_pure_test_AtomicTest;
import org.finos.legend.pure.generated.Root_meta_pure_test_TestSuite;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.CompiledSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import static org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder.getElementFullPath;

public class FunctionTestRunner implements TestRunner
{
    private static final Logger LOGGER = LoggerFactory.getLogger(FunctionTestRunner.class);
    private ConcreteFunctionDefinition<?> pureFunction;
    private MutableList<PlanGeneratorExtension> extensions;
    private PlanExecutor planExecutor;
    private String pureVersion;

    public FunctionTestRunner(ConcreteFunctionDefinition<?> pureFunction, String pureVersion)
    {
        this.pureFunction = pureFunction;
        this.planExecutor = PlanExecutor.newPlanExecutorWithAvailableStoreExecutors();
        this.extensions = Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));
        this.pureVersion = pureVersion;
    }

    @Override
    public TestResult executeAtomicTest(Root_meta_pure_test_AtomicTest atomicTest, PureModel pureModel, PureModelContextData data)
    {
        Function function = ListIterate.detect(data.getElementsOfType(Function.class), ele -> ele.getPath().equals(pureFunction._functionName()));
        MutableList<PlanTransformer> planTransformers = extensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers);

        Root_meta_legend_function_metamodel_FunctionTest_Impl pureFunctionTest = (Root_meta_legend_function_metamodel_FunctionTest_Impl) atomicTest;
        String testId = pureFunctionTest._id();
        AtomicTestId atomicTestId = new AtomicTestId();
        atomicTestId.atomicTestId = testId;

        FunctionTest functionTest = ListIterate.detect(function.tests, ft -> ft.id.equals(testId));

         try
         {
             Map<String, Object> parameters = Maps.mutable.empty();
             if (functionTest.parameters != null)
             {
                 for (FunctionTestParameter parameterValue : functionTest.parameters)
                 {
                     if (parameterValue instanceof FunctionTestParameterPrimitiveValue)
                     {
                         parameters.put(parameterValue.name, ((FunctionTestParameterPrimitiveValue)parameterValue).value.accept(new PrimitiveValueSpecificationToObjectVisitor()));
                     }
                     else if (parameterValue instanceof FunctionTestParameterComplexValue)
                     {
                         // TODO: edit this for complex value parameter
                         throw new RuntimeException("Cannot run tests for Complex type parameters");
                     }
                     else
                     {
                         throw new RuntimeException("Unsupported Function Test Parameter type");
                     }
                 }
             }

             // TODO: edit this for complex value assert
             if (functionTest.assertions.get(0) instanceof EqualToJson)
             {
                 throw new RuntimeException("Cannot run tests for Complex type assertion");
             }

             SingleExecutionPlan executionPlan = PlanGenerator.generateExecutionPlan(pureFunction, new Root_meta_pure_mapping_Mapping_Impl(""), new Root_meta_pure_runtime_Runtime_Impl(""), null, pureModel, PlanPlatform.JAVA, "", null, null, CompiledSupport.toPureCollection(Lists.mutable.empty()), planTransformers);


             Result result = this.planExecutor.execute(executionPlan, parameters);

             boolean isResultReusable = (executionPlan).rootExecutionNode.isResultPrimitiveType();
             if (isResultReusable && result instanceof StreamingResult)
             {
                 result = new ConstantResult(((StreamingResult) result).flush(((StreamingResult) result).getSerializer(SerializationFormat.RAW)));
             }

             AssertionStatus status = functionTest.assertions.get(0).accept(new FunctionTestAssertionEvaluator(result));

             TestResult testResult;
             List<AssertionStatus> assertionStatusList = Lists.mutable.empty();

             if (status == null)
             {
                 throw new RuntimeException("Can't evaluate the test assertion: '" + functionTest.assertions.get(0).id + "'");
             }
             assertionStatusList.add(status);
             if (!isResultReusable)
             {
                 result = this.planExecutor.execute(executionPlan, parameters);
             }

             List<AssertFail> failedAsserts = ListIterate.selectInstancesOf(assertionStatusList, AssertFail.class);

             if (failedAsserts.isEmpty())
             {
                 testResult = new TestPassed();
                 testResult.atomicTestId = atomicTestId;
             }
             else
             {
                 TestFailed testFailed = new TestFailed();
                 testFailed.assertStatuses = assertionStatusList;
                 testResult = testFailed;
                 testResult.atomicTestId = atomicTestId;
             }
             testResult.testable = getElementFullPath(pureFunction, pureModel.getExecutionSupport());
             return testResult;
         }

         catch (Exception e)
         {
             TestError testError = new TestError();
             testError.atomicTestId = atomicTestId;
             testError.error = e.toString();
             testError.testable = getElementFullPath(pureFunction, pureModel.getExecutionSupport());

             return testError;
         }
    }

    @Override
    public List<TestResult> executeTestSuite(Root_meta_pure_test_TestSuite testSuite, List<AtomicTestId> atomicTestIds, PureModel pureModel, PureModelContextData data)
    {
        throw new UnsupportedOperationException("Function Test should be executed in context of Atomic Test only");
    }

    public static ConcreteFunctionDefinition<?> findPureFunction(Function function, PureModel pureModel)
    {
        String functionSignature = HelperModelBuilder.getSignature(function);
        String functionFullName = pureModel.buildPackageString(function._package, functionSignature);
        PackageableElement foundElement = pureModel.getPackageableElement(functionFullName);
        if (!(foundElement instanceof ConcreteFunctionDefinition<?>))
        {
            throw new RuntimeException("Could not find function '" + function.getPath() + "' in Pure model");
        }
        return (ConcreteFunctionDefinition<?>) foundElement;
    }
}


