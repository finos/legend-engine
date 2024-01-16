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

import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.protocol.graphQL.metamodel.Definition;
import org.finos.legend.engine.protocol.graphQL.metamodel.DefinitionVisitor;
import org.finos.legend.engine.protocol.graphQL.metamodel.Directive;
import org.finos.legend.engine.protocol.graphQL.metamodel.Document;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.ExecutableDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.Field;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.FragmentDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.FragmentSpread;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.InLineFragment;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.OperationDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.OperationType;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.Objects;

public class GraphQLExecutionHelper
{
    static Object argumentValueToObject(Value value)
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
                return val;
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
                    return val.type == OperationType.query;
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

    static Map<String, Result> getParameterMap(OperationDefinition graphQLQuery, String fieldName)
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
                    parameterMap.put(a.name, new ConstantResult(argumentValueToObject(a.value)));
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
                        l.add(argumentValueToObject(v.next()));
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
                throw new UnsupportedOperationException("Variables are not supported yet");
            }
        });
    }
}
