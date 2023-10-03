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

package org.finos.legend.engine.language.stores.elasticsearch.v7.test;

import java.io.Closeable;
import java.util.Collections;
import java.util.List;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.data.DataElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.Store;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.runtime.Elasticsearch7StoreConnection;
import org.finos.legend.engine.testable.connection.TestConnectionBuilder;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.testcontainers.DockerClientFactory;

public class TestElasticsearchV7TestConnectionBuilder
{
    private static final String GRAMMAR =
            "###Elasticsearch\n" +
                    "Elasticsearch7Cluster abc::abc::Store\n" +
                    "{\n" +
                    "  indices: [\n" +
                    "    hello_index: {\n" +
                    "      properties: [\n" +
                    "        a: Keyword,\n" +
                    "        b: Keyword\n" +
                    "      ];\n" +
                    "    }\n" +
                    "  ];\n" +
                    "}\n" +
                    "###Connection\n" +
                    "Elasticsearch7ClusterConnection abc::abc::Connection\n" +
                    "{\n" +
                    "  store: test::esStore;\n" +
                    "  clusterDetails: # URL { http://dummyurl.com:1234/api }#;\n" +
                    "  authentication: # UserPassword {\n" +
                    "    username: 'hello_user';\n" +
                    "    password: SystemPropertiesSecret\n" +
                    "    {\n" +
                    "      systemPropertyName: 'sys.prop.name';\n" +
                    "    };\n" +
                    "  }#;\n" +
                    "}\n" +
                    "###Data\n" +
                    "Data meta::data::MyData\n" +
                    "{\n" +
                    "  Elasticsearch\n" +
                    "  #{\n" +
                    "    hello_index:\n" +
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
                    "  }#\n" +
                    "}\n";

    private static final PureModelContextData data = PureGrammarParser.newInstance().parseModel(GRAMMAR);

    @AfterClass
    public static void afterClass()
    {
        ElasticsearchV7ContainerManager.INSTANCE.tearDown();
    }

    @Test
    public void testTestConnectionBuilder()
    {
        DockerClientFactory instance = DockerClientFactory.instance();
        Assume.assumeTrue(instance.isDockerAvailable());

        String dockerHostIpAddress = instance.dockerHostIpAddress();

        EmbeddedData embeddedData = ListIterate.detect(data.getElementsOfType(DataElement.class), d -> d.getPath().equals("meta::data::MyData")).data;
        Store esStore = ListIterate.detect(data.getElementsOfType(Store.class), d -> d.getPath().equals("abc::abc::Store"));
        TestConnectionBuilder testConnectionBuilder = new TestConnectionBuilder(esStore, embeddedData, data);
        PackageableConnection connection = ListIterate.detect(data.getElementsOfType(PackageableConnection.class), ele -> "abc::abc::Connection".equals(ele.getPath()));

        List<Closeable> closeables = Collections.emptyList();
        try
        {
            Pair<Connection, List<Closeable>> testConnectionWithCloseables = connection.connectionValue.accept(testConnectionBuilder);
            closeables = testConnectionWithCloseables.getTwo();

            Assert.assertTrue(testConnectionWithCloseables.getOne() instanceof Elasticsearch7StoreConnection);
            Assert.assertEquals(1, closeables.size());

            Elasticsearch7StoreConnection testServiceStoreConnection = (Elasticsearch7StoreConnection) testConnectionWithCloseables.getOne();

            Assert.assertEquals("abc::abc::Store", testServiceStoreConnection.element);
            Assert.assertTrue(testServiceStoreConnection.sourceSpec.url.toString().startsWith("http://" + dockerHostIpAddress + ":"));

        }
        finally
        {
            for (Closeable c : closeables)
            {
                try
                {
                    c.close();
                }
                catch (Exception ignore)
                {
                    // ignore close failure
                }
            }
        }
    }
}
