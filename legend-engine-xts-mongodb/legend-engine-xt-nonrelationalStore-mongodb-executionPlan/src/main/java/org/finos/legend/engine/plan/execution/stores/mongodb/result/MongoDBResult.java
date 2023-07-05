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

package org.finos.legend.engine.plan.execution.stores.mongodb.result;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCursor;
import org.bson.Document;
import org.finos.legend.engine.plan.execution.result.ExecutionActivity;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.ResultVisitor;
import org.finos.legend.engine.plan.execution.result.builder.Builder;
import org.finos.legend.engine.plan.execution.result.builder.stream.StreamBuilder;

import java.util.Collections;
import java.util.List;

public class MongoDBResult extends Result
{
    private final MongoCursor<Document> mongoCursor;

    private final MongoClient mongoClient;

    public MongoDBResult(MongoClient mongoClient, MongoCursor<Document> mongoCursor)
    {
        this(mongoClient, mongoCursor, Collections.emptyList());
    }

    public MongoDBResult(MongoClient mongoClient, MongoCursor<Document> mongoCursor, List<ExecutionActivity> activities)
    {
        super("success", activities);
        this.mongoCursor = mongoCursor;
        this.mongoClient = mongoClient;
    }

    public MongoCursor<Document> getMongoCursor()
    {
        return this.mongoCursor;
    }

    public Builder getResultBuilder()
    {
        return new StreamBuilder();
    }

    @Override
    public void close()
    {
        this.mongoCursor.close();
        this.mongoClient.close();
    }

    @Override
    public <V> V accept(ResultVisitor<V> resultVisitor)
    {
        throw new UnsupportedOperationException("Streaming MongoDBResult result is not supported. Please raise a issue with dev team");
    }
}
