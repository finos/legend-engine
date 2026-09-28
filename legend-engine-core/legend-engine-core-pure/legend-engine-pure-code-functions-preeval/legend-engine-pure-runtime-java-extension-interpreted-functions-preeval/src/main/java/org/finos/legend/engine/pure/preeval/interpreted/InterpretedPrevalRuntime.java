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

package org.finos.legend.engine.pure.preeval.interpreted;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.finos.legend.engine.pure.preeval.PrevalResult;
import org.finos.legend.engine.pure.preeval.PrevalRuntime;
import org.finos.legend.pure.m3.coreinstance.helper.AnyHelper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinitionCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.KeyExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.KeyExpressionCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunctionCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.MultiplicityCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionTypeCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericTypeCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpressionCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValueCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecificationCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpressionCoreInstanceWrapper;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.AbstractCoreInstanceWrapper;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.BooleanCoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.LambdaWithContext;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.MapCoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public final class InterpretedPrevalRuntime implements PrevalRuntime
{
    private static final String PREVAL_RESULT = "meta::pure::functions::preeval::PrevalResult";

    private final FunctionExecutionInterpreted functionExecution;
    private final ModelRepository repository;
    private final ProcessorSupport processorSupport;
    private final Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters;
    private final Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters;
    private final MutableStack<CoreInstance> functionExpressionCallStack;
    private final Profiler profiler;
    private final InstantiationContext instantiationContext;
    private final ExecutionSupport executionSupport;
    private final MutableMap<String, CoreInstance> elementsByPath = Maps.mutable.empty();

    public InterpretedPrevalRuntime(FunctionExecutionInterpreted functionExecution, ModelRepository repository, ProcessorSupport processorSupport, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport)
    {
        this.functionExecution = functionExecution;
        this.repository = repository;
        this.processorSupport = processorSupport;
        this.resolvedTypeParameters = resolvedTypeParameters;
        this.resolvedMultiplicityParameters = resolvedMultiplicityParameters;
        this.functionExpressionCallStack = functionExpressionCallStack;
        this.profiler = profiler;
        this.instantiationContext = instantiationContext;
        this.executionSupport = executionSupport;
    }

    public ImmutableMap<String, ImmutableList<Object>> toVars(CoreInstance mapArgument)
    {
        MapCoreInstance map = (MapCoreInstance) Instance.getValueForMetaPropertyToManyResolved(mapArgument, M3Properties.values, this.processorSupport).getFirst();
        MutableMap<String, ImmutableList<Object>> vars = Maps.mutable.empty();
        map.getMap().forEachKeyValue((key, list) -> vars.put(key.getName(), Lists.immutable.withAll(list.getValueForMetaPropertyToMany(M3Properties.values))));
        return vars.toImmutable();
    }

    public CoreInstance toPureResult(PrevalResult result)
    {
        CoreInstance pureResult = this.processorSupport.newEphemeralAnonymousCoreInstance(PREVAL_RESULT);
        Instance.setValueForProperty(pureResult, "value", toCoreInstance(result.getValue()), this.processorSupport);
        Instance.setValueForProperty(pureResult, "canPreval", this.repository.newBooleanCoreInstance(result.canPreval()), this.processorSupport);
        Instance.setValuesForProperty(pureResult, "openVars", result.getOpenVars().collect(this.repository::newStringCoreInstance), this.processorSupport);
        Instance.setValueForProperty(pureResult, "modified", this.repository.newBooleanCoreInstance(result.isModified()), this.processorSupport);
        return pureResult;
    }

    public boolean evaluateBoolean(CoreInstance function, VariableContext context, ListIterable<?> values)
    {
        CoreInstance result = this.functionExecution.executeFunction(false, FunctionCoreInstanceWrapper.toFunction(function),
                Lists.immutable.with(ValueSpecificationBootstrap.wrapValueSpecification(toCoreInstances(values), false, this.processorSupport)),
                this.resolvedTypeParameters, this.resolvedMultiplicityParameters, context, this.functionExpressionCallStack, this.profiler, this.instantiationContext, this.executionSupport);
        return PrimitiveUtilities.getBooleanValue(Instance.getValueForMetaPropertyToOneResolved(result, M3Properties.values, this.processorSupport));
    }

    @Override
    public boolean isInstanceOf(Object value, String typePath)
    {
        return this.processorSupport.package_getByUserPath(typePath) != null && Instance.instanceOf(toCoreInstance(value), typePath, this.processorSupport);
    }

    @Override
    public String typeDescription(Object value)
    {
        CoreInstance type = this.processorSupport.getClassifier(toCoreInstance(value));
        return Instance.instanceOf(type, M3Paths.PackageableElement, this.processorSupport)
                ? PackageableElement.getUserPathForPackageableElement(type)
                : type.getName();
    }

    @Override
    public boolean isFunction(Object function, String functionPath)
    {
        CoreInstance element = this.processorSupport.package_getByUserPath(functionPath);
        return element != null && element == function;
    }

    @Override
    public Boolean booleanValue(Object value)
    {
        if (!(value instanceof CoreInstance))
        {
            return null;
        }
        CoreInstance instance = (CoreInstance) value;
        return (instance instanceof BooleanCoreInstance) || Instance.instanceOf(instance, M3Paths.Boolean, this.processorSupport)
                ? PrimitiveUtilities.getBooleanValue(instance)
                : null;
    }

    @Override
    public Object function(String functionPath)
    {
        return element(functionPath);
    }

    @Override
    public boolean hasStereotype(Object function, String profilePath, String stereotype)
    {
        return Instance.getValueForMetaPropertyToManyResolved((CoreInstance) function, M3Properties.stereotypes, this.processorSupport).anySatisfy(s ->
        {
            CoreInstance profile = Instance.getValueForMetaPropertyToOneResolved(s, M3Properties.profile, this.processorSupport);
            return stereotype.equals(PrimitiveUtilities.getStringValue(s.getValueForMetaPropertyToOne(M3Properties.value)))
                    && profile != null && profilePath.equals(PackageableElement.getUserPathForPackageableElement(profile));
        });
    }

    @Override
    public ImmutableList<String> parameterNames(Object function)
    {
        CoreInstance functionType = this.processorSupport.function_getFunctionType((CoreInstance) function);
        return Lists.immutable.withAll(Instance.getValueForMetaPropertyToManyResolved(functionType, M3Properties.parameters, this.processorSupport))
                .collect(p -> PrimitiveUtilities.getStringValue(p.getValueForMetaPropertyToOne(M3Properties.name)));
    }

    @Override
    public ImmutableList<String> typeParameterNames(Object function)
    {
        CoreInstance functionType = this.processorSupport.function_getFunctionType((CoreInstance) function);
        return Lists.immutable.withAll(Instance.getValueForMetaPropertyToManyResolved(functionType, M3Properties.typeParameters, this.processorSupport))
                .collect(p -> PrimitiveUtilities.getStringValue(p.getValueForMetaPropertyToOne(M3Properties.name)));
    }

    @Override
    public ImmutableList<GenericType> resolvedTypeParameters(FunctionExpression expression)
    {
        return Lists.immutable.withAll(Instance.getValueForMetaPropertyToManyResolved(expression, M3Properties.resolvedTypeParameters, this.processorSupport))
                .collect(GenericTypeCoreInstanceWrapper::toGenericType);
    }

    @Override
    public boolean isQualifiedPropertyOf(Object function, String ownerPath)
    {
        CoreInstance instance = toCoreInstance(function);
        if (!Instance.instanceOf(instance, M3Paths.QualifiedProperty, this.processorSupport))
        {
            return false;
        }
        CoreInstance owner = Instance.getValueForMetaPropertyToOneResolved(instance, M3Properties.owner, this.processorSupport);
        return owner != null && Instance.instanceOf(owner, M3Paths.PackageableElement, this.processorSupport) && ownerPath.equals(PackageableElement.getUserPathForPackageableElement(owner));
    }

    @Override
    public String stringValue(Object primitive)
    {
        return primitive instanceof String ? (String) primitive : PrimitiveUtilities.getStringValue((CoreInstance) primitive);
    }

    @Override
    public Multiplicity pureOne()
    {
        return MultiplicityCoreInstanceWrapper.toMultiplicity(this.processorSupport.package_getByUserPath(M3Paths.PureOne));
    }

    @Override
    public Multiplicity pureZero()
    {
        return MultiplicityCoreInstanceWrapper.toMultiplicity(this.processorSupport.package_getByUserPath(M3Paths.PureZero));
    }

    @Override
    public boolean isPureOne(Multiplicity multiplicity)
    {
        return isPackagedMultiplicity(multiplicity, M3Paths.PureOne);
    }

    @Override
    public boolean isPureZero(Multiplicity multiplicity)
    {
        return isPackagedMultiplicity(multiplicity, M3Paths.PureZero);
    }

    private boolean isPackagedMultiplicity(Multiplicity multiplicity, String path)
    {
        CoreInstance packaged = this.processorSupport.package_getByUserPath(path);
        return multiplicity == packaged || (multiplicity instanceof AbstractCoreInstanceWrapper && multiplicity.equals(packaged));
    }

    @Override
    public Long lowerBound(Multiplicity multiplicity)
    {
        return boundValue(multiplicity, M3Properties.lowerBound);
    }

    @Override
    public Long upperBound(Multiplicity multiplicity)
    {
        return boundValue(multiplicity, M3Properties.upperBound);
    }

    private Long boundValue(Multiplicity multiplicity, String bound)
    {
        CoreInstance multiplicityValue = Instance.getValueForMetaPropertyToOneResolved(multiplicity, bound, this.processorSupport);
        CoreInstance value = multiplicityValue == null ? null : Instance.getValueForMetaPropertyToOneResolved(multiplicityValue, M3Properties.value, this.processorSupport);
        return value == null ? null : PrimitiveUtilities.getIntegerValue(value).longValue();
    }

    @Override
    public boolean isMultiplicityConcrete(Multiplicity multiplicity)
    {
        return Instance.getValueForMetaPropertyToOneResolved(multiplicity, M3Properties.multiplicityParameter, this.processorSupport) == null;
    }

    @Override
    public Multiplicity exactly(int size)
    {
        if (size == 0)
        {
            return pureZero();
        }
        if (size == 1)
        {
            return pureOne();
        }
        return MultiplicityCoreInstanceWrapper.toMultiplicity(org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.newMultiplicity(size, this.processorSupport));
    }

    @Override
    public ImmutableMap<String, ImmutableList<Object>> openVariableValues(LambdaFunction<?> lambda)
    {
        MutableMap<String, ImmutableList<Object>> values = Maps.mutable.empty();
        if (lambda instanceof LambdaWithContext)
        {
            VariableContext lambdaContext = ((LambdaWithContext) lambda).getVariableContext();
            lambda.getValueForMetaPropertyToMany(M3Properties.openVariables).forEach(variable ->
            {
                String name = variable.getName();
                CoreInstance instanceValue = lambdaContext.getValue(name);
                if (instanceValue != null)
                {
                    values.put(name, Lists.immutable.withAll(instanceValue.getValueForMetaPropertyToMany(M3Properties.values)));
                }
            });
        }
        return values.toImmutable();
    }

    @Override
    public ImmutableList<Object> values(InstanceValue instanceValue)
    {
        return Lists.immutable.withAll(Instance.getValueForMetaPropertyToManyResolved(instanceValue, M3Properties.values, this.processorSupport));
    }

    @Override
    public InstanceValue newInstanceValue(GenericType genericType, Multiplicity multiplicity, ListIterable<?> values)
    {
        CoreInstance instanceValue = this.processorSupport.newEphemeralAnonymousCoreInstance(M3Paths.InstanceValue);
        Instance.setValueForProperty(instanceValue, M3Properties.genericType, genericType, this.processorSupport);
        Instance.setValueForProperty(instanceValue, M3Properties.multiplicity, multiplicity, this.processorSupport);
        Instance.setValuesForProperty(instanceValue, M3Properties.values, toCoreInstances(values), this.processorSupport);
        return InstanceValueCoreInstanceWrapper.toInstanceValue(instanceValue);
    }

    @Override
    public FunctionDefinition<?> withExpressionSequence(FunctionDefinition<?> function, ListIterable<? extends ValueSpecification> expressionSequence, ListIterable<String> openVariables)
    {
        MutableMap<String, ListIterable<? extends CoreInstance>> overrides = Maps.mutable.<String, ListIterable<? extends CoreInstance>>with(M3Properties.expressionSequence, expressionSequence);
        if (openVariables != null)
        {
            overrides.put(M3Properties.openVariables, openVariables.collect(this.repository::newStringCoreInstance));
        }
        // parity: a LambdaWithContext copies to a plain lambda, as interpreted Pure's ^$lf(...) does
        CoreInstance copy = copy(function, overrides);
        return (function instanceof LambdaFunction ? LambdaFunctionCoreInstanceWrapper.toLambdaFunction(copy) : FunctionDefinitionCoreInstanceWrapper.toFunctionDefinition(copy));
    }

    @Override
    public FunctionExpression withParametersAndGenericType(FunctionExpression expression, ListIterable<? extends ValueSpecification> parameters, GenericType genericType)
    {
        return FunctionExpressionCoreInstanceWrapper.toFunctionExpression(copy(expression, Maps.mutable.<String, ListIterable<? extends CoreInstance>>with(M3Properties.parametersValues, parameters, M3Properties.genericType, one(genericType))));
    }

    @Override
    public FunctionExpression withFuncAndParameters(FunctionExpression expression, Object func, ListIterable<? extends ValueSpecification> parameters)
    {
        return FunctionExpressionCoreInstanceWrapper.toFunctionExpression(copy(expression, Maps.mutable.<String, ListIterable<? extends CoreInstance>>with(
                M3Properties.func, one((CoreInstance) func),
                M3Properties.parametersValues, parameters)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ValueSpecification> T withGenericType(T valueSpecification, GenericType genericType)
    {
        CoreInstance copy = copy(valueSpecification, Maps.mutable.<String, ListIterable<? extends CoreInstance>>with(M3Properties.genericType, one(genericType)));
        if (valueSpecification instanceof FunctionExpression)
        {
            return (T) FunctionExpressionCoreInstanceWrapper.toFunctionExpression(copy);
        }
        if (valueSpecification instanceof VariableExpression)
        {
            return (T) VariableExpressionCoreInstanceWrapper.toVariableExpression(copy);
        }
        if (valueSpecification instanceof InstanceValue)
        {
            return (T) InstanceValueCoreInstanceWrapper.toInstanceValue(copy);
        }
        return (T) ValueSpecificationCoreInstanceWrapper.toValueSpecification(copy);
    }

    @Override
    public InstanceValue withValues(InstanceValue instanceValue, ListIterable<?> values, GenericType genericType, Multiplicity multiplicity)
    {
        return InstanceValueCoreInstanceWrapper.toInstanceValue(copy(instanceValue, Maps.mutable.<String, ListIterable<? extends CoreInstance>>with(
                M3Properties.values, toCoreInstances(values),
                M3Properties.genericType, one(genericType),
                M3Properties.multiplicity, one(multiplicity))));
    }

    @Override
    public KeyExpression withExpression(KeyExpression keyExpression, ValueSpecification expression)
    {
        return KeyExpressionCoreInstanceWrapper.toKeyExpression(copy(keyExpression, Maps.mutable.<String, ListIterable<? extends CoreInstance>>with(M3Properties.expression, one(expression))));
    }

    @Override
    public GenericType withTypeArguments(GenericType genericType, ListIterable<? extends GenericType> typeArguments)
    {
        return GenericTypeCoreInstanceWrapper.toGenericType(copy(genericType, Maps.mutable.<String, ListIterable<? extends CoreInstance>>with(M3Properties.typeArguments, typeArguments)));
    }

    @Override
    public GenericType withRawType(GenericType genericType, FunctionType rawType)
    {
        return GenericTypeCoreInstanceWrapper.toGenericType(copy(genericType, Maps.mutable.<String, ListIterable<? extends CoreInstance>>with(M3Properties.rawType, one(rawType))));
    }

    @Override
    public FunctionType withSignature(FunctionType functionType, ListIterable<? extends VariableExpression> parameters, GenericType returnType)
    {
        return FunctionTypeCoreInstanceWrapper.toFunctionType(copy(functionType, Maps.mutable.<String, ListIterable<? extends CoreInstance>>with(M3Properties.parameters, parameters, M3Properties.returnType, one(returnType))));
    }

    @Override
    public ImmutableList<Object> reactivate(ValueSpecification valueSpecification, ImmutableMap<String, ImmutableList<Object>> inScopeVars)
    {
        VariableContext context = VariableContext.newVariableContext();
        inScopeVars.forEachKeyValue((name, values) ->
        {
            CoreInstance value = this.processorSupport.newEphemeralAnonymousCoreInstance(M3Paths.InstanceValue);
            Instance.setValuesForProperty(value, M3Properties.values, toCoreInstances(values), this.processorSupport);
            try
            {
                context.registerValue(name, value);
            }
            catch (VariableContext.VariableNameConflictException e)
            {
                throw error(e.getMessage());
            }
        });
        CoreInstance result = this.functionExecution.executeValueSpecification(valueSpecification, this.resolvedTypeParameters, this.resolvedMultiplicityParameters, this.functionExpressionCallStack, context, this.profiler, this.instantiationContext, this.executionSupport);
        return Lists.immutable.withAll(Instance.getValueForMetaPropertyToManyResolved(result, M3Properties.values, this.processorSupport));
    }

    @Override
    public Object getPropertyValue(Object instance, String property)
    {
        return Instance.getValueForMetaPropertyToOneResolved(toCoreInstance(instance), property, this.processorSupport);
    }

    @Override
    public ImmutableList<Object> getPropertyValues(Object instance, String property)
    {
        return Lists.immutable.withAll(Instance.getValueForMetaPropertyToManyResolved(toCoreInstance(instance), property, this.processorSupport));
    }

    @Override
    public Object withPropertyValues(Object instance, String property, ListIterable<?> values)
    {
        return copy(toCoreInstance(instance), Maps.mutable.<String, ListIterable<? extends CoreInstance>>with(property, toCoreInstances(values)));
    }

    @Override
    public RuntimeException error(String message)
    {
        return new PureExecutionException(this.functionExpressionCallStack.isEmpty() ? null : this.functionExpressionCallStack.peek().getSourceInformation(), message);
    }

    private CoreInstance element(String path)
    {
        if (!this.elementsByPath.containsKey(path))
        {
            this.elementsByPath.put(path, this.processorSupport.package_getByUserPath(path));
        }
        return this.elementsByPath.get(path);
    }

    private CoreInstance copy(CoreInstance source, MapIterable<String, ? extends ListIterable<? extends CoreInstance>> overrides)
    {
        CoreInstance classifier = this.processorSupport.getClassifier(source);
        CoreInstance copy = this.repository.newEphemeralAnonymousCoreInstance(null, classifier);
        this.processorSupport.class_getSimplePropertiesByName(classifier).forEachKey(key ->
        {
            ListIterable<? extends CoreInstance> values = overrides.containsKey(key) ? overrides.get(key) : source.getValueForMetaPropertyToMany(key);
            if (values.notEmpty())
            {
                Instance.setValuesForProperty(copy, key, values, this.processorSupport);
            }
        });
        copy.setSourceInformation(source.getSourceInformation());
        return copy;
    }

    private static ImmutableList<CoreInstance> one(CoreInstance value)
    {
        return value == null ? Lists.immutable.empty() : Lists.immutable.with(value);
    }

    private ListIterable<CoreInstance> toCoreInstances(ListIterable<?> values)
    {
        return values.collect(this::toCoreInstance);
    }

    private CoreInstance toCoreInstance(Object value)
    {
        return AnyHelper.wrapPrimitive(value, this.repository);
    }
}
