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

package org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications;

import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.AthenaDatasourceSpecificationKey;

import java.util.Properties;

public class AthenaDatasourceSpecificationRuntime extends org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification
{
    public AthenaDatasourceSpecificationRuntime(AthenaDatasourceSpecificationKey key, DatabaseManager driver, org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy authenticationStrategyRuntime)
    {
        super(key, driver, authenticationStrategyRuntime, addPropertiesFromDataSource(key));
    }

    private static Properties addPropertiesFromDataSource(AthenaDatasourceSpecificationKey key)
    {
        Properties props = new Properties();
        props.put("awsRegion", key.getAwsRegion());
        props.put("s3OutputLocation", key.getS3OutputLocation());
        return props;
    }

    @Override
    protected String getJdbcUrl(String host, int port, String databaseName, Properties properties)
    {
        // usually defaults for host, port and databaseName are passed to this method in the original call.
        // This method is supposed to reset to correct values, if required, and construct the jdbc url by relaying to super class, which in turn relays to Driver.
        AthenaDatasourceSpecificationKey key = (AthenaDatasourceSpecificationKey) this.datasourceKey;
        return super.getJdbcUrl(host, port, key.getDatabaseName(), properties);
    }
}
