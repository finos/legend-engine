// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.testable.persistence.extension;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.authentication.credential.CredentialSupplier;
import org.finos.legend.engine.authentication.demoflows.H2LocalWithDefaultUserPasswordFlow;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcHelper;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.h2.H2Manager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.LocalH2DataSourceSpecification;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactoryProvider;

import javax.security.auth.Subject;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public class PersistenceTestH2Connection
{
    private Connection connection;

    Connection getConnection()
    {
        // Close connection to close any left over open connections
        closeConnection();
        Properties properties = new Properties();
        properties.put("DATABASE_TO_UPPER", false);
        LocalH2DataSourceSpecification specification = new LocalH2DataSourceSpecification(
            Lists.mutable.empty(),
            new H2Manager(),
            new TestDatabaseAuthenticationStrategy(),
            properties);
        Identity identity = IdentityFactoryProvider.getInstance().makeIdentity((Subject) null);
        this.connection = specification.getConnectionUsingIdentity(identity, plainTextCredentialSupplier());
        return this.connection;
    }

    void closeConnection()
    {
        if (this.connection != null)
        {
            JdbcHelper.of(this.connection).executeStatement("DROP ALL OBJECTS");
            JdbcHelper.of(this.connection).close();
        }
    }

    private Optional<CredentialSupplier> plainTextCredentialSupplier()
    {
        CredentialSupplier credentialSupplier = new CredentialSupplier(new H2LocalWithDefaultUserPasswordFlow(), null, null);
        return Optional.of(credentialSupplier);
    }

    List<Map<String, Object>> readTable(DatasetDefinition datasetDefinition)
    {
        List<Map<String, Object>> result = JdbcHelper.of(this.connection).executeQuery(String.format("select * from " + datasetDefinition.group().get() + "." + datasetDefinition.name()));
        enrichTimestampFields(result);
        return result;
    }

    private void enrichTimestampFields(List<Map<String, Object>> result)
    {
        for (Map<String, Object> row : result)
        {
            for (Map.Entry<String, Object> entry : row.entrySet())
            {
                if (entry.getValue() instanceof Timestamp)
                {
                    entry.setValue(String.valueOf(entry.getValue()));
                }
            }
        }
    }

}
