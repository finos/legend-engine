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

import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategyRuntime;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecificationRuntime;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.EmbeddedH2DataSourceSpecificationKey;

import java.util.Properties;


public class EmbeddedH2DataSourceSpecificationRuntime extends DataSourceSpecificationRuntime
{
    public static final String H2_DATA_DIRECTORY_PATH = "h2_data_directory_path";
    public static final String H2_AUTO_SERVER_MODE = "h2_auto_server_mode";
    private static final int MAX_POOL_SIZE = 10;
    private static final int MIN_POOL_SIZE = 0;

    public EmbeddedH2DataSourceSpecificationRuntime(EmbeddedH2DataSourceSpecificationKey key, DatabaseManager databaseManager, AuthenticationStrategyRuntime authenticationStrategyRuntime)
    {
        super(key, databaseManager, authenticationStrategyRuntime, new Properties(), MAX_POOL_SIZE, MIN_POOL_SIZE);
        this.extraDatasourceProperties.put(H2_DATA_DIRECTORY_PATH, key.getDirectory().getAbsolutePath());
        this.extraDatasourceProperties.put(H2_AUTO_SERVER_MODE, String.valueOf(key.isAutoServerMode()).toUpperCase());
    }

}
