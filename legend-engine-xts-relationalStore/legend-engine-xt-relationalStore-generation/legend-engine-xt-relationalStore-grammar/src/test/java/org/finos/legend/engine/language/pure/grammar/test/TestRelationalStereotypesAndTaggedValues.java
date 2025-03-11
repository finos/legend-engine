// Copyright 2025 Goldman Sachs
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

import org.junit.Test;

public class TestRelationalStereotypesAndTaggedValues extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testTableStereotypeAndTaggedValues()
    {
        test("###Relational\n" +
             "Database app::db\n" +
             "(\n" +
             "  Table <<meta::pure::profiles::storeType.type2>> {meta::pure::profiles::doc.doc = 'Table documentation'} Product\n" +
             "  (\n" +
             "    ProductID VARCHAR(30) PRIMARY KEY\n" +
             "  )\n" +
             ")\n" +
             "\n" +
             "###Pure\n" +
             "Profile meta::pure::profiles::storeType\n" +
             "{\n" +
             "  stereotypes: [type1, type2];\n" +
             "}\n" +
             "\n" +
             "Profile meta::pure::profiles::doc\n" +
             "{\n" +
             "  tags: [doc];\n" +
             "}\n");
    }

    @Test
    public void testColumnStereotypeAndTaggedValues()
    {
        test("###Relational\n" +
             "Database app::db\n" +
             "(\n" +
             "  Table Product\n" +
             "  (\n" +
             "    ProductID VARCHAR(30) PRIMARY KEY,\n" +
             "    Name <<meta::pure::profiles::storeType.type1>> {meta::pure::profiles::doc.doc = 'Product name'} VARCHAR(100)\n" +
             "  )\n" +
             ")\n" +
             "\n" +
             "###Pure\n" +
             "Profile meta::pure::profiles::storeType\n" +
             "{\n" +
             "  stereotypes: [type1, type2];\n" +
             "}\n" +
             "\n" +
             "Profile meta::pure::profiles::doc\n" +
             "{\n" +
             "  tags: [doc];\n" +
             "}\n");
    }

    @Test
    public void testViewStereotypeAndTaggedValues()
    {
        test("###Relational\n" +
             "Database app::db\n" +
             "(\n" +
             "  Schema productSchema\n" +
             "  (\n" +
             "    Table Product\n" +
             "    (\n" +
             "      ProductID VARCHAR(30) PRIMARY KEY,\n" +
             "      Name VARCHAR(100)\n" +
             "    )\n" +
             "    \n" +
             "    View <<meta::pure::profiles::storeType.type1>> {meta::pure::profiles::doc.doc = 'Product view'} ProductView\n" +
             "    (\n" +
             "      productId: Product.ProductID,\n" +
             "      productName: Product.Name\n" +
             "    )\n" +
             "  )\n" +
             ")\n" +
             "\n" +
             "###Pure\n" +
             "Profile meta::pure::profiles::storeType\n" +
             "{\n" +
             "  stereotypes: [type1, type2];\n" +
             "}\n" +
             "\n" +
             "Profile meta::pure::profiles::doc\n" +
             "{\n" +
             "  tags: [doc];\n" +
             "}\n");
    }

    @Test
    public void testSchemaStereotypeAndTaggedValues()
    {
        test("###Relational\n" +
             "Database app::db\n" +
             "(\n" +
             "  Schema <<meta::pure::profiles::storeType.type2>> {meta::pure::profiles::doc.doc = 'Product schema'} productSchema\n" +
             "  (\n" +
             "    Table Product\n" +
             "    (\n" +
             "      ProductID VARCHAR(30) PRIMARY KEY,\n" +
             "      Name VARCHAR(100)\n" +
             "    )\n" +
             "  )\n" +
             ")\n" +
             "\n" +
             "###Pure\n" +
             "Profile meta::pure::profiles::storeType\n" +
             "{\n" +
             "  stereotypes: [type1, type2];\n" +
             "}\n" +
             "\n" +
             "Profile meta::pure::profiles::doc\n" +
             "{\n" +
             "  tags: [doc];\n" +
             "}\n");
    }

    @Test
    public void testMultipleStereotypesAndTaggedValues()
    {
        test("###Relational\n" +
             "Database app::db\n" +
             "(\n" +
             "  Schema <<meta::pure::profiles::storeType.type1, meta::pure::profiles::storeType.type2>> {meta::pure::profiles::doc.doc = 'Schema doc', meta::pure::profiles::doc.owner = 'Data Team'} productSchema\n" +
             "  (\n" +
             "    Table <<meta::pure::profiles::storeType.type1>> {meta::pure::profiles::doc.doc = 'Product table'} Product\n" +
             "    (\n" +
             "      ProductID VARCHAR(30) PRIMARY KEY,\n" +
             "      Name <<meta::pure::profiles::storeType.type2>> {meta::pure::profiles::doc.doc = 'Product name'} VARCHAR(100)\n" +
             "    )\n" +
             "  )\n" +
             ")\n" +
             "\n" +
             "###Pure\n" +
             "Profile meta::pure::profiles::storeType\n" +
             "{\n" +
             "  stereotypes: [type1, type2];\n" +
             "}\n" +
             "\n" +
             "Profile meta::pure::profiles::doc\n" +
             "{\n" +
             "  tags: [doc, owner];\n" +
             "}\n");
    }
}
