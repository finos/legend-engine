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

package org.finos.legend.engine.plan.execution.stores.mongodb.specifics;

import org.finos.legend.engine.plan.dependencies.store.inMemory.IStoreStreamReader;
import org.finos.legend.engine.plan.execution.stores.mongodb.result.MongoDBResult;

public interface IMongoDocumentDeserializeExecutionNodeSpecifics
{
    IStoreStreamReader streamReader(MongoDBResult mongoResult);
}
