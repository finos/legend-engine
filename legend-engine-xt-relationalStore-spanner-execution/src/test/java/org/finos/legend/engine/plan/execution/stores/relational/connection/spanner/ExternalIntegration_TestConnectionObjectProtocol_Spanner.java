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

package org.finos.legend.engine.plan.execution.stores.relational.connection.spanner;

import java.sql.Connection;
import javax.security.auth.Subject;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.GCPApplicationDefaultCredentialsAuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.spanner.driver.SpannerManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.spanner.ds.specifications.SpannerDataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.spanner.ds.specifications.keys.SpannerDataSourceSpecificationKey;
import org.junit.Test;

public class ExternalIntegration_TestConnectionObjectProtocol_Spanner extends DbSpecificTests
{

    @Override
    protected Subject getSubject()
    {
        return null;
    }

    @Test
    public void testSpannerPublicConnection_subject() throws Exception
    {
        SpannerDataSourceSpecification ds = new SpannerDataSourceSpecification(
                new SpannerDataSourceSpecificationKey(
                        "spanner-emulator-test",
                        "test-instance",
                        "test-db",
                        null,
                        null),
                new SpannerManager(),
                new GCPApplicationDefaultCredentialsAuthenticationStrategy());
        try (Connection connection = ds.getConnectionUsingSubject(getSubject()))
        {
            testConnection(connection, "select 1");
        }
    }
}
