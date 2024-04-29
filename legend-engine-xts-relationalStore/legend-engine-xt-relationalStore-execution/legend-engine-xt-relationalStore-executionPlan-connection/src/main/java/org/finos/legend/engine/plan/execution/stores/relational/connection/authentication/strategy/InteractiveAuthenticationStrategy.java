// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy;

import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.shared.core.operational.Assert;

import javax.security.auth.Subject;
import java.security.AccessController;
import java.util.Properties;

public abstract class InteractiveAuthenticationStrategy extends AuthenticationStrategy
{
    @Override
    public Pair<String, Properties> handleConnection(String url, Properties properties, DatabaseManager databaseManager)
    {
        Assert.assertTrue(Subject.getSubject(AccessController.getContext()) != null, () -> "A 'Secure Connect' or a Kerberos connection can't be built outside of a user call");
        return Tuples.pair(url, properties);
    }
}
