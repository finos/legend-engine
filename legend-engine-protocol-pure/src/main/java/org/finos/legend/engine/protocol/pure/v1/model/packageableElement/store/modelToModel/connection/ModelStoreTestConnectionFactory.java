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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.protocol.pure.v1.extension.ConnectionFactoryExtension;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.data.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.data.DataElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.Store;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.ModelStore;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.PackageableElementPtr;

public class ModelStoreTestConnectionFactory implements ConnectionFactoryExtension
{
    public static String MODEL_STORE = "ModelStore";
    //TODO: after refactor use the already present variables
    //currently creates a circular dependency, hence cannot use the already present variables
    private static String DATA_PROTOCOL_NAME = "data";
    private static String APPLICATION_JSON = "application/json";
    private static String APPLICATION_XML = "application/xml";

    private Connection resolveExternalFormatData(ExternalFormatData externalFormatData, String _class, boolean useDefaultExecutor)
    {
        if (APPLICATION_JSON.equals(externalFormatData.contentType))
        {
            JsonModelConnection jsonModelConnection = new JsonModelConnection();
            jsonModelConnection._class = _class;
            jsonModelConnection.element = MODEL_STORE;
            if (useDefaultExecutor)
            {
                jsonModelConnection.url = "executor:default";
            }
            else
            {
                jsonModelConnection.url = buildModelConnectionURL(externalFormatData, APPLICATION_JSON);
            }
            return jsonModelConnection;
        }
        else if (APPLICATION_XML.equals((externalFormatData.contentType)))
        {
            XmlModelConnection xmlModelConnection = new XmlModelConnection();
            xmlModelConnection._class = _class;
            xmlModelConnection.element = MODEL_STORE;
            if (useDefaultExecutor)
            {
                xmlModelConnection.url = "executor:default";
            }
            else
            {
                xmlModelConnection.url = buildModelConnectionURL(externalFormatData, APPLICATION_XML);
            }
            return xmlModelConnection;
        }
        else
        {
            throw new RuntimeException("Data format specified is invalid, allowed types: JSON, XML for external format data");
        }
    }

    private Optional<Pair<Connection, List<Closeable>>> buildModelStoreConnectionsForStore(List<DataElement> dataElements, ModelStoreData modelStoreData, boolean useDefaultExecutor)
    {
        List<ModelTestData> modelTestData = modelStoreData.modelData;
        for (ModelTestData data : modelTestData)
        {
            String _class = data.model;
            if (data instanceof ModelEmbeddedTestData)
            {
                ModelEmbeddedTestData modelEmbeddedData = (ModelEmbeddedTestData) data;
                EmbeddedData resolvedEmbeddedData = EmbeddedDataHelper.resolveDataElementWithList(dataElements, modelEmbeddedData.data);
                if (resolvedEmbeddedData instanceof ExternalFormatData)
                {
                    Connection connection = resolveExternalFormatData((ExternalFormatData) resolvedEmbeddedData, _class, useDefaultExecutor);
                    return Optional.of(Tuples.pair(connection, Collections.emptyList()));
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
                        Connection connection = resolveExternalFormatData((ExternalFormatData) testDataElement, _class, useDefaultExecutor);
                        return Optional.of(Tuples.pair(connection, Collections.emptyList()));
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
    public Optional<Pair<Connection, List<Closeable>>> tryBuildTestConnectionsForStore(List<DataElement> dataElements, Store store, EmbeddedData testData)
    {
        if (store instanceof ModelStore && testData instanceof ModelStoreData)
        {
            return buildModelStoreConnectionsForStore(dataElements, (ModelStoreData) testData, false);
        }
        return Optional.empty();
    }


    @Override
    public Optional<Pair<Connection, List<Closeable>>> tryBuildTestConnectionsForStoreWithMultiInputs(List<DataElement> dataElements, Store store, EmbeddedData testData)
    {
        if (store instanceof ModelStore && testData instanceof ModelStoreData)
        {
            return buildModelStoreConnectionsForStore(dataElements, (ModelStoreData) testData, true);
        }
        return Optional.empty();
    }


    @Override
    public Optional<InputStream> tryBuildInputStreamForStore(PureModelContextData pureModelContextData, Store store, EmbeddedData testData)
    {
        if (store instanceof ModelStore && testData instanceof ModelStoreData)
        {
            ModelStoreData modelStoreData = (ModelStoreData) testData;
            for (ModelTestData _data : modelStoreData.modelData)
            {
                if (_data instanceof ModelEmbeddedTestData)
                {

                    EmbeddedData _embeddedData = EmbeddedDataHelper.resolveEmbeddedData(pureModelContextData, ((ModelEmbeddedTestData) _data).data);
                    if (_embeddedData instanceof ExternalFormatData)
                    {
                        return Optional.of(new ByteArrayInputStream((((ExternalFormatData) _embeddedData).data).getBytes(StandardCharsets.UTF_8)));
                    }
                }
                else if (_data instanceof ModelInstanceTestData)
                {
                    ValueSpecification valueSpecification = ((ModelInstanceTestData) _data).instances;
                    if (valueSpecification instanceof PackageableElementPtr)
                    {
                        PackageableElementPtr packageableElementPtr = (PackageableElementPtr) valueSpecification;
                        DataElement dElement = EmbeddedDataHelper.resolveDataElement(pureModelContextData, packageableElementPtr.fullPath);
                        EmbeddedData testDataElement = dElement.data;
                        if (testDataElement instanceof ExternalFormatData)
                        {
                            return Optional.of(new ByteArrayInputStream((((ExternalFormatData) testDataElement).data).getBytes(StandardCharsets.UTF_8)));
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }


    @Override
    public Optional<Pair<Connection, List<Closeable>>> tryBuildTestConnection(Connection sourceConnection, EmbeddedData embeddedData)
    {
        if (sourceConnection instanceof JsonModelConnection)
        {
            JsonModelConnection jsonModelConnection = (JsonModelConnection) sourceConnection;
            if (!(embeddedData instanceof ExternalFormatData && APPLICATION_JSON.equals(((ExternalFormatData) embeddedData).contentType)))
            {
                throw new UnsupportedOperationException("Json data should be provided for JsonModelConnection");
            }

            JsonModelConnection testConnection = new JsonModelConnection();
            testConnection.element = jsonModelConnection.element;
            testConnection._class = jsonModelConnection._class;
            testConnection.url = buildModelConnectionURL((ExternalFormatData) embeddedData, APPLICATION_JSON);
            return Optional.of(Tuples.pair(testConnection, Collections.emptyList()));
        }
        else if (sourceConnection instanceof XmlModelConnection)
        {
            XmlModelConnection xmlModelConnection = (XmlModelConnection) sourceConnection;
            if (!(embeddedData instanceof ExternalFormatData && APPLICATION_XML.equals(((ExternalFormatData) embeddedData).contentType)))
            {
                throw new UnsupportedOperationException("Xml data should be provided for XmlModelConnection");
            }

            XmlModelConnection testConnection = new XmlModelConnection();
            testConnection.element = xmlModelConnection.element;
            testConnection._class = xmlModelConnection._class;
            testConnection.url = buildModelConnectionURL((ExternalFormatData) embeddedData, APPLICATION_XML);

            return Optional.of(Tuples.pair(testConnection, Collections.emptyList()));
        }
        return Optional.empty();
    }


    private String buildModelConnectionURL(ExternalFormatData externalFormatData, String type)
    {
        return DATA_PROTOCOL_NAME + ":" + type + ";base64," + Base64.getEncoder().encodeToString(externalFormatData.data.getBytes(StandardCharsets.UTF_8));
    }

}
