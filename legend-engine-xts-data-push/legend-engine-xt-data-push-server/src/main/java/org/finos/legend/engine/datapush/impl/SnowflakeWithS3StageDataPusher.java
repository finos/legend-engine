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

import io.deephaven.csv.CsvSpecs;
import io.deephaven.csv.reading.CsvReader;
import io.deephaven.csv.sinks.SinkFactory;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.connection.Connection;
import org.finos.legend.engine.datapush.DataPusher;
import org.finos.legend.engine.datapush.data.CSVData;
import org.finos.legend.engine.datapush.data.Data;
import org.finos.legend.engine.shared.core.identity.Identity;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.sql.Statement;
import java.util.UUID;

public class SnowflakeWithS3StageDataPusher extends DataPusher
{
    private final String tableName;
    private final String stageName;
    private final S3DataStage s3DataStage;

    public SnowflakeWithS3StageDataPusher(String s3StageBucketName, String s3StageEndpoint, AwsCredentialsProvider s3StageCredentialProvider, String tableName, String stageName)
    {
        this.tableName = tableName;
        this.stageName = stageName;
        this.s3DataStage = new S3DataStage(s3StageBucketName, s3StageEndpoint, s3StageCredentialProvider);
    }

    @Override
    public void writeCSV(Identity identity, Connection connection, Data data) throws Exception
    {
        // TODO: this is probably not performant for streaming/large CSV, we should think of how to optimize this later
        CSVData csvData = (CSVData) data;
        CsvSpecs specs = CsvSpecs.csv();
        CsvReader.Result csvParserResult = CsvReader.read(specs, new ByteArrayInputStream(csvData.value.getBytes()), SinkFactory.arrays());
        String filePath = this.s3DataStage.write(identity, csvData);
        this.uploadCSVToSnowflake(identity, connection, filePath, csvParserResult);
    }

    public void uploadCSVToSnowflake(Identity identity, Connection connection, String filePath, CsvReader.Result csvParserResult) throws Exception
    {
        java.sql.Connection jdbcConnection = this.connectionFactory.getConnection(identity, connection);

        String tableCreationQuery = String.format("CREATE TABLE %s (%s);", this.tableName, ListIterate.collect(
                Lists.mutable.of(csvParserResult.columns()), column ->
                {
                    String dataType = null;
                    // NOTE: these are specific to Snowflake
                    // See https://docs.snowflake.com/en/sql-reference/data-types
                    switch (column.dataType())
                    {
                        case BOOLEAN_AS_BYTE:
                            dataType = "BOOLEAN";
                            break;
                        case BYTE:
                        case SHORT:
                        case INT:
                        case LONG:
                        case DATETIME_AS_LONG:
                        case TIMESTAMP_AS_LONG:
                            dataType = "INTEGER";
                            break;
                        case FLOAT:
                        case DOUBLE:
                            dataType = "FLOAT";
                            break;
                        case STRING:
                        case CHAR:
                            dataType = "STRING";
                            break;
                        case CUSTOM:
                            throw new RuntimeException("Not possible");
                    }
                    // Put quote around table name to avoid problems with column names with spaces
                    return String.format("\"%s\" %s", column.name(), dataType);
                }
        ).makeString(","));
        // Give Snowflake the full s3 path to improve performance as no lookup is necessary
        // See https://community.snowflake.com/s/question/0D50Z00009Y7eCRSAZ/copy-from-s3-into-table-command-is-extremely-slow
        String insertQuery = String.format("COPY INTO %s FROM @%s/%s file_format = (type = csv skip_header = 1);", this.tableName, this.stageName, filePath);

        try
        {
            Statement statement = jdbcConnection.createStatement();
            statement.execute(String.format("DROP TABLE IF EXISTS %s;", this.tableName));
            statement.execute(tableCreationQuery);
            statement.execute(insertQuery);
            statement.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static class S3DataStage
    {
        private final String bucket;
        private final String endpoint;
        private final AwsCredentialsProvider credentialsProvider;

        public S3DataStage(String bucket, String endpoint, AwsCredentialsProvider credentialsProvider)
        {
            this.bucket = bucket;
            this.endpoint = endpoint;
            this.credentialsProvider = credentialsProvider;
        }

        private static String generateBucketName(Identity identity)
        {
            return identity.getName().replaceAll("_", "").toLowerCase();
        }

        private static String generateObjectKey()
        {
            return UUID.randomUUID().toString();
        }

        private static String generateObjectPrefix(Identity identity)
        {
            return identity.getName();
        }

        private S3Client getS3Client()
        {
            S3ClientBuilder clientBuilder = S3Client
                    .builder()
                    .credentialsProvider(this.credentialsProvider)
                    .region(Region.US_EAST_1);
            if (this.endpoint != null)
            {
                clientBuilder.endpointOverride(URI.create(this.endpoint));
            }
            return clientBuilder.build();
        }

        public String write(Identity identity, Data data) throws Exception
        {
            CSVData csvData = (CSVData) data;
            S3Client s3Client = this.getS3Client();
            String key = String.format("%s/%s", generateObjectPrefix(identity), generateObjectKey());
            try
            {
                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(this.bucket)
                        .key(key).build();
                RequestBody requestBody = RequestBody.fromString(csvData.value);
                PutObjectResponse putObjectResponse = s3Client.putObject(putObjectRequest, requestBody);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            return key;
        }
    }
}
