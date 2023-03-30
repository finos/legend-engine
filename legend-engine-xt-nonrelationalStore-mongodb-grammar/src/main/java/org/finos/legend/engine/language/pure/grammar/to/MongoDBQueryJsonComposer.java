// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.language.pure.grammar.to;

import org.finos.legend.engine.language.pure.grammar.to.visitors.MongoDBOperationElementVisitorImpl;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.DatabaseCommand;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.Stage;

import java.util.List;
import java.util.stream.Collectors;

public class MongoDBQueryJsonComposer
{

    final String const_TypeValue = "\"_type\": \"databaseCommand\",";

    private boolean withTypeIdentifier;

    public MongoDBQueryJsonComposer()
    {
        this.withTypeIdentifier = true;
    }

    public MongoDBQueryJsonComposer(boolean withTypeIdentifier)
    {
        this.withTypeIdentifier = withTypeIdentifier;
    }

    public String parseDatabaseCommand(DatabaseCommand databaseCommand)
    {
        String collectionName = databaseCommand.collectionName;
        return withTypeIdentifier
                ? "{ " + const_TypeValue + "\"aggregate\": \"" + collectionName + "\" , " + visitDatabaseCommand(databaseCommand) + ", \"cursor\": {} }"
                : "{ \"aggregate\": \"" + collectionName + "\" , " + visitDatabaseCommand(databaseCommand) + ", \"cursor\": {} }";
    }

    private String visitDatabaseCommand(DatabaseCommand databaseCommand)
    {
        String pipelineStages = visitPipelineStages(databaseCommand.aggregationPipeline.stages);
        return "\"pipeline\" : [" + pipelineStages + "]";
    }

    private String visitPipelineStages(List<Stage> stages)
    {
        List<String> strings = stages.stream().map(this::visitPipelineStage).collect(Collectors.toList());
        return String.join(",", strings);
    }

    private String visitPipelineStage(Stage stage)
    {
        return stage.accept(new MongoDBOperationElementVisitorImpl());
    }


}
