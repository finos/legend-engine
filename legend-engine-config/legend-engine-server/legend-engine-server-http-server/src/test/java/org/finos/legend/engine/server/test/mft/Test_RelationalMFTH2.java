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

package org.finos.legend.engine.server.test.mft;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.engine.plan.execution.stores.relational.test.H2TestServerResource;
import org.finos.legend.engine.test.mft.MFTReport;
import org.finos.legend.engine.test.mft.MFTTestSuitBuilder;
import org.finos.legend.engine.test.shared.framework.PureTestHelperFramework;
import org.finos.legend.pure.code.core.relational.dbSpecific.RelationalMFTReportH2;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.getClassLoaderExecutionSupport;


public class Test_RelationalMFTH2 extends RelationalMFTReportH2
{
    public static Test suite()
    {
        CompiledExecutionSupport executionSupport = getClassLoaderExecutionSupport();
        MutableMap<String, String> exclusions =  MFTReport.explodeExpectedFailures(getExpectedFailures(),executionSupport.getProcessorSupport());
        return PureTestHelperFramework.wrapSuite(
                () -> true,
                () -> MFTTestSuitBuilder.buildMFTTestSuiteWithExecutorFunctionFromList(testCollection(), exclusions, "meta::relational::mft::relationalEvaluator__MFTEvaluator_1_", "meta::relational::mft::relationalAdaptorH2__MFTAdapter_1_",executionSupport),
                () -> false,
                Lists.mutable.with(new H2TestServerResource())
        );

   }

}
