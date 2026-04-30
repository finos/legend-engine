// Copyright 2025 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.language.deephaven.compiler;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_store_Column;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_store_DeephavenStore;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_store_Table;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_BooleanType;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_FloatType;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_IntType;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_StringType;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_DateTimeType;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class TestDeephavenCompiler extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{
    private static final String BASIC_STORE = "###Deephaven\n" +
            "import abc::abc::*;\n" +
            "Deephaven test::Store::foo\n" +
            "(\n" +
            "    Table xyz\n" +
            "    (\n" +
            "        prop1: STRING,\n" +
            "        prop2: INT,\n" +
            "        prop3: BOOLEAN,\n" +
            "        prop4: DATETIME,\n" +
            "        prop5: FLOAT\n" +
            "    )\n" +
            ")\n\n";

    private static final String BASIC_CONN = "###Connection\n" +
            "DeephavenConnection test::DeephavenConnection\n" +
            "{\n" +
            "    store: test::DeephavenStore;\n" +
            "    serverUrl: 'http://dummyurl.com:12345'\n" +
            "    authentication: # PSK {\n" +
            "        psk: 'abcde';\n" +
            "    }#;\n" +
            "}";

    @Override
    protected String getDuplicatedElementTestCode()
    {
        return BASIC_STORE + BASIC_STORE;
    }

    @Override
    protected String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [17:1-27:1]: Duplicated element 'test::Store::foo'";
    }

    @Test
    public void testCompileStore() throws Exception
    {
        Pair<PureModelContextData, PureModel> result = test(BASIC_STORE);

        Store store = result.getTwo().getStore("test::Store::foo");
        Assert.assertTrue(store instanceof Root_meta_external_store_deephaven_metamodel_store_DeephavenStore);
        RichIterable<? extends Root_meta_external_store_deephaven_metamodel_store_Table> tables = ((Root_meta_external_store_deephaven_metamodel_store_DeephavenStore) store)._tables();
        Assert.assertEquals(1, tables.size());
        RichIterable<? extends Root_meta_external_store_deephaven_metamodel_store_Column> columns = tables.getAny()._columns();
        Assert.assertEquals(5, columns.size());

        Map<String, Class<? extends Root_meta_external_store_deephaven_metamodel_type_Type>> actualCols = new HashMap<String, Class<? extends Root_meta_external_store_deephaven_metamodel_type_Type>>();
        columns.forEach(x -> actualCols.put(x._name(), x._type().getClass()));

        Assert.assertTrue(Root_meta_external_store_deephaven_metamodel_type_StringType.class.isAssignableFrom(actualCols.get("prop1")));
        Assert.assertTrue(Root_meta_external_store_deephaven_metamodel_type_IntType.class.isAssignableFrom(actualCols.get("prop2")));
        Assert.assertTrue(Root_meta_external_store_deephaven_metamodel_type_BooleanType.class.isAssignableFrom(actualCols.get("prop3")));
        Assert.assertTrue(Root_meta_external_store_deephaven_metamodel_type_DateTimeType.class.isAssignableFrom(actualCols.get("prop4")));
        Assert.assertTrue(Root_meta_external_store_deephaven_metamodel_type_FloatType.class.isAssignableFrom(actualCols.get("prop5")));
    }

    @Test
    public void testCompileConnection()
    {
        Pair<PureModelContextData, PureModel> result = test(BASIC_CONN);
    }

    @Test
    public void testFullCompileStore()
    {
        test(BASIC_STORE);
    }

    @Test
    public void testCompileDeephavenApp()
    {
        String grammar = BASIC_STORE +
                "DeephavenApp test::MyDeephavenApp\n" +
                "{\n" +
                "    applicationName: 'TestApp';\n" +
                "    function: test::myFunc():Any[*];\n" +
                "    description: 'Test description';\n" +
                "    ownership: Deployment { identifier: 'owner1' };\n" +
                "}\n\n" +
                "###Pure\n" +
                "function test::myFunc(): Any[*]\n" +
                "{\n" +
                "    #>{test::Store::foo.xyz}#->select(~[prop1, prop2])->from(test::DeephavenRuntime)\n" +
                "}\n\n" +
                "###Connection\n" +
                "DeephavenConnection test::DeephavenConnection\n" +
                "{\n" +
                "    store: test::Store::foo;\n" +
                "    serverUrl: 'http://localhost:10000'\n" +
                "    authentication: # PSK {\n" +
                "        psk: 'testPSK';\n" +
                "    }#;\n" +
                "}\n\n" +
                "###Runtime\n" +
                "Runtime test::DeephavenRuntime\n" +
                "{\n" +
                "    mappings:\n" +
                "    [\n" +
                "    ];\n" +
                "    connections:\n" +
                "    [\n" +
                "        test::Store::foo:\n" +
                "        [\n" +
                "            connection: test::DeephavenConnection\n" +
                "        ]\n" +
                "    ];\n" +
                "}\n";

        Pair<PureModelContextData, PureModel> result = test(grammar, null, Lists.fixedSize.with("COMPILATION warning at [40:1-52:1]: Runtime must cover at least one mapping"));
        Assert.assertNotNull(result.getTwo().getPackageableElement("test::MyDeephavenApp"));
    }
}
