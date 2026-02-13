// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.generated;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Stacks;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.generated.CoreGen;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ConstraintsOverride;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.measure.Measure;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.CompiledSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Pure;
import org.finos.legend.pure.runtime.java.extension.external.json.compiled.JsonNativeImplementation;
import org.finos.legend.pure.runtime.java.extension.external.json.compiled.natives.JsonParserHelper;
import org.finos.legend.pure.runtime.java.extension.external.json.shared.JsonDeserializationCache;
import org.finos.legend.pure.runtime.java.extension.external.json.shared.JsonDeserializationContext;
import org.finos.legend.pure.runtime.java.extension.external.json.shared.JsonDeserializer;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.ObjectFactory;

import java.util.Map;

public class JsonGen
{
    @Deprecated
    public static <T> T fromJsonDeprecated(String json, Class<T> clazz, Root_meta_json_JSONDeserializationConfig config, SourceInformation si, ExecutionSupport es)
    {
        java.lang.Class c = ((CompiledExecutionSupport) es).getClassCache().getIfAbsentPutInterfaceForType(clazz);
        T obj = (T) JsonParserHelper.fromJson(json, c, "", "", ((CompiledExecutionSupport) es).getMetadataAccessor(), ((CompiledExecutionSupport) es).getClassLoader(), si, config._typeKeyName(), config._failOnUnknownProperties(), config._constraintsHandler(), es);
        return (T) Pure.handleValidation(true, obj, si, es);
    }

    public static String toJson(Object pureObject, Root_meta_json_JSONSerializationConfig jsonConfig, SourceInformation si, ExecutionSupport es)
    {
        return toJson(CompiledSupport.toPureCollection(pureObject), jsonConfig, si, es);
    }

    private static String toJson(RichIterable<?> pureObject, Root_meta_json_JSONSerializationConfig jsonConfig, final SourceInformation si, final ExecutionSupport es)
    {
        String typeKeyName = jsonConfig._typeKeyName();
        boolean includeType = jsonConfig._includeType() != null ? jsonConfig._includeType() : false;
        boolean fullyQualifiedTypePath = jsonConfig._fullyQualifiedTypePath() != null ? jsonConfig._fullyQualifiedTypePath() : false;
        boolean serializeQualifiedProperties = jsonConfig._serializeQualifiedProperties() != null ? jsonConfig._serializeQualifiedProperties() : false;
        String dateTimeFormat = jsonConfig._dateTimeFormat();
        boolean serializePackageableElementName = jsonConfig._serializePackageableElementName() != null ? jsonConfig._serializePackageableElementName() : false;
        boolean removePropertiesWithEmptyValues = jsonConfig._removePropertiesWithEmptyValues() != null ? jsonConfig._removePropertiesWithEmptyValues() : false;
        boolean serializeMultiplicityAsNumber = jsonConfig._serializeMultiplicityAsNumber() != null ? jsonConfig._serializeMultiplicityAsNumber() : false;
        String encryptionKey = jsonConfig._encryptionKey();
        String decryptionKey = jsonConfig._decryptionKey();
        RichIterable<? extends CoreInstance> encryptionStereotypes = jsonConfig._encryptionStereotypes();
        RichIterable<? extends CoreInstance> decryptionStereotypes = jsonConfig._decryptionStereotypes();

        return JsonNativeImplementation._toJson(pureObject, si, es, typeKeyName, includeType, fullyQualifiedTypePath, serializeQualifiedProperties, dateTimeFormat, serializePackageableElementName, removePropertiesWithEmptyValues, serializeMultiplicityAsNumber, encryptionKey, decryptionKey, encryptionStereotypes, decryptionStereotypes);
    }

    public static <T> T fromJson(String json, Class<T> clazz, Root_meta_json_JSONDeserializationConfig config, SourceInformation si, ExecutionSupport es)
    {
        return _fromJson(json, clazz, config._typeKeyName(), config._failOnUnknownProperties(), si, es, config._constraintsHandler(), config._typeLookup());
    }

    public static <T> T _fromJson(String json, Class<T> clazz, String _typeKeyName, boolean _failOnUnknownProperties, SourceInformation si, ExecutionSupport es, ConstraintsOverride constraintsHandler, RichIterable<? extends Pair<? extends String, ? extends String>> _typeLookup)
    {
        return _fromJson(json, clazz, _typeKeyName, _failOnUnknownProperties, si, (CompiledExecutionSupport) es, constraintsHandler, _typeLookup);
    }

    @SuppressWarnings("unchecked")
    public static <T> T _fromJson(String json, Class<T> clazz, String _typeKeyName, boolean _failOnUnknownProperties, final SourceInformation si, final CompiledExecutionSupport es, final ConstraintsOverride constraintsHandler, RichIterable<? extends Pair<? extends String, ? extends String>> _typeLookup)
    {
        String targetClassName = JavaPackageAndImportBuilder.buildInterfaceReferenceFromType(clazz, es.getProcessorSupport());
        try
        {
            es.getClassLoader().loadClass(targetClassName);
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException("Unable to find  class " + targetClassName, e);
        }


        Map<String, Class> typeLookup = Maps.mutable.empty();
        for (Pair<? extends String, ? extends String> pair : _typeLookup)
        {
            typeLookup.put(pair._first(), es.getMetadataAccessor().getClass(pair._second()));
        }

        return (T) JsonDeserializer.fromJson(json, (Class<? extends Any>) clazz, new JsonDeserializationContext(new JsonDeserializationCache(), si, es.getProcessorSupport(), _typeKeyName, typeLookup, _failOnUnknownProperties, new ObjectFactory()
        {
            @Override
            public <U extends Any> U newObject(Class<U> clazz, Map<String, RichIterable<?>> properties)
            {
                MutableList<Root_meta_pure_functions_lang_KeyValue> keyValues = Lists.mutable.ofInitialCapacity(properties.size());
                for (Map.Entry<String, RichIterable<?>> property : properties.entrySet())
                {
                    Root_meta_pure_functions_lang_KeyValue keyValue = new Root_meta_pure_functions_lang_KeyValue_Impl("Anonymous");
                    keyValue._key(property.getKey());
                    for (Object value : property.getValue())
                    {
                        keyValue._valueAdd(value);
                    }
                    keyValues.add(keyValue);
                }
                U result = (U) CoreGen.newObject(clazz, keyValues, null, null, null, null, null, null, es);
                result._elementOverride(constraintsHandler);
                return (U) Pure.handleValidation(true, result, si, es);
            }

            @Override
            public <T extends Any> T newUnitInstance(CoreInstance propertyType, String unitTypeString, Number unitValue)
            {
                CoreInstance unitRetrieved = Measure.getUnitByUserPath(unitTypeString, es.getProcessorSupport());
                if (!es.getProcessorSupport().type_subTypeOf(unitRetrieved, propertyType))
                {
                    StringBuilder builder = new StringBuilder("Cannot match unit type: ").append(unitTypeString).append(" as subtype of type: ");
                    if (Measure.isUnit(propertyType, es.getProcessorSupport()))
                    {
                        Measure.writeUserPathForUnit(builder, propertyType);
                    }
                    else
                    {
                        PackageableElement.writeUserPathForPackageableElement(builder, propertyType);
                    }
                    throw new PureExecutionException(builder.toString(), Stacks.mutable.<org.finos.legend.pure.m4.coreinstance.CoreInstance>empty());
                }

                return (T) CompiledSupport.newUnitInstance(unitRetrieved, unitValue, es);
            }
        }));
    }
}
