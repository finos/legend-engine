// Copyright 2020 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.extension.external.json.interpreted.natives;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ConstraintsOverride;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.EnumInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.navigation.measure.Measure;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.primitive.BooleanCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.runtime.java.extension.external.json.shared.JsonDeserializationCache;
import org.finos.legend.pure.runtime.java.extension.external.json.shared.JsonDeserializationContext;
import org.finos.legend.pure.runtime.java.extension.external.json.shared.JsonDeserializer;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.ObjectFactory;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.DefaultConstraintHandler;
import org.finos.legend.pure.runtime.java.interpreted.natives.DeserializationUtils;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.natives.NumericUtilities;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.lang.unit.NewUnit;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Stack;

public class FromJson extends NativeFunction
{
    private final FunctionExecutionInterpreted functionExecution;
    private final ModelRepository repository;

    public FromJson(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        this.functionExecution = functionExecution;
        this.repository = repository;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        SourceInformation si = functionExpressionToUseInStack.getSourceInformation();

        Class startingClass = (Class) Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport);
        CoreInstance config = Instance.getValueForMetaPropertyToOneResolved(params.get(2), M3Properties.values, processorSupport);
        String typeKeyName = PrimitiveUtilities.getStringValue(config.getValueForMetaPropertyToOne("typeKeyName"));
        Map<String, Class> keyLookup = Maps.mutable.empty();
        for (CoreInstance o : config.getValueForMetaPropertyToMany("typeLookup"))
        {
            keyLookup.put(o.getValueForMetaPropertyToOne("first").getName(), (Class<?>) _Package.getByUserPath(o.getValueForMetaPropertyToOne("second").getName(), functionExecution.getProcessorSupport()));
        }
        Boolean failOnUnknownProperties = ((BooleanCoreInstance) config.getValueForMetaPropertyToOne("failOnUnknownProperties")).getValue();
        ConstraintsOverride constraintsOverride = (ConstraintsOverride) config.getValueForMetaPropertyToOne("constraintsHandler");

        String jsonText = PrimitiveUtilities.getStringValue(Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport));
        return JsonDeserializer.fromJson(jsonText, startingClass, new JsonDeserializationContext(new JsonDeserializationCache(), si, processorSupport, typeKeyName, keyLookup, failOnUnknownProperties, new ObjectFactory()
        {
            @Override
            public <T extends Any> T newObject(Class<T> clazz, Map<String, RichIterable<?>> properties)
            {
                CoreInstance instance = FromJson.this.repository.newEphemeralAnonymousCoreInstance(functionExpressionToUseInStack.getSourceInformation(), clazz);
                properties.forEach((key, values) -> Instance.setValuesForProperty(instance, key, values.collect(value -> convertValue(key, value, processorSupport, si), Lists.mutable.empty()), processorSupport));
                DeserializationUtils.replaceReverseProperties(instance, processorSupport, si);

                CoreInstance override = processorSupport.newAnonymousCoreInstance(si, M3Paths.ConstraintsGetterOverride);
                if (constraintsOverride != null)
                {
                    Instance.addValueToProperty(override, M3Properties.constraintsManager, constraintsOverride._constraintsManager(), processorSupport);
                    Instance.addValueToProperty(instance, M3Properties.elementOverride, override, processorSupport);
                }
                CoreInstance value = ValueSpecificationBootstrap.wrapValueSpecification(instance, true, processorSupport);

                return (T) DefaultConstraintHandler.handleConstraints(clazz, value, si, functionExecution, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport);
            }

            public <T extends Any> T newUnitInstance(CoreInstance propertyType, String unitTypeString, Number unitValue)
            {
                CoreInstance retrievedUnit = processorSupport.package_getByUserPath(unitTypeString);
                if (!processorSupport.type_subTypeOf(retrievedUnit, propertyType))
                {
                    throw new PureExecutionException("Cannot match unit type: " + unitTypeString + " as subtype of type: " + PackageableElement.getUserPathForPackageableElement(propertyType));
                }

                ListIterable<CoreInstance> params = Lists.immutable.with(
                        ValueSpecificationBootstrap.wrapValueSpecification(retrievedUnit, false, processorSupport),
                        NumericUtilities.toPureNumberValueExpression(unitValue, false, repository, processorSupport));
                return (T) new NewUnit(functionExecution, repository).execute(params, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport, context, processorSupport);
            }
        }));
    }

    private CoreInstance convertValue(String key, Object value, ProcessorSupport processorSupport, SourceInformation sourceInfo)
    {
        if (value instanceof String)
        {
            return this.repository.newStringCoreInstance((String) value);
        }
        if (value instanceof Boolean)
        {
            return this.repository.newBooleanCoreInstance((Boolean) value);
        }
        if (value instanceof Integer)
        {
            this.repository.newIntegerCoreInstance((Integer) value);
        }
        if (value instanceof Long)
        {
            return this.repository.newIntegerCoreInstance((Long) value);
        }
        if (value instanceof BigDecimal)
        {
            return this.repository.newDecimalCoreInstance((BigDecimal) value);
        }
        if (value instanceof Double)
        {
            return this.repository.newFloatCoreInstance(BigDecimal.valueOf((Double) value));
        }
        if (value instanceof Number)
        {
            this.repository.newFloatCoreInstance(new BigDecimal(value.toString()));
        }
        if (value instanceof PureDate)
        {
            return this.repository.newDateCoreInstance((PureDate) value);
        }
        if (value instanceof InstanceValue)
        {
            InstanceValue asInstanceValue = (InstanceValue) value;
            if (Measure.isUnitOrMeasureInstance(asInstanceValue, processorSupport))
            {
                return asInstanceValue;
            }
            return (CoreInstance) asInstanceValue._values().getFirst();
        }
        if (value instanceof EnumInstance)
        {
            return (CoreInstance) value;
        }
        throw new PureExecutionException(sourceInfo, "Unknown type from output of JsonDeserializer for property: " + key);
    }
}
