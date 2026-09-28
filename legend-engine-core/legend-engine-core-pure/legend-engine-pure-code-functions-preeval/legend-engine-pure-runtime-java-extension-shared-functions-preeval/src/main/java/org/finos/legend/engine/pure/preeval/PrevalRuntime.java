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

package org.finos.legend.engine.pure.preeval;

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.ImmutableMap;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.KeyExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;

public interface PrevalRuntime
{
    boolean isInstanceOf(Object value, String typePath);

    String typeDescription(Object value);

    boolean isFunction(Object function, String functionPath);

    boolean hasStereotype(Object function, String profilePath, String stereotype);

    ImmutableList<String> parameterNames(Object function);

    String stringValue(Object primitive);

    Multiplicity pureOne();

    Multiplicity pureZero();

    boolean isPureOne(Multiplicity multiplicity);

    boolean isPureZero(Multiplicity multiplicity);

    Multiplicity exactly(int size);

    ImmutableMap<String, ImmutableList<Object>> openVariableValues(LambdaFunction<?> lambda);

    ImmutableList<Object> values(InstanceValue instanceValue);

    InstanceValue newInstanceValue(GenericType genericType, Multiplicity multiplicity, ListIterable<?> values);

    <T extends FunctionDefinition<?>> T withExpressionSequence(T function, ListIterable<? extends ValueSpecification> expressionSequence, ListIterable<String> openVariables);

    FunctionExpression withParametersAndGenericType(FunctionExpression expression, ListIterable<? extends ValueSpecification> parameters, GenericType genericType);

    <T extends ValueSpecification> T withGenericType(T valueSpecification, GenericType genericType);

    InstanceValue withValues(InstanceValue instanceValue, ListIterable<?> values, GenericType genericType, Multiplicity multiplicity);

    KeyExpression withExpression(KeyExpression keyExpression, ValueSpecification expression);

    GenericType withTypeArguments(GenericType genericType, ListIterable<? extends GenericType> typeArguments);

    GenericType withRawType(GenericType genericType, FunctionType rawType);

    FunctionType withSignature(FunctionType functionType, ListIterable<? extends VariableExpression> parameters, GenericType returnType);

    ImmutableList<Object> reactivate(ValueSpecification valueSpecification, ImmutableMap<String, ImmutableList<Object>> inScopeVars);

    Object getPropertyValue(Object instance, String property);

    ImmutableList<Object> getPropertyValues(Object instance, String property);

    Object withPropertyValues(Object instance, String property, ListIterable<?> values);

    RuntimeException error(String message);
}
