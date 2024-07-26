//  Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.pure.runtime.testConnection;

import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProvider;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;

public class CoreExternalTestConnectionCodeRepositoryProvider implements CodeRepositoryProvider
{
    public static final Adapter H2Adapter = new Adapter(
            "H2",
            "Store_Relational",
            "meta::relational::tests::pct::testAdapterForRelationalWithH2Execution_Function_1__X_o_"
    );
    public static final Adapter duckDBAdapter = new Adapter(
            "DuckDB",
            "Store_Relational",
            "meta::relational::tests::pct::testAdapterForRelationalWithDuckDBExecution_Function_1__X_o_"
    );
    public static final Adapter snowflakeAdapter = new Adapter(
            "Snowflake",
            "Store_Relational",
            "meta::relational::tests::pct::testAdapterForRelationalWithSnowflakeExecution_Function_1__X_o_"
    );
    public static final Adapter postgresAdapter = new Adapter(
            "Postgres",
            "Store_Relational",
            "meta::relational::tests::pct::testAdapterForRelationalWithPostgresExecution_Function_1__X_o_"
    );

    @Override
    public CodeRepository repository()
    {
        return GenericCodeRepository.build("core_external_test_connection.definition.json");
    }
}