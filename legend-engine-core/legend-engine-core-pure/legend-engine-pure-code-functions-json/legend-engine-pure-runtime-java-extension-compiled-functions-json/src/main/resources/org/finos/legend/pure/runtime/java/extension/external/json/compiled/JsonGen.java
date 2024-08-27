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
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ConstraintsOverride;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.CompiledSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Pure;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.measureUnit.UnitProcessor;
import org.finos.legend.pure.runtime.java.extension.external.json.compiled.natives.JsonParserHelper;
import org.finos.legend.pure.runtime.java.extension.external.json.shared.JsonDeserializationCache;
import org.finos.legend.pure.runtime.java.extension.external.json.shared.JsonDeserializationContext;
import org.finos.legend.pure.runtime.java.extension.external.json.shared.JsonDeserializer;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.ObjectFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class JsonGen
{
    @Deprecated
    public static <T> T fromJsonDeprecated(String json, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<T> clazz, Root_meta_json_JSONDeserializationConfig config, SourceInformation si, ExecutionSupport es)
    {
        java.lang.Class c = ((CompiledExecutionSupport) es).getClassCache().getIfAbsentPutInterfaceForType(clazz);
        T obj = (T) JsonParserHelper.fromJson(json, c, "", "", ((CompiledExecutionSupport) es).getMetadataAccessor(), ((CompiledExecutionSupport) es).getClassLoader(), si, config._typeKeyName(), config._failOnUnknownProperties(), config._constraintsHandler(), es);
        return (T) Pure.handleValidation(true, obj, si, es);
    }

    public static String toJson(Object pureObject, Root_meta_json_JSONSerializationConfig jsonConfig, final SourceInformation si, final ExecutionSupport es)
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

        return org.finos.legend.pure.runtime.java.extension.external.json.compiled.JsonNativeImplementation._toJson(pureObject, si, es, typeKeyName, includeType, fullyQualifiedTypePath, serializeQualifiedProperties, dateTimeFormat, serializePackageableElementName, removePropertiesWithEmptyValues, serializeMultiplicityAsNumber, encryptionKey, decryptionKey, encryptionStereotypes, decryptionStereotypes);
    }

    public static <T> T fromJson(String json, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<T> clazz, Root_meta_json_JSONDeserializationConfig config, final SourceInformation si, final ExecutionSupport es)
    {
        final ConstraintsOverride constraintsHandler = config._constraintsHandler();
        final RichIterable<? extends Pair<? extends String, ? extends String>> _typeLookup = config._typeLookup();
        return _fromJson(json, clazz, config._typeKeyName(), config._failOnUnknownProperties(), si, es, constraintsHandler, _typeLookup);
    }

    public static <T> T _fromJson(String json, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<T> clazz, String _typeKeyName, boolean _failOnUnknownProperties, final SourceInformation si, final ExecutionSupport es, final ConstraintsOverride constraintsHandler, RichIterable<? extends Pair<? extends String, ? extends String>> _typeLookup)
    {
        java.lang.Class c;
        String targetClassName = null;
        try
        {
            targetClassName = JavaPackageAndImportBuilder.platformJavaPackage() + ".Root_" + Pure.elementToPath(clazz, "_");
            c = ((CompiledExecutionSupport) es).getClassLoader().loadClass(targetClassName);
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException("Unable to find  class " + targetClassName, e);
        }


        Map<String, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class> typeLookup = new HashMap<String, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class>();
        for (Pair<? extends String, ? extends String> pair : _typeLookup)
        {
            typeLookup.put(pair._first(), ((CompiledExecutionSupport) es).getMetadataAccessor().getClass("Root::" + pair._second()));
        }

        return (T) JsonDeserializer.fromJson(json, (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<? extends Any>) clazz, new JsonDeserializationContext(new JsonDeserializationCache(), si, ((CompiledExecutionSupport) es).getProcessorSupport(), _typeKeyName, typeLookup, _failOnUnknownProperties, new ObjectFactory()
        {
            public <U extends Any> U newObject(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<U> clazz, Map<String, RichIterable<?>> properties)
            {
                FastList<Root_meta_pure_functions_lang_KeyValue> keyValues = new FastList<>();
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
                U result = (U) org.finos.legend.pure.generated.CoreGen.newObject(clazz, keyValues, null, null, null, null, null, null, es);
                result._elementOverride(constraintsHandler);
                return (U) Pure.handleValidation(true, result, si, es);
            }

            public <T extends Any> T newUnitInstance(CoreInstance propertyType, String unitTypeString, Number unitValue) throws Exception
            {
                CoreInstance unitRetrieved = ((CompiledExecutionSupport) es).getProcessorSupport().package_getByUserPath(unitTypeString);
                if (!((CompiledExecutionSupport) es).getProcessorSupport().type_subTypeOf(unitRetrieved, propertyType))
                {
                    throw new PureExecutionException("Cannot match unit type: " + unitTypeString + " as subtype of type: " + PackageableElement.getUserPathForPackageableElement(propertyType));
                }

                String unitClassName = UnitProcessor.convertToJavaCompatibleClassName(JavaPackageAndImportBuilder.buildImplUnitInstanceClassNameFromType(unitRetrieved));

                java.lang.Class c = ((CompiledExecutionSupport) es).getClassLoader().loadClass("org.finos.legend.pure.generated." + unitClassName);

                java.lang.Class paramClasses[] = new java.lang.Class[]{String.class, ExecutionSupport.class};
                Method method = c.getMethod("_val", Number.class);
                Object classInstance = c.getConstructor(paramClasses).newInstance("Anonymous_NoCounter", es);
                method.invoke(classInstance, unitValue);
                return (T) classInstance;
            }
        }));
    }
}
