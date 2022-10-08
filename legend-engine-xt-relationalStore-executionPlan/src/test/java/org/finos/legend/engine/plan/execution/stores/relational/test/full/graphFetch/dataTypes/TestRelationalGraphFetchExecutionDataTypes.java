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
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.plan.execution.result.json.JsonStreamToPureFormatSerializer;
import org.finos.legend.engine.plan.execution.result.json.JsonStreamingResult;
import org.finos.legend.engine.plan.execution.stores.relational.connection.AlloyTestServer;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.junit.Assert;
import org.junit.Test;

import java.sql.SQLException;
import java.sql.Statement;

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
            "        c CHAR(2),\n" +
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

        SingleExecutionPlan plan = buildPlan(LOGICAL_MODEL + STORE_MODEL + MAPPING + RUNTIME + fetchFunction);
        JsonStreamingResult res = (JsonStreamingResult) this.planExecutor.execute(plan, Maps.mutable.empty(), (String) null, null);
        String stringResult = res.flush(new JsonStreamToPureFormatSerializer(res));

        String expected = "[" +
                "{\"tinyInt\":1,\"smallInt\":2,\"integer\":3,\"bigInt\":1000,\"varchar\":\"Something\",\"char\":\"c\",\"date\":\"2003-07-19\",\"timestamp\":\"2003-07-19T00:00:00.000000000\",\"float\":1.1,\"double\":2.2,\"decimalAsFloat\":123456789.12345679,\"numericAsFloat\":987654321.0987654,\"bit\":true,\"decimal\":123456789.123456789012345,\"numeric\":987654321.098765432154321,\"floatAsDecimal\":1.1}," +
                "{\"tinyInt\":null,\"smallInt\":null,\"integer\":null,\"bigInt\":null,\"varchar\":null,\"char\":null,\"date\":null,\"timestamp\":null,\"float\":null,\"double\":null,\"decimalAsFloat\":null,\"numericAsFloat\":null,\"bit\":null,\"decimal\":null,\"numeric\":null,\"floatAsDecimal\":null}" +
                "]";

        Assert.assertEquals(expected, new ObjectMapper().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).readTree(stringResult).toString());
    }

    @Override
    protected void insertTestData(Statement s) throws SQLException
    {
        s.execute("Drop table if exists dataTable;");
        s.execute("Create Table dataTable(pk INT NOT NULL,ti TINYINT NULL,si SMALLINT NULL,int INT NULL,bi BIGINT NULL,vc VARCHAR(200) NULL,c CHAR(2) NULL,date DATE NULL,ts TIMESTAMP NULL,f FLOAT NULL,d DOUBLE NULL,bit BIT NULL,dec DECIMAL(38,15) NULL, n NUMERIC(38,15) NULL, PRIMARY KEY(pk));");
        s.execute("insert into dataTable (pk, ti, si, int, bi, vc, c, date, ts, f, d, bit, dec, n) values (0, 1, 2, 3, 1000, 'Something', 'c', '2003-07-19', '2003-07-19 00:00:00', 1.1, 2.2, 1, 123456789.123456789012345, 987654321.098765432154321)");
        s.execute("insert into dataTable (pk, ti, si, int, bi, vc, c, date, ts, f, d, bit, dec, n) values (1, null, null, null, null, null, null, null, null, null, null, null, null, null)");
    }
}
