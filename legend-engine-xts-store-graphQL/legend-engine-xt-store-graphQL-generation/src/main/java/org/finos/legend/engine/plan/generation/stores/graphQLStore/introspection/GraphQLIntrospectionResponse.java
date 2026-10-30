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

package org.finos.legend.engine.plan.generation.stores.graphQLStore.introspection;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.finos.legend.engine.protocol.graphQLStore.introspection.GraphQLIntrospectionSchema;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphQLIntrospectionResponse
{
    public Data data;
    public List<GraphQLIntrospectionError> errors;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data
    {
        public GraphQLIntrospectionSchema __schema;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GraphQLIntrospectionError
    {
        public String message;
    }
}
