// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.ide.api.execution.test;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.primitive.BooleanObjectPair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.finos.legend.pure.m3.execution.test.TestCallBack;
import org.finos.legend.pure.m3.execution.test.TestStatus;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class CallBack implements TestCallBack
{
    private final MutableSet<CoreInstance> tests = Sets.mutable.with();
    private final MutableList<org.finos.legend.engine.ide.api.execution.test.TestResult> containers = Lists.mutable.with();

    @Override
    public void foundTests(Iterable<? extends CoreInstance> tests)
    {
        this.tests.addAllIterable(tests);
    }

    @Override
    public void executedTest(CoreInstance test, String testParameterizationId, String consoleOutput, TestStatus status)
    {
        synchronized (this.containers)
        {
            this.tests.remove(test);
            this.containers.add(new org.finos.legend.engine.ide.api.execution.test.TestResult(test, testParameterizationId, consoleOutput, status));
        }
    }

    public BooleanObjectPair<ListIterable<org.finos.legend.engine.ide.api.execution.test.TestResult>> pullNewResults()
    {
        synchronized (this.containers)
        {
            ListIterable<TestResult> newContainers = Lists.mutable.withAll(this.containers);
            this.containers.clear();
            return PrimitiveTuples.pair(this.tests.isEmpty(), newContainers);
        }
    }

    public void clear()
    {
        synchronized (this.containers)
        {
            this.tests.clear();
            this.containers.clear();
        }
    }
}
