// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.test;

import org.antlr.v4.runtime.Vocabulary;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.RelationalParserGrammar;
import org.junit.Test;

import java.util.List;

public class TestRelationalGrammarParser extends TestGrammarParser.TestGrammarParserTestSuite
{
    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return RelationalParserGrammar.VOCABULARY;
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return "###Relational\n" +
                "Database " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "(\n" +
                ")\n";
    }

    @Test
    public void testRelationalStoreTableColumnType()
    {
        // test unsupported column data type
        test("###Relational\n" +
                "\n" +
                "Database apps::pure::dsb::sample::store::DsbSampleDb\n" +
                "(\n" +
                "   Table ORDERS (\n" +
                "      clientId UNKNOWN\n" +
                "   )\n" +
                ")", "PARSER error at [6:16-22]: Unsupported column data type 'UNKNOWN'");
        // CHAR
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId CHAR\n" +
                ")\n)", "PARSER error at [5:16-19]: Column data type CHAR requires 1 parameter (size) in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId CHAR(1)\n" +
                ")\n)");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId CHAR(2,2)\n" +
                ")\n)", "PARSER error at [5:16-24]: Column data type CHAR requires 1 parameter (size) in declaration");
        // VARCHAR
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId VARCHAR\n" +
                ")\n)", "PARSER error at [5:16-22]: Column data type VARCHAR requires 1 parameter (size) in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId VARCHAR(1)\n" +
                ")\n)");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId VARCHAR(2,2)\n" +
                ")\n)", "PARSER error at [5:16-27]: Column data type VARCHAR requires 1 parameter (size) in declaration");
        // BINARY
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId BINARY\n" +
                ")\n)", "PARSER error at [5:16-21]: Column data type BINARY requires 1 parameter (size) in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId BINARY(1)\n" +
                ")\n)");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId BINARY(2,2)\n" +
                ")\n)", "PARSER error at [5:16-26]: Column data type BINARY requires 1 parameter (size) in declaration");
        // VARBINARY
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId VARBINARY\n" +
                ")\n)", "PARSER error at [5:16-24]: Column data type VARBINARY requires 1 parameter (size) in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId VARBINARY(1)\n" +
                ")\n)");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId VARBINARY(2,2)\n" +
                ")\n)", "PARSER error at [5:16-29]: Column data type VARBINARY requires 1 parameter (size) in declaration");
        // BIT
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId BIT\n" +
                ")\n)");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId BIT(1)\n" +
                ")\n)", "PARSER error at [5:16-21]: Column data type BIT does not expect any parameters in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId BIT(2,2)\n" +
                ")\n)", "PARSER error at [5:16-23]: Column data type BIT does not expect any parameters in declaration");
        // INT
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId INT\n" +
                ")\n)");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId INT(1)\n" +
                ")\n)", "PARSER error at [5:16-21]: Column data type INTEGER does not expect any parameters in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId INT(2,2)\n" +
                ")\n)", "PARSER error at [5:16-23]: Column data type INTEGER does not expect any parameters in declaration");
        // INTEGER
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId INTEGER\n" +
                ")\n)");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId INTEGER(1)\n" +
                ")\n)", "PARSER error at [5:16-25]: Column data type INTEGER does not expect any parameters in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId INTEGER(2,2)\n" +
                ")\n)", "PARSER error at [5:16-27]: Column data type INTEGER does not expect any parameters in declaration");
        // BIGINT
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId BIGINT\n" +
                ")\n)");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId BIGINT(1)\n" +
                ")\n)", "PARSER error at [5:16-24]: Column data type BIGINT does not expect any parameters in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId BIGINT(2,2)\n" +
                ")\n)", "PARSER error at [5:16-26]: Column data type BIGINT does not expect any parameters in declaration");
        // SMALLINT
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId SMALLINT\n" +
                ")\n)");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId SMALLINT(1)\n" +
                ")\n)", "PARSER error at [5:16-26]: Column data type SMALLINT does not expect any parameters in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId SMALLINT(2,2)\n" +
                ")\n)", "PARSER error at [5:16-28]: Column data type SMALLINT does not expect any parameters in declaration");
        // TINYINT
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId TINYINT\n" +
                ")\n)");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId TINYINT(1)\n" +
                ")\n)", "PARSER error at [5:16-25]: Column data type TINYINT does not expect any parameters in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId TINYINT(2,2)\n" +
                ")\n)", "PARSER error at [5:16-27]: Column data type TINYINT does not expect any parameters in declaration");
        // TIMESTAMP
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId TIMESTAMP\n" +
                ")\n)");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId TIMESTAMP(1)\n" +
                ")\n)", "PARSER error at [5:16-27]: Column data type TIMESTAMP does not expect any parameters in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId TIMESTAMP(2,2)\n" +
                ")\n)", "PARSER error at [5:16-29]: Column data type TIMESTAMP does not expect any parameters in declaration");
        // DATE
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId DATE\n" +
                ")\n)");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId DATE(1)\n" +
                ")\n)", "PARSER error at [5:16-22]: Column data type DATE does not expect any parameters in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId DATE(2,2)\n" +
                ")\n)", "PARSER error at [5:16-24]: Column data type DATE does not expect any parameters in declaration");
        // NUMERIC
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId NUMERIC\n" +
                ")\n)", "PARSER error at [5:16-22]: Column data type NUMERIC requires 2 parameters (precision, scale) in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId NUMERIC(1)\n" +
                ")\n)", "PARSER error at [5:16-25]: Column data type NUMERIC requires 2 parameters (precision, scale) in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId NUMERIC(2,2)\n" +
                ")\n)");
        // DECIMAL
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId DECIMAL\n" +
                ")\n)", "PARSER error at [5:16-22]: Column data type DECIMAL requires 2 parameters (precision, scale) in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId DECIMAL(1)\n" +
                ")\n)", "PARSER error at [5:16-25]: Column data type DECIMAL requires 2 parameters (precision, scale) in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId DECIMAL(2,2)\n" +
                ")\n)");
        // FLOAT
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId FLOAT\n" +
                ")\n)");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId FLOAT(1)\n" +
                ")\n)", "PARSER error at [5:16-23]: Column data type FLOAT does not expect any parameters in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId FLOAT(2,2)\n" +
                ")\n)", "PARSER error at [5:16-25]: Column data type FLOAT does not expect any parameters in declaration");
        // DOUBLE
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId DOUBLE\n" +
                ")\n)");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId DOUBLE(1)\n" +
                ")\n)", "PARSER error at [5:16-24]: Column data type DOUBLE does not expect any parameters in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId DOUBLE(2,2)\n" +
                ")\n)", "PARSER error at [5:16-26]: Column data type DOUBLE does not expect any parameters in declaration");
        // REAL
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId REAL\n" +
                ")\n)");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId REAL(1)\n" +
                ")\n)", "PARSER error at [5:16-22]: Column data type REAL does not expect any parameters in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId REAL(2,2)\n" +
                ")\n)", "PARSER error at [5:16-24]: Column data type REAL does not expect any parameters in declaration");
        // ARRAY
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId ARRAY\n" +
                ")\n)");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId ARRAY(1)\n" +
                ")\n)", "PARSER error at [5:16-23]: Column data type ARRAY does not expect any parameters in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId ARRAY(2,2)\n" +
                ")\n)", "PARSER error at [5:16-25]: Column data type ARRAY does not expect any parameters in declaration");
        // OTHER
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId OTHER\n" +
                ")\n)");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId OTHER(1)\n" +
                ")\n)", "PARSER error at [5:16-23]: Column data type OTHER does not expect any parameters in declaration");
        test("###Relational\nDatabase test::db\n(\nTable ORDERS (\n" +
                "      clientId OTHER(2,2)\n" +
                ")\n)", "PARSER error at [5:16-25]: Column data type OTHER does not expect any parameters in declaration");
    }
}
