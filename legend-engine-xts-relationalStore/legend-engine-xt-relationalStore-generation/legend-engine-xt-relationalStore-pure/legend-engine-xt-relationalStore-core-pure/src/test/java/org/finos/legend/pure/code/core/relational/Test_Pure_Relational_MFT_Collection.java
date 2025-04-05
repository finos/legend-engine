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

import org.finos.legend.engine.test.mft.MFTTestCollection;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import static org.finos.legend.engine.test.mft.MFTTestSuitBuilder.buildMFTTestCollection;

public class Test_Pure_Relational_MFT_Collection
{
    public static MFTTestCollection buildCollection(CompiledExecutionSupport support)
    {
        return buildMFTTestCollection("meta::relational::tests::mft::mapping", "relational", support.getProcessorSupport());
    }

}
