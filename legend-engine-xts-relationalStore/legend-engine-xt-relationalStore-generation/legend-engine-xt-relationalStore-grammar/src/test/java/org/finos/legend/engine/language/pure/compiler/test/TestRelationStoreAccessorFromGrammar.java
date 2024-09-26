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

package org.finos.legend.engine.language.pure.compiler.test;

import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.RelationTypeHelper;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.relationType.Column;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.junit.Assert;
import org.junit.Test;

public class TestRelationStoreAccessorFromGrammar extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{

    @Test
    public void testCompilationOfRelationStoreAccessor()
    {
        Pair<PureModelContextData, PureModel> pureModelPair = test("###Relational\n" +
                "Database my::Store" +
                "(" +
                "   Table myTable" +
                "   (" +
                "       id INT," +
                "       name VARCHAR(200)" +
                "   )" +
                ")\n" +
                "###Pure\n" +
                "function my::func():Relation[1]" +
                "{" +
                "   #>{my::Store.myTable}#->filter(c|$c.name == 'ok');" +
                "}");

        ConcreteFunctionDefinition<?> func = pureModelPair.getTwo().getConcreteFunctionDefinition("my::func__Relation_1_", null);
        FunctionType fType = (FunctionType) func._classifierGenericType()._typeArguments().getOnly()._rawType();
        List<Column> columns = RelationTypeHelper.convert((RelationType<?>) fType._returnType()._typeArguments().getAny()._rawType()).columns;
        Assert.assertEquals(Lists.mutable.with("id: Integer", "name: String"), columns.stream().map(x -> x.name + ": " + x.type).collect(Collectors.toList()));
    }

    @Test
    public void testCompilationOfRelationStoreAccessorWithTypes()
    {
        test("###Relational\n" +
                "Database test::myDB\n" +
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
                "        dec DECIMAL(18,6),\n" +
                "        r REAL,\n" +
                "        n NUMERIC(18,6)\n" +
                "    )\n" +
                ")\n" +
                "###Pure\n" +
                "function my::func():Any[*]" +
                "{" +
                "   #>{test::myDB.dataTable}#->filter(c|$c.pk == 1);" +
                "}");
    }

    @Test
    public void testCompilationWithSchema()
    {
        test("###Relational\n" +
                "Database my::Store\n" +
                "(\n" +
                "  Schema mySchema\n" +
                "  (\n" +
                "    Table myTable\n" +
                "    (\n" +
                "      id INTEGER,\n" +
                "      name VARCHAR(200)\n" +
                "    )\n" +
                "  )\n" +
                "\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Pure\n" +
                "function my::func(): Relation[1]\n" +
                "{\n" +
                "  #>{my::Store.mySchema.myTable}#->filter(\n" +
                "    c|$c.name == 'ok'\n" +
                "  )\n" +
                "}\n");
    }

    @Test
    public void testCompilationWithSchemaError()
    {
        test("###Relational\n" +
                "Database my::Store\n" +
                "(\n" +
                "  Schema mySchema\n" +
                "  (\n" +
                "    Table myTable\n" +
                "    (\n" +
                "      id INTEGER,\n" +
                "      name VARCHAR(200)\n" +
                "    )\n" +
                "  )\n" +
                "\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Pure\n" +
                "function my::func(): Relation[1]\n" +
                "{\n" +
                "  #>{my::Store.SchemaMissing.myTable}#->filter(\n" +
                "    c|$c.name == 'ok'\n" +
                "  )\n" +
                "}\n", "COMPILATION error at [19:3-38]: The schema SchemaMissing can't be found in the store Store");
    }

    @Test
    public void testCompilationOfRelationStoreAccessorUnknownTable()
    {
        try
        {
            test("###Relational\n" +
                    "Database my::Store" +
                    "(" +
                    "   Table myTable" +
                    "   (" +
                    "       id INT," +
                    "       name VARCHAR(200)" +
                    "   )" +
                    ")\n" +
                    "###Pure\n" +
                    "function my::func():Relation[1]" +
                    "{" +
                    "   #>{my::Store.myTabe}#->filter(c|$c.name == 'ok');" +
                    "}");
            Assert.fail();
        }
        catch (EngineException e)
        {
            Assert.assertEquals("COMPILATION error at [4:36-56]: The table myTabe can't be found in the store Store", e.toPretty());
        }
    }

    @Test
    public void testCompilationOfRelationStoreAccessorUnknownColumn()
    {
        try
        {
            test("###Relational\n" +
                    "Database my::Store" +
                    "(" +
                    "   Table myTable" +
                    "   (" +
                    "       id INT," +
                    "       name VARCHAR(200)" +
                    "   )" +
                    ")\n" +
                    "###Pure\n" +
                    "function my::func():Relation[1]" +
                    "{" +
                    "   #>{my::Store.myTable}#->filter(c|$c.naeme == 'ok');" +
                    "}");
            Assert.fail();
        }
        catch (EngineException e)
        {
            Assert.assertEquals("COMPILATION error at [4:72-76]: The column 'naeme' can't be found in the relation (id:Integer, name:String)", e.toPretty());
        }
    }


    @Test
    public void testCompilationErrorMissingTable()
    {
        try
        {
            test("###Relational\n" +
                    "Database my::Store" +
                    "(" +
                    "   Table myTable" +
                    "   (" +
                    "       id INT," +
                    "       name VARCHAR(200)" +
                    "   )" +
                    ")\n" +
                    "###Pure\n" +
                    "function my::func():Relation[1]" +
                    "{" +
                    "   #>{my::Store}#->filter(c|$c.naeme == 'ok');" +
                    "}");
            Assert.fail();
        }
        catch (EngineException e)
        {
            Assert.assertEquals("COMPILATION error at [4:36-49]: Error in the accessor definition. Please provide a table.", e.toPretty());
        }
    }

    @Override
    public String getDuplicatedElementTestCode()
    {
        return "Class anything::class {}\n" +
                "###Mapping\n" +
                "Mapping anything::somethingelse ()\n" +
                "###Relational\n" +
                "Database anything::somethingelse\n" +
                "(\n" +
                ")";
    }

    @Override
    public String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [5:1-7:1]: Duplicated element 'anything::somethingelse'";
    }
}
