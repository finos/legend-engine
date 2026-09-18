// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.language.snowflake.generator.extension;

import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SnowflakeDatasourceSpecification;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.ServiceLoader;

public class SnowflakeDeploymentDatasourceResolver
{
    private SnowflakeDeploymentDatasourceResolver()
    {
    }

    public static SnowflakeDatasourceSpecification resolve(RelationalDatabaseConnection connection, Identity identity)
    {
        DatasourceSpecification spec = connection.datasourceSpecification;
        if (spec instanceof SnowflakeDatasourceSpecification)
        {
            return (SnowflakeDatasourceSpecification) spec;
        }

        for (SnowflakeDeploymentDatasourceExtension extension : ServiceLoader.load(SnowflakeDeploymentDatasourceExtension.class))
        {
            SnowflakeDatasourceSpecification resolved = spec.accept(extension.getExtraSnowflakeDatasourceSpecificationVisitor(identity));
            if (resolved != null)
            {
                return resolved;
            }
        }
        throw new EngineException(
                "Unsupported datasource specification type '" + spec.getClass().getSimpleName() + "' - no registered SnowflakeDeploymentDatasourceExtension could resolve this connection to a Snowflake datasource specification",
                spec.sourceInformation,
                EngineErrorType.COMPILATION);
    }
}
