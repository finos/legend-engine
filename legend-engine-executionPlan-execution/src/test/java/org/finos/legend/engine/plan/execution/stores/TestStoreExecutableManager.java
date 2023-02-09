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

package org.finos.legend.engine.plan.execution.stores;

import org.junit.Test;

public class TestStoreExecutableManager
{
    @Test
    public void testInitializeRequired()
    {
        String session = "testSession";
        StoreExecutableManager.INSTANCE.reset();
        StoreExecutableManager.INSTANCE.addExecutable(session, new TestExecutable());
        assert (StoreExecutableManager.INSTANCE.getExecutables(session).size() == 0);

        StoreExecutableManager.INSTANCE.registerManager();
        TestExecutable test1 = new TestExecutable();
        TestExecutable test2 = new TestExecutable();
        assert (StoreExecutableManager.INSTANCE.getExecutables(session).size() == 0);
        StoreExecutableManager.INSTANCE.addExecutable(session, test1);
        assert (StoreExecutableManager.INSTANCE.getExecutables(session).size() == 1);
        StoreExecutableManager.INSTANCE.addExecutable(session, test2);
        assert (StoreExecutableManager.INSTANCE.getExecutables(session).size() == 2);
        StoreExecutableManager.INSTANCE.removeExecutable(session, test1);
        assert (StoreExecutableManager.INSTANCE.getExecutables(session).size() == 1);
        StoreExecutableManager.INSTANCE.removeExecutable(session, test2);
        assert (StoreExecutableManager.INSTANCE.getExecutables(session).size() == 0);
        StoreExecutableManager.INSTANCE.reset(); //clean up state of singleton

    }

    private class TestExecutable implements StoreExecutable
    {

        @Override
        public void cancel()
        {

        }
    }

}
