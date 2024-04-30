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
//

package org.finos.legend.engine.language.stores.elasticsearch.v7.compiler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.core_elasticsearch_seven_metamodel_extensions_store_contract;
import org.finos.legend.pure.generated.core_pure_protocol_vX_X_X_scan_buildBasePureModel;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;
import org.junit.Assert;
import org.junit.Test;

public class TestElasticsearchCompiler extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{
    private static final String BASIC_STORE = "###Elasticsearch\n" +
            "Elasticsearch7Cluster abc::abc::Store\n" +
            "{\n" +
            "  indices: [\n" +
            "    index1: {\n" +
            "      properties: [\n" +
            "        prop1: Keyword,\n" +
            "        prop2: Text,\n" +
            "        prop3: Date,\n" +
            "        prop4: Short,\n" +
            "        prop5: Byte,\n" +
            "        prop6: Integer,\n" +
            "        prop7: Long,\n" +
            "        prop8: Float,\n" +
            "        prop9: HalfFloat,\n" +
            "        prop10: Double,\n" +
            "        prop11: Boolean,\n" +
            "        prop12: Text {\n" +
            "          fields: [\n" +
            "            field1: Keyword\n" +
            "          ];\n" +
            "        },\n" +
            "        prop13: Object {\n" +
            "          properties: [\n" +
            "            prop1: Object {\n" +
            "              properties: [\n" +
            "                prop1: Keyword\n" +
            "              ];\n" +
            "            }\n" +
            "          ];\n" +
            "        },\n" +
            "        prop14: Nested {\n" +
            "          properties: [\n" +
            "            prop1: Nested {\n" +
            "              properties: [\n" +
            "                prop1: Keyword\n" +
            "              ];\n" +
            "            }\n" +
            "          ];\n" +
            "        }\n" +
            "      ];\n" +
            "    }\n" +
            "  ];\n" +
            "}\n\n";

    private static final String BASIC_CONNECTION = "###Connection\n" +
            "Elasticsearch7ClusterConnection abc::abc::Connection\n" +
            "{\n" +
            "  store: abc::abc::Store;\n" +
            "  clusterDetails: # URL { http://dummyurl.com:1234/api }#;\n" +
            "  authentication: # UserPassword {\n" +
            "    username: 'hello_user';\n" +
            "    password: SystemPropertiesSecret\n" +
            "    {\n" +
            "      systemPropertyName: 'sys.prop.name';\n" +
            "    };\n" +
            "  }#;\n" +
            "}\n";

    @Override
    protected String getDuplicatedElementTestCode()
    {
        return BASIC_STORE +
                "\n" +
                BASIC_STORE;
    }

    @Override
    protected String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [48:1-90:1]: Duplicated element 'abc::abc::Store'";
    }

    @Test
    public void testCompileStore() throws Exception
    {
        PureModel pureModel = test(BASIC_STORE).getTwo();
        Store store = pureModel.getStore("abc::abc::Store");

        Root_meta_pure_extension_Extension extension = core_elasticsearch_seven_metamodel_extensions_store_contract.Root_meta_external_store_elasticsearch_v7_extension_elasticsearchV7Extension__Extension_1_(pureModel.getExecutionSupport());
        String pmcdJson = core_pure_protocol_vX_X_X_scan_buildBasePureModel.Root_meta_protocols_pure_vX_X_X_transformation_fromPureGraph_buildBasePureModelFromStoreStr_String_1__Extension_MANY__String_1_("abc::abc::Store", Lists.fixedSize.of(extension), pureModel.getExecutionSupport());

        ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
        PureModelContextData pmcd = objectMapper.readValue(pmcdJson, PureModelContextData.class);

        PureGrammarComposer grammarTransformer = PureGrammarComposer.newInstance(PureGrammarComposerContext.Builder.newInstance().build());
        Assert.assertEquals(BASIC_STORE, grammarTransformer.renderPureModelContextData(pmcd));
    }

    @Test
    public void testCompileConnection()
    {
        test(BASIC_STORE + BASIC_CONNECTION);
    }

    @Test
    public void testCompileIndexToTdsFunctionHandler()
    {
        test(BASIC_STORE + "###Pure\n" +
                "function abc::abc::indexToTdsFunction(): TabularDataSet[1] {\n" +
                "  indexToTDS(abc::abc::Store, 'index1')" +
                "}\n"
        );
    }
}
