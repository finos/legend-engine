//  Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.query.graphQL.api.execute;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.protocol.graphQL.metamodel.Definition;
import org.finos.legend.engine.protocol.graphQL.metamodel.DefinitionVisitor;
import org.finos.legend.engine.protocol.graphQL.metamodel.Directive;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.graphQL.metamodel.Document;
import org.finos.legend.engine.protocol.graphQL.metamodel.ProtocolToMetamodelTranslator;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.Argument;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.ExecutableDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.Field;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.FragmentDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.FragmentSpread;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.InLineFragment;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.OperationDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.OperationType;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.Selection;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.SelectionVisitor;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.VariableDefinition;

import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.DirectiveDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.EnumTypeDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.InputObjectTypeDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.InterfaceTypeDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.ListTypeReference;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.NamedTypeReference;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.ObjectTypeDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.ScalarTypeDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.SchemaDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.Type;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.TypeReference;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.TypeSystemDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.UnionTypeDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.value.BooleanValue;
import org.finos.legend.engine.protocol.graphQL.metamodel.value.EnumValue;
import org.finos.legend.engine.protocol.graphQL.metamodel.value.FloatValue;
import org.finos.legend.engine.protocol.graphQL.metamodel.value.IntValue;
import org.finos.legend.engine.protocol.graphQL.metamodel.value.ListValue;
import org.finos.legend.engine.protocol.graphQL.metamodel.value.NullValue;
import org.finos.legend.engine.protocol.graphQL.metamodel.value.ObjectField;
import org.finos.legend.engine.protocol.graphQL.metamodel.value.ObjectValue;
import org.finos.legend.engine.protocol.graphQL.metamodel.value.StringValue;
import org.finos.legend.engine.protocol.graphQL.metamodel.value.Value;
import org.finos.legend.engine.protocol.graphQL.metamodel.value.ValueVisitor;
import org.finos.legend.engine.protocol.graphQL.metamodel.value.Variable;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.Objects;

public class GraphQLExecutionHelper
{
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.getNewStandardObjectMapper();
    private static final ObjectMapper OBJECT_ARGUMENT_MAPPER = ObjectMapperFactory.getNewStandardObjectMapper().setSerializationInclusion(JsonInclude.Include.ALWAYS);

    public static org.finos.legend.pure.generated.Root_meta_external_query_graphQL_metamodel_sdl_Document toPureModel(Document document, PureModel pureModel)
    {
        return new ProtocolToMetamodelTranslator().translate(document, pureModel);
    }

    static final class VariableContext
    {
        static final VariableContext EMPTY = new VariableContext(Collections.emptyMap(), NullNode.getInstance());
        static final VariableContext MUTATION = new VariableContext(Collections.emptyMap(), NullNode.getInstance());

        final Map<String, VariableDefinition> definitions;
        final JsonNode values;

        VariableContext(Map<String, VariableDefinition> definitions, JsonNode values)
        {
            this.definitions = definitions;
            this.values = values;
        }

        static VariableContext from(OperationDefinition operationDefinition, String variablesJson)
        {
            // Mutations carry their whole 'variables' string as a Binding payload, so it must not be parsed here
            if (operationDefinition.type == OperationType.mutation)
            {
                return MUTATION;
            }
            if (operationDefinition.variables.isEmpty())
            {
                return EMPTY;
            }
            Map<String, VariableDefinition> definitions = operationDefinition.variables.stream()
                    .collect(Collectors.toMap(v -> v.name.startsWith("$") ? v.name.substring(1) : v.name, v -> v, (first, second) ->
                    {
                        throw new RuntimeException("Variable '" + first.name + "' is declared more than once");
                    }));
            JsonNode values;
            if (variablesJson == null || variablesJson.trim().isEmpty())
            {
                values = NullNode.getInstance();
            }
            else
            {
                try
                {
                    values = OBJECT_MAPPER.readTree(variablesJson);
                }
                catch (IOException e)
                {
                    throw new RuntimeException("Unable to parse 'variables' as JSON: " + e.getMessage(), e);
                }
                if (!values.isObject() && !values.isNull())
                {
                    throw new RuntimeException("'variables' must be a JSON object but was " + values.getNodeType());
                }
            }
            return new VariableContext(definitions, values);
        }
    }

    static Value resolveVariableValue(String variableName, VariableContext variableContext)
    {
        if (variableContext == VariableContext.MUTATION)
        {
            throw new RuntimeException("Variable '$" + variableName + "' cannot be used in a mutation argument; a mutation's 'variables' are passed to its binding as a whole");
        }
        VariableDefinition definition = variableContext.definitions.get(variableName);
        if (definition == null)
        {
            throw new RuntimeException("Variable '$" + variableName + "' is used but not declared on the operation");
        }
        JsonNode node = variableContext.values.get(variableName);
        if (node == null)
        {
            if (definition.defaultValue != null)
            {
                return definition.defaultValue;
            }
            if (definition.type.nullable)
            {
                return new NullValue();
            }
            throw new RuntimeException("Missing value for required variable '$" + variableName + "'");
        }
        return jsonToValue(node, definition.type, variableName);
    }

    private static final Set<String> BUILT_IN_SCALARS = new HashSet<>(Arrays.asList("Int", "Float", "String", "Boolean", "ID"));

    private static RuntimeException invalidVariableValue(String variableName, String expected, JsonNode node)
    {
        return new RuntimeException("Invalid value for variable '$" + variableName + "': expected " + expected + " but got " + node.getNodeType());
    }

    private static String namedTypeName(TypeReference declaredType)
    {
        return declaredType instanceof NamedTypeReference ? ((NamedTypeReference) declaredType).name : null;
    }

    private static void requireBuiltInScalarCompatibility(String variableName, TypeReference declaredType, JsonNode node, String... compatibleScalars)
    {
        String scalarName = namedTypeName(declaredType);
        if (BUILT_IN_SCALARS.contains(scalarName) && !Arrays.asList(compatibleScalars).contains(scalarName))
        {
            throw invalidVariableValue(variableName, scalarName, node);
        }
    }

    private static ListValue jsonArrayToListValue(JsonNode array, TypeReference itemType, String variableName)
    {
        ListValue listValue = new ListValue();
        List<Value> values = new ArrayList<>();
        array.forEach(element -> values.add(jsonToValue(element, itemType, variableName)));
        listValue.values = values;
        return listValue;
    }

    private static Value jsonToValue(JsonNode node, TypeReference declaredType, String variableName)
    {
        if (node.isNull())
        {
            if (declaredType != null && !declaredType.nullable)
            {
                throw invalidVariableValue(variableName, "a non-null value", node);
            }
            return new NullValue();
        }
        if (declaredType instanceof ListTypeReference)
        {
            TypeReference itemType = ((ListTypeReference) declaredType).itemType;
            if (node.isArray())
            {
                return jsonArrayToListValue(node, itemType, variableName);
            }
            ListValue singleElementList = new ListValue();
            singleElementList.values = Collections.singletonList(jsonToValue(node, itemType, variableName));
            return singleElementList;
        }

        switch (node.getNodeType())
        {
            case STRING:
            {
                requireBuiltInScalarCompatibility(variableName, declaredType, node, "String", "ID");
                // An Enum scalar value and a String scalar value are indistinguishable at the JSON level
                StringValue stringValue = new StringValue();
                stringValue.value = node.textValue();
                return stringValue;
            }
            case BOOLEAN:
            {
                requireBuiltInScalarCompatibility(variableName, declaredType, node, "Boolean");
                BooleanValue booleanValue = new BooleanValue();
                booleanValue.value = node.booleanValue();
                return booleanValue;
            }
            case NUMBER:
            {
                boolean isFloatTyped = "Float".equals(namedTypeName(declaredType));
                boolean isIdTyped = "ID".equals(namedTypeName(declaredType));
                if (node.isIntegralNumber())
                {
                    requireBuiltInScalarCompatibility(variableName, declaredType, node, "Int", "Float", "ID");
                    if (isIdTyped)
                    {
                        StringValue idValue = new StringValue();
                        idValue.value = node.asText();
                        return idValue;
                    }
                    if (!isFloatTyped && node.canConvertToLong())
                    {
                        IntValue intValue = new IntValue();
                        intValue.value = node.longValue();
                        return intValue;
                    }
                    if (!isFloatTyped)
                    {
                        throw invalidVariableValue(variableName, "an Int within the 64-bit range", node);
                    }
                }
                else
                {
                    requireBuiltInScalarCompatibility(variableName, declaredType, node, "Float");
                }
                FloatValue floatValue = new FloatValue();
                floatValue.value = node.doubleValue();
                return floatValue;
            }
            case OBJECT:
            {
                requireBuiltInScalarCompatibility(variableName, declaredType, node);
                ObjectValue objectValue = new ObjectValue();
                List<ObjectField> fields = new ArrayList<>();
                node.fields().forEachRemaining(entry ->
                {
                    ObjectField field = new ObjectField();
                    field.name = entry.getKey();
                    field.value = jsonToValue(entry.getValue(), null, variableName);
                    fields.add(field);
                });
                objectValue.fields = fields;
                return objectValue;
            }
            case ARRAY:
            {
                if (declaredType != null)
                {
                    throw invalidVariableValue(variableName, "a non-list value", node);
                }
                return jsonArrayToListValue(node, null, variableName);
            }
            default:
                throw new RuntimeException("Unsupported JSON value for variable '$" + variableName + "': " + node.getNodeType());
        }
    }

    private static String objectValueToJson(ObjectValue objectValue, VariableContext variableContext)
    {
        try
        {
            return OBJECT_ARGUMENT_MAPPER.writeValueAsString(objectValueToJsonMap(objectValue, variableContext));
        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException("Unable to serialize an object argument as JSON: " + e.getMessage(), e);
        }
    }

    private static Map<String, Object> objectValueToJsonMap(ObjectValue objectValue, VariableContext variableContext)
    {
        Map<String, Object> map = new LinkedHashMap<>();
        objectValue.fields.forEach(field -> map.put(field.name, valueToJsonStructure(field.value, variableContext)));
        return map;
    }

    private static Object valueToJsonStructure(Value value, VariableContext variableContext)
    {
        Value resolved = value instanceof Variable ? resolveVariableValue(((Variable) value).name, variableContext) : value;
        if (resolved instanceof ObjectValue)
        {
            return objectValueToJsonMap((ObjectValue) resolved, variableContext);
        }
        if (resolved instanceof ListValue)
        {
            return ((ListValue) resolved).values.stream().map(element -> valueToJsonStructure(element, variableContext)).collect(Collectors.toList());
        }
        return argumentValueToObject(resolved, variableContext);
    }

    static Object argumentValueToObject(Value value, VariableContext variableContext)
    {
        return value.accept(new ValueVisitor<Object>()
        {
            @Override
            public Object visit(BooleanValue val)
            {
                return val.value;
            }

            @Override
            public Object visit(EnumValue val)
            {
                return val.value;
            }

            @Override
            public Object visit(FloatValue val)
            {
                return val.value;
            }

            @Override
            public Object visit(IntValue val)
            {
                return val.value;
            }

            @Override
            public Object visit(ListValue val)
            {
                return val.values.stream().map(v -> argumentValueToObject(v, variableContext)).collect(Collectors.toList());
            }

            @Override
            public Object visit(NullValue val)
            {
                return null;
            }

            @Override
            public Object visit(ObjectValue val)
            {
                return objectValueToJson(val, variableContext);
            }

            @Override
            public Object visit(StringValue val)
            {
                return val.value;
            }

            @Override
            public Object visit(Variable val)
            {
                return resolveVariableValue(val.name, variableContext).accept(this);
            }
        });
    }

    static Field extractFieldByName(OperationDefinition definition, String propertyName)
    {
        return definition.selectionSet.stream().map(s -> s.accept(new SelectionVisitor<Field>()
        {
            @Override
            public Field visit(Field val)
            {
                return val.name.equals(propertyName) ? val : null;
            }

            @Override
            public Field visit(FragmentSpread val)
            {
                return null;
            }

            @Override
            public Field visit(InLineFragment val)
            {
                return null;
            }
        })).collect(Collectors.toList()).get(0);
    }

    static boolean isARootField(String propertyName, OperationDefinition graphQLQuery)
    {
        return extractFieldByName(graphQLQuery, propertyName) != null;
    }

    static List<Directive> findDirectives(OperationDefinition query)
    {
        // assuming there's only root field
        return ((Field)(query.selectionSet.get(0))).directives.stream().distinct().collect(Collectors.toList());
    }

    static String getPlanNameForDirective(String rootFieldName, Directive directive)
    {
        return rootFieldName + '@' + directive.name;
    }

    static OperationDefinition findQuery(Document document)
    {
        Collection<Definition> res = Iterate.select(document.definitions, d -> d.accept(new DefinitionVisitor<Boolean>()
            {

                @Override
                public Boolean visit(DirectiveDefinition val)
                {
                    return false;
                }

                @Override
                public Boolean visit(EnumTypeDefinition val)
                {
                    return false;
                }

                @Override
                public Boolean visit(ExecutableDefinition val)
                {
                    return false;
                }

                @Override
                public Boolean visit(FragmentDefinition val)
                {
                    return false;
                }

                @Override
                public Boolean visit(InterfaceTypeDefinition val)
                {
                    return false;
                }

                @Override
                public Boolean visit(ObjectTypeDefinition val)
                {
                    return false;
                }

                @Override
                public Boolean visit(InputObjectTypeDefinition val)
                {
                    return false;
                }

                @Override
                public Boolean visit(OperationDefinition val)
                {
                    return val.type == OperationType.query || val.type == OperationType.mutation;
                }

                @Override
                public Boolean visit(ScalarTypeDefinition val)
                {
                    return false;
                }

                @Override
                public Boolean visit(SchemaDefinition val)
                {
                    return false;
                }

                @Override
                public Boolean visit(Type val)
                {
                    return false;
                }

                @Override
                public Boolean visit(TypeSystemDefinition val)
                {
                    return false;
                }

                @Override
                public Boolean visit(UnionTypeDefinition val)
                {
                    return false;
                }
            }
        ));

        if (res.isEmpty())
        {
            throw new RuntimeException("Please provide a query");
        }
        else if (res.size() > 1)
        {
            throw new RuntimeException("Found more than one query");
        }
        else
        {
            return (OperationDefinition) res.iterator().next();
        }
    }

    static final String WHERE_VARIABLES_UNSUPPORTED_MESSAGE = "Variables are not supported inside the 'where' argument; provide the filter values inline in the query";

    private static boolean containsVariable(Value value)
    {
        if (value instanceof Variable)
        {
            return true;
        }
        if (value instanceof ObjectValue)
        {
            return ((ObjectValue) value).fields.stream().anyMatch(field -> containsVariable(field.value));
        }
        if (value instanceof ListValue)
        {
            return ((ListValue) value).values.stream().anyMatch(GraphQLExecutionHelper::containsVariable);
        }
        return false;
    }

    // Must run before the plan cache is consulted: a literal and a variable in the same position share a cache key
    static void rejectVariablesInWhereArguments(OperationDefinition operationDefinition)
    {
        for (Selection selection : operationDefinition.selectionSet)
        {
            if (selection instanceof Field)
            {
                Field field = (Field) selection;
                for (Argument argument : field.arguments)
                {
                    if (argument.name.equals("where") && containsVariable(argument.value))
                    {
                        throw new RuntimeException(WHERE_VARIABLES_UNSUPPORTED_MESSAGE + " (field '" + field.name + "')");
                    }
                }
            }
        }
    }

    static Map<String, Result> getParameterMap(OperationDefinition graphQLQuery, String fieldName, VariableContext variableContext)
    {
        Map<String, Result> parameterMap = new HashMap<>();
        extractFieldByName(graphQLQuery, fieldName).arguments.forEach(a ->
            {
                if (a.name.equals("where"))
                {
                    parameterMap.putAll(visitWhere(a.value, "where"));
                }
                else
                {
                    parameterMap.put(a.name, new ConstantResult(argumentValueToObject(a.value, variableContext)));
                }
            }
        );
        return parameterMap;
    }

    public static Map<String, Result> visitWhere(Value value, String prefix)
    {
        return value.accept(new ValueVisitor<Map<String, Result>>()
        {
            @Override
            public Map<String, Result> visit(BooleanValue val)
            {
                Map<String, Result> m = new HashMap<>();
                m.put(prefix, new ConstantResult(val.value));
                return m;
            }

            @Override
            public Map<String, Result> visit(EnumValue val)
            {
                Map<String, Result> m = new HashMap<>();
                m.put(prefix, new ConstantResult(val.value));
                return m;
            }

            @Override
            public Map<String, Result> visit(FloatValue val)
            {
                Map<String, Result> m = new HashMap<>();
                m.put(prefix, new ConstantResult(val.value));
                return m;
            }

            @Override
            public Map<String, Result> visit(IntValue val)
            {
                Map<String, Result> m = new HashMap<>();
                m.put(prefix, new ConstantResult(val.value));
                return m;
            }

            @Override
            public Map<String, Result> visit(ListValue val)
            {
                Map<String, Result> m = new HashMap<>();
                List<Object> l = new ArrayList<>();
                Iterator<Value> v = val.values.stream().iterator();
                int idx = 0;
                while (v.hasNext())
                {
                    if (prefix.endsWith("__in"))
                    {
                        l.add(argumentValueToObject(v.next(), VariableContext.EMPTY));
                    }
                    else
                    {
                        m.putAll(visitWhere(v.next(), prefix + idx));
                        idx++;
                    }
                }
                if (prefix.endsWith("__in"))
                {
                    m.put(prefix, new ConstantResult(l));
                }
                return m;
            }

            @Override
            public Map<String, Result> visit(NullValue val)
            {
                return new HashMap<String, Result>();
            }

            @Override
            public Map<String, Result> visit(ObjectValue val)
            {
                Map<String, Result> m = new HashMap<>();
                val.fields.stream().map(v ->
                {
                    if (Objects.equals(v.name, "_contains"))
                    {
                        return visitWhere(v.value, prefix.replace("_","") + v.name.replace("_",""));
                    }
                    else
                    {
                        return visitWhere(v.value, prefix + "_" + v.name);
                    }
                }).forEach(m::putAll);
                return m;
            }

            @Override
            public Map<String, Result> visit(StringValue val)
            {
                Map<String, Result> m = new HashMap<>();
                m.put(prefix, new ConstantResult(val.value));
                return m;
            }

            @Override
            public Map<String, Result> visit(Variable val)
            {
                throw new UnsupportedOperationException(WHERE_VARIABLES_UNSUPPORTED_MESSAGE);
            }
        });
    }
}
