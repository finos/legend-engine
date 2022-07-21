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

package org.finos.legend.engine.testable.function;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.testable.function.extension.FunctionTestRunner;
import org.finos.legend.pure.generated.Root_meta_legend_function_metamodel_FunctionTest;
import org.finos.legend.pure.generated.Root_meta_legend_function_metamodel_FunctionTest_Impl;

public class TestFunctionAtomicTestHelper
{
    public static TestResult runTest(String grammar, String testId)
    {
        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(grammar);
        PureModel pureModel = Compiler.compile(modelData, DeploymentMode.TEST, null);
        Root_meta_legend_function_metamodel_FunctionTest atomicTest = new Root_meta_legend_function_metamodel_FunctionTest_Impl("")._id(testId);
        Function function = ListIterate.detect(modelData.getElementsOfType(Function.class), ele -> ele.tests != null);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition<?> pureFunction = FunctionTestRunner.findPureFunction(function, pureModel);
        org.finos.legend.engine.testable.function.extension.FunctionTestRunner functionTestRunner = new org.finos.legend.engine.testable.function.extension.FunctionTestRunner(pureFunction, "vX_X_X");

        return functionTestRunner.executeAtomicTest(atomicTest, pureModel, modelData);
    }

}
