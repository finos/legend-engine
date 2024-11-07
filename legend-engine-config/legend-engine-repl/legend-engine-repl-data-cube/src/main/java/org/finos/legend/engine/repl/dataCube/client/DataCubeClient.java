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

package org.finos.legend.engine.repl.dataCube.client;

import org.apache.commons.io.FileUtils;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.dataCube.DataCubeReplExtension;
import org.finos.legend.engine.repl.relational.RelationalReplExtension;
import org.finos.legend.engine.repl.relational.autocomplete.RelationalCompleterExtension;

import java.nio.file.Paths;

public class DataCubeClient
{
    public static void main(String[] args) throws Exception
    {
        // NOTE: this is exclusively used for development of DataCube when we need to boot multiple instances
        // of the REPL at the same time and want to avoid locking on the DuckDB instances
        String DEV__homeDir = System.getProperty("legend.repl.dataCube.devHomeDir");
        Client client = new Client(
                Lists.mutable.with(
                        new DataCubeReplExtension(),
                        new RelationalReplExtension()
                ),
                Lists.mutable.with(
                        new RelationalCompleterExtension()
                ),
                PlanExecutor.newPlanExecutorBuilder().withAvailableStoreExecutors().build(),
                DEV__homeDir != null ? Paths.get(DEV__homeDir) : FileUtils.getUserDirectory().toPath().resolve(".legend/repl")
        );
        client.loop();
    }
}
