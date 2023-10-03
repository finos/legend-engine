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

package org.finos.legend.engine.language.stores.elasticsearch.v7.compiler.data;

import java.util.List;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.pure.generated.Root_meta_external_store_elasticsearch_v7_metamodel_store_data_ElasticsearchV7EmbeddedData;
import org.finos.legend.pure.generated.Root_meta_external_store_elasticsearch_v7_metamodel_store_data_ElasticsearchV7IndexEmbeddedData;
import org.finos.legend.pure.generated.Root_meta_pure_data_DataElement;
import org.junit.Assert;
import org.junit.Test;

import static org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test;

public class TestElasticsearchV7EmbeddedDataCompiler
{
    @Test
    public void testCompile()
    {
        Pair<PureModelContextData, PureModel> pureModelPair = test("###Data\n" +
                "Data meta::data::MyData\n" +
                "{\n" +
                "  Elasticsearch\n" +
                "  #{\n" +
                "    helloIndex:\n" +
                "      [\n" +
                "        {\n" +
                "          \"_id\" : \"uuid1234\",\n" +
                "          \"a\" : \"hello\",\n" +
                "          \"b\" : \"bye\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"_id\" : \"uuid1235\",\n" +
                "          \"a\" : \"hello\",\n" +
                "          \"b\" : \"bye\"\n" +
                "        }\n" +
                "      ];\n" +
                "    helloIndex2:\n" +
                "      {\n" +
                "        \"_id\" : \"uuid1237\",\n" +
                "        \"a\" : \"hello\",\n" +
                "        \"b\" : \"bye\"\n" +
                "      };\n" +
                "  }#\n" +
                "}\n"
        );

        Root_meta_pure_data_DataElement dataElement = (Root_meta_pure_data_DataElement) pureModelPair.getTwo().getPackageableElement("meta::data::MyData");
        Root_meta_external_store_elasticsearch_v7_metamodel_store_data_ElasticsearchV7EmbeddedData esData = (Root_meta_external_store_elasticsearch_v7_metamodel_store_data_ElasticsearchV7EmbeddedData) dataElement._data();
        List<? extends Root_meta_external_store_elasticsearch_v7_metamodel_store_data_ElasticsearchV7IndexEmbeddedData> iData = esData._indexData().toList();
        Assert.assertEquals(2, iData.size());
        Root_meta_external_store_elasticsearch_v7_metamodel_store_data_ElasticsearchV7IndexEmbeddedData iData1 = iData.get(0);
        Assert.assertEquals("helloIndex", iData1._index());
        Assert.assertEquals("[{\"_id\":\"uuid1234\",\"a\":\"hello\",\"b\":\"bye\"},{\"_id\":\"uuid1235\",\"a\":\"hello\",\"b\":\"bye\"}]", iData1._documentsAsJson());
        Root_meta_external_store_elasticsearch_v7_metamodel_store_data_ElasticsearchV7IndexEmbeddedData iData2 = iData.get(1);
        Assert.assertEquals("helloIndex2", iData2._index());
        Assert.assertEquals("{\"_id\":\"uuid1237\",\"a\":\"hello\",\"b\":\"bye\"}", iData2._documentsAsJson());
    }
}
