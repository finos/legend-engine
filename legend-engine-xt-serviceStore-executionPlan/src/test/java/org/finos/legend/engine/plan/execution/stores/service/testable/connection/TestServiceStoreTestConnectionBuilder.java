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

package org.finos.legend.engine.plan.execution.stores.service.testable.connection;

import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.data.DataElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.connection.ServiceStoreConnection;
import org.finos.legend.engine.testable.connection.TestConnectionBuilder;
import org.junit.Assert;
import org.junit.Test;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

public class TestServiceStoreTestConnectionBuilder
{
    private static final String GRAMMAR = "###Connection\n" +
            "ServiceStoreConnection demo::serviceStoreConnection\n" +
            "{\n" +
            "  store: test::serviceStore;\n" +
            "  baseUrl: 'http://baseUrl';\n" +
            "}\n" +
            "\n\n" +
            "###Data\n" +
            "Data demo::ServiceStoreData\n" +
            "{\n" +
            "  ServiceStore\n" +
            "  #{\n" +
            "    [\n" +
            "      {\n" +
            "        request:\n" +
            "        {\n" +
            "          method: GET;\n" +
            "          url: '/employees';\n" +
            "        };\n" +
            "        response:\n" +
            "        {\n" +
            "          body:\n" +
            "            ExternalFormat\n" +
            "            #{\n" +
            "              contentType: 'application/json';\n" +
            "              data: '[\\n" +
            "                       {\\n" +
            "                           \"firstName\": \"FirstName A\",\\n" +
            "                           \"lastName\": \"LastName A\",\\n" +
            "                           \"firmId\": \"A\"\\n" +
            "                       },\\n" +
            "                       {\\n" +
            "                           \"firstName\": \"FirstName B\",\\n" +
            "                           \"lastName\": \"LastName B\",\\n" +
            "                           \"firmId\": \"B\"\\n" +
            "                       }\\n" +
            "                     ]\\n';\n" +
            "            }#;\n" +
            "        };\n" +
            "      }\n" +
            "    ]\n" +
            "  }#\n" +
            "}\n";

    private static final PureModelContextData data = PureGrammarParser.newInstance().parseModel(GRAMMAR);

    @Test
    public void testServiceStoreTestConnectionBuilder() throws IOException
    {
        EmbeddedData embeddedData = ListIterate.detect(data.getElementsOfType(DataElement.class), d -> d.getPath().equals("demo::ServiceStoreData")).data;
        TestConnectionBuilder testConnectionBuilder = new TestConnectionBuilder(embeddedData, data);
        PackageableConnection serviceStoreConnection = ListIterate.detect(data.getElementsOfType(PackageableConnection.class), ele -> "demo::serviceStoreConnection".equals(ele.getPath()));

        Pair<Connection, List<Closeable>> testConnectionWithCloseables = serviceStoreConnection.connectionValue.accept(testConnectionBuilder);

        Assert.assertTrue(testConnectionWithCloseables.getOne() instanceof ServiceStoreConnection);
        Assert.assertEquals(1, testConnectionWithCloseables.getTwo().size());

        ServiceStoreConnection testServiceStoreConnection = (ServiceStoreConnection) testConnectionWithCloseables.getOne();

        Assert.assertEquals("test::serviceStore", testServiceStoreConnection.element);
        Assert.assertTrue(testServiceStoreConnection.baseUrl.startsWith("http://127.0.0.1:"));

        testConnectionWithCloseables.getTwo().get(0).close();
    }
}
