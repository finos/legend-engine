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

package org.finos.legend.engine.pure.runtime.testConnection.compiled.natives;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.pure.runtime.testConnection.shared.GetTestConnectionShared;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_external_store_relational_runtime_RelationalDatabaseConnection;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.valuespecification.ValueSpecificationProcessor;

public class GetTestConnection extends AbstractNative
{
    public GetTestConnection()
    {
        super("getTestConnection_DatabaseType_1__RelationalDatabaseConnection_1_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        final ProcessorSupport processorSupport = processorContext.getSupport();
        final ListIterable<? extends CoreInstance> parametersValues = Instance.getValueForMetaPropertyToManyResolved(functionExpression, M3Properties.parametersValues, processorSupport);
        String code = ValueSpecificationProcessor.processValueSpecification(topLevelElement, parametersValues.get(0), processorContext);
        return "org.finos.legend.engine.pure.runtime.testConnection.compiled.natives.GetTestConnection.compileExec(" + code + ", es)";
    }

    public static Root_meta_external_store_relational_runtime_RelationalDatabaseConnection compileExec(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum code, final ExecutionSupport es)
    {
        PackageableConnection connection = new PackageableConnection();
        connection._package = "toGetValue";
        connection.name = "Conn";
        connection.connectionValue = GetTestConnectionShared.getDatabaseConnection(DatabaseType.valueOf(code._name()));
        PureModelContextData data = PureModelContextData.newPureModelContextData(null, null, Lists.mutable.with(connection));

        PureModel pureModel = org.finos.legend.engine.language.pure.compiler.Compiler.compile(data, DeploymentMode.PROD, Identity.getAnonymousIdentity().getName(), "", ((CompiledExecutionSupport) es).getProcessorSupport().getMetadata());

        return (Root_meta_external_store_relational_runtime_RelationalDatabaseConnection) pureModel.getConnection("toGetValue::Conn", null);
    }
}
