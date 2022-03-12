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

package org.finos.legend.engine.authentication;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class TestDefaultSupportedFlowsMetadata
{
    private static LegendDefaultDatabaseAuthenticationFlowProvider DEFAULT_PROVIDER = new LegendDefaultDatabaseAuthenticationFlowProvider();

    @Test
    public void testMetadata() throws JsonProcessingException {
        ImmutableList<DatabaseAuthenticationFlowMetadata> supportedFlowsMetadata = DEFAULT_PROVIDER.getSupportedFlowsMetadata();
        MutableList<DatabaseAuthenticationFlowMetadata> sortedMetadata = supportedFlowsMetadata.toSortedList();
        String metadataString = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(sortedMetadata);
        String expected = "[ {\n" +
                "  \"databaseType\" : \"H2\",\n" +
                "  \"datasourceType\" : \"static\",\n" +
                "  \"authenticationType\" : \"test\"\n" +
                "}, {\n" +
                "  \"databaseType\" : \"SqlServer\",\n" +
                "  \"datasourceType\" : \"static\",\n" +
                "  \"authenticationType\" : \"userNamePassword\"\n" +
                "}, {\n" +
                "  \"databaseType\" : \"Snowflake\",\n" +
                "  \"datasourceType\" : \"snowflake\",\n" +
                "  \"authenticationType\" : \"snowflakePublic\"\n" +
                "}, {\n" +
                "  \"databaseType\" : \"BigQuery\",\n" +
                "  \"datasourceType\" : \"bigQuery\",\n" +
                "  \"authenticationType\" : \"gcpApplicationDefaultCredentials\"\n" +
                "}, {\n" +
                "  \"databaseType\" : \"Databricks\",\n" +
                "  \"datasourceType\" : \"databricks\",\n" +
                "  \"authenticationType\" : \"apiToken\"\n" +
                "} ]";
        assertEquals(expected, metadataString.replaceAll("\\r", ""));
    }
}