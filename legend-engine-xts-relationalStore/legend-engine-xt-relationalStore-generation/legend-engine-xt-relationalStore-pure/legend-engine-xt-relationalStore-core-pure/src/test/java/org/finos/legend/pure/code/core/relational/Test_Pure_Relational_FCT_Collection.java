/*
 * //  Copyright 2023 Goldman Sachs
 * //
 * //  Licensed under the Apache License, Version 2.0 (the "License");
 * //  you may not use this file except in compliance with the License.
 * //  You may obtain a copy of the License at
 * //
 * //       http://www.apache.org/licenses/LICENSE-2.0
 * //
 * //  Unless required by applicable law or agreed to in writing, software
 * //  distributed under the License is distributed on an "AS IS" BASIS,
 * //  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * //  See the License for the specific language governing permissions and
 * //  limitations under the License.
 */

package org.finos.legend.pure.code.core.relational;

import org.finos.legend.engine.test.fct.FCTTestCollection;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import static org.finos.legend.engine.test.fct.FCTTestSuitBuilder.buildFCTTestCollection;

public class  Test_Pure_Relational_FCT_Collection
{
    public static FCTTestCollection buildCollection(CompiledExecutionSupport support)
    {
        return buildFCTTestCollection("meta::relational::tests::fct::mapping",  "meta::relational::fct::relationalRunTime_FCTMappingTest_1__Runtime_1_", "meta::relational::fct::relationalTestSetUpH2_Function_1__Any_1_", "meta::relational::fct::relationalRunTimeMock_FCTMappingTest_1__Runtime_1_", support.getProcessorSupport());
    }

}
