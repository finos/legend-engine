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

package org.finos.legend.engine.protocol.graphQLStore.introspection;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// Mirrors the introspection spec's "__Type" (also used as the TypeRef/ofType wrapper); renamed to
// satisfy this repo's checkstyle TypeName rule, which rejects a leading underscore.
@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphQLIntrospectionType
{
    public String kind;
    public String name;
    public List<GraphQLIntrospectionField> fields;
    public List<GraphQLIntrospectionType> interfaces;
    public List<GraphQLIntrospectionType> possibleTypes;
    public List<GraphQLIntrospectionEnumValue> enumValues;
    public GraphQLIntrospectionType ofType;
    public List<GraphQLIntrospectionInputValue> inputFields;
}
