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

import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.protocol.pure.v1.extension.ConnectionFactoryExtension;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.data.ExternalFormatData;
import org.finos.legend.engine.protocol.pure.v1.model.data.ModelStoreData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.data.DataElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.Store;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.ModelStore;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.PackageableElementPtr;

import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Collections;
import java.util.Base64;

public class ModelStoreTestConnectionFactory implements ConnectionFactoryExtension
{
    public static String MODEL_STORE = "ModelStore";
    //TODO: after refactor use the already present variables
    //currently creates a circular dependency, hence cannot use the already present variables
    private static String DATA_PROTOCOL_NAME = "data";
    private static String APPLICATION_JSON = "application/json";
    private static String APPLICATION_XML = "application/xml";

    @Override
    public Optional<Pair<Connection, List<Closeable>>> tryBuildTestConnectionsForStore(Store store, EmbeddedData testData, List<DataElement> dataElementList)
    {
        if (store instanceof ModelStore && testData instanceof ModelStoreData)
        {
            for (Map.Entry<String, ValueSpecification> map : ((ModelStoreData) testData).instances.entrySet())
            {
                ValueSpecification vs = map.getValue();
                if (vs instanceof PackageableElementPtr)
                {
                    EmbeddedData testDataElement = Iterate.detect(dataElementList, e -> ((PackageableElementPtr) vs).fullPath.equals(e.getPath())).data;
                    //We assume that there is a default binding being used and therefore external data could be referencd in model store data
                    if (testDataElement instanceof ExternalFormatData && APPLICATION_JSON.equals(((ExternalFormatData) testDataElement).contentType))
                    {
                        JsonModelConnection jsonModelConnection = new JsonModelConnection();
                        jsonModelConnection._class = map.getKey();
                        jsonModelConnection.element = MODEL_STORE;
                        return this.tryBuildTestConnection(jsonModelConnection, testDataElement);
                    }
                    else if (testDataElement instanceof ExternalFormatData && APPLICATION_XML.equals(((ExternalFormatData) testDataElement).contentType))
                    {
                        XmlModelConnection xmlModelConnection = new XmlModelConnection();
                        xmlModelConnection._class = map.getKey();
                        xmlModelConnection.element = MODEL_STORE;
                        return this.tryBuildTestConnection(xmlModelConnection, testDataElement);
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
            testConnection.url = DATA_PROTOCOL_NAME + ":" + APPLICATION_JSON + ";base64," + Base64.getEncoder().encodeToString(((ExternalFormatData) embeddedData).data.getBytes(StandardCharsets.UTF_8));

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
            testConnection.url = DATA_PROTOCOL_NAME + ":" + APPLICATION_XML + ";base64," + Base64.getEncoder().encodeToString(((ExternalFormatData) embeddedData).data.getBytes(StandardCharsets.UTF_8));

            return Optional.of(Tuples.pair(testConnection, Collections.emptyList()));
        }
        return Optional.empty();
    }
}
