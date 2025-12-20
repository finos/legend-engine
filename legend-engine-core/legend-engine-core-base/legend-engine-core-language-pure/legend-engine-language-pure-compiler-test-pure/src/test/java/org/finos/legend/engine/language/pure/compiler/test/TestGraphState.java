//  Copyright 2025 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.


package org.finos.legend.engine.language.pure.compiler.test;

import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.pure.generated.core_compiler_test_test;
import org.junit.Test;

public class TestGraphState
{
    @Test
    public void testAccessFunctionPtr()
    {
        PureModel pureModel = PureModel.getCorePureModel();
        core_compiler_test_test.Root_meta_legend_compiler_test__Any_1_(pureModel.getExecutionSupport());
    }

    @Test
    public void testElementCleared()
    {
            PureModel pureModel = new PureModel(PureGrammarParser.newInstance().parseModel("Class x::NewElement{}"), "", DeploymentMode.TEST);
            core_compiler_test_test.Root_meta_legend_compiler_testElementFound__Any_1_(pureModel.getExecutionSupport());

            PureModel pureModel2 = PureModel.getCorePureModel();
            core_compiler_test_test.Root_meta_legend_compiler_testElementNotHere__Any_1_(pureModel2.getExecutionSupport());
    }

}
