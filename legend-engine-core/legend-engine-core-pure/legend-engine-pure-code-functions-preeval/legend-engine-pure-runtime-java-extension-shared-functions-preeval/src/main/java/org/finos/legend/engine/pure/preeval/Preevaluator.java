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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.KeyExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.Column;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;

public final class Preevaluator
{
    private final PrevalRuntime runtime;
    private final PrevalHooks hooks;
    private final Scope scope;
    private final GenericTypes genericTypes;

    public Preevaluator(PrevalRuntime runtime, PrevalHooks hooks)
    {
        this.runtime = runtime;
        this.hooks = hooks;
        this.scope = new Scope(v -> v instanceof VariableExpression ? ((VariableExpression) v)._name() : null);
        this.genericTypes = new GenericTypes(runtime);
    }

    public PrevalResult preval(Object item, PrevalState state)
    {
        return prevalInternal(item, state);
    }

    PrevalResult prevalInternal(Object item, PrevalState origState)
    {
        PrevalState state = origState.deeper(item instanceof FunctionDefinition ? item : null);
        DebugTrace.processing(state, item);
        PrevalResult result = dispatch(item, state);
        DebugTrace.returning(state, result);
        return result;
    }

    private PrevalResult dispatch(Object item, PrevalState state)
    {
        if (item instanceof LambdaFunction)
        {
            return prevalLambda((LambdaFunction<?>) item, state);
        }
        if (item instanceof FunctionDefinition)
        {
            return prevalFunctionDefinition((FunctionDefinition<?>) item, state.withInScopeVars(Maps.immutable.empty()));
        }
        if (item instanceof FunctionExpression)
        {
            return prevalFunctionExpression((FunctionExpression) item, state);
        }
        if (item instanceof VariableExpression)
        {
            return prevalVariable((VariableExpression) item, state);
        }
        if (item instanceof InstanceValue)
        {
            return prevalInstanceValue((InstanceValue) item, state);
        }
        if (item instanceof KeyExpression)
        {
            return prevalKeyExpression((KeyExpression) item, state);
        }
        if (isAnyOf(item, MetamodelPaths.SCHEMA_STATE, MetamodelPaths.ROOT_GRAPH_FETCH_TREE, MetamodelPaths.BINDING, MetamodelPaths.STORE))
        {
            return PrevalResult.unmodified(item);
        }
        if (this.runtime.isInstanceOf(item, MetamodelPaths.RELATION_STORE_ACCESSOR))
        {
            return PrevalResult.unmodified(item, false);
        }
        if (isAnyOf(item, MetamodelPaths.AGG_COL_SPEC, MetamodelPaths.FUNC_COL_SPEC, MetamodelPaths.TEST_PARAMETERS))
        {
            return PrevalResult.unmodified(item);
        }
        if (this.runtime.isInstanceOf(item, MetamodelPaths.RELATION_ELEMENT_ACCESSOR))
        {
            return PrevalResult.unmodified(item, false);
        }
        assertStops(Lists.immutable.with(item));
        return PrevalResult.unmodified(item);
    }

    private PrevalResult prevalLambda(LambdaFunction<?> lambda, PrevalState state)
    {
        ImmutableList<String> argumentNames = this.runtime.parameterNames(lambda);
        MutableMap<String, ImmutableList<Object>> vars = Maps.mutable.withMapIterable(this.runtime.openVariableValues(lambda));
        state.getInScopeVars().forEachKeyValue((name, value) ->
        {
            if (!argumentNames.contains(name))
            {
                vars.put(name, value);
            }
        });
        return prevalFunctionDefinition(lambda, state.withInScopeVars(vars.toImmutable()));
    }

    private PrevalResult prevalFunctionDefinition(FunctionDefinition<?> function, PrevalState origState)
    {
        ListIterable<? extends ValueSpecification> items = Lists.mutable.withAll(function._expressionSequence());
        MutableList<PrevalResult> results = Lists.mutable.empty();
        PrevalState state = origState;
        for (int index = 0; index < items.size(); index++)
        {
            ValueSpecification item = items.get(index);
            boolean isLast = index == items.size() - 1;
            if (!isLast && !isLet(item))
            {
                continue;
            }
            PrevalResult r = prevalInternal(item, state);
            if (!isLet(r.getValue()))
            {
                results.add(r);
                continue;
            }
            FunctionExpression let = (FunctionExpression) r.getValue();
            ListIterable<? extends ValueSpecification> letParameters = Lists.mutable.withAll(let._parametersValues());
            String varName = this.runtime.stringValue(((InstanceValue) letParameters.get(0))._values().getOnly());
            ImmutableList<Object> varValue = letParameters.get(1) instanceof InstanceValue
                    ? Lists.immutable.withAll(((InstanceValue) letParameters.get(1))._values())
                    : Lists.immutable.with(letParameters.get(1));
            boolean shouldInlineVariable = isSingle(varValue, VariableExpression.class)
                    || (r.canPreval() && this.scope.areAllInScope(r.getOpenVars(), state.getInScopeVars()) && !isSingle(varValue, LambdaFunction.class));
            state = state.withRollingInScopeVars(state.getRollingInScopeVars().newWithKeyValue(varName, varValue));
            if (shouldInlineVariable)
            {
                state = state.withInScopeVars(state.getInScopeVars().newWithKeyValue(varName, varValue));
            }
            if (isLast)
            {
                Object lastValue = isSingle(varValue, ValueSpecification.class)
                        ? varValue.getOnly()
                        : this.runtime.newInstanceValue(let._genericType(), this.runtime.exactly(varValue.size()), varValue);
                results.add(new PrevalResult(lastValue, r.canPreval(), r.getOpenVars(), true));
            }
            else if (!shouldInlineVariable)
            {
                results.add(r);
            }
        }
        boolean modified = results.size() != items.size() || PrevalResult.anyModified(results);
        Object value = function;
        if (modified)
        {
            ListIterable<ValueSpecification> sequence = results.collect(r -> (ValueSpecification) r.getValue());
            ListIterable<String> openVariables = null;
            if (function instanceof LambdaFunction)
            {
                ImmutableList<String> resultOpen = results.flatCollect(PrevalResult::getOpenVars).collect(v -> this.scope.resolveVariable(v, origState.getInScopeVars())).toImmutable();
                openVariables = Lists.mutable.withAll(((LambdaFunction<?>) function)._openVariables())
                        .collect(v -> this.scope.resolveVariable(v, origState.getInScopeVars()))
                        .distinct()
                        .select(resultOpen::contains);
            }
            value = this.runtime.withExpressionSequence(function, sequence, openVariables);
        }
        ImmutableList<String> openVars = value instanceof LambdaFunction ? Lists.immutable.withAll(((LambdaFunction<?>) value)._openVariables()) : Lists.immutable.empty();
        return new PrevalResult(value, PrevalResult.allCanPreval(results), openVars, modified);
    }

    private PrevalResult prevalFunctionExpression(FunctionExpression expression, PrevalState state)
    {
        MutableList<? extends ValueSpecification> parameters = Lists.mutable.withAll(expression._parametersValues());
        // parity: Pure zips parameter names with values, so surplus values are dropped
        int parameterCount = parameters.isEmpty() ? 0 : Math.min(parameters.size(), parameterNameCount(expression._func()));
        ListIterable<PrevalResult> results = parameters.subList(0, parameterCount).collect(p -> prevalInternal(p, state));
        PrevalResult genericType = this.genericTypes.resolveGenericType(expression._genericType(), state);
        boolean modified = PrevalResult.anyModified(results) || genericType.isModified();
        FunctionExpression newExpression = expression;
        if (modified)
        {
            ListIterable<? extends ValueSpecification> newParameters = PrevalResult.anyModified(results)
                    ? results.collect(r -> this.runtime.withGenericType((ValueSpecification) r.getValue(), (GenericType) this.genericTypes.resolveGenericType(((ValueSpecification) r.getValue())._genericType(), state).getValue()))
                    : parameters;
            newExpression = this.runtime.withParametersAndGenericType(expression, newParameters, (GenericType) genericType.getValue());
        }
        ImmutableList<String> openVars = this.scope.openVars(results.flatCollect(PrevalResult::getOpenVars), state.getInScopeVars());
        boolean canPrevalFunction = !(this.runtime.hasStereotype(newExpression._func(), MetamodelPaths.FUNCTION_TYPE_PROFILE, "SideEffectFunction")
                || this.runtime.hasStereotype(newExpression._func(), MetamodelPaths.FUNCTION_TYPE_PROFILE, "NotImplementedFunction")
                || this.hooks.stopPreeval(Lists.immutable.with(newExpression)));
        if (!canPrevalFunction)
        {
            boolean canPreval = this.runtime.isFunction(newExpression._func(), MetamodelPaths.LET_FUNCTION) && PrevalResult.allCanPreval(results);
            return new PrevalResult(newExpression, canPreval, openVars, modified);
        }
        String notPrevalReason = notPrevalReason(newExpression, results, state);
        if (notPrevalReason != null)
        {
            return new PrevalResult(newExpression, PrevalResult.allCanPreval(results), openVars, modified);
        }
        return new PrevalResult(newExpression, PrevalResult.allCanPreval(results), openVars, modified);
    }

    private int parameterNameCount(Object function)
    {
        return (function instanceof Property || function instanceof Column) ? 1 : this.runtime.parameterNames(function).size();
    }

    private String notPrevalReason(FunctionExpression expression, ListIterable<PrevalResult> results, PrevalState state)
    {
        if (Lists.mutable.withAll(expression._parametersValues()).anySatisfy(pv -> !isInstanceValue(pv, state.getInScopeVars())))
        {
            return "params are not instance values";
        }
        if (!PrevalResult.allCanPreval(results))
        {
            return "params can not preval";
        }
        if (!this.scope.areAllInScope(results.flatCollect(PrevalResult::getOpenVars).distinct(), state.getInScopeVars()))
        {
            return "open variables not in scope";
        }
        if (this.runtime.isFunction(expression._func(), MetamodelPaths.CAST_FUNCTION) && results.notEmpty()
                && results.get(0).getValue() instanceof InstanceValue && ((InstanceValue) results.get(0).getValue())._values().isEmpty())
        {
            return "cast of empty collection";
        }
        return null;
    }

    private PrevalResult prevalVariable(VariableExpression variable, PrevalState state)
    {
        String name = variable._name();
        ImmutableList<Object> values = state.getInScopeVars().get(name);
        if (values == null)
        {
            return new PrevalResult(variable, true, Lists.immutable.with(name), false);
        }
        Object substitute = substituteFor(variable, values);
        PrevalState withoutVariable = state.withInScopeVars(state.getInScopeVars().newWithoutKey(name));
        return prevalInternal(substitute, withoutVariable).markModified();
    }

    private Object substituteFor(VariableExpression variable, ImmutableList<Object> values)
    {
        if (values.isEmpty())
        {
            return this.runtime.newInstanceValue(variable._genericType(), variable._multiplicity(), values);
        }
        if (isSingle(values, InstanceValue.class) || isSingle(values, VariableExpression.class) || isSingle(values, FunctionExpression.class))
        {
            return values.getOnly();
        }
        if (allAre(values, MetamodelPaths.TDS_AGGREGATE_VALUE) || allAre(values, MetamodelPaths.COLLECTION_AGGREGATE_VALUE)
                || allAre(values, MetamodelPaths.AGG_COL_SPEC_ARRAY) || allAre(values, MetamodelPaths.AGG_COL_SPEC) || allAre(values, MetamodelPaths.BASIC_COLUMN_SPECIFICATION)
                || (values.size() == 1 && isAnyOf(values.getOnly(), MetamodelPaths.ROOT_GRAPH_FETCH_TREE, MetamodelPaths.BINDING, MetamodelPaths.SCHEMA_STATE, MetamodelPaths.STORE, MetamodelPaths.TEST_PARAMETERS)))
        {
            return this.runtime.newInstanceValue(variable._genericType(), variable._multiplicity(), values);
        }
        assertStops(values);
        return this.runtime.newInstanceValue(variable._genericType(), variable._multiplicity(), values);
    }

    private PrevalResult prevalInstanceValue(InstanceValue instanceValue, PrevalState state)
    {
        ListIterable<PrevalResult> values = Lists.mutable.withAll(instanceValue._values()).collect(v -> prevalInternal(v, state));
        ImmutableList<String> openVars = this.scope.openVars(values.flatCollect(PrevalResult::getOpenVars), state.getInScopeVars());
        if (!PrevalResult.anyModified(values))
        {
            return new PrevalResult(instanceValue, PrevalResult.allCanPreval(values), openVars, false);
        }
        MutableList<Object> cleanValues = Lists.mutable.empty();
        values.forEach(r ->
        {
            Object v = r.getValue();
            if (v instanceof InstanceValue && ((InstanceValue) v)._values().size() == 1)
            {
                cleanValues.addAllIterable(((InstanceValue) v)._values());
            }
            else
            {
                cleanValues.add(v);
            }
        });
        GenericType genericType = (GenericType) this.genericTypes.resolveGenericType(instanceValue._genericType(), state).getValue();
        // parity: Cast(@X) produces an empty InstanceValue with multiplicity PureOne, which Pure preserves
        Multiplicity multiplicity = instanceValue._multiplicity() == this.runtime.pureOne() && cleanValues.isEmpty() ? instanceValue._multiplicity() : this.runtime.exactly(cleanValues.size());
        return new PrevalResult(this.runtime.withValues(instanceValue, cleanValues, genericType, multiplicity), PrevalResult.allCanPreval(values), openVars, true);
    }

    private PrevalResult prevalKeyExpression(KeyExpression keyExpression, PrevalState state)
    {
        PrevalResult expression = prevalInternal(keyExpression._expression(), state);
        Object value = expression.isModified() ? this.runtime.withExpression(keyExpression, (ValueSpecification) expression.getValue()) : keyExpression;
        return new PrevalResult(value, expression.canPreval(), this.scope.openVars(expression.getOpenVars(), state.getInScopeVars()), expression.isModified());
    }

    private boolean isInstanceValue(Object value, ImmutableMap<String, ImmutableList<Object>> inScopeVars)
    {
        if (value instanceof InstanceValue)
        {
            return Lists.mutable.withAll(((InstanceValue) value)._values()).allSatisfy(v -> (v instanceof ValueSpecification) ? isInstanceValue(v, inScopeVars) : !this.runtime.isInstanceOf(v, MetamodelPaths.TDS));
        }
        if (value instanceof VariableExpression)
        {
            return inScopeVars.containsKey(((VariableExpression) value)._name());
        }
        if (value instanceof FunctionExpression)
        {
            return false;
        }
        throw this.runtime.error("Unexpected value in isInstanceValue: " + this.runtime.typeDescription(value));
    }

    private boolean isLet(Object value)
    {
        return value instanceof FunctionExpression && this.runtime.isFunction(((FunctionExpression) value)._func(), MetamodelPaths.LET_FUNCTION);
    }

    private void assertStops(ImmutableList<Object> values)
    {
        if (!this.hooks.stopPreeval(values))
        {
            throw this.runtime.error("Unsupported type: " + this.runtime.typeDescription(values.getFirst()));
        }
    }

    private boolean isAnyOf(Object value, String... typePaths)
    {
        for (String typePath : typePaths)
        {
            if (this.runtime.isInstanceOf(value, typePath))
            {
                return true;
            }
        }
        return false;
    }

    private boolean allAre(ImmutableList<Object> values, String typePath)
    {
        return values.allSatisfy(v -> this.runtime.isInstanceOf(v, typePath));
    }

    private static boolean isSingle(ImmutableList<Object> values, Class<?> type)
    {
        return values.size() == 1 && type.isInstance(values.getOnly());
    }
}
