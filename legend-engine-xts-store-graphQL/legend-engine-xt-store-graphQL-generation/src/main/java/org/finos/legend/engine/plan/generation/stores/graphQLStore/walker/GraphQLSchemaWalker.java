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

package org.finos.legend.engine.plan.generation.stores.graphQLStore.walker;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.protocol.graphQLStore.introspection.GraphQLIntrospectionField;
import org.finos.legend.engine.protocol.graphQLStore.introspection.GraphQLIntrospectionInputValue;
import org.finos.legend.engine.protocol.graphQLStore.introspection.GraphQLIntrospectionSchema;
import org.finos.legend.engine.protocol.graphQLStore.introspection.GraphQLIntrospectionType;
import org.finos.legend.engine.protocol.graphQLStore.introspection.UnwrappedType;

import java.util.List;
import java.util.Map;

public class GraphQLSchemaWalker
{
    private final org.finos.legend.engine.protocol.graphQLStore.introspection.GraphQLSchemaWalker shared = new org.finos.legend.engine.protocol.graphQLStore.introspection.GraphQLSchemaWalker();

    public static boolean isValidGraphQLName(String name)
    {
        return org.finos.legend.engine.protocol.graphQLStore.introspection.GraphQLSchemaWalker.isValidGraphQLName(name);
    }

    public static Map<String, GraphQLIntrospectionType> typesByName(GraphQLIntrospectionSchema schema)
    {
        return org.finos.legend.engine.protocol.graphQLStore.introspection.GraphQLSchemaWalker.typesByName(schema);
    }

    public UnwrappedType unwrap(GraphQLIntrospectionType type, Map<String, GraphQLIntrospectionType> typesByName)
    {
        return shared.unwrap(type, typesByName);
    }

    public boolean isRelayConnection(GraphQLIntrospectionType baseType, Map<String, GraphQLIntrospectionType> typesByName)
    {
        return shared.isRelayConnection(baseType, typesByName);
    }

    public GraphQLIntrospectionType relayNodeType(GraphQLIntrospectionType baseType, Map<String, GraphQLIntrospectionType> typesByName)
    {
        return shared.relayNodeType(baseType, typesByName);
    }

    public static String pureTypeForGraphQLScalar(String graphQLScalarName)
    {
        if (graphQLScalarName == null)
        {
            return null;
        }
        switch (graphQLScalarName)
        {
            case "Int":
                return "Integer";
            case "String":
                return "String";
            case "Boolean":
                return "Boolean";
            case "Float":
                return "Float";
            case "ID":
                return "String";
            default:
                return null;
        }
    }

    public List<QueryWrapperFieldCandidate> selectQueryWrapperFields(GraphQLIntrospectionSchema schema, MutableList<String> skipReasons)
    {
        MutableList<QueryWrapperFieldCandidate> results = Lists.mutable.empty();
        if (schema.queryType == null || schema.queryType.name == null || schema.types == null)
        {
            return results;
        }
        Map<String, GraphQLIntrospectionType> typesByName = typesByName(schema);
        GraphQLIntrospectionType queryType = org.finos.legend.engine.protocol.graphQLStore.introspection.GraphQLSchemaWalker.findTypeByName(schema.types, schema.queryType.name);
        if (queryType == null || queryType.fields == null)
        {
            return results;
        }

        for (GraphQLIntrospectionField field : queryType.fields)
        {
            if (!isValidGraphQLName(field.name))
            {
                skipReasons.add(queryType.name + "." + field.name + ": field name is not a valid GraphQL identifier");
                continue;
            }
            UnwrappedType unwrapped = unwrap(field.type, typesByName);
            if (unwrapped.baseType == null || unwrapped.baseType.name == null)
            {
                skipReasons.add(queryType.name + "." + field.name + ": return type could not be resolved from the introspection response");
                continue;
            }
            if (isRelayConnection(unwrapped.baseType, typesByName))
            {
                skipReasons.add(queryType.name + "." + field.name + ": pagination is not supported (Relay-connection shaped)");
                continue;
            }
            String kind = unwrapped.baseType.kind;
            if (!"SCALAR".equals(kind) && !"ENUM".equals(kind) && !"OBJECT".equals(kind) && !"INTERFACE".equals(kind) && !"UNION".equals(kind))
            {
                skipReasons.add(queryType.name + "." + field.name + ": return type is not representable (" + kind + ") - map manually");
                continue;
            }

            if (field.args == null || field.args.isEmpty())
            {
                results.add(QueryWrapperFieldCandidate.plain(field));
                continue;
            }

            String unsupportedArgument = firstUnsupportedArgument(field, typesByName);
            if (unsupportedArgument != null)
            {
                skipReasons.add(queryType.name + "." + field.name + ": argument '" + unsupportedArgument + "' is not a supported scalar type for Query-wrapper generation - map manually");
                continue;
            }
            results.add(QueryWrapperFieldCandidate.qualified(field));
        }
        return results;
    }

    private String firstUnsupportedArgument(GraphQLIntrospectionField field, Map<String, GraphQLIntrospectionType> typesByName)
    {
        for (GraphQLIntrospectionInputValue argument : field.args)
        {
            UnwrappedType argUnwrapped = unwrap(argument.type, typesByName);
            if (argUnwrapped.baseType == null)
            {
                return argument.name;
            }
            String kind = argUnwrapped.baseType.kind;
            boolean supported = "ENUM".equals(kind) || "INPUT_OBJECT".equals(kind)
                    ? isValidGraphQLName(argUnwrapped.baseType.name)
                    : "SCALAR".equals(kind) && pureTypeForGraphQLScalar(argUnwrapped.baseType.name) != null;
            if (!supported)
            {
                return argument.name;
            }
        }
        return null;
    }
}
