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

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.language.graphQL.grammar.from.antlr4.GraphQLParser;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.Argument;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.Field;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.FragmentSpread;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.InLineFragment;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.OperationDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.Selection;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.SelectionVisitor;

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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GraphQLExecutionHelper
{


    public static Object argumentValueToObject(Value value)
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
                return val.values.stream().map(GraphQLExecutionHelper::argumentValueToObject).collect(Collectors.toList());
            }

            @Override
            public Object visit(NullValue val)
            {
                return null;
            }

            @Override
            public Object visit(ObjectValue val)
            {
                throw new UnsupportedOperationException("Unsupported value specification type");
            }

            @Override
            public Object visit(StringValue val)
            {
                return val.value;
            }

            @Override
            public Object visit(Variable val)
            {
                throw new UnsupportedOperationException("Unsupported value specification type");
            }
        });


    }

    public static Field extractFieldByName(OperationDefinition definition, String propertyName)
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

    public static List<Argument> collectArguments(List<Selection> selectionSet, String prefix, Boolean isRoot)
    {
        List<Argument> arguments = Lists.mutable.empty();
        selectionSet.forEach(selection ->
        {
            if (selection instanceof Field)
            {
                if (!isRoot)
                {
                    ((Field) selection).arguments.forEach(argument -> argument.name = prefix + ((Field) selection).name + '_' + argument.name);
                }
                arguments.addAll(((Field) selection).arguments);
                arguments.addAll(collectArguments(((Field) selection).selectionSet, isRoot ? prefix : prefix + ((Field) selection).name + '_', false));
            }
        });
        return arguments;
    }

}
