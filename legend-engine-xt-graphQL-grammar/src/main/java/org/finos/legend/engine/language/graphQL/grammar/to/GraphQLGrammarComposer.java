// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.language.graphQL.grammar.to;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.graphQL.metamodel.Definition;
import org.finos.legend.engine.protocol.graphQL.metamodel.DefinitionVisitor;
import org.finos.legend.engine.protocol.graphQL.metamodel.Document;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.ExecutableDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.Field;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.FragmentDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.FragmentSpread;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.InLineFragment;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.OperationDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.Selection;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.SelectionVisitor;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.VariableDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.DirectiveDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.EnumTypeDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.FieldDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.InputValueDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.InterfaceTypeDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.ListTypeReference;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.NamedTypeReference;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.ObjectTypeDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.ScalarTypeDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.SchemaDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.Type;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.TypeReference;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.TypeReferenceVisitor;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.TypeSystemDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.UnionTypeDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.value.BooleanValue;
import org.finos.legend.engine.protocol.graphQL.metamodel.value.EnumValue;
import org.finos.legend.engine.protocol.graphQL.metamodel.value.FloatValue;
import org.finos.legend.engine.protocol.graphQL.metamodel.value.IntValue;
import org.finos.legend.engine.protocol.graphQL.metamodel.value.ListValue;
import org.finos.legend.engine.protocol.graphQL.metamodel.value.NullValue;
import org.finos.legend.engine.protocol.graphQL.metamodel.value.ObjectValue;
import org.finos.legend.engine.protocol.graphQL.metamodel.value.StringValue;
import org.finos.legend.engine.protocol.graphQL.metamodel.value.Value;
import org.finos.legend.engine.protocol.graphQL.metamodel.value.ValueVisitor;
import org.finos.legend.engine.protocol.graphQL.metamodel.value.Variable;

import java.util.List;

public class GraphQLGrammarComposer
{
    private GraphQLGrammarComposer()
    {
    }

    public static GraphQLGrammarComposer newInstance()
    {
        return new GraphQLGrammarComposer();
    }

    public String renderDocument(Document document)
    {
        return ListIterate.collect(document.definitions, this::renderDefinition).makeString("\n\n");
    }

    public String renderDefinition(Definition definition)
    {
        return definition.accept(new DefinitionVisitor<String>()
        {
            @Override
            public String visit(ScalarTypeDefinition scalarTypeDefinition)
            {
                return "scalar " + scalarTypeDefinition.name;
            }

            @Override
            public String visit(EnumTypeDefinition enumTypeDefinition)
            {
                return "enum " + enumTypeDefinition.name + " {\n" +
                        ListIterate.collect(enumTypeDefinition.values, v -> "  " + v.value).makeString("\n") +
                        "\n}";
            }

            @Override
            public String visit(UnionTypeDefinition unionTypeDefinition)
            {
                return "union " + unionTypeDefinition.name + " = " + ListAdapter.adapt(unionTypeDefinition.members).makeString(" | ");
            }

            @Override
            public String visit(InterfaceTypeDefinition interfaceTypeDefinition)
            {
                return "interface " + interfaceTypeDefinition.name + " {\n" +
                        ListIterate.collect(interfaceTypeDefinition.fields, f -> "  " + renderField(f)).makeString("\n") +
                        "\n}";
            }

            @Override
            public String visit(ObjectTypeDefinition objectTypeDefinition)
            {
                return "type " + objectTypeDefinition.name + (objectTypeDefinition._implements.isEmpty() ? "" : " implements " + ListAdapter.adapt(objectTypeDefinition._implements).makeString(" & ")) + " {\n" +
                        ListIterate.collect(objectTypeDefinition.fields, f -> "  " + renderField(f)).makeString("\n") +
                        "\n}";
            }

            @Override
            public String visit(ExecutableDefinition val)
            {
                return null;
            }

            @Override
            public String visit(TypeSystemDefinition val)
            {
                return null;
            }

            @Override
            public String visit(FragmentDefinition fragmentDefinition)
            {
                return "fragment " + fragmentDefinition.name + (fragmentDefinition.typeCondition != null ? " on " + fragmentDefinition.typeCondition + " " : " ") + "{\n" + renderSelectionSet(fragmentDefinition.selectionSet, "  ") + "\n}";
            }

            @Override
            public String visit(OperationDefinition operationDefinition)
            {
                return (operationDefinition.type == null ? "" : operationDefinition.type.name()) + " " + operationDefinition.name + (operationDefinition.variables.isEmpty() ? "" : "(" + ListIterate.collect(operationDefinition.variables, v -> render(v)).makeString(", ") + ")") + " {\n" +
                        renderSelectionSet(operationDefinition.selectionSet, "  ") +
                        "\n}";
            }

            @Override
            public String visit(DirectiveDefinition directiveDefinition)
            {
                return "directive @" + directiveDefinition.name +
                        renderArgumentDefinitions(directiveDefinition.argumentDefinitions) +
                        " on " + Lists.mutable.withAll(ListIterate.collect(directiveDefinition.typeSystemLocation, Enum::name))
                        .withAll(ListIterate.collect(directiveDefinition.executableLocation, Enum::name)).makeString(" | ")
                        ;
            }

            @Override
            public String visit(SchemaDefinition schemaDefinition)
            {
                return "schema {\n" + ListIterate.collect(schemaDefinition.rootOperationTypeDefinitions, r -> "  " + r.operationType + " : " + r.type).makeString("\n") + "\n}";
            }

            @Override
            public String visit(Type val)
            {
                return null;
            }
        });
    }

    private Object render(VariableDefinition v)
    {
        return v.name + ": " + renderType(v.type) + (v.defaultValue == null ? "" : " = " + renderValue(v.defaultValue));
    }

    public String renderSelectionSet(List<Selection> selectionSet, String space)
    {
        return ListIterate.collect(selectionSet, a -> space + a.accept(new SelectionVisitor<String>()
        {
            @Override
            public String visit(FragmentSpread fragmentSpread)
            {
                return "... " + fragmentSpread.name;
            }

            @Override
            public String visit(InLineFragment inLineFragment)
            {
                return null;
            }

            @Override
            public String visit(Field field)
            {
                return renderField(field, space) + (field.selectionSet.isEmpty() ? "" : " {\n" + renderSelectionSet(field.selectionSet, space + "  ") + "\n" + space + "}");
            }
        })).makeString("\n");
    }

    public String renderField(Field field, String space)
    {
        return field.name + (field.arguments.isEmpty() ? "" : "(" + ListIterate.collect(field.arguments, a -> a.name + ": " + renderValue(a.value)).makeString(", ") + ")");
    }

    public String renderField(FieldDefinition fieldDefinition)
    {
        return fieldDefinition.name +
                renderArgumentDefinitions(fieldDefinition.argumentDefinitions) +
                ": " + renderType(fieldDefinition.type);
    }

    private String renderArgumentDefinitions(List<InputValueDefinition> argumentDefinitions)
    {
        return (argumentDefinitions.isEmpty() ? "" : "(" + ListIterate.collect(argumentDefinitions, this::renderInputValueDefinition).makeString(", ") + ")");
    }

    private String renderType(TypeReference type)
    {
        return type.accept(new TypeReferenceVisitor<String>()
        {
            @Override
            public String visit(ListTypeReference val)
            {
                return "[" + renderType(val.itemType) + "]";
            }

            @Override
            public String visit(NamedTypeReference val)
            {
                return val.name;
            }
        }) + (type.nullable ? "" : "!");
    }

    public String renderInputValueDefinition(InputValueDefinition inputValueDefinition)
    {
        return inputValueDefinition.name + ": " + renderType(inputValueDefinition.type) +
                (inputValueDefinition.defaultValue == null ? "" : " = " + renderValue(inputValueDefinition.defaultValue));
    }

    public String renderValue(Value value)
    {
        return value.accept(new ValueVisitor<String>()
        {
            @Override
            public String visit(IntValue intValue)
            {
                return String.valueOf(intValue.value);
            }

            @Override
            public String visit(NullValue nullValue)
            {
                return "null";
            }

            @Override
            public String visit(ObjectValue val)
            {
                return null;
            }

            @Override
            public String visit(EnumValue enumValue)
            {
                return enumValue.value;
            }

            @Override
            public String visit(FloatValue floatValue)
            {
                return String.valueOf(floatValue.value);
            }

            @Override
            public String visit(ListValue listValue)
            {
                return "[" + ListIterate.collect(listValue.values, v -> renderValue(v)).makeString(", ") + "]";
            }

            @Override
            public String visit(StringValue stringValue)
            {
                return "\"" + stringValue.value + "\"";
            }

            @Override
            public String visit(Variable variable)
            {
                return "$" + variable.name;
            }

            @Override
            public String visit(BooleanValue booleanValue)
            {
                return String.valueOf(booleanValue.value);
            }
        });
    }
}
