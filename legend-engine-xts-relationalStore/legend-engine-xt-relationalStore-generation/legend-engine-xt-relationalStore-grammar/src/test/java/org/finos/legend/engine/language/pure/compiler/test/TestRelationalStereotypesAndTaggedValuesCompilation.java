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

package org.finos.legend.engine.language.pure.compiler.test;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.TaggedValue;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Column;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Schema;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Table;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.View;
import org.junit.Assert;
import org.junit.Test;

public class TestRelationalStereotypesAndTaggedValuesCompilation extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{
    @Test
    public void testDatabaseTaggedValues()
    {
        PureModel model = test("###Pure\n" +
                "Profile meta::pure::profiles::doc\n" +
                "{\n" +
                "    tags: [doc, owner];\n" +
                "}\n" +
                "\n" +
                "###Relational\n" +
                "Database {meta::pure::profiles::doc.doc = 'Database documentation', meta::pure::profiles::doc.owner = 'Data Team'} app::db\n" +
                "(\n" +
                "  Table Product\n" +
                "  (\n" +
                "    ProductID VARCHAR(30) PRIMARY KEY\n" +
                "  )\n" +
                ")\n").getTwo();

        Database database = (Database) model.getStore("app::db");
        Assert.assertNotNull(database);
        
        MutableList<? extends TaggedValue> taggedValues = database._taggedValues().toList();
        Assert.assertEquals(2, taggedValues.size());
        
        TaggedValue docTaggedValue = ListIterate.detect(taggedValues, tv -> "doc".equals(tv._tag()._name()));
        Assert.assertNotNull(docTaggedValue);
        Assert.assertEquals("Database documentation", docTaggedValue._value());
        
        TaggedValue ownerTaggedValue = ListIterate.detect(taggedValues, tv -> "owner".equals(tv._tag()._name()));
        Assert.assertNotNull(ownerTaggedValue);
        Assert.assertEquals("Data Team", ownerTaggedValue._value());
    }

    @Test
    public void testSchemaStereotypesAndTaggedValues()
    {
        PureModel model = test("###Pure\n" +
                "Profile meta::pure::profiles::storeType\n" +
                "{\n" +
                "    stereotypes: [type1, type2];\n" +
                "}\n" +
                "\n" +
                "Profile meta::pure::profiles::doc\n" +
                "{\n" +
                "    tags: [doc];\n" +
                "}\n" +
                "\n" +
                "###Relational\n" +
                "Database app::db\n" +
                "(\n" +
                "  Schema <<meta::pure::profiles::storeType.type2>> {meta::pure::profiles::doc.doc = 'Schema documentation'} productSchema\n" +
                "  (\n" +
                "    Table Product\n" +
                "    (\n" +
                "      ProductID VARCHAR(30) PRIMARY KEY\n" +
                "    )\n" +
                "  )\n" +
                ")\n").getTwo();

        Database database = (Database) model.getStore("app::db");
        Assert.assertNotNull(database);
        
        Schema schema = ListIterate.detect(database._schemas().toList(), s -> "productSchema".equals(s._name()));
        Assert.assertNotNull(schema);
        
        Assert.assertEquals(1, schema._stereotypes().size());
        Assert.assertEquals("type2", schema._stereotypes().getFirst()._value());
        
        Assert.assertEquals(1, schema._taggedValues().size());
        TaggedValue docTaggedValue = schema._taggedValues().getFirst();
        Assert.assertEquals("doc", docTaggedValue._tag()._name());
        Assert.assertEquals("Schema documentation", docTaggedValue._value());
    }

    @Test
    public void testTableStereotypesAndTaggedValues()
    {
        PureModel model = test("###Pure\n" +
                "Profile meta::pure::profiles::storeType\n" +
                "{\n" +
                "    stereotypes: [type1, type2];\n" +
                "}\n" +
                "\n" +
                "Profile meta::pure::profiles::doc\n" +
                "{\n" +
                "    tags: [doc];\n" +
                "}\n" +
                "\n" +
                "###Relational\n" +
                "Database app::db\n" +
                "(\n" +
                "  Table <<meta::pure::profiles::storeType.type1>> {meta::pure::profiles::doc.doc = 'Table documentation'} Product\n" +
                "  (\n" +
                "    ProductID VARCHAR(30) PRIMARY KEY\n" +
                "  )\n" +
                ")\n").getTwo();

        Database database = (Database) model.getStore("app::db");
        Assert.assertNotNull(database);
        
        Table table = (Table) ListIterate.detect(database._schemas().getFirst()._tables().toList(), t -> "Product".equals(t._name()));
        Assert.assertNotNull(table);
        
        Assert.assertEquals(1, table._stereotypes().size());
        Assert.assertEquals("type1", table._stereotypes().getFirst()._value());
        
        Assert.assertEquals(1, table._taggedValues().size());
        TaggedValue docTaggedValue = table._taggedValues().getFirst();
        Assert.assertEquals("doc", docTaggedValue._tag()._name());
        Assert.assertEquals("Table documentation", docTaggedValue._value());
    }

    @Test
    public void testViewStereotypesAndTaggedValues()
    {
        PureModel model = test("###Pure\n" +
                "Profile meta::pure::profiles::storeType\n" +
                "{\n" +
                "    stereotypes: [type1, type2];\n" +
                "}\n" +
                "\n" +
                "Profile meta::pure::profiles::doc\n" +
                "{\n" +
                "    tags: [doc];\n" +
                "}\n" +
                "\n" +
                "###Relational\n" +
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
                ")\n").getTwo();

        Database database = (Database) model.getStore("app::db");
        Assert.assertNotNull(database);
        
        Schema schema = ListIterate.detect(database._schemas().toList(), s -> "productSchema".equals(s._name()));
        Assert.assertNotNull(schema);
        
        View view = (View) ListIterate.detect(schema._tables().toList(), t -> "ProductView".equals(t._name()));
        Assert.assertNotNull(view);
        
        Assert.assertEquals(1, view._stereotypes().size());
        Assert.assertEquals("type1", view._stereotypes().getFirst()._value());
        
        Assert.assertEquals(1, view._taggedValues().size());
        TaggedValue docTaggedValue = view._taggedValues().getFirst();
        Assert.assertEquals("doc", docTaggedValue._tag()._name());
        Assert.assertEquals("Product view", docTaggedValue._value());
    }

    @Test
    public void testColumnStereotypesAndTaggedValues()
    {
        PureModel model = test("###Pure\n" +
                "Profile meta::pure::profiles::storeType\n" +
                "{\n" +
                "    stereotypes: [type1, type2];\n" +
                "}\n" +
                "\n" +
                "Profile meta::pure::profiles::doc\n" +
                "{\n" +
                "    tags: [doc];\n" +
                "}\n" +
                "\n" +
                "###Relational\n" +
                "Database app::db\n" +
                "(\n" +
                "  Table Product\n" +
                "  (\n" +
                "    ProductID VARCHAR(30) PRIMARY KEY,\n" +
                "    Name <<meta::pure::profiles::storeType.type1>> {meta::pure::profiles::doc.doc = 'Product name'} VARCHAR(100)\n" +
                "  )\n" +
                ")\n").getTwo();

        Database database = (Database) model.getStore("app::db");
        Assert.assertNotNull(database);
        
        Table table = (Table) ListIterate.detect(database._schemas().getFirst()._tables().toList(), t -> "Product".equals(t._name()));
        Assert.assertNotNull(table);
        
        Column column = ListIterate.detect(table._columns().toList(), c -> "Name".equals(c._name()));
        Assert.assertNotNull(column);
        
        Assert.assertEquals(1, column._stereotypes().size());
        Assert.assertEquals("type1", column._stereotypes().getFirst()._value());
        
        Assert.assertEquals(1, column._taggedValues().size());
        TaggedValue docTaggedValue = column._taggedValues().getFirst();
        Assert.assertEquals("doc", docTaggedValue._tag()._name());
        Assert.assertEquals("Product name", docTaggedValue._value());
    }

    @Test
    public void testMultipleStereotypesAndTaggedValues()
    {
        PureModel model = test("###Pure\n" +
                "Profile meta::pure::profiles::storeType\n" +
                "{\n" +
                "    stereotypes: [type1, type2];\n" +
                "}\n" +
                "\n" +
                "Profile meta::pure::profiles::doc\n" +
                "{\n" +
                "    tags: [doc, owner];\n" +
                "}\n" +
                "\n" +
                "###Relational\n" +
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
                ")\n").getTwo();

        Database database = (Database) model.getStore("app::db");
        Assert.assertNotNull(database);
        
        Schema schema = ListIterate.detect(database._schemas().toList(), s -> "productSchema".equals(s._name()));
        Assert.assertNotNull(schema);
        
        // Check schema stereotypes
        Assert.assertEquals(2, schema._stereotypes().size());
        MutableList<String> schemaStereotypeValues = Lists.mutable.empty();
        schema._stereotypes().forEach(s -> schemaStereotypeValues.add(s._value()));
        Assert.assertTrue(schemaStereotypeValues.contains("type1"));
        Assert.assertTrue(schemaStereotypeValues.contains("type2"));
        
        // Check schema tagged values
        Assert.assertEquals(2, schema._taggedValues().size());
        TaggedValue schemaDocTaggedValue = ListIterate.detect(schema._taggedValues().toList(), tv -> "doc".equals(tv._tag()._name()));
        Assert.assertNotNull(schemaDocTaggedValue);
        Assert.assertEquals("Schema doc", schemaDocTaggedValue._value());
        
        TaggedValue schemaOwnerTaggedValue = ListIterate.detect(schema._taggedValues().toList(), tv -> "owner".equals(tv._tag()._name()));
        Assert.assertNotNull(schemaOwnerTaggedValue);
        Assert.assertEquals("Data Team", schemaOwnerTaggedValue._value());
        
        // Check table
        Table table = (Table) ListIterate.detect(schema._tables().toList(), t -> "Product".equals(t._name()));
        Assert.assertNotNull(table);
        
        Assert.assertEquals(1, table._stereotypes().size());
        Assert.assertEquals("type1", table._stereotypes().getFirst()._value());
        
        Assert.assertEquals(1, table._taggedValues().size());
        TaggedValue tableDocTaggedValue = table._taggedValues().getFirst();
        Assert.assertEquals("doc", tableDocTaggedValue._tag()._name());
        Assert.assertEquals("Product table", tableDocTaggedValue._value());
        
        // Check column
        Column column = ListIterate.detect(table._columns().toList(), c -> "Name".equals(c._name()));
        Assert.assertNotNull(column);
        
        Assert.assertEquals(1, column._stereotypes().size());
        Assert.assertEquals("type2", column._stereotypes().getFirst()._value());
        
        Assert.assertEquals(1, column._taggedValues().size());
        TaggedValue columnDocTaggedValue = column._taggedValues().getFirst();
        Assert.assertEquals("doc", columnDocTaggedValue._tag()._name());
        Assert.assertEquals("Product name", columnDocTaggedValue._value());
    }

    @Test
    public void testInvalidStereotype()
    {
        test("###Pure\n" +
                "Profile meta::pure::profiles::storeType\n" +
                "{\n" +
                "    stereotypes: [type1, type2];\n" +
                "}\n" +
                "\n" +
                "###Relational\n" +
                "Database app::db\n" +
                "(\n" +
                "  Table <<meta::pure::profiles::storeType.type3>> Product\n" +
                "  (\n" +
                "    ProductID VARCHAR(30) PRIMARY KEY\n" +
                "  )\n" +
                ")\n",
                "COMPILATION error at [10:44-49]: Can't find stereotype 'type3' in profile 'meta::pure::profiles::storeType'"
        );
    }

    @Test
    public void testInvalidTaggedValue()
    {
        test("###Pure\n" +
                "Profile meta::pure::profiles::doc\n" +
                "{\n" +
                "    tags: [doc];\n" +
                "}\n" +
                "\n" +
                "###Relational\n" +
                "Database app::db\n" +
                "(\n" +
                "  Table {meta::pure::profiles::doc.owner = 'Data Team'} Product\n" +
                "  (\n" +
                "    ProductID VARCHAR(30) PRIMARY KEY\n" +
                "  )\n" +
                ")\n",
                "COMPILATION error at [10:31-36]: Can't find tag 'owner' in profile 'meta::pure::profiles::doc'"
        );
    }

    @Test
    public void testInvalidProfile()
    {
        test("###Pure\n" +
                "Profile meta::pure::profiles::doc\n" +
                "{\n" +
                "    tags: [doc];\n" +
                "}\n" +
                "\n" +
                "###Relational\n" +
                "Database app::db\n" +
                "(\n" +
                "  Table {meta::pure::profiles::invalid.doc = 'Table documentation'} Product\n" +
                "  (\n" +
                "    ProductID VARCHAR(30) PRIMARY KEY\n" +
                "  )\n" +
                ")\n",
                "COMPILATION error at [10:10-37]: Can't find profile 'meta::pure::profiles::invalid'"
        );
    }
}
