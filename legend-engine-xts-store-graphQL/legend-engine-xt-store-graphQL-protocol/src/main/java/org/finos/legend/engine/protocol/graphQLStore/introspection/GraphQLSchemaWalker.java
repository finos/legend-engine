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

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.utility.ListIterate;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

// Lives in the protocol module, not the generation module that's its only current caller, so another
// caller needing the same introspection-shape-walking logic doesn't have to duplicate it.
public class GraphQLSchemaWalker
{
    // The GraphQL spec's Name production (http://spec.graphql.org/#sec-Names): a name that doesn't
    // match can only come from a non-conformant/untrusted endpoint and must be rejected before it
    // flows unquoted into synthesized JSON Schema or composed Pure grammar text.
    private static final Pattern VALID_GRAPHQL_NAME = Pattern.compile("[_A-Za-z][_0-9A-Za-z]*");

    public static boolean isValidGraphQLName(String name)
    {
        return name != null && VALID_GRAPHQL_NAME.matcher(name).matches();
    }

    // A field's "type" (and its ofType chain) is always a bare TypeRef - {kind, name, ofType} only,
    // never carrying "fields" inline; a type's real fields live only on its own entry in schema.types.
    public static Map<String, GraphQLIntrospectionType> typesByName(GraphQLIntrospectionSchema schema)
    {
        MutableMap<String, GraphQLIntrospectionType> byName = Maps.mutable.empty();
        if (schema.types != null)
        {
            for (GraphQLIntrospectionType type : schema.types)
            {
                if (type.name != null)
                {
                    byName.put(type.name, type);
                }
            }
        }
        return byName;
    }

    private static GraphQLIntrospectionType resolve(GraphQLIntrospectionType ref, Map<String, GraphQLIntrospectionType> typesByName)
    {
        if (ref == null || ref.fields != null || ref.name == null || typesByName == null)
        {
            return ref;
        }
        GraphQLIntrospectionType resolved = typesByName.get(ref.name);
        return resolved != null ? resolved : ref;
    }

    public UnwrappedType unwrap(GraphQLIntrospectionType type, Map<String, GraphQLIntrospectionType> typesByName)
    {
        boolean isNonNull = type != null && "NON_NULL".equals(type.kind);
        boolean isList = false;
        GraphQLIntrospectionType current = type;
        while (current != null && ("NON_NULL".equals(current.kind) || "LIST".equals(current.kind)))
        {
            if ("LIST".equals(current.kind))
            {
                isList = true;
            }
            current = current.ofType;
        }
        return new UnwrappedType(isList, isNonNull, resolve(current, typesByName));
    }

    public boolean isRelayConnection(GraphQLIntrospectionType baseType, Map<String, GraphQLIntrospectionType> typesByName)
    {
        return relayNodeType(baseType, typesByName) != null;
    }

    public GraphQLIntrospectionType relayNodeType(GraphQLIntrospectionType baseType, Map<String, GraphQLIntrospectionType> typesByName)
    {
        if (baseType == null || !"OBJECT".equals(baseType.kind) || baseType.fields == null)
        {
            return null;
        }
        GraphQLIntrospectionField edges = findField(baseType.fields, "edges");
        GraphQLIntrospectionField pageInfo = findField(baseType.fields, "pageInfo");
        if (edges == null || pageInfo == null)
        {
            return null;
        }
        UnwrappedType edgesUnwrapped = unwrap(edges.type, typesByName);
        if (edgesUnwrapped.baseType == null || !"OBJECT".equals(edgesUnwrapped.baseType.kind) || edgesUnwrapped.baseType.fields == null)
        {
            return null;
        }
        GraphQLIntrospectionField node = findField(edgesUnwrapped.baseType.fields, "node");
        if (node == null)
        {
            return null;
        }
        UnwrappedType pageInfoUnwrapped = unwrap(pageInfo.type, typesByName);
        if (pageInfoUnwrapped.baseType == null || !"OBJECT".equals(pageInfoUnwrapped.baseType.kind) || pageInfoUnwrapped.baseType.fields == null)
        {
            return null;
        }
        boolean hasHasNextPage = findField(pageInfoUnwrapped.baseType.fields, "hasNextPage") != null;
        boolean hasEndCursor = findField(pageInfoUnwrapped.baseType.fields, "endCursor") != null;
        if (!hasHasNextPage || !hasEndCursor)
        {
            return null;
        }
        return unwrap(node.type, typesByName).baseType;
    }

    public static GraphQLIntrospectionField findField(List<GraphQLIntrospectionField> fields, String name)
    {
        return ListIterate.detect(fields, f -> name.equals(f.name));
    }

    public static GraphQLIntrospectionType findTypeByName(List<GraphQLIntrospectionType> types, String name)
    {
        return ListIterate.detect(types, t -> name.equals(t.name));
    }
}
