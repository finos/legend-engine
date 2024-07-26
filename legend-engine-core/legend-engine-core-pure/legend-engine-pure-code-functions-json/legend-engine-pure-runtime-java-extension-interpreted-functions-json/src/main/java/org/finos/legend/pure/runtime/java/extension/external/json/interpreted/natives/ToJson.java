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
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.AnyCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.FloatCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.PrimitiveCoreInstance;
import org.finos.legend.pure.m4.coreinstance.simple.SimpleCoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.json.shared.JsonSerializationCache;
import org.finos.legend.pure.runtime.java.extension.external.json.shared.JsonSerializationContext;
import org.finos.legend.pure.runtime.java.extension.external.json.shared.JsonSerializer;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.collection.anonymous.map.KeyValues;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.math.BigDecimal;
import java.util.Stack;

public class ToJson extends NativeFunction
{
    private final ModelRepository repository;
    private final FunctionExecutionInterpreted functionExecution;

    public ToJson(FunctionExecutionInterpreted functionExecution, ModelRepository modelRepository)
    {
        this.repository = modelRepository;
        this.functionExecution = functionExecution;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, final Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, final Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, final VariableContext variableContext, final CoreInstance functionExpressionToUseInStack, final Profiler profiler, final InstantiationContext instantiationContext, final ExecutionSupport executionSupport, Context context, final ProcessorSupport processorSupport) throws PureExecutionException
    {
        ListIterable<? extends CoreInstance> pureObjects = Instance.getValueForMetaPropertyToManyResolved(params.get(0), M3Properties.values, processorSupport);
        if (pureObjects.getFirst() instanceof SimpleCoreInstance && !"Map".equals(pureObjects.getFirst().getClassifier().getName()))
        {
            pureObjects = pureObjects.collect(AnyCoreInstanceWrapper::toAny);
        }
        CoreInstance jsonConfig = Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport);
        String typeKeyName = PrimitiveUtilities.getStringValue(jsonConfig.getValueForMetaPropertyToOne("typeKeyName"));
        boolean includeType = PrimitiveUtilities.getBooleanValue(jsonConfig.getValueForMetaPropertyToOne("includeType"), false);
        boolean fullyQualifiedTypePath = PrimitiveUtilities.getBooleanValue(jsonConfig.getValueForMetaPropertyToOne("fullyQualifiedTypePath"), false);
        boolean serializeQualifiedProperties = PrimitiveUtilities.getBooleanValue(jsonConfig.getValueForMetaPropertyToOne("serializeQualifiedProperties"), false);
        String dateTimeFormat = PrimitiveUtilities.getStringValue(jsonConfig.getValueForMetaPropertyToOne("dateTimeFormat"), null);
        boolean serializePackageableElementName = PrimitiveUtilities.getBooleanValue(jsonConfig.getValueForMetaPropertyToOne("serializePackageableElementName"), false);
        boolean removePropertiesWithEmptyValues = PrimitiveUtilities.getBooleanValue(jsonConfig.getValueForMetaPropertyToOne("removePropertiesWithEmptyValues"), false);
        boolean serializeMultiplicityAsNumber = PrimitiveUtilities.getBooleanValue(jsonConfig.getValueForMetaPropertyToOne("serializeMultiplicityAsNumber"), false);
        String encryptionKey = PrimitiveUtilities.getStringValue(jsonConfig.getValueForMetaPropertyToOne("encryptionKey"), null);
        String decryptionKey = PrimitiveUtilities.getStringValue(jsonConfig.getValueForMetaPropertyToOne("decryptionKey"), null);
        ListIterable<? extends CoreInstance> encryptionStereotypes = jsonConfig.getValueForMetaPropertyToMany("encryptionStereotypes");
        ListIterable<? extends CoreInstance> decryptionStereotypes = jsonConfig.getValueForMetaPropertyToMany("decryptionStereotypes");

        String json = JsonSerializer.toJson(pureObjects, processorSupport, new JsonSerializationContext<CoreInstance, CoreInstance>(new JsonSerializationCache(), functionExpressionToUseInStack.getSourceInformation(), processorSupport, new Stack<>(), typeKeyName, includeType, fullyQualifiedTypePath, serializeQualifiedProperties, dateTimeFormat, serializePackageableElementName, removePropertiesWithEmptyValues, serializeMultiplicityAsNumber, encryptionKey, encryptionStereotypes, decryptionKey, decryptionStereotypes)
        {
            @Override
            protected Object extractPrimitiveValue(Object potentiallyWrappedPrimitive)
            {
                Object val = potentiallyWrappedPrimitive instanceof PrimitiveCoreInstance ? ((PrimitiveCoreInstance) potentiallyWrappedPrimitive).getValue() : potentiallyWrappedPrimitive;
                val = potentiallyWrappedPrimitive instanceof FloatCoreInstance && val instanceof BigDecimal ? ((BigDecimal) val).doubleValue() : val;
                return val;
            }

            @Override
            protected Object getValueForProperty(CoreInstance pureObject, Property property, String className)
            {
                CoreInstance res = ToJson.this.functionExecution.executeProperty(property, true, resolvedTypeParameters, resolvedMultiplicityParameters, getParentOrEmptyVariableContext(variableContext), profiler, Lists.immutable.with(ValueSpecificationBootstrap.wrapValueSpecification(pureObject, true, processorSupport)), functionExpressionToUseInStack, instantiationContext, executionSupport);

                if (res instanceof InstanceValue)
                {
                    RichIterable<?> values = ((InstanceValue) res)._values();
                    return (values.getFirst() instanceof SimpleCoreInstance && !"Map".equals(((SimpleCoreInstance) values.getFirst()).getClassifier().getName())) ? values.collect(v -> AnyCoreInstanceWrapper.toAny((CoreInstance) v)) : values;
                }
                return res;
            }

            @Override
            protected Object evaluateQualifiedProperty(CoreInstance pureObject, QualifiedProperty qualifiedProperty, Type type, Multiplicity multiplicity, String propertyName)
            {
                CoreInstance res = ToJson.this.functionExecution.executeFunction(false, qualifiedProperty, Lists.immutable.with(ValueSpecificationBootstrap.wrapValueSpecification(pureObject, true, processorSupport)), new Stack<>(), new Stack<>(), getParentOrEmptyVariableContextForLambda(variableContext, qualifiedProperty), functionExpressionToUseInStack, profiler, instantiationContext, executionSupport);
                if (res instanceof InstanceValue)
                {
                    return ((InstanceValue) res)._values();
                }
                return res;
            }

            @Override
            protected CoreInstance getClassifier(CoreInstance pureObject)
            {
                return pureObject.getClassifier();
            }

            @Override
            protected RichIterable<CoreInstance> getMapKeyValues(CoreInstance pureObject)
            {
                ListIterable<CoreInstance> newPureObjects = Lists.immutable.with(ValueSpecificationBootstrap.wrapValueSpecification(pureObject, true, processorSupport));
                return KeyValues.executeMap(repository, newPureObjects, processorSupport);
            }

        }, functionExpressionToUseInStack.getSourceInformation());
        return ValueSpecificationBootstrap.newStringLiteral(this.repository, json, processorSupport);
    }
}
