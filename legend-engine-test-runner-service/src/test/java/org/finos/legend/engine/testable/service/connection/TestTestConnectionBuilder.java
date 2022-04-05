package org.finos.legend.engine.testable.service.connection;

import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.ConnectionPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.data.DataElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.JsonModelConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.ModelChainConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.XmlModelConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.connection.ServiceStoreConnection;
import org.junit.Assert;
import org.junit.Test;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

public class TestTestConnectionBuilder
{
    private static final String GRAMMAR = "###Connection\n" +
            "ModelChainConnection demo::modelChainConnection\n" +
            "{\n" +
            "  mappings: [\n" +
            "    demo::modelChainConnection::mapping\n" +
            "  ];\n" +
            "}\n" +
            "\n" +
            "JsonModelConnection demo::jsonModelConnection\n" +
            "{\n" +
            "  class : anything::class;\n" +
            "  url : 'executor:default';\n" +
            "}\n" +
            "\n" +
            "XmlModelConnection demo::xmlModelConnection\n" +
            "{\n" +
            "  class : anything::class;\n" +
            "  url : 'executor:default';\n" +
            "}\n" +
            "\n" +
            "ServiceStoreConnection demo::serviceStoreConnection\n" +
            "{\n" +
            "  store: test::serviceStore;\n" +
            "  baseUrl: 'http://baseUrl';\n" +
            "}\n" +
            "\n\n" +
            "###Data" +
            "\n" +
            "Data demo::JsonData\n" +
            "{\n" +
            "  ExternalFormat\n" +
            "  #{\n" +
            "    contentType: 'application/json';\n" +
            "    data: '{\"some\":\"data\"}';\n" +
            "  }#\n" +
            "}\n" +
            "\n" +
            "Data demo::XmlData\n" +
            "{\n" +
            "  ExternalFormat\n" +
            "  #{\n" +
            "    contentType: 'application/xml';\n" +
            "    data: ' <?xml version=\"1.0\" encoding=\"utf-8\"?>\\n" +
            "            <firm>\\n" +
            "                <name>Acme Co.</name>\\n" +
            "                <ranking>2</ranking>\\n" +
            "            </firm>';\n" +
            "  }#\n" +
            "}\n" +
            "\n" +
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
    public void testConnectionBuilderForModelChainConnection()
    {
        TestConnectionBuilder testConnectionBuilder = new TestConnectionBuilder(null, data);
        PackageableConnection modelChainConnection = ListIterate.detect(data.getElementsOfType(PackageableConnection.class), ele -> "demo::modelChainConnection".equals(ele.getPath()));

        Pair<Connection, List<Closeable>> testConnectionWithCloseables = modelChainConnection.connectionValue.accept(testConnectionBuilder);

        Assert.assertTrue(testConnectionWithCloseables.getOne() instanceof ModelChainConnection);
        Assert.assertEquals(0, testConnectionWithCloseables.getTwo().size());

        ModelChainConnection testModelChainConnection = (ModelChainConnection) testConnectionWithCloseables.getOne();

        Assert.assertEquals(1, testModelChainConnection.mappings.size());
        Assert.assertEquals("demo::modelChainConnection::mapping", testModelChainConnection.mappings.get(0));
    }

    @Test
    public void testConnectionBuilderForJsonModelConnection()
    {
        EmbeddedData embeddedData = ListIterate.detect(data.getElementsOfType(DataElement.class), d -> d.getPath().equals("demo::JsonData")).data;
        TestConnectionBuilder testConnectionBuilder = new TestConnectionBuilder(embeddedData, data);
        PackageableConnection jsonModelConnection = ListIterate.detect(data.getElementsOfType(PackageableConnection.class), ele -> "demo::jsonModelConnection".equals(ele.getPath()));

        Pair<Connection, List<Closeable>> testConnectionWithCloseables = jsonModelConnection.connectionValue.accept(testConnectionBuilder);

        Assert.assertTrue(testConnectionWithCloseables.getOne() instanceof JsonModelConnection);
        Assert.assertEquals(0, testConnectionWithCloseables.getTwo().size());

        JsonModelConnection testJsonModelConnection = (JsonModelConnection) testConnectionWithCloseables.getOne();

        Assert.assertEquals("anything::class", testJsonModelConnection._class);
        Assert.assertEquals("data:application/json;base64,eyJzb21lIjoiZGF0YSJ9", testJsonModelConnection.url);
    }

    @Test
    public void testConnectionBuilderForJsonModelConnectionWithNonJsonData()
    {
        try
        {
            EmbeddedData embeddedData = ListIterate.detect(data.getElementsOfType(DataElement.class), d -> d.getPath().equals("demo::XmlData")).data;
            TestConnectionBuilder testConnectionBuilder = new TestConnectionBuilder(embeddedData, data);
            PackageableConnection jsonModelConnection = ListIterate.detect(data.getElementsOfType(PackageableConnection.class), ele -> "demo::jsonModelConnection".equals(ele.getPath()));
            jsonModelConnection.connectionValue.accept(testConnectionBuilder);
        }
        catch (Exception e)
        {
            Assert.assertEquals("Json data should be provided for JsonModelConnection", e.getMessage());
        }
    }

    @Test
    public void testConnectionBuilderForXmlModelConnection()
    {
        EmbeddedData embeddedData = ListIterate.detect(data.getElementsOfType(DataElement.class), d -> d.getPath().equals("demo::XmlData")).data;
        TestConnectionBuilder testConnectionBuilder = new TestConnectionBuilder(embeddedData, data);
        PackageableConnection xmlModelConnection = ListIterate.detect(data.getElementsOfType(PackageableConnection.class), ele -> "demo::xmlModelConnection".equals(ele.getPath()));

        Pair<Connection, List<Closeable>> testConnectionWithCloseables = xmlModelConnection.connectionValue.accept(testConnectionBuilder);

        Assert.assertTrue(testConnectionWithCloseables.getOne() instanceof XmlModelConnection);
        Assert.assertEquals(0, testConnectionWithCloseables.getTwo().size());

        XmlModelConnection testXmlModelConnection = (XmlModelConnection) testConnectionWithCloseables.getOne();

        Assert.assertEquals("anything::class", testXmlModelConnection._class);
        Assert.assertEquals("data:application/xml;base64,IDw/eG1sIHZlcnNpb249IjEuMCIgZW5jb2Rpbmc9InV0Zi04Ij8+CiAgICAgICAgICAgIDxmaXJtPgogICAgICAgICAgICAgICAgPG5hbWU+QWNtZSBDby48L25hbWU+CiAgICAgICAgICAgICAgICA8cmFua2luZz4yPC9yYW5raW5nPgogICAgICAgICAgICA8L2Zpcm0+", testXmlModelConnection.url);
    }

    @Test
    public void testConnectionBuilderForXmlModelConnectionWithNonXmlData()
    {
        try
        {
            EmbeddedData embeddedData = ListIterate.detect(data.getElementsOfType(DataElement.class), d -> d.getPath().equals("demo::JsonData")).data;
            TestConnectionBuilder testConnectionBuilder = new TestConnectionBuilder(embeddedData, data);
            PackageableConnection xmlModelConnection = ListIterate.detect(data.getElementsOfType(PackageableConnection.class), ele -> "demo::xmlModelConnection".equals(ele.getPath()));
            xmlModelConnection.connectionValue.accept(testConnectionBuilder);
        }
        catch (Exception e)
        {
            Assert.assertEquals("Xml data should be provided for XmlModelConnection", e.getMessage());
        }
    }

    @Test
    public void testConnectionBuilderWithConnectionPointer()
    {
        EmbeddedData embeddedData = ListIterate.detect(data.getElementsOfType(DataElement.class), d -> d.getPath().equals("demo::XmlData")).data;
        TestConnectionBuilder testConnectionBuilder = new TestConnectionBuilder(embeddedData, data);

        ConnectionPointer connectionPointer = new ConnectionPointer();
        connectionPointer.connection = "demo::xmlModelConnection";

        Pair<Connection, List<Closeable>> testConnectionWithCloseables = connectionPointer.accept(testConnectionBuilder);

        Assert.assertTrue(testConnectionWithCloseables.getOne() instanceof XmlModelConnection);
        Assert.assertEquals(0, testConnectionWithCloseables.getTwo().size());

        XmlModelConnection testXmlModelConnection = (XmlModelConnection) testConnectionWithCloseables.getOne();

        Assert.assertEquals("anything::class", testXmlModelConnection._class);
        Assert.assertEquals("data:application/xml;base64,IDw/eG1sIHZlcnNpb249IjEuMCIgZW5jb2Rpbmc9InV0Zi04Ij8+CiAgICAgICAgICAgIDxmaXJtPgogICAgICAgICAgICAgICAgPG5hbWU+QWNtZSBDby48L25hbWU+CiAgICAgICAgICAgICAgICA8cmFua2luZz4yPC9yYW5raW5nPgogICAgICAgICAgICA8L2Zpcm0+", testXmlModelConnection.url);
    }

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
