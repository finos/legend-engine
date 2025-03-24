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

package org.finos.legend.engine.plan.execution.stores.deephaven.test.shared;

import org.eclipse.collections.api.factory.Stacks;
import org.finos.legend.engine.plan.execution.stores.deephaven.test.TestConnectionIntegration;
import org.finos.legend.engine.plan.execution.stores.deephaven.test.TestDeephavenConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.deephaven.metamodel.runtime.DeephavenConnection;
import org.finos.legend.pure.m3.exception.PureExecutionException;

import static org.junit.Assert.fail;

public class GetDeephavenTestConnectionShared
{
    public static DeephavenConnection getDatabaseConnection()
    {
        TestConnectionIntegration found = TestDeephavenConnectionIntegrationLoader.extensions().getFirst();
        if (found == null)
        {
            throw new PureExecutionException("Can't find a TestConnectionIntegration for dbType Deephaven.", Stacks.mutable.empty());
        }
        try
        {
            return found.getConnection();
        }
        catch (Exception e)
        {
            fail("Unable to connect to Deephaven test container!");
        }
        return null;
    }

}