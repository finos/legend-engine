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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.test.fct.FCTReport;
import org.finos.legend.engine.test.fct.FCTTestCollection;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.getClassLoaderExecutionSupport;

public class RelationalFCTReport extends FCTReport
{
    private final String databaseType;

       public RelationalFCTReport(String databaseType)
    {
        this.databaseType = databaseType;
    }

    public String getDatabaseType()
    {
        return databaseType;
    }


    protected static MutableList<ExclusionSpecification> getExpectedFailures()
    {
        return Lists.mutable.empty();
    }

    protected static ImmutableList<FCTTestCollection> testCollection()
    {  CompiledExecutionSupport support = getClassLoaderExecutionSupport();
        return Lists.immutable.with(
                Test_Pure_Relational_FCT_Collection.buildCollection(support));
    }

    @Override
    public ImmutableList<FCTTestCollection> getTestCollection()
    {
        return testCollection();
    }

    @Override
    public String getReportID()
    {
        return "Execution";
    }

    @Override
    public MutableList<ExclusionSpecification> expectedFailures()
    {
        return getExpectedFailures();
    }

    @Override
    public String getEvaluatorFunction()
    {
        return evaluatorFunction();
    }

    public static String evaluatorFunction()
    {
        return "meta::relational::fct::relationalEvaluator__FCTEvaluator_1_";
    }


}
