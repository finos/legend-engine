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

package org.finos.legend.engine.pure.preeval.compiled;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.engine.pure.preeval.PrevalRuntime;
import org.finos.legend.pure.generated.CoreGen;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_multiplicity_MultiplicityValue_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_multiplicity_Multiplicity_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.ElementWithStereotypes;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.KeyExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.TypeParameter;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.CompiledSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Pure;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

final class CompiledPrevalRuntime implements PrevalRuntime
{
    private static final String PACKAGEABLE_MULTIPLICITY = "meta::pure::metamodel::multiplicity::PackageableMultiplicity";

    private final CompiledExecutionSupport executionSupport;
    private final ProcessorSupport processorSupport;
    private final SourceInformation sourceInformation;
    private final MutableMap<String, CoreInstance> elementsByPath = Maps.mutable.empty();
    private Multiplicity pureOne;
    private Multiplicity pureZero;

    CompiledPrevalRuntime(SourceInformation sourceInformation, ExecutionSupport executionSupport)
    {
        this.executionSupport = (CompiledExecutionSupport) executionSupport;
        this.processorSupport = this.executionSupport.getProcessorSupport();
        this.sourceInformation = sourceInformation;
    }

    @Override
    public boolean isInstanceOf(Object value, String typePath)
    {
        CoreInstance type = element(typePath);
        return type != null && Pure.instanceOf(value, (Type) type, this.executionSupport);
    }

    @Override
    public String typeDescription(Object value)
    {
        Type type = CoreGen.safeGetGenericType(value, this.executionSupport)._rawType();
        return type instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement
                ? PackageableElement.getUserPathForPackageableElement(type)
                : CompiledSupport.pureToString(type, this.executionSupport);
    }

    @Override
    public boolean isFunction(Object function, String functionPath)
    {
        CoreInstance element = element(functionPath);
        return element != null && element == function;
    }

    @Override
    public Boolean booleanValue(Object value)
    {
        return value instanceof Boolean ? (Boolean) value : null;
    }

    @Override
    public Object function(String functionPath)
    {
        return element(functionPath);
    }

    @Override
    public boolean hasStereotype(Object function, String profilePath, String stereotype)
    {
        return function instanceof ElementWithStereotypes && ((ElementWithStereotypes) function)._stereotypes().anySatisfy(s -> stereotype.equals(s._value()) && s._profile() != null && profilePath.equals(PackageableElement.getUserPathForPackageableElement(s._profile())));
    }

    @Override
    public ImmutableList<String> parameterNames(Object function)
    {
        FunctionType functionType = (FunctionType) this.processorSupport.function_getFunctionType((CoreInstance) function);
        return Lists.immutable.<VariableExpression>withAll(functionType._parameters()).collect(VariableExpression::_name);
    }

    @Override
    public ImmutableList<String> typeParameterNames(Object function)
    {
        FunctionType functionType = (FunctionType) this.processorSupport.function_getFunctionType((CoreInstance) function);
        return Lists.immutable.<TypeParameter>withAll(functionType._typeParameters()).collect(TypeParameter::_name);
    }

    @Override
    public ImmutableList<GenericType> resolvedTypeParameters(FunctionExpression expression)
    {
        return Lists.immutable.withAll(expression._resolvedTypeParameters());
    }

    @Override
    public boolean isQualifiedPropertyOf(Object function, String ownerPath)
    {
        return function instanceof QualifiedProperty
                && ((QualifiedProperty<?>) function)._owner() instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement
                && ownerPath.equals(PackageableElement.getUserPathForPackageableElement(((QualifiedProperty<?>) function)._owner()));
    }

    @Override
    public String stringValue(Object primitive)
    {
        return (String) primitive;
    }

    @Override
    public Multiplicity pureOne()
    {
        if (this.pureOne == null)
        {
            this.pureOne = (Multiplicity) this.executionSupport.getMetadata(PACKAGEABLE_MULTIPLICITY, "meta::pure::metamodel::multiplicity::PureOne");
        }
        return this.pureOne;
    }

    @Override
    public Multiplicity pureZero()
    {
        if (this.pureZero == null)
        {
            this.pureZero = (Multiplicity) this.executionSupport.getMetadata(PACKAGEABLE_MULTIPLICITY, "meta::pure::metamodel::multiplicity::PureZero");
        }
        return this.pureZero;
    }

    @Override
    public boolean isPureOne(Multiplicity multiplicity)
    {
        return multiplicity == pureOne();
    }

    @Override
    public boolean isPureZero(Multiplicity multiplicity)
    {
        return multiplicity == pureZero();
    }

    @Override
    public Long lowerBound(Multiplicity multiplicity)
    {
        return multiplicity._lowerBound() == null ? null : multiplicity._lowerBound()._value();
    }

    @Override
    public Long upperBound(Multiplicity multiplicity)
    {
        return multiplicity._upperBound() == null ? null : multiplicity._upperBound()._value();
    }

    @Override
    public boolean isMultiplicityConcrete(Multiplicity multiplicity)
    {
        return multiplicity._multiplicityParameter() == null;
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
        return new Root_meta_pure_metamodel_multiplicity_Multiplicity_Impl("Anonymous_NoCounter")
                ._lowerBound(new Root_meta_pure_metamodel_multiplicity_MultiplicityValue_Impl("Anonymous_NoCounter")._value((long) size))
                ._upperBound(new Root_meta_pure_metamodel_multiplicity_MultiplicityValue_Impl("Anonymous_NoCounter")._value((long) size));
    }

    @Override
    public ImmutableMap<String, ImmutableList<Object>> openVariableValues(LambdaFunction<?> lambda)
    {
        MutableMap<String, ImmutableList<Object>> values = Maps.mutable.empty();
        Pure.getOpenVariables(lambda, CoreGen.bridge).getMap().forEachKeyValue((name, value) -> values.put((String) name, toValues(value)));
        return values.toImmutable();
    }

    @Override
    public ImmutableList<Object> values(InstanceValue instanceValue)
    {
        return Lists.immutable.withAll(instanceValue._values());
    }

    @Override
    public InstanceValue newInstanceValue(GenericType genericType, Multiplicity multiplicity, ListIterable<?> values)
    {
        InstanceValue instanceValue = new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("Anonymous_NoCounter");
        instanceValue._genericType(genericType);
        instanceValue._multiplicity(multiplicity);
        instanceValue._values(Lists.mutable.withAll(values));
        return instanceValue;
    }

    @Override
    public FunctionDefinition<?> withExpressionSequence(FunctionDefinition<?> function, ListIterable<? extends ValueSpecification> expressionSequence, ListIterable<String> openVariables)
    {
        // parity: compiled Pure uses the setter's return value, which unwraps a PureCompiledLambda, and its second ^ copy gives the lambda the call site's source information, so the original precompiled body is not found and reused
        FunctionDefinition<?> copy = CompiledSupport.copy(function, this.sourceInformation)._expressionSequence(Lists.mutable.withAll(expressionSequence));
        return openVariables == null ? copy : CompiledSupport.copy((LambdaFunction<?>) copy, this.sourceInformation)._openVariables(Lists.mutable.withAll(openVariables));
    }

    @Override
    public FunctionExpression withParametersAndGenericType(FunctionExpression expression, ListIterable<? extends ValueSpecification> parameters, GenericType genericType)
    {
        FunctionExpression copy = CompiledSupport.copy(expression);
        copy._parametersValues(Lists.mutable.withAll(parameters));
        copy._genericType(genericType);
        return copy;
    }

    @Override
    public FunctionExpression withFuncAndParameters(FunctionExpression expression, Object func, ListIterable<? extends ValueSpecification> parameters)
    {
        FunctionExpression copy = CompiledSupport.copy(expression);
        copy._func((Function<?>) func);
        copy._parametersValues(Lists.mutable.withAll(parameters));
        return copy;
    }

    @Override
    public <T extends ValueSpecification> T withGenericType(T valueSpecification, GenericType genericType)
    {
        T copy = CompiledSupport.copy(valueSpecification);
        copy._genericType(genericType);
        return copy;
    }

    @Override
    public InstanceValue withValues(InstanceValue instanceValue, ListIterable<?> values, GenericType genericType, Multiplicity multiplicity)
    {
        InstanceValue copy = CompiledSupport.copy(instanceValue);
        copy._values(Lists.mutable.withAll(values));
        copy._genericType(genericType);
        copy._multiplicity(multiplicity);
        return copy;
    }

    @Override
    public KeyExpression withExpression(KeyExpression keyExpression, ValueSpecification expression)
    {
        KeyExpression copy = CompiledSupport.copy(keyExpression);
        copy._expression(expression);
        return copy;
    }

    @Override
    public GenericType withTypeArguments(GenericType genericType, ListIterable<? extends GenericType> typeArguments)
    {
        GenericType copy = CompiledSupport.copy(genericType);
        copy._typeArguments(Lists.mutable.withAll(typeArguments));
        return copy;
    }

    @Override
    public GenericType withRawType(GenericType genericType, FunctionType rawType)
    {
        GenericType copy = CompiledSupport.copy(genericType);
        copy._rawType(rawType);
        return copy;
    }

    @Override
    public FunctionType withSignature(FunctionType functionType, ListIterable<? extends VariableExpression> parameters, GenericType returnType)
    {
        FunctionType copy = CompiledSupport.copy(functionType);
        copy._parameters(Lists.mutable.withAll(parameters));
        copy._returnType(returnType);
        return copy;
    }

    @Override
    public ImmutableList<Object> reactivate(ValueSpecification valueSpecification, ImmutableMap<String, ImmutableList<Object>> inScopeVars)
    {
        PureMap vars = new PureMap(Maps.mutable.empty());
        inScopeVars.forEachKeyValue((name, values) -> vars.getMap().put(name, CoreGen.bridge.buildList()._valuesAddAll(values)));
        // parity: Pure's result match makes a single-element nested collection one value; other nested collections are left as they are
        return Lists.immutable.withAll(CompiledSupport.toPureCollection(Pure.reactivate(valueSpecification, vars, CoreGen.bridge, this.executionSupport)))
                .collect(CompiledPrevalRuntime::unwrapSingleton);
    }

    @Override
    public Object getPropertyValue(Object instance, String property)
    {
        return ((CoreInstance) instance).getValueForMetaPropertyToOne(property);
    }

    @Override
    public ImmutableList<Object> getPropertyValues(Object instance, String property)
    {
        return Lists.immutable.withAll(((CoreInstance) instance).getValueForMetaPropertyToMany(property));
    }

    @Override
    public Object withPropertyValues(Object instance, String property, ListIterable<?> values)
    {
        Object copy = CompiledSupport.copy(instance);
        Method toOneSetter = null;
        Method toManySetter = null;
        for (Method method : copy.getClass().getMethods())
        {
            if (method.getName().equals("_" + property) && method.getParameterCount() == 1)
            {
                Class<?> parameterType = method.getParameterTypes()[0];
                if (RichIterable.class.isAssignableFrom(parameterType))
                {
                    toManySetter = method;
                }
                else if (!parameterType.isPrimitive())
                {
                    toOneSetter = method;
                }
            }
        }
        if (toOneSetter == null && toManySetter == null)
        {
            throw new IllegalStateException("No setter for property " + property + " on " + copy.getClass().getName());
        }
        try
        {
            if (toOneSetter != null)
            {
                toOneSetter.invoke(copy, values.getOnly());
            }
            else
            {
                toManySetter.invoke(copy, Lists.mutable.withAll(values));
            }
        }
        catch (IllegalAccessException e)
        {
            throw new IllegalStateException("Cannot set property " + property + " on " + copy.getClass().getName(), e);
        }
        catch (InvocationTargetException e)
        {
            throw new IllegalStateException("Failed to set property " + property + " on " + copy.getClass().getName(), e.getCause());
        }
        return copy;
    }

    @Override
    public RuntimeException error(String message)
    {
        return new PureExecutionException(this.sourceInformation, message);
    }

    private CoreInstance element(String path)
    {
        if (!this.elementsByPath.containsKey(path))
        {
            this.elementsByPath.put(path, this.processorSupport.package_getByUserPath(path));
        }
        return this.elementsByPath.get(path);
    }

    private static Object unwrapSingleton(Object value)
    {
        if (value instanceof Iterable)
        {
            ImmutableList<Object> nested = Lists.immutable.withAll((Iterable<?>) value);
            return nested.size() == 1 ? nested.getOnly() : value;
        }
        return value;
    }

    private static ImmutableList<Object> toValues(Object value)
    {
        if (value instanceof List)
        {
            return Lists.immutable.withAll(((List<?>) value)._values());
        }
        return Lists.immutable.withAll(CompiledSupport.toPureCollection(value));
    }
}
