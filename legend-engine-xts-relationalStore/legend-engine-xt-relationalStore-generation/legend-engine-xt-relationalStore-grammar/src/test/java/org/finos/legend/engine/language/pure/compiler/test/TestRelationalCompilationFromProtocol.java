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

package org.finos.legend.engine.language.pure.compiler.test;

import org.junit.Test;

public class TestRelationalCompilationFromProtocol extends TestCompilationFromProtocol.TestCompilationFromProtocolTestSuite
{
    @Test
    public void testEmbeddedMappingWithCorrectIdSetForClassMapping()
    {
        testWithProtocolPath("simpleEmbeddedMapping.json");
    }

    @Test
    public void testMappingWithPropertyFromAssociation()
    {
        testWithProtocolPath("mappingWithMappedPropertyFromAssociation.json");
    }

    @Test
    public void testRelationalDatabaseConnectionWithTimeZone()
    {
        testWithJson(relationalDatabaseConnectionWithTimeZone("America/New_York"), null);
        testWithJson(relationalDatabaseConnectionWithTimeZone("EST"), null);
        testWithJson(relationalDatabaseConnectionWithTimeZone("-0500"), null);
    }

    @Test
    public void testRelationalDatabaseConnectionWithUnknownTimeZone()
    {
        testWithJson(relationalDatabaseConnectionWithTimeZone("America/NewYork"), "COMPILATION error: Error in 'test::runtime::FirmConnection': Unknown time zone: America/NewYork");
        testWithJson(relationalDatabaseConnectionWithTimeZone("+3000"), "COMPILATION error: Error in 'test::runtime::FirmConnection': Unknown time zone: +3000");
    }

    // An empty zone names no zone. A connection that means to name none leaves the time zone out. The old parser wrote
    // an empty zone as '', which comes out empty once its quotes come off.
    @Test
    public void testRelationalDatabaseConnectionWithEmptyTimeZone()
    {
        testWithJson(relationalDatabaseConnectionWithTimeZone(""), "COMPILATION error: Error in 'test::runtime::FirmConnection': Unknown time zone: ");
        testWithJson(relationalDatabaseConnectionWithTimeZone("''"), "COMPILATION error: Error in 'test::runtime::FirmConnection': Unknown time zone: ");
    }

    // Before the connection parser stripped the grammar's quotes from a zone id, they were kept in the protocol, so
    // JSON written then names 'America/New_York', quotes and all. The quotes come off as the JSON is read, before the
    // zone is validated.
    @Test
    public void testRelationalDatabaseConnectionWithTimeZoneQuotedByOldParser()
    {
        testWithJson(relationalDatabaseConnectionWithTimeZone("'America/New_York'"), null);
        testWithJson(relationalDatabaseConnectionWithTimeZone("'America/NewYork'"), "COMPILATION error: Error in 'test::runtime::FirmConnection': Unknown time zone: America/NewYork");
    }

    private static String relationalDatabaseConnectionWithTimeZone(String timeZone)
    {
        return "{\n" +
                "  \"_type\": \"data\",\n" +
                "  \"elements\": [\n" +
                "    {\n" +
                "      \"_type\": \"connection\",\n" +
                "      \"name\": \"FirmConnection\",\n" +
                "      \"package\": \"test::runtime\",\n" +
                "      \"connectionValue\": {\n" +
                "        \"_type\": \"RelationalDatabaseConnection\",\n" +
                "        \"type\": \"H2\",\n" +
                "        \"timeZone\": \"" + timeZone + "\",\n" +
                "        \"datasourceSpecification\": {\n" +
                "          \"_type\": \"h2Local\"\n" +
                "        },\n" +
                "        \"authenticationStrategy\": {\n" +
                "          \"_type\": \"h2Default\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";
    }
}
