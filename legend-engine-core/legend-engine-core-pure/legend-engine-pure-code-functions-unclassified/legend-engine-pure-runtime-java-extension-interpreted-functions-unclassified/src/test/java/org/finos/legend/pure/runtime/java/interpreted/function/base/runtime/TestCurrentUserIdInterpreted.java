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

package org.finos.legend.pure.runtime.java.interpreted.function.base.runtime;

import org.finos.legend.engine.pure.code.core.functions.unclassified.base.runtime.AbstractTestCurrentUserId;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.shared.identity.IdentityManager;
import org.junit.Before;
import org.junit.BeforeClass;

public class TestCurrentUserIdInterpreted extends AbstractTestCurrentUserId
{
    @Before
    public void setupCurrentUser()
    {
        IdentityManager.setAuthenticatedUserId(getTestUserId());
    }

    @BeforeClass
    public static void setUpRuntime()
    {
        setUpRuntime(getFunctionExecution(), getCodeStorage(), getExtra());
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}
