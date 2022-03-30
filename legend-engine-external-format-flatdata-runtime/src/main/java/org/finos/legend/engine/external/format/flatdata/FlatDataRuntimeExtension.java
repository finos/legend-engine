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

package org.finos.legend.engine.external.format.flatdata;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.external.format.flatdata.read.FlatDataReader;
import org.finos.legend.engine.external.format.flatdata.read.IFlatDataDeserializeExecutionNodeSpecifics;
import org.finos.legend.engine.external.shared.runtime.ExternalFormatRuntimeExtension;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.ExecutionNodeJavaPlatformHelper;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.JavaHelper;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.object.StreamingObjectResult;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.JavaPlatformImplementation;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.external.shared.InternalizeExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.external.shared.RuntimeVariableInternalizeDataSource;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.external.shared.StringConstantInternalizeDataSource;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.external.shared.UrlConstantInternalizeDataSource;
import org.finos.legend.engine.shared.core.url.UrlFactory;
import org.pac4j.core.profile.CommonProfile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;

public class FlatDataRuntimeExtension implements ExternalFormatRuntimeExtension
{
    private static long DEFAULT_MAX_SCHEMA_OBJECT_SIZE = 50 *1024 * 1024;

    @Override
    public List<String> getContentTypes()
    {
        return Collections.singletonList("application/x.flatdata");
    }

    @Override
    public Result executeInternalize(InternalizeExecutionNode node, MutableList<CommonProfile> profiles, ExecutionState executionState)
    {
        try
        {
            String specificsClassName = JavaHelper.getExecutionClassFullName((JavaPlatformImplementation) node.implementation);
            Class<?> specificsClass = ExecutionNodeJavaPlatformHelper.getClassToExecute(node, specificsClassName, executionState, profiles);
            IFlatDataDeserializeExecutionNodeSpecifics<?> specifics = (IFlatDataDeserializeExecutionNodeSpecifics<?>) specificsClass.getConstructor().newInstance();
            // TODO Allow size to vary when run from jar
            specifics.setMaximumSchemaObjectSize(DEFAULT_MAX_SCHEMA_OBJECT_SIZE);
            FlatDataContext<?> context = specifics.createContext();

            InputStream stream = null;
            URL url = null;
            if (node.dataSource instanceof StringConstantInternalizeDataSource)
            {
                stream = new ByteArrayInputStream(((StringConstantInternalizeDataSource) node.dataSource).data.getBytes());
            }
            else if (node.dataSource instanceof UrlConstantInternalizeDataSource)
            {
                url = UrlFactory.create(((UrlConstantInternalizeDataSource) node.dataSource).url);
            }
            else if (node.dataSource instanceof RuntimeVariableInternalizeDataSource)
            {
                RuntimeVariableInternalizeDataSource source = (RuntimeVariableInternalizeDataSource) node.dataSource;
                ConstantResult value = (ConstantResult) executionState.getResult(source.variable);
                if (source.type.equals("String"))
                {
                    stream = new ByteArrayInputStream(((String )value.getValue()).getBytes());
                }
                else if (source.type.equals("ByteStream"))
                {
                    stream = (InputStream) value.getValue();
                }
                else if (source.type.equals("Url"))
                {
                    url = UrlFactory.create((String) value.getValue());
                }
                else
                {
                    throw new IllegalStateException("Unsupported variable type: " + source.type);
                }
            }
            else
            {
                throw new IllegalStateException("Unsupported dataSource: " + node.dataSource);
            }

            if (stream == null && url == null)
            {
                throw new IllegalStateException("Must establish a stream or URL");
            }

            // TODO Pass URL directly to FlatDataReader when present so that (e.g.) Parquet driver can open directly
            if (stream == null)
            {
                stream = url.openStream();
            }
            FlatDataReader<?> deserializer = new FlatDataReader<>(context, stream);
            return new StreamingObjectResult<>(deserializer.startStream());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
