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

package org.finos.legend.engine.plan.generation.stores.graphQLStore.synthesize;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.generation.stores.graphQLStore.walker.GraphQLSchemaWalker;
import org.finos.legend.engine.protocol.graphQLStore.introspection.GraphQLIntrospectionField;
import org.finos.legend.engine.protocol.graphQLStore.introspection.GraphQLIntrospectionType;
import org.finos.legend.engine.protocol.graphQLStore.introspection.UnwrappedType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class GraphQLJsonSchemaSynthesizer
{
    private final GraphQLSchemaWalker walker;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GraphQLJsonSchemaSynthesizer(GraphQLSchemaWalker walker)
    {
        this.walker = walker;
    }

    public String toJsonSchemaText(GraphQLIntrospectionType entityType, String fullyQualifiedClassName, Map<String, GraphQLIntrospectionType> typesByName, MutableList<String> warnings)
    {
        String packagePrefix = packageOf(fullyQualifiedClassName);

        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("$schema", "http://json-schema.org/draft-07/schema#");
        schema.put("title", fullyQualifiedClassName);
        schema.put("$ref", "#/definitions/" + fullyQualifiedClassName);

        ObjectNode definitions = schema.putObject("definitions");
        Set<String> defined = new TreeSet<>();
        defineObjectType(definitions, entityType, fullyQualifiedClassName, packagePrefix, defined, typesByName, warnings);

        return schema.toString();
    }

    private void defineObjectType(ObjectNode definitions, GraphQLIntrospectionType objectType, String fullyQualifiedName, String packagePrefix, Set<String> defined, Map<String, GraphQLIntrospectionType> typesByName, MutableList<String> warnings)
    {
        defined.add(fullyQualifiedName);
        ObjectNode node = definitions.putObject(fullyQualifiedName);
        populateObjectSchema(node, objectType, definitions, packagePrefix, defined, typesByName, warnings, new TreeSet<>());
    }

    // excludedFieldNames lets a polymorphic subtype's own definition skip re-declaring fields already
    // carried by its base type's definition (reached via allOf/$ref) - without this, a field would be
    // declared twice in the JSON Schema (once on the base, once redundantly on every subtype).
    private void populateObjectSchema(ObjectNode node, GraphQLIntrospectionType objectType, ObjectNode definitions, String packagePrefix, Set<String> defined, Map<String, GraphQLIntrospectionType> typesByName, MutableList<String> warnings, Set<String> excludedFieldNames)
    {
        node.put("type", "object");
        ObjectNode properties = node.putObject("properties");
        ArrayNode required = objectMapper.createArrayNode();

        if (objectType.fields != null)
        {
            for (GraphQLIntrospectionField field : objectType.fields)
            {
                if (excludedFieldNames.contains(field.name))
                {
                    continue;
                }
                if (!GraphQLSchemaWalker.isValidGraphQLName(field.name))
                {
                    warnings.add(objectType.name + "." + field.name + ": field name is not a valid GraphQL identifier, omitted from generated schema");
                    continue;
                }
                UnwrappedType unwrapped = walker.unwrap(field.type, typesByName);
                if (unwrapped.baseType == null)
                {
                    continue;
                }
                ObjectNode fieldSchema = toFieldSchema(unwrapped, definitions, packagePrefix, defined, typesByName, warnings, objectType.name + "." + field.name);
                if (fieldSchema == null)
                {
                    continue;
                }
                properties.set(field.name, fieldSchema);
                if (unwrapped.isNonNull)
                {
                    required.add(field.name);
                }
            }
        }

        if (required.size() > 0)
        {
            node.set("required", required);
        }
    }

    private ObjectNode toFieldSchema(UnwrappedType unwrapped, ObjectNode definitions, String packagePrefix, Set<String> defined, Map<String, GraphQLIntrospectionType> typesByName, MutableList<String> warnings, String owningFieldPath)
    {
        ObjectNode fieldSchema = objectMapper.createObjectNode();
        if (unwrapped.isList)
        {
            fieldSchema.put("type", "array");
            ObjectNode items = fieldSchema.putObject("items");
            if (!populateLeafOrRefSchema(items, unwrapped.baseType, definitions, packagePrefix, defined, typesByName, warnings, owningFieldPath))
            {
                return null;
            }
            return fieldSchema;
        }
        return populateLeafOrRefSchema(fieldSchema, unwrapped.baseType, definitions, packagePrefix, defined, typesByName, warnings, owningFieldPath) ? fieldSchema : null;
    }

    private boolean populateLeafOrRefSchema(ObjectNode target, GraphQLIntrospectionType baseType, ObjectNode definitions, String packagePrefix, Set<String> defined, Map<String, GraphQLIntrospectionType> typesByName, MutableList<String> warnings, String owningFieldPath)
    {
        if ("SCALAR".equals(baseType.kind))
        {
            target.put("type", scalarToJsonSchemaType(baseType.name));
            return true;
        }
        // Referenced via $ref/definitions like OBJECT below - an inline "enum" keyword with no $ref is
        // NOT picked up as a real Enumeration by the JSON-Schema-to-Pure generator this feeds into.
        if ("ENUM".equals(baseType.kind))
        {
            if (!GraphQLSchemaWalker.isValidGraphQLName(baseType.name))
            {
                warnings.add(owningFieldPath + ": referenced enum type name is not a valid GraphQL identifier, field omitted from generated schema");
                return false;
            }
            if (baseType.enumValues == null || baseType.enumValues.isEmpty())
            {
                warnings.add(owningFieldPath + ": referenced enum type '" + baseType.name + "' has no enum values in the introspection response, field omitted from generated schema");
                return false;
            }
            String fullyQualifiedName = packagePrefix + "::" + baseType.name;
            target.put("$ref", "#/definitions/" + fullyQualifiedName);
            if (!defined.contains(fullyQualifiedName))
            {
                defineEnumType(definitions, baseType, fullyQualifiedName, defined);
            }
            return true;
        }
        if ("OBJECT".equals(baseType.kind))
        {
            if (!GraphQLSchemaWalker.isValidGraphQLName(baseType.name))
            {
                warnings.add(owningFieldPath + ": referenced type name is not a valid GraphQL identifier, field omitted from generated schema");
                return false;
            }
            String fullyQualifiedName = packagePrefix + "::" + baseType.name;
            target.put("$ref", "#/definitions/" + fullyQualifiedName);
            if (!defined.contains(fullyQualifiedName))
            {
                defineObjectType(definitions, baseType, fullyQualifiedName, packagePrefix, defined, typesByName, warnings);
            }
            return true;
        }
        // Resolved at execution time via GraphQL's own __typename, not via query shape (unlike most other
        // stores' subtype resolution)
        if ("INTERFACE".equals(baseType.kind) || "UNION".equals(baseType.kind))
        {
            if (!GraphQLSchemaWalker.isValidGraphQLName(baseType.name))
            {
                warnings.add(owningFieldPath + ": referenced interface/union type name is not a valid GraphQL identifier, field omitted from generated schema");
                return false;
            }
            if (baseType.possibleTypes == null || baseType.possibleTypes.isEmpty())
            {
                warnings.add(owningFieldPath + ": referenced interface/union type '" + baseType.name + "' has no known implementations in the introspection response, field omitted from generated schema");
                return false;
            }
            String fullyQualifiedName = packagePrefix + "::" + baseType.name;
            if (!defined.contains(fullyQualifiedName))
            {
                definePolymorphicBaseType(definitions, baseType, fullyQualifiedName, packagePrefix, defined, typesByName, warnings);
            }
            String wrapperName = fullyQualifiedName + "_OneOf";
            target.put("$ref", "#/definitions/" + wrapperName);
            if (!defined.contains(wrapperName))
            {
                defineOneOfWrapper(definitions, baseType, wrapperName, packagePrefix, defined, warnings);
            }
            return true;
        }
        return false;
    }

    private void defineOneOfWrapper(ObjectNode definitions, GraphQLIntrospectionType baseType, String wrapperName, String packagePrefix, Set<String> defined, MutableList<String> warnings)
    {
        defined.add(wrapperName);
        ObjectNode node = definitions.putObject(wrapperName);
        ArrayNode oneOf = objectMapper.createArrayNode();
        ObjectNode discriminator = objectMapper.createObjectNode();
        discriminator.put("propertyName", "__typename");
        ObjectNode mapping = objectMapper.createObjectNode();

        for (GraphQLIntrospectionType possibleType : baseType.possibleTypes)
        {
            if (!GraphQLSchemaWalker.isValidGraphQLName(possibleType.name))
            {
                warnings.add(wrapperName + ": possible type name is not a valid GraphQL identifier, omitted from oneOf wrapper");
                continue;
            }
            String subTypeFullyQualifiedName = packagePrefix + "::" + possibleType.name;
            ObjectNode ref = objectMapper.createObjectNode();
            ref.put("$ref", "#/definitions/" + subTypeFullyQualifiedName);
            oneOf.add(ref);
            mapping.put(possibleType.name, "#/definitions/" + subTypeFullyQualifiedName);
        }
        node.set("oneOf", oneOf);
        discriminator.set("mapping", mapping);
        node.set("discriminator", discriminator);
    }

    private void definePolymorphicBaseType(ObjectNode definitions, GraphQLIntrospectionType baseType, String fullyQualifiedName, String packagePrefix, Set<String> defined, Map<String, GraphQLIntrospectionType> typesByName, MutableList<String> warnings)
    {
        defined.add(fullyQualifiedName);
        ObjectNode node = definitions.putObject(fullyQualifiedName);
        populateObjectSchema(node, baseType, definitions, packagePrefix, defined, typesByName, warnings, new TreeSet<>());

        // The discriminator's propertyName must be a real schema property (OpenAPI convention) - the
        // generic JSON-Schema-to-Pure generator's discriminator-constraint logic navigates it as an actual
        // Pure property post-deserialization, not just a raw JSON field, so it has to be declared here too.
        ((ObjectNode) node.get("properties")).putObject("__typename").put("type", "string");

        ObjectNode discriminator = node.putObject("discriminator");
        discriminator.put("propertyName", "__typename");
        ObjectNode mapping = discriminator.putObject("mapping");

        // The discriminator stays flat regardless of how many interface levels sit between the root and a
        // leaf (it maps a concrete __typename straight to its own schema) - only the leaf's own allOf/$ref
        // chain (built below via ensureIntermediateInterfaceTypeDefined) reflects the real hierarchy depth.
        for (GraphQLIntrospectionType possibleType : baseType.possibleTypes)
        {
            if (!GraphQLSchemaWalker.isValidGraphQLName(possibleType.name))
            {
                warnings.add(fullyQualifiedName + ": possible type name is not a valid GraphQL identifier, omitted from discriminator mapping");
                continue;
            }
            String subTypeFullyQualifiedName = packagePrefix + "::" + possibleType.name;
            mapping.put(possibleType.name, "#/definitions/" + subTypeFullyQualifiedName);
            if (!defined.contains(subTypeFullyQualifiedName))
            {
                GraphQLIntrospectionType resolvedPossibleType = typesByName.getOrDefault(possibleType.name, possibleType);
                List<GraphQLIntrospectionType> directParents = computeDirectParentInterfaces(resolvedPossibleType, baseType, typesByName);
                if (directParents.isEmpty())
                {
                    directParents = Collections.singletonList(baseType);
                }
                Set<String> baseFieldNames = new TreeSet<>();
                List<String> baseFullyQualifiedNames = new ArrayList<>();
                for (GraphQLIntrospectionType directParent : directParents)
                {
                    baseFieldNames.addAll(ensureIntermediateInterfaceTypeDefined(directParent, baseType, packagePrefix, definitions, defined, typesByName, warnings));
                    baseFullyQualifiedNames.add(packagePrefix + "::" + directParent.name);
                }
                defineSubType(definitions, resolvedPossibleType, subTypeFullyQualifiedName, baseFullyQualifiedNames, packagePrefix, defined, typesByName, warnings, baseFieldNames);
            }
        }
    }

    private List<GraphQLIntrospectionType> computeDirectParentInterfaces(GraphQLIntrospectionType type, GraphQLIntrospectionType root, Map<String, GraphQLIntrospectionType> typesByName)
    {
        if (type.interfaces == null || type.interfaces.isEmpty())
        {
            return Collections.emptyList();
        }
        List<GraphQLIntrospectionType> candidates = type.interfaces.stream()
                .map(ref -> typesByName.getOrDefault(ref.name, ref))
                .filter(i -> !i.name.equals(type.name) && !i.name.equals(root.name))
                .filter(i -> isTransitiveSubInterfaceOf(i, root, typesByName, new HashSet<>()))
                .collect(Collectors.toList());
        return candidates.stream()
                .filter(i -> candidates.stream().noneMatch(j -> !j.name.equals(i.name) && isTransitiveSubInterfaceOf(j, i, typesByName, new HashSet<>())))
                .collect(Collectors.toList());
    }

    private boolean isTransitiveSubInterfaceOf(GraphQLIntrospectionType type, GraphQLIntrospectionType ancestor, Map<String, GraphQLIntrospectionType> typesByName, Set<String> visited)
    {
        if (type.name.equals(ancestor.name))
        {
            return true;
        }
        if (type.interfaces == null || !visited.add(type.name))
        {
            return false;
        }
        return type.interfaces.stream().anyMatch(ref -> isTransitiveSubInterfaceOf(typesByName.getOrDefault(ref.name, ref), ancestor, typesByName, visited));
    }

    private Set<String> ensureIntermediateInterfaceTypeDefined(GraphQLIntrospectionType interfaceType, GraphQLIntrospectionType root, String packagePrefix, ObjectNode definitions, Set<String> defined, Map<String, GraphQLIntrospectionType> typesByName, MutableList<String> warnings)
    {
        if (interfaceType.name.equals(root.name))
        {
            Set<String> rootFieldNames = new TreeSet<>();
            if (root.fields != null)
            {
                root.fields.forEach(f -> rootFieldNames.add(f.name));
            }
            return rootFieldNames;
        }

        List<GraphQLIntrospectionType> directParents = computeDirectParentInterfaces(interfaceType, root, typesByName);
        if (directParents.isEmpty())
        {
            directParents = Collections.singletonList(root);
        }
        Set<String> inheritedFieldNames = new TreeSet<>();
        List<String> parentFullyQualifiedNames = new ArrayList<>();
        for (GraphQLIntrospectionType parent : directParents)
        {
            inheritedFieldNames.addAll(ensureIntermediateInterfaceTypeDefined(parent, root, packagePrefix, definitions, defined, typesByName, warnings));
            parentFullyQualifiedNames.add(packagePrefix + "::" + parent.name);
        }

        String fullyQualifiedName = packagePrefix + "::" + interfaceType.name;
        if (!defined.contains(fullyQualifiedName))
        {
            defineSubType(definitions, interfaceType, fullyQualifiedName, parentFullyQualifiedNames, packagePrefix, defined, typesByName, warnings, inheritedFieldNames);
        }

        Set<String> ownFieldNames = new TreeSet<>(inheritedFieldNames);
        if (interfaceType.fields != null)
        {
            interfaceType.fields.forEach(f -> ownFieldNames.add(f.name));
        }
        return ownFieldNames;
    }

    private void defineSubType(ObjectNode definitions, GraphQLIntrospectionType subType, String fullyQualifiedName, List<String> baseFullyQualifiedNames, String packagePrefix, Set<String> defined, Map<String, GraphQLIntrospectionType> typesByName, MutableList<String> warnings, Set<String> baseFieldNames)
    {
        defined.add(fullyQualifiedName);
        ObjectNode node = definitions.putObject(fullyQualifiedName);
        ArrayNode allOf = objectMapper.createArrayNode();
        for (String baseFullyQualifiedName : baseFullyQualifiedNames)
        {
            ObjectNode ref = objectMapper.createObjectNode();
            ref.put("$ref", "#/definitions/" + baseFullyQualifiedName);
            allOf.add(ref);
        }
        node.set("allOf", allOf);
        populateObjectSchema(node, subType, definitions, packagePrefix, defined, typesByName, warnings, baseFieldNames);
    }

    private void defineEnumType(ObjectNode definitions, GraphQLIntrospectionType enumType, String fullyQualifiedName, Set<String> defined)
    {
        defined.add(fullyQualifiedName);
        ObjectNode node = definitions.putObject(fullyQualifiedName);
        node.put("type", "string");
        ArrayNode enumValues = objectMapper.createArrayNode();
        enumType.enumValues.forEach(v -> enumValues.add(v.name));
        node.set("enum", enumValues);
    }

    private static String packageOf(String fullyQualifiedClassName)
    {
        int sepPos = fullyQualifiedClassName.lastIndexOf("::");
        return sepPos < 0 ? fullyQualifiedClassName : fullyQualifiedClassName.substring(0, sepPos);
    }

    private static String scalarToJsonSchemaType(String graphQLScalarName)
    {
        if (graphQLScalarName == null)
        {
            return "string";
        }
        switch (graphQLScalarName)
        {
            case "Int":
                return "integer";
            case "Float":
                return "number";
            case "Boolean":
                return "boolean";
            case "String":
            case "ID":
            default:
                return "string";
        }
    }
}
