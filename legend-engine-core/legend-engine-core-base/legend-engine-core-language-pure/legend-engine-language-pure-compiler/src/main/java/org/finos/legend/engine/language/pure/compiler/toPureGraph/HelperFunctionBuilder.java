// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.data.EmbeddedDataFirstPassBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.data.EmbeddedDataPrerequisiteElementsPassBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.StoreProviderCompilerHelper;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.test.TestBuilderHelper;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.test.assertion.TestAssertionFirstPassBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.test.assertion.TestAssertionPrerequisiteElementsPassBuilder;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.m3.function.Function;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.ParameterValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.function.FunctionTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.function.FunctionTestSuite;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.function.StoreTestData;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_legend_function_metamodel_FunctionTest;
import org.finos.legend.pure.generated.Root_meta_legend_function_metamodel_FunctionTestSuite;
import org.finos.legend.pure.generated.Root_meta_legend_function_metamodel_FunctionTestSuite_Impl;
import org.finos.legend.pure.generated.Root_meta_legend_function_metamodel_FunctionTest_Impl;
import org.finos.legend.pure.generated.Root_meta_legend_function_metamodel_ParameterValue;
import org.finos.legend.pure.generated.Root_meta_legend_function_metamodel_ParameterValue_Impl;
import org.finos.legend.pure.generated.Root_meta_legend_function_metamodel_StoreTestData;
import org.finos.legend.pure.generated.Root_meta_legend_function_metamodel_StoreTestData_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_test_AtomicTest;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;

import java.util.Set;

public class HelperFunctionBuilder
{
    public static void processFunctionSuites(Function func, ConcreteFunctionDefinition<?> metamodelFunction, CompileContext compileContext, ProcessingContext ctx)
    {
        if (func.tests != null && !func.tests.isEmpty())
        {
            TestBuilderHelper.validateTestSuiteIdsList(func.tests, func.sourceInformation);
            metamodelFunction._tests(ListIterate.collect(func.tests, suite -> buildFunctionTestSuites(metamodelFunction, suite, compileContext, ctx)));
        }
    }

    static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.testable.Test buildFunctionTestSuites(ConcreteFunctionDefinition<?> metamodelFunction, org.finos.legend.engine.protocol.pure.v1.model.test.Test test, CompileContext compileContext, ProcessingContext processingContext
    )
    {
        if (test instanceof FunctionTestSuite)
        {
            // validate tests and test suite ids
            FunctionTestSuite testSuite = (FunctionTestSuite) test;
            TestBuilderHelper.validateNonEmptySuite(testSuite);
            TestBuilderHelper.validateTestIds(testSuite.tests, testSuite.sourceInformation);
            Root_meta_legend_function_metamodel_FunctionTestSuite metamodelSuite =  new Root_meta_legend_function_metamodel_FunctionTestSuite_Impl("", SourceInformationHelper.toM3SourceInformation(test.sourceInformation), compileContext.pureModel.getClass("meta::legend::function::metamodel::FunctionTestSuite"));
            if (testSuite.testData != null && !testSuite.testData.isEmpty())
            {
                TestBuilderHelper.validateIds(ListIterate.collect(testSuite.testData, testData -> testData.store.path), testSuite.sourceInformation, "Multiple test data found for stores");
                RichIterable<? extends org.finos.legend.pure.generated.Root_meta_core_runtime_Runtime>  runtimes;
                // TODO: we can remove some of these checks once we support these use cases
                try
                {
                     runtimes = org.finos.legend.pure.generated.core_pure_corefunctions_metaExtension.Root_meta_pure_functions_meta_extractRuntimesFromFunctionDefinition_FunctionDefinition_1__Runtime_MANY_(metamodelFunction, compileContext.pureModel.getExecutionSupport());
                }
                catch (Exception error)
                {
                    throw new EngineException("Unable to extract runtime from function which test data is provided for. Test Data is only supported to be provided for runtimes", testSuite.sourceInformation, EngineErrorType.COMPILATION, error);
                }
                if (runtimes.isEmpty())
                {
                    throw new EngineException("Function test data requires a function to have one runtime: No runtimes found in function." + metamodelFunction.getName(), testSuite.sourceInformation, EngineErrorType.COMPILATION);
                }
                if (runtimes.size() > 1)
                {
                    throw new EngineException("Function test data requires a function to have one runtime. Found " + runtimes.size() + " runtimes in function " + metamodelFunction.getName(), testSuite.sourceInformation, EngineErrorType.COMPILATION);
                }
                org.finos.legend.pure.generated.Root_meta_core_runtime_Runtime runtime = runtimes.getOnly();
                metamodelSuite._testData(ListIterate.collect(testSuite.testData, storeData -> buildFunctionTestData(runtime, storeData, compileContext, processingContext)));
            }
            metamodelSuite
                    ._id(testSuite.id)
                    ._tests(ListIterate.collect(testSuite.tests, unitTest -> (Root_meta_pure_test_AtomicTest) buildFunctionTestSuites(metamodelFunction, unitTest, compileContext, processingContext)))
                    ._testable(metamodelFunction);
            return metamodelSuite;
        }
        else if (test instanceof FunctionTest)
        {
            FunctionTest functionTest = (FunctionTest) test;
            Root_meta_legend_function_metamodel_FunctionTest metamodelTest = new Root_meta_legend_function_metamodel_FunctionTest_Impl("", SourceInformationHelper.toM3SourceInformation(test.sourceInformation), compileContext.pureModel.getClass("meta::legend::function::metamodel::FunctionTest"))
                    ._id(functionTest.id)
                    ._doc(functionTest.doc);
            if (functionTest.parameters != null && !functionTest.parameters.isEmpty())
            {
                metamodelTest._parameters(ListIterate.collect(functionTest.parameters, param -> processFunctionTestParameterValue(param, compileContext)));
            }
            TestBuilderHelper.validateNonEmptyTest(functionTest);
            if (functionTest.assertions.size() > 1)
            {
                throw new EngineException("Function test only support one assertion", test.sourceInformation, EngineErrorType.COMPILATION);
            }
            metamodelTest._assertions(ListIterate.collect(functionTest.assertions, assertion -> assertion.accept(new TestAssertionFirstPassBuilder(compileContext, processingContext))));
            return metamodelTest;
         }
        return null;
    }

    static void collectPrerequisiteElementsFromFunctionTestSuites(Set<PackageableElementPointer> prerequisiteElements, org.finos.legend.engine.protocol.pure.v1.model.test.Test test, CompileContext compileContext)
    {
        if (test instanceof FunctionTestSuite)
        {
            FunctionTestSuite testSuite = (FunctionTestSuite) test;
            if (testSuite.testData != null && !testSuite.testData.isEmpty())
            {
                EmbeddedDataPrerequisiteElementsPassBuilder embeddedDataPrerequisiteElementsPassBuilder = new EmbeddedDataPrerequisiteElementsPassBuilder(compileContext, prerequisiteElements);
                ListIterate.forEach(testSuite.testData, storeData ->
                {
                    prerequisiteElements.add(storeData.store);
                    storeData.data.accept(embeddedDataPrerequisiteElementsPassBuilder);
                });
            }
            ListIterate.forEach(testSuite.tests, unitTest -> collectPrerequisiteElementsFromFunctionTestSuites(prerequisiteElements, unitTest, compileContext));
        }
        else if (test instanceof FunctionTest)
        {
            FunctionTest functionTest = (FunctionTest) test;

            if (functionTest.parameters != null && !functionTest.parameters.isEmpty())
            {
                ValueSpecificationPrerequisiteElementsPassBuilder valueSpecificationPrerequisiteElementsPassBuilder = new ValueSpecificationPrerequisiteElementsPassBuilder(compileContext, prerequisiteElements);
                ListIterate.forEach(functionTest.parameters, param -> param.value.accept(valueSpecificationPrerequisiteElementsPassBuilder));
            }
            ListIterate.forEach(functionTest.assertions, assertion -> assertion.accept(new TestAssertionPrerequisiteElementsPassBuilder(compileContext, prerequisiteElements)));
        }
    }

    private static Root_meta_legend_function_metamodel_StoreTestData buildFunctionTestData(org.finos.legend.pure.generated.Root_meta_core_runtime_Runtime runtime, StoreTestData storeTestData, CompileContext compileContext, ProcessingContext ctx)
    {
        Root_meta_legend_function_metamodel_StoreTestData_Impl metamodelStoreTestData = new Root_meta_legend_function_metamodel_StoreTestData_Impl("", SourceInformationHelper.toM3SourceInformation(storeTestData.sourceInformation), compileContext.pureModel.getClass("meta::legend::function::metamodel::StoreTestData"));
        Store resolvedStore = StoreProviderCompilerHelper.getStoreFromStoreProviderPointers(storeTestData.store, compileContext);
        try
        {
            org.finos.legend.pure.generated.Root_meta_core_runtime_Connection connection = runtime.connectionByElement(resolvedStore, compileContext.pureModel.getExecutionSupport());
            Assert.assertTrue(connection != null, () -> "connection not found");
        }
        catch (Exception exception)
        {
            // throw new EngineException("Store '" + storeTestData.store + "' not specified in the runtime in the function", storeTestData.sourceInformation, EngineErrorType.COMPILATION, exception);
        }
        metamodelStoreTestData._data(storeTestData.data.accept(new EmbeddedDataFirstPassBuilder(compileContext, ctx)));
        metamodelStoreTestData._store(resolvedStore);
        metamodelStoreTestData._doc(storeTestData.doc);
        return metamodelStoreTestData;
    }

    private static Root_meta_legend_function_metamodel_ParameterValue processFunctionTestParameterValue(ParameterValue parameterValue, CompileContext context)
    {
        Root_meta_legend_function_metamodel_ParameterValue pureParameterValue = new Root_meta_legend_function_metamodel_ParameterValue_Impl("", SourceInformationHelper.toM3SourceInformation(parameterValue.sourceInformation), context.pureModel.getClass("meta::legend::function::metamodel::ParameterValue"));
        if (parameterValue.name == null || parameterValue.name.isEmpty())
        {
            throw new EngineException("No associated parameter found for value.", parameterValue.sourceInformation, EngineErrorType.COMPILATION);
        }
        pureParameterValue._name(parameterValue.name);
        pureParameterValue._value(Lists.immutable.with(parameterValue.value.accept(new ValueSpecificationBuilder(context, Lists.mutable.empty(), new ProcessingContext("")))));
        return pureParameterValue;
    }
}
