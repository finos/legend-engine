// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.datapush.impl;

import org.finos.legend.connection.RelationalDatabaseStoreSupport;
import org.finos.legend.connection.StoreInstance;
import org.finos.legend.engine.datapush.DataPusher;
import org.finos.legend.engine.datapush.data.CSVData;
import org.finos.legend.engine.protocol.pure.v1.connection.AuthenticationConfiguration;
import org.finos.legend.engine.shared.core.identity.Identity;

import java.sql.Connection;
import java.sql.Statement;

public class SnowflakeJDBCDataPusher extends DataPusher
{
    private final S3DataStager s3DataStager;
    private final String tableName;
    private final String stageName;

    public SnowflakeJDBCDataPusher(S3DataStager s3DataStager, String tableName, String stageName)
    {
        this.s3DataStager = s3DataStager;
        this.tableName = tableName;
        this.stageName = stageName;
    }

    @Override
    public void writeCSV(Identity identity, StoreInstance connectionInstance, AuthenticationConfiguration authenticationConfiguration, CSVData csvData) throws Exception
    {
        String s3Key = this.s3DataStager.write(identity, csvData);
        this.uploadCSVToSnowflake(identity, connectionInstance, authenticationConfiguration, s3Key);
    }

    public void uploadCSVToSnowflake(Identity identity, StoreInstance connectionInstance, AuthenticationConfiguration authenticationConfiguration, String filePattern) throws Exception
    {
        RelationalDatabaseStoreSupport.cast(connectionInstance.getStoreSupport());
        Connection connection = this.connectionFactory.getConnection(identity, connectionInstance, authenticationConfiguration);

        String sql = String.format("COPY INTO %s FROM @%s PATTERN='%s';", this.tableName, this.stageName, filePattern);
        System.out.println(sql);
        try
        {
            Statement statement = connection.createStatement();
            statement.execute(sql);
            statement.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
