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

package org.finos.legend.engine.application.query.api;

import com.mongodb.client.MongoClient;
import org.finos.legend.engine.application.query.model.ApplicationStoredQuery;
import org.finos.legend.engine.shared.mongo.api.BaseStoredVersionedAssetDao;

/**
 * DAO for ApplicationStoredQuery that handles persistence with embedded audit information.
 * Extends BaseStoredVersionedAssetDao to inherit common versioning and audit logic.
 */
public class ApplicationQueryDao extends BaseStoredVersionedAssetDao<ApplicationStoredQuery, String>
{
    public ApplicationQueryDao(MongoClient client, String database, String collectionName)
    {
        super(client, database, collectionName, ApplicationStoredQuery.class);
    }
}
