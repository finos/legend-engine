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

package org.finos.legend.engine.plan.execution.stores.relational.test.full.graphFetch.dataTypes;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.plan.execution.result.json.JsonStreamToPureFormatSerializer;
import org.finos.legend.engine.plan.execution.result.json.JsonStreamingResult;
import org.finos.legend.engine.plan.execution.stores.relational.connection.AlloyTestServer;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.*;
import org.junit.Assert;
import org.junit.Test;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Stream;

public class TestRelationalGraphFetchExecutionDataTypes extends AlloyTestServer
{
    private static final String LOGICAL_MODEL = "###Pure\n" +
            "Class test::DataTypesClass\n" +
            "{\n" +
            "    tinyInt: Integer[0..1];\n" +
            "    smallInt: Integer[0..1];\n" +
            "    integer: Integer[0..1];\n" +
            "    bigInt: Integer[0..1];\n" +
            "    varchar: String[0..1];\n" +
            "    char: String[0..1];\n" +
            "    date : Date[0..1];\n" +
            "    timestamp : Date[0..1];\n" +
            "    float: Float[0..1];\n" +
            "    double: Float[0..1];\n" +
            "    decimalAsFloat: Float[0..1];\n" +
            "    real: Float[0..1];\n" +
            "    numericAsFloat: Float[0..1];\n" +
            "    bit:Boolean[0..1];\n" +
            "    decimal: Decimal[0..1];\n" +
            "    numeric: Decimal[0..1];\n" +
            "    floatAsDecimal: Decimal[0..1];\n" +
            "}\n\n";

    private static final String STORE_MODEL = "###Relational\n" +
            "Database test::DataTypesDB\n" +
            "(\n" +
            "    Table dataTable\n" +
            "    (\n" +
            "        pk INTEGER PRIMARY KEY,\n" +
            "        ti TINYINT,\n" +
            "        si SMALLINT,\n" +
            "        int INTEGER,\n" +
            "        bi BIGINT,\n" +
            "        vc VARCHAR(200),\n" +
            "        c CHAR(1),\n" +
            "        date DATE,\n" +
            "        ts TIMESTAMP,\n" +
            "        f FLOAT,\n" +
            "        d DOUBLE,\n" +
            "        bit BIT,\n" +
            "        dec DECIMAL(38,15),\n" +
            "        r REAL,\n" +
            "        n NUMERIC(38,15)\n" +
            "    )\n" +
            ")\n\n";

    private static final String STORE_MODEL_WITH_ALL_COLUMNS_AS_PK = "###Relational\n" +
            "Database test::DataTypesDB\n" +
            "(\n" +
            "    Table dataTable\n" +
            "    (\n" +
            "        pk INTEGER PRIMARY KEY,\n" +
            "        ti TINYINT PRIMARY KEY,\n" +
            "        si SMALLINT PRIMARY KEY,\n" +
            "        int INTEGER PRIMARY KEY,\n" +
            "        bi BIGINT PRIMARY KEY,\n" +
            "        vc VARCHAR(200) PRIMARY KEY,\n" +
            "        c CHAR(1) PRIMARY KEY,\n" +
            "        date DATE PRIMARY KEY,\n" +
            "        ts TIMESTAMP PRIMARY KEY,\n" +
            "        f FLOAT PRIMARY KEY,\n" +
            "        d DOUBLE PRIMARY KEY,\n" +
            "        bit BIT PRIMARY KEY,\n" +
            "        dec DECIMAL(38,15) PRIMARY KEY,\n" +
            "        r REAL PRIMARY KEY,\n" +
            "        n NUMERIC(38,15) PRIMARY KEY\n" +
            "    )\n" +
            ")\n\n";

    private static final String MAPPING = "###Mapping\n" +
            "Mapping test::Map\n" +
            "(\n" +
            "    test::DataTypesClass: Relational\n" +
            "    {\n" +
            "       scope([test::DataTypesDB] dataTable)\n" +
            "       (\n" +
            "          tinyInt: ti,\n" +
            "          smallInt: si,\n" +
            "          integer: int,\n" +
            "          bigInt: bi,\n" +
            "          varchar: vc,\n" +
            "          char: c,\n" +
            "          date : date,\n" +
            "          timestamp : ts,\n" +
            "          float: f,\n" +
            "          double: d,\n" +
            "          bit: bit,\n" +
            "          decimalAsFloat: dec,\n" +
            "          real: r,\n" +
            "          numericAsFloat: n,\n" +
            "          decimal: dec,\n" +
            "          numeric: n,\n" +
            "          floatAsDecimal: f\n" +
            "       )\n" +
            "    }\n" +
            ")\n\n";

    private static final String RUNTIME = "###Runtime\n" +
            "Runtime test::Runtime\n" +
            "{\n" +
            "  mappings:\n" +
            "  [\n" +
            "    test::Map\n" +
            "  ];\n" +
            "  connections:\n" +
            "  [\n" +
            "    test::DataTypesDB:\n" +
            "    [\n" +
            "      c1: #{\n" +
            "        RelationalDatabaseConnection\n" +
            "        {\n" +
            "          type: H2;\n" +
            "          specification: LocalH2 {};\n" +
            "          auth: DefaultH2;\n" +
            "        }\n" +
            "      }#\n" +
            "    ]\n" +
            "  ];\n" +
            "}\n\n";


    @Test
    public void testGraphFetchDataTypes() throws Exception
    {
        JsonStreamingResult res = getJsonStreamingResultForAllDataTypes(STORE_MODEL);
        String stringResult = res.flush(new JsonStreamToPureFormatSerializer(res));

        String expected = "[" +
                "{\"tinyInt\":1,\"smallInt\":2,\"integer\":3,\"bigInt\":1000,\"varchar\":\"Something\",\"char\":\"c\",\"date\":\"2003-07-19\",\"timestamp\":\"2003-07-19T00:00:00.000000000\",\"float\":1.1,\"double\":2.2,\"decimalAsFloat\":123456789.12345679,\"numericAsFloat\":987654321.0987654,\"bit\":true,\"decimal\":123456789.123456789012345,\"numeric\":987654321.098765432154321,\"floatAsDecimal\":1.1}," +
                "{\"tinyInt\":null,\"smallInt\":null,\"integer\":null,\"bigInt\":null,\"varchar\":null,\"char\":null,\"date\":null,\"timestamp\":null,\"float\":null,\"double\":null,\"decimalAsFloat\":null,\"numericAsFloat\":null,\"bit\":null,\"decimal\":null,\"numeric\":null,\"floatAsDecimal\":null}" +
                "]";

        Assert.assertEquals(expected, new ObjectMapper().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).readTree(stringResult).toString());
    }

    @Test
    public void testGraphFetchDataTypesAsPrimaryKeys() throws Exception
    {
        JsonStreamingResult res = getJsonStreamingResultForAllDataTypes(STORE_MODEL_WITH_ALL_COLUMNS_AS_PK);
        String stringResult = res.flush(new JsonStreamToPureFormatSerializer(res));

        String expected = "[" +
                "{\"tinyInt\":1,\"smallInt\":2,\"integer\":3,\"bigInt\":1000,\"varchar\":\"Something\",\"char\":\"c\",\"date\":\"2003-07-19\",\"timestamp\":\"2003-07-19T00:00:00.000000000\",\"float\":1.1,\"double\":2.2,\"decimalAsFloat\":123456789.12345679,\"numericAsFloat\":987654321.0987654,\"bit\":true,\"decimal\":123456789.123456789012345,\"numeric\":987654321.098765432154321,\"floatAsDecimal\":1.1}," +
                "{\"tinyInt\":null,\"smallInt\":null,\"integer\":null,\"bigInt\":null,\"varchar\":null,\"char\":null,\"date\":null,\"timestamp\":null,\"float\":null,\"double\":null,\"decimalAsFloat\":null,\"numericAsFloat\":null,\"bit\":null,\"decimal\":null,\"numeric\":null,\"floatAsDecimal\":null}" +
                "]";

        Assert.assertEquals(expected, new ObjectMapper().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).readTree(stringResult).toString());
    }

    @Test
    public void testGraphFetchDataTypesFromJavaStream() throws Exception
    {
        Stream<ObjectNode> stream = getJsonStreamingResultForAllDataTypes(STORE_MODEL).toStream();

        String expected = "[" +
                    "{\"tinyInt\":1,\"smallInt\":2,\"integer\":3,\"bigInt\":1000,\"varchar\":\"Something\",\"char\":\"c\",\"date\":\"2003-07-19\",\"timestamp\":\"2003-07-19T00:00:00.000000000\",\"float\":1.1,\"double\":2.2,\"decimalAsFloat\":1.2345678912345679E8,\"numericAsFloat\":9.876543210987654E8,\"bit\":true,\"decimal\":123456789.123456789012345,\"numeric\":987654321.098765432154321,\"floatAsDecimal\":1.1}," +
                    "{\"tinyInt\":null,\"smallInt\":null,\"integer\":null,\"bigInt\":null,\"varchar\":null,\"char\":null,\"date\":null,\"timestamp\":null,\"float\":null,\"double\":null,\"decimalAsFloat\":null,\"numericAsFloat\":null,\"bit\":null,\"decimal\":null,\"numeric\":null,\"floatAsDecimal\":null}" +
                "]";

        Assert.assertEquals(expected, new ObjectMapper().writeValueAsString(stream.iterator()));
    }

    @Test
    public void testGraphFetchDataTypeErrorMessage()
    {
        String invalidMapping = "###Mapping\n" +
                "Mapping test::Map\n" +
                "(\n" +
                "    test::DataTypesClass: Relational\n" +
                "    {\n" +
                "       scope([test::DataTypesDB] dataTable)\n" +
                "       (\n" +
                "          integer: vc\n" +
                "       )\n" +
                "    }\n" +
                ")\n\n";

        String fetchFunction = "###Pure\n" +
                "function test::fetch(): Any[*]\n" +
                "{\n" +
                "  |test::DataTypesClass.all()\n" +
                "    ->graphFetch(#{\n" +
                "      test::DataTypesClass {\n" +
                "         integer\n" +
                "      }\n" +
                "   }#, 1)\n" +
                "    ->serialize(#{\n" +
                "      test::DataTypesClass {\n" +
                "         integer\n" +
                "      }\n" +
                "   }#)\n" +
                "}";

        SingleExecutionPlan plan = buildPlan(LOGICAL_MODEL + STORE_MODEL + invalidMapping + RUNTIME + fetchFunction);
        RuntimeException e = Assert.assertThrows(RuntimeException.class, () ->
        {
            JsonStreamingResult res  = (JsonStreamingResult) this.planExecutor.execute(plan, Maps.mutable.empty(), (String) null, Identity.getAnonymousIdentity());
            res.flush(new JsonStreamToPureFormatSerializer(res));
        });
        Assert.assertEquals("Error reading in property 'integer' of type Integer from SQL column of type 'VARCHAR'.", e.getMessage());
    }

    @Test
    public void testGraphFetchTimestampWithTimeZoneColumnDefaultConnectionTimeZone() throws Exception
    {
        assertTimestampWithTimeZoneColumns(null, "2020-01-01T00:00:00.000000000");
    }

    @Test
    public void testGraphFetchTimestampWithTimeZoneColumnConnectionTimeZoneBehindUtc() throws Exception
    {
        assertTimestampWithTimeZoneColumns("America/New_York", "2020-01-01T05:00:00.000000000");
    }

    @Test
    public void testGraphFetchTimestampWithTimeZoneColumnConnectionTimeZoneAheadOfUtc() throws Exception
    {
        assertTimestampWithTimeZoneColumns("Asia/Tokyo", "2019-12-31T15:00:00.000000000");
    }

    private void assertTimestampWithTimeZoneColumns(String connectionTimeZone, String expectedPlainTimestamp) throws Exception
    {
        String model = "###Pure\n" +
                "Class test::TzClass\n" +
                "{\n" +
                "    id: Integer[1];\n" +
                "    plain: DateTime[0..1];\n" +
                "    zoned: DateTime[0..1];\n" +
                "    zonedAsDate: Date[0..1];\n" +
                "}\n\n" +
                "###Relational\n" +
                "Database test::TzDB\n" +
                "(\n" +
                "    Table tzTable\n" +
                "    (\n" +
                "        pk INTEGER PRIMARY KEY,\n" +
                "        ts TIMESTAMP,\n" +
                "        tstz TIMESTAMP\n" +
                "    )\n" +
                ")\n\n" +
                "###Mapping\n" +
                "Mapping test::Map\n" +
                "(\n" +
                "    test::TzClass: Relational\n" +
                "    {\n" +
                "       scope([test::TzDB] tzTable)\n" +
                "       (\n" +
                "          id: pk,\n" +
                "          plain: ts,\n" +
                "          zoned: tstz,\n" +
                "          zonedAsDate: tstz\n" +
                "       )\n" +
                "    }\n" +
                ")\n\n" +
                "###Runtime\n" +
                "Runtime test::Runtime\n" +
                "{\n" +
                "  mappings: [test::Map];\n" +
                "  connections:\n" +
                "  [\n" +
                "    test::TzDB:\n" +
                "    [\n" +
                "      c1: #{\n" +
                "        RelationalDatabaseConnection\n" +
                "        {\n" +
                "          type: H2;\n" +
                "          specification: LocalH2 {};\n" +
                "          auth: DefaultH2;\n" +
                "        }\n" +
                "      }#\n" +
                "    ]\n" +
                "  ];\n" +
                "}\n\n" +
                "###Pure\n" +
                "function test::fetch(): Any[*]\n" +
                "{\n" +
                "  |test::TzClass.all()\n" +
                "    ->graphFetch(#{test::TzClass{id, plain, zoned, zonedAsDate}}#, 1)\n" +
                "    ->serialize(#{test::TzClass{id, plain, zoned, zonedAsDate}}#)\n" +
                "}";

        SingleExecutionPlan plan = buildPlan(model, connectionTimeZone);
        JsonStreamingResult res = (JsonStreamingResult) this.planExecutor.execute(plan, Maps.mutable.empty(), (String) null, Identity.getAnonymousIdentity());
        String stringResult = res.flush(new JsonStreamToPureFormatSerializer(res));

        String expected = "[" +
                "{\"id\":0,\"plain\":\"" + expectedPlainTimestamp + "\",\"zoned\":\"2020-01-01T05:00:00.000000000\",\"zonedAsDate\":\"2020-01-01T05:00:00.000000000\"}," +
                "{\"id\":1,\"plain\":\"" + expectedPlainTimestamp + "\",\"zoned\":\"2020-01-01T00:00:00.000000000\",\"zonedAsDate\":\"2020-01-01T00:00:00.000000000\"}," +
                "{\"id\":2,\"plain\":\"" + expectedPlainTimestamp + "\",\"zoned\":\"2020-01-01T12:30:00.000000000\",\"zonedAsDate\":\"2020-01-01T12:30:00.000000000\"}," +
                "{\"id\":3,\"plain\":null,\"zoned\":null,\"zonedAsDate\":null}" +
                "]";
        Assert.assertEquals(expected, new ObjectMapper().readTree(stringResult).toString());
    }

    private JsonStreamingResult getJsonStreamingResultForAllDataTypes(String storeModel)
    {
        String fetchFunction = "###Pure\n" +
                "function test::fetch(): Any[*]\n" +
                "{\n" +
                "  |test::DataTypesClass.all()\n" +
                "    ->graphFetch(#{\n" +
                "      test::DataTypesClass {\n" +
                "         tinyInt,\n" +
                "         smallInt,\n" +
                "         integer,\n" +
                "         bigInt,\n" +
                "         varchar,\n" +
                "         char,\n" +
                "         date,\n" +
                "         timestamp,\n" +
                "         float,\n" +
                "         double,\n" +
                "         decimalAsFloat,\n" +
                "         // real,\n" +
                "         numericAsFloat,\n" +
                "         bit,\n" +
                "         decimal,\n" +
                "         numeric,\n" +
                "         floatAsDecimal\n" +
                "      }\n" +
                "   }#, 1)\n" +
                "    ->serialize(#{\n" +
                "      test::DataTypesClass {\n" +
                "         tinyInt,\n" +
                "         smallInt,\n" +
                "         integer,\n" +
                "         bigInt,\n" +
                "         varchar,\n" +
                "         char,\n" +
                "         date,\n" +
                "         timestamp,\n" +
                "         float,\n" +
                "         double,\n" +
                "         decimalAsFloat,\n" +
                "         // real,\n" +
                "         numericAsFloat,\n" +
                "         bit,\n" +
                "         decimal,\n" +
                "         numeric,\n" +
                "         floatAsDecimal\n" +
                "      }\n" +
                "   }#)\n" +
                "}";

        SingleExecutionPlan plan = buildPlan(LOGICAL_MODEL + storeModel + MAPPING + RUNTIME + fetchFunction);
        return (JsonStreamingResult) this.planExecutor.execute(plan, Maps.mutable.empty(), (String) null, Identity.getAnonymousIdentity());
    }

    @Override
    protected void insertTestData(Statement s) throws SQLException
    {
        s.execute("Drop table if exists dataTable;");
        s.execute("Create Table dataTable(pk INT NOT NULL,ti TINYINT NULL,si SMALLINT NULL,int INT NULL,bi BIGINT NULL,vc VARCHAR(200) NULL,c CHAR(1) NULL,date DATE NULL,ts TIMESTAMP NULL,f FLOAT NULL,d DOUBLE NULL,bit BIT NULL,dec DECIMAL(38,15) NULL, r REAL NULL, n NUMERIC(38,15) NULL, PRIMARY KEY(pk));");
        s.execute("insert into dataTable (pk, ti, si, int, bi, vc, c, date, ts, f, d, bit, dec, r, n) values (0, 1, 2, 3, 1000, 'Something', 'c', '2003-07-19', '2003-07-19 00:00:00', 1.1, 2.2, 1, 123456789.123456789012345, 987654321.098765432154321, 987654321.098765432154321)");
        s.execute("insert into dataTable (pk, ti, si, int, bi, vc, c, date, ts, f, d, bit, dec, r, n) values (1, null, null, null, null, null, null, null, null, null, null, null, null, null, null)");
        s.execute("Drop table if exists tzTable;");
        s.execute("Create Table tzTable(pk INT NOT NULL, ts TIMESTAMP NULL, tstz TIMESTAMP WITH TIME ZONE NULL, PRIMARY KEY(pk));");
        s.execute("insert into tzTable (pk, ts, tstz) values (0, TIMESTAMP '2020-01-01 00:00:00', TIMESTAMP WITH TIME ZONE '2020-01-01 00:00:00-05:00')");
        s.execute("insert into tzTable (pk, ts, tstz) values (1, TIMESTAMP '2020-01-01 00:00:00', TIMESTAMP WITH TIME ZONE '2020-01-01 09:00:00+09:00')");
        s.execute("insert into tzTable (pk, ts, tstz) values (2, TIMESTAMP '2020-01-01 00:00:00', TIMESTAMP WITH TIME ZONE '2020-01-01 18:00:00+05:30')");
        s.execute("insert into tzTable (pk, ts, tstz) values (3, null, null)");
    }
}
