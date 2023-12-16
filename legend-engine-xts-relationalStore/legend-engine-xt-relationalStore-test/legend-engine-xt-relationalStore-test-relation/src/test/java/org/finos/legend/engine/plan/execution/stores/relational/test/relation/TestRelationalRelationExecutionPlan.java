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
//

package org.finos.legend.engine.plan.execution.stores.relational.test.relation;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.plan.execution.stores.relational.test.H2TestServerResource;
import org.finos.legend.engine.shared.core.port.DynamicPortGenerator;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.*;

public class TestRelationalRelationExecutionPlan extends TestSuite
{
    public static Test suite()
    {
        System.setProperty("legend.test.h2.port", String.valueOf(DynamicPortGenerator.generatePort()));

        System.out.println(System.getProperty("legend.test.h2.port"));

        return wrapSuite(
                () -> true,
                () ->
                {
                    CompiledExecutionSupport executionSupport = getClassLoaderExecutionSupport(true);
                    TestSuite suite = new TestSuite();
                    suite.addTest(buildSuite(TestCollection.collectTests("meta::relational::relation", executionSupport.getProcessorSupport(), ci -> satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));
                    return suite;
                },
                () -> false,
                Lists.mutable.with(new H2TestServerResource())
        );
    }
}