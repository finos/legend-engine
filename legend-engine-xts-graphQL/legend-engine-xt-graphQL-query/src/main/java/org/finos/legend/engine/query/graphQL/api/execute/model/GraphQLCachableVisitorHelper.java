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


package org.finos.legend.engine.query.graphQL.api.execute.model;

import org.finos.legend.engine.protocol.graphQL.metamodel.Definition;
import org.finos.legend.engine.protocol.graphQL.metamodel.DefinitionVisitor;
import org.finos.legend.engine.protocol.graphQL.metamodel.Document;
import org.finos.legend.engine.protocol.graphQL.metamodel.DocumentVisitor;
import org.finos.legend.engine.protocol.graphQL.metamodel.ExecutableDocument;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.Argument;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.ExecutableDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.Field;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.FragmentDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.FragmentSpread;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.InLineFragment;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.OperationDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.Selection;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.SelectionVisitor;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.DirectiveDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.EnumTypeDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.InputObjectTypeDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.InterfaceTypeDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.ObjectTypeDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.ScalarTypeDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.SchemaDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.Type;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.TypeSystemDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.UnionTypeDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.value.*;

import java.util.Objects;
import java.util.stream.Collectors;

public class GraphQLCachableVisitorHelper
{
    private static final StringValue placeholderValue = new StringValue();

    public static Document createCachableGraphQLQuery(org.finos.legend.engine.protocol.graphQL.metamodel.Document document)
    {
        return documentVisitor(document);
    }


    private static Document documentVisitor(Document document)
    {
        return document.accept((DocumentVisitor<Document>) val ->
        {
            ExecutableDocument doc = new ExecutableDocument();
            doc.definitions = val.definitions.stream().map(d -> definitionVisitor(d)).collect(Collectors.toList());
            return doc;
        });
    }


    private static Selection selectionVisitor(Selection selection)
    {
        return selection.accept(new SelectionVisitor<Selection>()
        {
            @Override
            public Selection visit(Field val)
            {
                Field field = new Field();
                field.name = val.name;
                field.directives = val.directives;
                field.arguments = val.arguments.stream().map(a ->
                {
                    Argument arg = new Argument();
                    arg.name = a.name;
                    if (Objects.equals(a.name, "where"))
                    {
                        arg.value = getValueWithoutLiterals(a.value);
                    }
                    else
                    {
                        arg.value = placeholderValue;
                    }
                    return arg;

                }).collect(Collectors.toList());
                field.selectionSet = val.selectionSet.stream().map(s -> selectionVisitor(s)).collect(Collectors.toList());
                return field;
            }

            @Override
            public Selection visit(FragmentSpread val)
            {
                return val;
            }

            @Override
            public Selection visit(InLineFragment val)
            {
                return val;
            }
        });
    }

    private static Value getValueWithoutLiterals(Value value)
    {
        if (value instanceof ObjectValue)
        {
            ObjectValue objectValue = new ObjectValue();
            objectValue.fields = ((ObjectValue) value).fields.stream().map(v ->
                {
                    ObjectField objectField = new ObjectField();
                    objectField.name = v.name;
                    if (v.name.equals("_in"))
                    {
                        objectField.value = placeholderValue;
                    }
                    else
                    {
                        objectField.value = getValueWithoutLiterals(v.value);
                    }
                    return objectField;
                }
            ).collect(Collectors.toList());
            return objectValue;
        }
        else if (value instanceof ListValue)
        {
            ListValue listValue = new ListValue();
            listValue.values = ((ListValue) value).values.stream().map(
                    GraphQLCachableVisitorHelper::getValueWithoutLiterals
            ).collect(Collectors.toList());
            return listValue;
        }
        else
        {
            return placeholderValue;
        }
    }

    private static Definition definitionVisitor(Definition definition)
    {
        return definition.accept(new DefinitionVisitor<Definition>()
        {
            @Override
            public Definition visit(DirectiveDefinition val)
            {
                return val;
            }

            @Override
            public Definition visit(EnumTypeDefinition val)
            {
                return val;
            }

            @Override
            public Definition visit(ExecutableDefinition val)
            {
                return val;
            }

            @Override
            public Definition visit(FragmentDefinition val)
            {
                return val;
            }

            @Override
            public Definition visit(InterfaceTypeDefinition val)
            {
                return val;
            }

            @Override
            public Definition visit(ObjectTypeDefinition val)
            {
                return val;
            }

            @Override
            public Definition visit(InputObjectTypeDefinition val)
            {
                return val;
            }
            
            @Override
            public Definition visit(OperationDefinition val)
            {
                OperationDefinition def = new OperationDefinition();
                def.selectionSet = val.selectionSet.stream().map(s -> selectionVisitor(s)).collect(Collectors.toList());
                return def;
            }

            @Override
            public Definition visit(ScalarTypeDefinition val)
            {
                return val;
            }

            @Override
            public Definition visit(SchemaDefinition val)
            {
                return val;
            }

            @Override
            public Definition visit(Type val)
            {
                return val;
            }

            @Override
            public Definition visit(TypeSystemDefinition val)
            {
                return val;
            }

            @Override
            public Definition visit(UnionTypeDefinition val)
            {
                return val;
            }
        });

    }
}
