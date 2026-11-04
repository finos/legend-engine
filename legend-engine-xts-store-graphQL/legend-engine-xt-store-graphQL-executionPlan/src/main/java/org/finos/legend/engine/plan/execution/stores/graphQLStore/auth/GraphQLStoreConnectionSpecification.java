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

package org.finos.legend.engine.plan.execution.stores.graphQLStore.auth;

import org.apache.http.entity.StringEntity;
import org.finos.legend.connection.legacy.ConnectionSpecification;

import java.net.URI;

// Far simpler than ServiceStore's equivalent - GraphQL always POSTs a JSON body to a fixed URL, no
// path/query/header parameter machinery needed.
public class GraphQLStoreConnectionSpecification extends ConnectionSpecification
{
    public URI uri;
    public StringEntity body;

    public GraphQLStoreConnectionSpecification(URI uri, StringEntity body)
    {
        this.uri = uri;
        this.body = body;
    }
}
