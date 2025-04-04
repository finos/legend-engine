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

package org.finos.legend.engine.server.test.fct;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.engine.plan.execution.stores.relational.test.H2TestServerResource;
import org.finos.legend.engine.test.fct.FCTReport;
import org.finos.legend.engine.test.fct.FCTTestSuitBuilder;
import org.finos.legend.engine.test.shared.framework.PureTestHelperFramework;
import org.finos.legend.pure.code.core.relational.dbSpecific.RelationalFCTReportH2;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.getClassLoaderExecutionSupport;


public class Test_RelationalFCTH2 extends RelationalFCTReportH2
{
    public static Test suite()
    {
        CompiledExecutionSupport executionSupport = getClassLoaderExecutionSupport();
        MutableMap<String, String> exclusions =  FCTReport.explodeExpectedFailures(getExpectedFailures(),executionSupport.getProcessorSupport());
        return PureTestHelperFramework.wrapSuite(
                () -> true,
                () -> FCTTestSuitBuilder.buildFCTTestSuiteWithExecutorFunctionFromList(testCollection(), exclusions, "meta::relational::fct::relationalEvaluator__FCTEvaluator_1_", "meta::relational::fct::relationalAdaptorH2__FCTAdapter_1_",executionSupport),
                () -> false,
                Lists.mutable.with(new H2TestServerResource())
        );

   }

}
