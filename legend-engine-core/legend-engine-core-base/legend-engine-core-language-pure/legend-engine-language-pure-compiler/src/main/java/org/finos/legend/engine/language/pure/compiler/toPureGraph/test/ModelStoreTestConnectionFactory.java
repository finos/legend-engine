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

package org.finos.legend.engine.language.pure.compiler.toPureGraph.test;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.protocol.pure.v1.extension.ConnectionFactoryExtension;
import org.finos.legend.engine.protocol.pure.v1.model.data.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.data.DataElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.Store;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.ModelStore;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.JsonModelConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.XmlModelConnection;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.packageableElement.PackageableElementPtr;
import org.finos.legend.engine.shared.core.url.InputStreamProvider;
import org.finos.legend.engine.shared.core.url.StreamProviderHolder;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

// TO BE MOVED TO MODEL STORE MODULE ONCE CREATED
public class ModelStoreTestConnectionFactory implements ConnectionFactoryExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "M2M");
    }

    public static final String MODEL_STORE = "ModelStore";
    //TODO: after refactor use the already present variables
    //currently creates a circular dependency, hence cannot use the already present variables
    private static final String DATA_PROTOCOL_NAME = "data";
    private static final String APPLICATION_JSON = "application/json";
    private static final String APPLICATION_XML = "application/xml";

    private static JsonModelConnection jsonModelConnection = new JsonModelConnection();
    private static XmlModelConnection xmlModelConnection = new XmlModelConnection();


    public Pair<Connection, List<Closeable>> buildCloseableConnectionFromExternalFormat(ExternalFormatData externalFormatData, String _class)
    {
        Closeable closeable = new Closeable()
        {
            @Override
            public void close() throws IOException
            {
                StreamProviderHolder.streamProviderThreadLocal.remove();
            }
        };
        if (APPLICATION_JSON.equals(externalFormatData.contentType))
        {
            jsonModelConnection._class = _class;
            jsonModelConnection.element = MODEL_STORE;
            jsonModelConnection.url = "executor:default";
            String inputData = externalFormatData.data;
            InputStream stream = new ByteArrayInputStream(inputData.getBytes(StandardCharsets.UTF_8));
            StreamProviderHolder.streamProviderThreadLocal.set(new InputStreamProvider(stream));
            return  Tuples.pair(jsonModelConnection, Collections.singletonList(closeable));
        }
        else if (APPLICATION_XML.equals((externalFormatData.contentType)))
        {
            xmlModelConnection._class = _class;
            xmlModelConnection.element = MODEL_STORE;
            xmlModelConnection.url = "executor:default";
            String inputData = externalFormatData.data;
            InputStream stream =  new ByteArrayInputStream(inputData.getBytes(StandardCharsets.UTF_8));
            StreamProviderHolder.streamProviderThreadLocal.set(new InputStreamProvider(stream));
            return Tuples.pair(xmlModelConnection, Collections.singletonList(closeable));
        }
        else
        {
            throw new RuntimeException("Data format specified is invalid, allowed types: JSON, XML for external format data");
        }
    }

    private Optional<Pair<Connection, List<Closeable>>> buildModelStoreConnectionsForStore(Map<String, DataElement> dataElements, ModelStoreData modelStoreData)
    {
        List<ModelTestData> modelTestData = modelStoreData.modelData;
        for (ModelTestData data : modelTestData)
        {
            String _class = data.model;
            if (data instanceof ModelEmbeddedTestData)
            {
                ModelEmbeddedTestData modelEmbeddedData = (ModelEmbeddedTestData) data;
                EmbeddedData resolvedEmbeddedData = EmbeddedDataHelper.resolveDataElement(dataElements, modelEmbeddedData.data);
                if (resolvedEmbeddedData instanceof ExternalFormatData)
                {
                    return Optional.of(buildCloseableConnectionFromExternalFormat((ExternalFormatData) resolvedEmbeddedData, _class));
                }
                else
                {
                    throw new RuntimeException("Data format specified is invalid, allowed types: JSON, XML for external format data");
                }
            }
            else if (data instanceof ModelInstanceTestData)
            {
                ModelInstanceTestData modelInstanceData = (ModelInstanceTestData) data;
                ValueSpecification vs = modelInstanceData.instances;
                if (vs instanceof PackageableElementPtr)
                {
                    EmbeddedData testDataElement = EmbeddedDataHelper.findDataElement(dataElements, ((PackageableElementPtr) vs).fullPath).data;
                    if (testDataElement instanceof ExternalFormatData)
                    {
                        return Optional.of(buildCloseableConnectionFromExternalFormat((ExternalFormatData) testDataElement, _class));
                    }
                    else
                    {
                        throw new RuntimeException("Data format specified is invalid, allowed types: JSON, XML for external format data");
                    }
                }
                else
                {
                    throw new RuntimeException("Expected Model Store Data to contain Data Elements");
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Pair<Connection, List<Closeable>>> tryBuildTestConnectionsForStore(Map<String, DataElement>  dataElements, Store store, EmbeddedData testData)
    {
        if (store instanceof ModelStore && testData instanceof ModelStoreData)
        {
            return buildModelStoreConnectionsForStore(dataElements, (ModelStoreData) testData);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Pair<Connection, List<Closeable>>> tryBuildTestConnection(Connection sourceConnection, List<EmbeddedData> embeddedData)
    {
        if (sourceConnection instanceof JsonModelConnection)
        {
            if (embeddedData.size() == 1)
            {
                JsonModelConnection jsonModelConnection = (JsonModelConnection) sourceConnection;
                if (!(embeddedData.get(0) instanceof ExternalFormatData && APPLICATION_JSON.equals(((ExternalFormatData) embeddedData.get(0)).contentType)))
                {
                    throw new UnsupportedOperationException("Json data should be provided for JsonModelConnection");
                }

                JsonModelConnection testConnection = new JsonModelConnection();
                testConnection.element = jsonModelConnection.element;
                testConnection._class = jsonModelConnection._class;
                testConnection.url = buildModelConnectionURL((ExternalFormatData) embeddedData.get(0), APPLICATION_JSON);
                return Optional.of(Tuples.pair(testConnection, Collections.emptyList()));
            }
            else
            {
                throw new RuntimeException(JsonModelConnection.class.getSimpleName() + " cannot support multiple embedded data sources.");
            }
        }
        else if (sourceConnection instanceof XmlModelConnection)
        {
            if (embeddedData.size() == 1)
            {
                XmlModelConnection xmlModelConnection = (XmlModelConnection) sourceConnection;
                if (!(embeddedData.get(0) instanceof ExternalFormatData && APPLICATION_XML.equals(((ExternalFormatData) embeddedData.get(0)).contentType)))
                {
                    throw new UnsupportedOperationException("Xml data should be provided for XmlModelConnection");
                }

                XmlModelConnection testConnection = new XmlModelConnection();
                testConnection.element = xmlModelConnection.element;
                testConnection._class = xmlModelConnection._class;
                testConnection.url = buildModelConnectionURL((ExternalFormatData) embeddedData.get(0), APPLICATION_XML);

                return Optional.of(Tuples.pair(testConnection, Collections.emptyList()));
            }
            else
            {
                throw new RuntimeException(XmlModelConnection.class.getSimpleName() + " cannot support multiple embedded data sources.");
            }
        }
        return Optional.empty();
    }

    private String buildModelConnectionURL(ExternalFormatData externalFormatData, String type)
    {
        return DATA_PROTOCOL_NAME + ":" + type + ";base64," + Base64.getEncoder().encodeToString(externalFormatData.data.getBytes(StandardCharsets.UTF_8));
    }

}
