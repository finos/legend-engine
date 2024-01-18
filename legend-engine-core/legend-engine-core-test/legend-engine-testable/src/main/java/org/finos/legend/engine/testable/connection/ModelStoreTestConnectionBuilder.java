// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.testable.connection;

import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.data.ExternalFormatData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.JsonModelConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.XmlModelConnection;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.TestableTestDataExtension;
import org.finos.legend.engine.shared.core.url.InputStreamProvider;
import org.finos.legend.engine.shared.core.url.StreamProviderHolder;
import org.finos.legend.pure.generated.*;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ModelStoreTestConnectionBuilder implements TestableTestDataExtension
{

    public static final String MODEL_STORE = "ModelStore";

    private static final String APPLICATION_JSON = "application/json";
    private static final String APPLICATION_XML = "application/xml";

    private static JsonModelConnection jsonModelConnection = new JsonModelConnection();
    private static XmlModelConnection xmlModelConnection = new XmlModelConnection();


    private Pair<Connection, List<Closeable>> resolveExternalFormatData(ExternalFormatData externalFormatData, String _class)
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

    @Override
    public Optional<Pair<Connection, List<Closeable>>> buildConnectionTestData(PureModel pureModel, PureModelContextData pureModelContextData, Root_meta_core_runtime_ConnectionStore sourceConnection, EmbeddedData data)
    {
        if (data instanceof ExternalFormatData && sourceConnection != null && sourceConnection._element() instanceof Root_meta_external_store_model_ModelStore)
         {
            ExternalFormatData externalFormatData = (ExternalFormatData) data;
            Root_meta_core_runtime_Connection resolvedConnection = sourceConnection._connection();
            if (resolvedConnection instanceof Root_meta_external_store_model_PureModelConnection)
            {
                if (resolvedConnection instanceof Root_meta_external_store_model_JsonModelConnection)
                {
                    String _class = HelperModelBuilder.getElementFullPath(((Root_meta_external_store_model_JsonModelConnection) resolvedConnection)._class(), pureModel.getExecutionSupport());
                    return Optional.of(resolveExternalFormatData(externalFormatData, _class));
                }
                else if (resolvedConnection instanceof Root_meta_external_store_model_XmlModelConnection)
                {
                    String _class = HelperModelBuilder.getElementFullPath(((Root_meta_external_store_model_XmlModelConnection) resolvedConnection)._class(), pureModel.getExecutionSupport());
                    return Optional.of(resolveExternalFormatData(externalFormatData, _class));
                }
            }
            return Optional.empty();
        }
        return Optional.empty();
    }

}
