// Copyright 2025 Goldman Sachs
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


package org.finos.legend.engine.query.sql.reversePCT;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.generated.Root_meta_external_query_sql_reversePCT_framework_ReversesForSource;
import org.finos.legend.pure.generated.Root_meta_external_query_sql_reversePCT_framework_ReversesForTest;
import org.finos.legend.pure.generated.platform_pure_essential_meta_graph_elementToPath;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.execution.test.PureTestBuilder;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.AdapterQualifier;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionOneTest;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;

import java.util.Enumeration;
import java.util.Objects;

public class ExpectedFailuresBuilder
{
    public static MutableList<ExclusionSpecification> build(RichIterable<? extends Root_meta_external_query_sql_reversePCT_framework_ReversesForSource> reverses)
    {
        if (reverses == null)
        {
            return Lists.mutable.empty();
        }
        return reverses.toList().flatCollect(x ->
                x._reverses().collect(z ->
                        {
                            if (z._reverses().getFirst()._expectedError() != null || !z._reverses().getLast()._shouldBeSupported())
                            {
                                return (ExclusionSpecification) new ExclusionOneTest(
                                        z._testFunction(),
                                        !z._reverses().getFirst()._shouldBeSupported() ? "\"Should not be supported\"" : z._reverses().getFirst()._expectedError(),
                                        z._reverses().getFirst()._shouldBeSupported() ? AdapterQualifier.needsInvestigation : AdapterQualifier.unsupportedFeature
                                );
                            }
                            return null;
                        }
                ).select(Objects::nonNull));
    }

    public static MutableList<ExclusionSpecification> getMissingExpectedFailures(TestSuite ts, RichIterable<? extends Root_meta_external_query_sql_reversePCT_framework_ReversesForSource> reverseInfo)
    {
        MutableList<String> allTests = Lists.mutable.empty();
        collectTests(ts.tests(), allTests);
        RichIterable<String> managedTests =  collectManagedTests(reverseInfo);
        allTests.removeAll(managedTests.toList());
        return allTests.collect(x -> new ExclusionOneTest(x, "", AdapterQualifier.needsImplementation));
    }

    public static RichIterable<String> collectManagedTests(RichIterable<? extends Root_meta_external_query_sql_reversePCT_framework_ReversesForSource> reverses)
    {
        if (reverses == null)
        {
            return Lists.mutable.empty();
        }
        return reverses.flatCollect(x ->
                x._reverses().collect(Root_meta_external_query_sql_reversePCT_framework_ReversesForTest::_testFunction
                ).select(Objects::nonNull));
    }

    public static void collectTests(Enumeration<Test> w, MutableList<String> tests)
    {
        while (w.hasMoreElements())
        {
            Test test = w.nextElement();
            if (test instanceof TestSuite)
            {
                collectTests(((TestSuite) test).tests(), tests);
            }
            else if (test instanceof PureTestBuilder.PureTestCase)
            {
                tests.add(platform_pure_essential_meta_graph_elementToPath.Root_meta_pure_functions_meta_elementToPath_PackageableElement_1__String_1_((PackageableElement) ((PureTestBuilder.PureTestCase) test).getCoreInstance(), ((PureTestBuilder.PureTestCase) test).getExecutionSupport()));
            }
            else
            {
                throw new RuntimeException(test.getClass().getName());
            }
        }
    }
}
