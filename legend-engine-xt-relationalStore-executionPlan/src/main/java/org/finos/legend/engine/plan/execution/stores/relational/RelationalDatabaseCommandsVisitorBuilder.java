// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.plan.execution.result.StreamingResult;
import org.finos.legend.engine.plan.execution.stores.relational.config.RelationalExecutionConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommandsVisitor;

import java.sql.Connection;
import java.util.ServiceLoader;

public class RelationalDatabaseCommandsVisitorBuilder
{
    static RelationalStoreExecutorExtension extension;

    static
    {
        MutableList<RelationalStoreExecutorExtension> extensions = Iterate.addAllTo(ServiceLoader.load(RelationalStoreExecutorExtension.class), Lists.mutable.empty());
        if (extensions.size() > 1)
        {
            throw new RuntimeException("Found too many RelationalStoreExecutorExtensions");
        }
        if (!extensions.isEmpty())
        {
            extension = extensions.get(0);
        }
    }

    public static RelationalDatabaseCommandsVisitor<Boolean> getStreamResultToTempTableVisitor(RelationalExecutionConfiguration config, Connection connection, StreamingResult result, String tableName, String databaseTimeZone)
    {
        return (extension == null) ?
                new StreamResultToTempTableVisitor(config, connection, result, tableName, databaseTimeZone) :
                extension.getStreamResultToTempTableVisitor().value(config, connection, result, tableName, databaseTimeZone);
    }
}
