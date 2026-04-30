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

package org.finos.legend.engine.plan.execution.stores.relational.test.h2.semistructured;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;
import java.util.Set;
import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.m3.execution.test.PureTestBuilder;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;
import org.eclipse.collections.api.factory.Maps;

// TODO move to a better module!
public class Test_Relational_H2_Semistructured
{
    private static final Set<String> SKIPPED_TESTS = Sets.mutable.with(
            "meta::relational::tests::semistructured::flattening::testSemiStructuredArrayDirectIsEmpty_Connection_1__Boolean_1_",
            "meta::relational::tests::semistructured::flattening::testSemiStructuredArrayDirectIsNotEmpty_Connection_1__Boolean_1_",
            "meta::relational::tests::semistructured::flattening::testSemiStructuredArrayDirectSize_Connection_1__Boolean_1_",
            "meta::relational::tests::semistructured::flattening::testSemiStructuredArrayDirectAt_Connection_1__Boolean_1_",
            "meta::relational::tests::semistructured::flattening::testSemiStructuredArrayDirectFold_Connection_1__Boolean_1_",
            "meta::relational::tests::semistructured::flattening::testSemiStructuredArrayFilterAtIndex_Connection_1__Boolean_1_",
            "meta::relational::tests::semistructured::flattening::testSemiStructuredArrayFilterFirst_Connection_1__Boolean_1_",
            "meta::relational::tests::semistructured::flattening::testSemiStructuredArrayFilterFirstInIfElse_Connection_1__Boolean_1_",
            "meta::relational::tests::semistructured::flattening::testSemiStructuredArrayFilterFold_Connection_1__Boolean_1_",
            "meta::relational::tests::semistructured::flattening::testSemiStructuredArrayFilterMap_Connection_1__Boolean_1_",
            "meta::relational::tests::semistructured::flattening::testSemiStructuredArrayFilterOnly_Connection_1__Boolean_1_",
            "meta::relational::tests::semistructured::flattening::testSemiStructuredArrayFilterSize_Connection_1__Boolean_1_",
            "meta::relational::tests::semistructured::flattening::testSemiStructuredArrayFilterWithIsEmpty_Connection_1__Boolean_1_",
            "meta::relational::tests::semistructured::flattening::testMultiArrayOlapWithNestedIfExists_Connection_1__Boolean_1_"
    );

    public static Test suite()
    {
        String testPackage = "meta::relational::tests::pct::h2::semistructured";
        CompiledExecutionSupport executionSupport = PureTestBuilderCompiled.getClassLoaderExecutionSupport();

        Set<CoreInstance> skippedCoreInstances = Sets.mutable.empty();
        for (String path : SKIPPED_TESTS)
        {
            CoreInstance ci = executionSupport.getProcessorSupport().package_getByUserPath(path);
            if (ci != null)
            {
                skippedCoreInstances.add(ci);
            }
        }

        PureTestBuilder.F2<CoreInstance, MutableList<Object>, Object> executor = (test, params) ->
        {
            if (skippedCoreInstances.contains(test))
            {
                return true; // skip
            }
            return PureTestBuilderCompiled.executeFn(test, null, Maps.mutable.empty(), executionSupport, params);
        };

        return wrapSuite(
                () -> true,
                () -> PureTestBuilder.buildSuite(
                        TestCollection.collectTests(testPackage, executionSupport.getProcessorSupport(),
                                fn -> PureTestBuilderCompiled.generatePureTestCollection(fn, executionSupport),
                                ci -> PureTestBuilder.satisfiesConditions(ci, executionSupport.getProcessorSupport())
                        ),
                        executor,
                        executionSupport
                ),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.H2).getFirst())
        );
    }
}
