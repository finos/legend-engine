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

package org.finos.legend.pure.runtime.java.extension.external.json.compiled;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Stacks;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;
import org.finos.legend.pure.runtime.java.extension.external.json.shared.JsonSerializationCache;
import org.finos.legend.pure.runtime.java.extension.external.json.shared.JsonSerializationContext;
import org.finos.legend.pure.runtime.java.extension.external.json.shared.JsonSerializer;

import java.lang.reflect.Method;
import java.util.Stack;

public class JsonNativeImplementation
{
    public static String _toJson(RichIterable<?> pureObject, SourceInformation si, ExecutionSupport es, String typeKeyName, boolean includeType, boolean fullyQualifiedTypePath, boolean serializeQualifiedProperties, String dateTimeFormat, boolean serializePackageableElementName, boolean removePropertiesWithEmptyValues, boolean serializeMultiplicityAsNumber, String encryptionKey, String decryptionKey, RichIterable<? extends CoreInstance> encryptionStereotypes, RichIterable<? extends CoreInstance> decryptionStereotypes)
    {
        return JsonSerializer.toJson(pureObject, ((CompiledExecutionSupport)es).getProcessorSupport(), new JsonSerializationContext<Any, Object>(new JsonSerializationCache(), si, ((CompiledExecutionSupport)es).getProcessorSupport(), new Stack(), typeKeyName, includeType, fullyQualifiedTypePath, serializeQualifiedProperties, dateTimeFormat, serializePackageableElementName, removePropertiesWithEmptyValues, serializeMultiplicityAsNumber, encryptionKey, encryptionStereotypes, decryptionKey, decryptionStereotypes)
        {
            @Override
            protected Object extractPrimitiveValue(Object potentiallyWrappedPrimitive)
            {
                return potentiallyWrappedPrimitive;
            }

            @Override
            protected RichIterable<?> getValueForProperty(Any pureObject, Property property, String className)
            {
                return this.findAndInvokePropertyMethod(pureObject, property.getName(), className, false);
            }

            @Override
            protected Object evaluateQualifiedProperty(Any pureObject, QualifiedProperty qualifiedProperty, Type type, Multiplicity multiplicity, String propertyName)
            {
                return this.findAndInvokePropertyMethod(pureObject, propertyName, pureObject.getName(), true);
            }

            @Override
            protected CoreInstance getClassifier(Any pureObject)
            {
                return ((CompiledExecutionSupport)es).getProcessorSupport().getClassifier(pureObject);
            }

            @Override
            protected RichIterable<CoreInstance> getMapKeyValues(Any pureObject)
            {
                return ((PureMap)pureObject).getMap().keyValuesView();
            }

            private RichIterable<?> findAndInvokePropertyMethod(Any pureObject, String propertyName, String className, boolean isQualified)
            {
                Method propertyGetter = null;
                try
                {
                    Object value;
                    if (isQualified)
                    {
                        propertyGetter = pureObject.getClass().getMethod(propertyName, ExecutionSupport.class);
                        value = propertyGetter.invoke(pureObject, es);
                    }
                    else
                    {
                        propertyGetter = pureObject.getClass().getMethod("_" + propertyName);
                        value = propertyGetter.invoke(pureObject);
                    }
                    return value instanceof RichIterable ? (RichIterable<?>)value : Lists.immutable.of(value);
                }
                catch (NoSuchMethodException e)
                {
                    throw new PureExecutionException(si, "Error retrieving value of a property: " + propertyName + " from the class " + className + ". Property might not exist", e, Stacks.mutable.empty());
                }
                catch (Exception e)
                {
                    throw new PureExecutionException(si, "Error serializing property: " + propertyName, e, Stacks.mutable.empty());
                }
            }
        }, si);
    }
}
