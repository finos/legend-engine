// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.deephaven.test.compiled;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.execution.stores.deephaven.test.shared.DeephavenCommands;
import org.finos.legend.engine.plan.execution.stores.deephaven.test.shared.GetDeephavenTestConnectionShared;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_runtime_DeephavenConnection;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;

public class GetDeephavenTestConnection extends AbstractNative
{
    public GetDeephavenTestConnection()
    {
        super(DeephavenCommands.GET_DEEPHAVEN_TEST_CONNECTION_FUNCTION);
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        return GetDeephavenTestConnection.class.getCanonicalName() + ".compileExec(es)";
    }

    public static Root_meta_external_store_deephaven_metamodel_runtime_DeephavenConnection compileExec(final ExecutionSupport es)
    {
        PackageableConnection connection = new PackageableConnection();
        connection._package = "toGetValue";
        connection.name = "DeephavenTestConn";
        connection.connectionValue = GetDeephavenTestConnectionShared.getDatabaseConnection();
        PureModelContextData data = PureModelContextData.newPureModelContextData(null, null, Lists.mutable.with(connection));
        PureModel pureModel = Compiler.compile(data, DeploymentMode.PROD, Identity.getAnonymousIdentity().getName(), "", ((CompiledExecutionSupport) es).getProcessorSupport().getMetadata());
        return (Root_meta_external_store_deephaven_metamodel_runtime_DeephavenConnection) pureModel.getConnection("toGetValue::DeephavenTestConn", null);
    }
}
