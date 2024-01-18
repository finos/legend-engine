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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.TestableTestDataExtension;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.LocalH2DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.data.RelationalCSVData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.data.RelationalCSVTable;
import org.finos.legend.pure.generated.Root_meta_core_runtime_ConnectionStore;

import java.io.Closeable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class RelationalTestDataExtension implements TestableTestDataExtension
{
    public class HelperRelationalCSVBuilder
    {
        final RelationalCSVData relationalData;

        public HelperRelationalCSVBuilder(RelationalCSVData relationalData)
        {
            this.relationalData = relationalData;
        }

        public String build()
        {
            return ListIterate.collect(this.relationalData.tables, this::generateTableCSV).makeString("----\n");
        }

        private String generateTableCSV(RelationalCSVTable table)
        {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(table.schema);
            stringBuilder.append("\n");
            stringBuilder.append(table.table == null ? "" : table.table);
            stringBuilder.append("\n");
            stringBuilder.append(table.values);
            return stringBuilder.toString();
        }
    }

    public Optional<Pair<Connection, List<Closeable>>> buildConnectionTestData(PureModel pureModel, PureModelContextData pureModelContextData, Root_meta_core_runtime_ConnectionStore sourceConnection, EmbeddedData data)
    {
        if (data instanceof RelationalCSVData && sourceConnection != null && sourceConnection._element() instanceof org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database)
        {
            org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database database = (org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database) sourceConnection._element();
            RelationalDatabaseConnection connection = new RelationalDatabaseConnection();
            connection.databaseType = DatabaseType.H2;
            connection.type = DatabaseType.H2;
            connection.element = HelperModelBuilder.getElementFullPath(database, pureModel.getExecutionSupport());
            connection.authenticationStrategy = new TestDatabaseAuthenticationStrategy();
            LocalH2DatasourceSpecification localH2DatasourceSpecification = new LocalH2DatasourceSpecification();
            RelationalCSVData relationalData = (RelationalCSVData) data;
            localH2DatasourceSpecification.testDataSetupCsv = new HelperRelationalCSVBuilder(relationalData).build();
            connection.datasourceSpecification = localH2DatasourceSpecification;
            return Optional.of(Tuples.pair(connection, Collections.emptyList()));
        }
        return Optional.empty();
    }
}
