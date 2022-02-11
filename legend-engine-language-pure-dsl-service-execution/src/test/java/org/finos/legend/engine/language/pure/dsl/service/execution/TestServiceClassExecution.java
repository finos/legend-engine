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

package org.finos.legend.engine.language.pure.dsl.service.execution;

import org.finos.legend.engine.authentication.LegendDefaultDatabaseAuthenticationFlowProvider;
import org.finos.legend.engine.authentication.LegendDefaultDatabaseAuthenticationFlowProviderConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.config.RelationalExecutionConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.config.TemporaryTestDbConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestServiceClassExecution {

    @Test
    public void testSimpleRelationalServiceExecution() throws SQLException {

        RelationalExecutionConfiguration relationalExecutionConfiguration = RelationalExecutionConfiguration.newInstance()
                .withTemporaryTestDbConfiguration(new TemporaryTestDbConfiguration(9090))
                .withDatabaseAuthenticationFlowProvider(LegendDefaultDatabaseAuthenticationFlowProvider.class, new LegendDefaultDatabaseAuthenticationFlowProviderConfiguration())
                .build();

        SimpleRelationalService service = new SimpleRelationalService(relationalExecutionConfiguration);
        RelationalResult relationalResult = (RelationalResult) service.execute();
        ResultSet resultSet = relationalResult.getResultSet();
        assertTrue("failed to fetch data", resultSet.next());
        assertEquals(1, resultSet.getInt(1));
        assertEquals("f1", resultSet.getString(2));
        assertEquals("l1", resultSet.getString(3));
    }
}
