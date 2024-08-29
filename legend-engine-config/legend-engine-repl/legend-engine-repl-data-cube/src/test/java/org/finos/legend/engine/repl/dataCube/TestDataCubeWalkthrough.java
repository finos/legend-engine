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

package org.finos.legend.engine.repl.dataCube;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.dataCube.commands.DataCubeWalkthrough;
import org.finos.legend.engine.repl.relational.RelationalReplExtension;
import org.finos.legend.engine.repl.shared.ExecutionHelper;
import org.junit.Test;

import java.nio.file.Files;

public class TestDataCubeWalkthrough
{
    private final Client client;
    private final DataCubeWalkthrough.DataCubeWalkthrough1 walkthrough;

    public TestDataCubeWalkthrough() throws Exception
    {
        this.client = new Client(
                org.eclipse.collections.impl.factory.Lists.mutable.with(
                        new DataCubeReplExtension(),
                        new RelationalReplExtension()
                ),
                Lists.mutable.with(),
                PlanExecutor.newPlanExecutorBuilder().withAvailableStoreExecutors().build(),
                Files.createTempDirectory("legend-data-cube-walkthrough-test")
        );
        this.walkthrough = (DataCubeWalkthrough.DataCubeWalkthrough1) this.client.commands.selectInstancesOf(DataCubeWalkthrough.class).getAny().getWalkthrough();
        this.walkthrough.beforeStep();
    }

    @Test
    public void testAllCommandsExecution()
    {
        Lists.mutable.with(
                walkthrough.query(DataCubeWalkthrough.DataCubeWalkthrough1.SELECT_ALL),
                walkthrough.query(DataCubeWalkthrough.DataCubeWalkthrough1.FILTER),
                walkthrough.query(DataCubeWalkthrough.DataCubeWalkthrough1.EXTEND),
                walkthrough.query(DataCubeWalkthrough.DataCubeWalkthrough1.GROUP_BY),
                walkthrough.query(DataCubeWalkthrough.DataCubeWalkthrough1.SORT),
                walkthrough.query(DataCubeWalkthrough.DataCubeWalkthrough1.LIMIT)
        ).forEach(query -> ExecutionHelper.executeCode(query, this.client, (Result res, PureModelContextData pmcd, PureModel pureModel) ->
        {
            // do nothing
            return null;
        }));
    }
}
