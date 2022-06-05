//  Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.test;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.protocol.pure.v1.extension.ConnectionFactoryExtension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.InputData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.LocalH2DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.mappingTest.RelationalInputData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.mappingTest.RelationalInputType;

import java.util.Optional;

public class RelationalConnectionFactory implements ConnectionFactoryExtension
{
    @Override
    public Optional<Connection> tryBuildFromInputData(InputData inputData)
    {
        if (inputData instanceof RelationalInputData)
        {
            RelationalInputData relationalInputData = (RelationalInputData) inputData;
            RelationalDatabaseConnection connection = new RelationalDatabaseConnection();
            connection.databaseType = DatabaseType.H2;
            connection.type = DatabaseType.H2;
            connection.element = relationalInputData.database;
            connection.authenticationStrategy = new TestDatabaseAuthenticationStrategy();
            LocalH2DatasourceSpecification localH2DatasourceSpecification = new LocalH2DatasourceSpecification();
            if (relationalInputData.inputType == RelationalInputType.SQL)
            {
                localH2DatasourceSpecification.testDataSetupSqls = Lists.mutable.of(relationalInputData.data.split("(?<!\\\\);")).collect(r -> r.replace("\\;", ";") + ";");
            }
            else if (relationalInputData.inputType == RelationalInputType.CSV)
            {
                localH2DatasourceSpecification.testDataSetupCsv = relationalInputData.data;
            }
            else
            {
                throw new RuntimeException(relationalInputData.inputType + " is not supported");
            }
            connection.datasourceSpecification = localH2DatasourceSpecification;
            return Optional.of(connection);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Connection> tryBuildFromConnection(Connection connection, String testData, String element)
    {
        return ConnectionFactoryExtension.super.tryBuildFromConnection(connection, testData, element);
    }
}
