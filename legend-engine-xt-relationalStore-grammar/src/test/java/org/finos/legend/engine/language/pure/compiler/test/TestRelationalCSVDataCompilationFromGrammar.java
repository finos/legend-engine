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

package org.finos.legend.engine.language.pure.compiler.test;

import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_ConnectionTestData;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_Service;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_ServiceTest;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_ServiceTestSuite;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_TestData;
import org.finos.legend.pure.generated.Root_meta_pure_data_DataElement;
import org.finos.legend.pure.generated.Root_meta_pure_data_DataElementReference;
import org.finos.legend.pure.generated.Root_meta_pure_data_EmbeddedData;
import org.finos.legend.pure.generated.Root_meta_pure_test_AtomicTest;
import org.finos.legend.pure.generated.Root_meta_pure_test_Test;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_data_RelationalCSVData;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_data_RelationalCSVTable;
import org.finos.legend.pure.generated.Root_meta_pure_test_assertion_TestAssertion;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.junit.Assert;
import org.junit.Test;

import static org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test;

public class TestRelationalCSVDataCompilationFromGrammar
{
    @Test
    public void testRelationalCompilationFromGrammar()
    {
        Pair<PureModelContextData, PureModel> result  =
                test(
                        "###Data\n" +
                                "Data my::RelationalData\n" +
                                "{\n" +
                                "  Relational\n" +
                                "  #{\n" +
                                "    mySchema.MyTable:\n" +
                                "          'id,firstName,lastName,age\\n'+\n" +
                                "          '1,John,Doe\\n'+\n" +
                                "          '2,Nicole,Smith\\n'+\n" +
                                "          '3,Nick,Smith\\n';\n" +
                                "\n" +
                                "    mySchema.MyTable2:\n" +
                                "          'id,name\\n'+\n" +
                                "          '1,John\\n'+\n" +
                                "          '2,Jack\\n';\n" +
                                "  }#\n" +
                                "}\n"
                );

        // Main element
        PackageableElement element = result.getTwo().getPackageableElement("my::RelationalData");
        Assert.assertTrue(element instanceof Root_meta_pure_data_DataElement);
        Root_meta_pure_data_DataElement dataElement = (Root_meta_pure_data_DataElement) element;

        // embedded data
        Assert.assertTrue(dataElement._data() instanceof Root_meta_relational_metamodel_data_RelationalCSVData);
        Root_meta_relational_metamodel_data_RelationalCSVData relationalData = (Root_meta_relational_metamodel_data_RelationalCSVData) dataElement._data();
        Assert.assertEquals(2, relationalData._tables().size());

        // Table 1
        Root_meta_relational_metamodel_data_RelationalCSVTable t1 = relationalData._tables().select(t -> t._table().equals("MyTable")).getOnly();
        Assert.assertEquals(t1._table(), "MyTable");
        Assert.assertEquals(t1._schema(), "mySchema");
        Assert.assertEquals(t1._values(), "id,firstName,lastName,age\n" + "1,John,Doe\n" + "2,Nicole,Smith\n" + "3,Nick,Smith\n");
        // Table 2
        Root_meta_relational_metamodel_data_RelationalCSVTable t2 = relationalData._tables().select(t -> t._table().equals("MyTable2")).getOnly();
        Assert.assertEquals(t2._table(), "MyTable2");
        Assert.assertEquals(t2._schema(), "mySchema");
        Assert.assertEquals(t2._values(), "id,name\n" + "1,John\n" + "2,Jack\n");
    }

    @Test
    public void testFaultyRelationalCompilationFromGrammar()
    {
            // duplicate tables
                    test(

                        "###Data\n" +
                            "Data my::RelationalData\n" +
                            "{\n" +
                            "  Relational\n" +
                            "  #{\n" +
                            "    mySchema.MyTable:\n" +
                            "          'id,firstName,lastName,age\\n'+\n" +
                            "          '1,John,Doe\\n'+\n" +
                            "          '2,Nicole,Smith\\n'+\n" +
                            "          '3,Nick,Smith\\n';\n" +
                            "\n" +
                            "    mySchema.MyTable:\n" +
                            "          'id,name\\n'+\n" +
                            "          '1,John\\n'+\n" +
                            "          '2,Jack\\n';\n" +
                            "  }#\n" +
                            "}\n",
                        "COMPILATION error at [12:5-15:21]: Duplicated table name: 'mySchema.MyTable'"
                );
    }


    public final String SERVICE_RELATIONAL_TEST_COMPILE_MODEL =
         "###Data\n" +
                 "Data my::RelationalData\n" +
                 "{\n" +
                 "  Relational\n" +
                 "  #{\n" +
                 "       mySchema.MyTable:\n" +
                 "          'id,firstName,lastName,age\\n'+\n" +
                 "          '1,John,Doe\\n'+\n" +
                 "          '2,Nicole,Smith\\n'+\n" +
                 "          '3,Nick,Smith\\n';\n" +
                 "  }#\n" +
                 "}\n" +
                 "\n" +
                 "###Relational\n" +
                 "Database store::TestDB\n" +
                 "(\n" +
                 "  Table PersonTable\n" +
                 "  (\n" +
                 "    id INTEGER PRIMARY KEY,\n" +
                 "    firm_id INTEGER,\n" +
                 "    firstName VARCHAR(200),\n" +
                 "    lastName VARCHAR(200)\n" +
                 "  )\n" +
                 ")\n" +
                 "\n" +
                 "\n" +
                 "###Pure\n" +
                 "Class model::Person\n" +
                 "{\n" +
                 "  firstName: String[1];\n" +
                 "  lastName: String[1];\n" +
                 "}\n" +
                 "\n" +
                 "\n" +
                 "###Mapping\n" +
                 "Mapping mapping::RelationalMapping\n" +
                 "(\n" +
                 "  *model::Person: Relational\n" +
                 "  {\n" +
                 "    ~primaryKey\n" +
                 "    (\n" +
                 "      [store::TestDB]PersonTable.id\n" +
                 "    )\n" +
                 "    ~mainTable [store::TestDB]PersonTable\n" +
                 "    firstName: [store::TestDB]PersonTable.firstName,\n" +
                 "    lastName: [store::TestDB]PersonTable.lastName\n" +
                 "  }\n" +
                 ")\n" +
                 "\n" +
                 "\n" +
                 "###Connection\n" +
                 "RelationalDatabaseConnection model::MyConnection\n" +
                 "{\n" +
                 "  store: store::TestDB;\n" +
                 "  type: H2;\n" +
                 "  specification: LocalH2\n" +
                 "  {\n" +
                 "    testDataSetupSqls: [];\n" +
                 "  };\n" +
                 "  auth: DefaultH2;\n" +
                 "}\n" +
                 "\n" +
                 "\n" +
                 "###Runtime\n" +
                 "Runtime model::Runtime\n" +
                 "{\n" +
                 "  mappings:\n" +
                 "  [\n" +
                 "    mapping::RelationalMapping\n" +
                 "  ];\n" +
                 "  connections:\n" +
                 "  [\n" +
                 "    store::TestDB:\n" +
                 "    [\n" +
                 "      connection_1: model::MyConnection\n" +
                 "    ]\n" +
                 "  ];\n" +
                 "}\n";

}
