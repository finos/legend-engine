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

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.Native;
import org.finos.legend.pure.runtime.java.extension.external.json.compiled.natives.EqualJsonStrings;
import org.finos.legend.pure.runtime.java.extension.external.json.compiled.natives.Escape;
import org.finos.legend.pure.runtime.java.extension.external.json.compiled.natives.FromJson;
import org.finos.legend.pure.runtime.java.extension.external.json.compiled.natives.FromJsonDeprecated;
import org.finos.legend.pure.runtime.java.extension.external.json.compiled.natives.ParseJSON;
import org.finos.legend.pure.runtime.java.extension.external.json.compiled.natives.ToJsonBeta;
import org.finos.legend.pure.runtime.java.extension.external.json.shared.JsonParser;

import java.util.List;

public class JsonExtensionCompiled implements CompiledExtension
{
    static
    {
        JsonParser.processor = new CompiledJsonExtraTypeProcessor();
    }

    @Override
    public List<StringJavaSource> getExtraJavaSources()
    {
        return Lists.fixedSize.with(StringJavaSource.newStringJavaSource("org.finos.legend.pure.generated", "JsonGen",
                "package org.finos.legend.pure.generated;\n" +
                        "\n" +
                        "import org.eclipse.collections.api.RichIterable;\n" +
                        "import org.eclipse.collections.impl.list.mutable.FastList;\n" +
                        "import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair;\n" +
                        "import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;\n" +
                        "import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ConstraintsOverride;\n" +
                        "import org.finos.legend.pure.m3.exception.PureExecutionException;\n" +
                        "import org.finos.legend.pure.m3.execution.ExecutionSupport;\n" +
                        "import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;\n" +
                        "import org.finos.legend.pure.m4.coreinstance.CoreInstance;\n" +
                        "import org.finos.legend.pure.m4.coreinstance.SourceInformation;\n" +
                        "import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;\n" +
                        "import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;\n" +
                        "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.CompiledSupport;\n" +
                        "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Pure;\n" +
                        "import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.measureUnit.UnitProcessor;\n" +
                        "import org.finos.legend.pure.runtime.java.extension.external.json.compiled.natives.JsonParserHelper;\n" +
                        "import org.finos.legend.pure.runtime.java.extension.external.json.shared.JsonDeserializationCache;\n" +
                        "import org.finos.legend.pure.runtime.java.extension.external.json.shared.JsonDeserializationContext;\n" +
                        "import org.finos.legend.pure.runtime.java.extension.external.json.shared.JsonDeserializer;\n" +
                        "import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.ObjectFactory;\n" +
                        "\n" +
                        "import java.lang.reflect.Method;\n" +
                        "import java.util.HashMap;\n" +
                        "import java.util.Map;\n" +
                        "\n" +
                        "public class JsonGen\n" +
                        "{\n" +
                        "    @Deprecated\n" +
                        "    public static <T> T fromJsonDeprecated(String json, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<T> clazz, Root_meta_json_JSONDeserializationConfig config, SourceInformation si, ExecutionSupport es)\n" +
                        "    {\n" +
                        "        java.lang.Class c = ((CompiledExecutionSupport) es).getClassCache().getIfAbsentPutInterfaceForType(clazz);\n" +
                        "        T obj = (T) JsonParserHelper.fromJson(json, c, \"\", \"\", ((CompiledExecutionSupport) es).getMetadataAccessor(), ((CompiledExecutionSupport) es).getClassLoader(), si, config._typeKeyName(), config._failOnUnknownProperties(), config._constraintsHandler(), es);\n" +
                        "        return (T) Pure.handleValidation(true, obj, si, es);\n" +
                        "    }\n" +
                        "\n" +
                        "    public static String toJson(Object pureObject, Root_meta_json_JSONSerializationConfig jsonConfig, final SourceInformation si, final ExecutionSupport es)\n" +
                        "    {\n" +
                        "        return toJson(CompiledSupport.toPureCollection(pureObject), jsonConfig, si, es);\n" +
                        "    }\n" +
                        "\n" +
                        "    private static String toJson(RichIterable<?> pureObject, Root_meta_json_JSONSerializationConfig jsonConfig, final SourceInformation si, final ExecutionSupport es)\n" +
                        "    {\n" +
                        "        String typeKeyName = jsonConfig._typeKeyName();\n" +
                        "        boolean includeType = jsonConfig._includeType() != null ? jsonConfig._includeType() : false;\n" +
                        "        boolean fullyQualifiedTypePath = jsonConfig._fullyQualifiedTypePath() != null ? jsonConfig._fullyQualifiedTypePath() : false;\n" +
                        "        boolean serializeQualifiedProperties = jsonConfig._serializeQualifiedProperties() != null ? jsonConfig._serializeQualifiedProperties() : false;\n" +
                        "        String dateTimeFormat = jsonConfig._dateTimeFormat();\n" +
                        "        boolean serializePackageableElementName = jsonConfig._serializePackageableElementName() != null ? jsonConfig._serializePackageableElementName() : false;\n" +
                        "        boolean removePropertiesWithEmptyValues = jsonConfig._removePropertiesWithEmptyValues() != null ? jsonConfig._removePropertiesWithEmptyValues() : false;\n" +
                        "        boolean serializeMultiplicityAsNumber = jsonConfig._serializeMultiplicityAsNumber() != null ? jsonConfig._serializeMultiplicityAsNumber() : false;\n" +
                        "        String encryptionKey = jsonConfig._encryptionKey();\n" +
                        "        String decryptionKey = jsonConfig._decryptionKey();\n" +
                        "        RichIterable<? extends CoreInstance> encryptionStereotypes = jsonConfig._encryptionStereotypes();\n" +
                        "        RichIterable<? extends CoreInstance> decryptionStereotypes = jsonConfig._decryptionStereotypes();\n" +
                        "\n" +
                        "        return org.finos.legend.pure.runtime.java.extension.external.json.compiled.JsonNativeImplementation._toJson(pureObject, si, es, typeKeyName, includeType, fullyQualifiedTypePath, serializeQualifiedProperties, dateTimeFormat, serializePackageableElementName, removePropertiesWithEmptyValues, serializeMultiplicityAsNumber, encryptionKey, decryptionKey, encryptionStereotypes, decryptionStereotypes);\n" +
                        "    }\n" +
                        "\n" +
                        "    public static <T> T fromJson(String json, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<T> clazz, Root_meta_json_JSONDeserializationConfig config, final SourceInformation si, final ExecutionSupport es)\n" +
                        "    {\n" +
                        "        final ConstraintsOverride constraintsHandler = config._constraintsHandler();\n" +
                        "        final RichIterable<? extends Pair<? extends String, ? extends String>> _typeLookup = config._typeLookup();\n" +
                        "        return _fromJson(json, clazz, config._typeKeyName(), config._failOnUnknownProperties(), si, es, constraintsHandler, _typeLookup);\n" +
                        "    }\n" +
                        "\n" +
                        "    public static <T> T _fromJson(String json, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<T> clazz, String _typeKeyName, boolean _failOnUnknownProperties, final SourceInformation si, final ExecutionSupport es, final ConstraintsOverride constraintsHandler, RichIterable<? extends Pair<? extends String, ? extends String>> _typeLookup)\n" +
                        "    {\n" +
                        "        java.lang.Class c;\n" +
                        "        String targetClassName = null;\n" +
                        "        try\n" +
                        "        {\n" +
                        "            targetClassName = JavaPackageAndImportBuilder.platformJavaPackage() + \".Root_\" + Pure.elementToPath(clazz, \"_\");\n" +
                        "            c = ((CompiledExecutionSupport) es).getClassLoader().loadClass(targetClassName);\n" +
                        "        }\n" +
                        "        catch (ClassNotFoundException e)\n" +
                        "        {\n" +
                        "            throw new RuntimeException(\"Unable to find  class \" + targetClassName, e);\n" +
                        "        }\n" +
                        "\n" +
                        "\n" +
                        "        Map<String, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class> typeLookup = new HashMap<String, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class>();\n" +
                        "        for (Pair<? extends String, ? extends String> pair : _typeLookup)\n" +
                        "        {\n" +
                        "            typeLookup.put(pair._first(), ((CompiledExecutionSupport) es).getMetadataAccessor().getClass(\"Root::\" + pair._second()));\n" +
                        "        }\n" +
                        "\n" +
                        "        return (T) JsonDeserializer.fromJson(json, (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<? extends Any>) clazz, new JsonDeserializationContext(new JsonDeserializationCache(), si, ((CompiledExecutionSupport) es).getProcessorSupport(), _typeKeyName, typeLookup, _failOnUnknownProperties, new ObjectFactory()\n" +
                        "        {\n" +
                        "            public <U extends Any> U newObject(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<U> clazz, Map<String, RichIterable<?>> properties)\n" +
                        "            {\n" +
                        "                FastList<Root_meta_pure_functions_lang_KeyValue> keyValues = new FastList<>();\n" +
                        "                for (Map.Entry<String, RichIterable<?>> property : properties.entrySet())\n" +
                        "                {\n" +
                        "                    Root_meta_pure_functions_lang_KeyValue keyValue = new Root_meta_pure_functions_lang_KeyValue_Impl(\"Anonymous\");\n" +
                        "                    keyValue._key(property.getKey());\n" +
                        "                    for (Object value : property.getValue())\n" +
                        "                    {\n" +
                        "                        keyValue._valueAdd(value);\n" +
                        "                    }\n" +
                        "                    keyValues.add(keyValue);\n" +
                        "                }\n" +
                        "                U result = (U) org.finos.legend.pure.generated.CoreGen.newObject(clazz, keyValues, null, null, null, null, null, null, es);\n" +
                        "                result._elementOverride(constraintsHandler);\n" +
                        "                return (U) Pure.handleValidation(true, result, si, es);\n" +
                        "            }\n" +
                        "\n" +
                        "            public <T extends Any> T newUnitInstance(CoreInstance propertyType, String unitTypeString, Number unitValue) throws Exception\n" +
                        "            {\n" +
                        "                CoreInstance unitRetrieved = ((CompiledExecutionSupport) es).getProcessorSupport().package_getByUserPath(unitTypeString);\n" +
                        "                if (!((CompiledExecutionSupport) es).getProcessorSupport().type_subTypeOf(unitRetrieved, propertyType))\n" +
                        "                {\n" +
                        "                    throw new PureExecutionException(\"Cannot match unit type: \" + unitTypeString + \" as subtype of type: \" + PackageableElement.getUserPathForPackageableElement(propertyType));\n" +
                        "                }\n" +
                        "\n" +
                        "                String unitClassName = UnitProcessor.convertToJavaCompatibleClassName(JavaPackageAndImportBuilder.buildImplUnitInstanceClassNameFromType(unitRetrieved));\n" +
                        "\n" +
                        "                java.lang.Class c = ((CompiledExecutionSupport) es).getClassLoader().loadClass(\"org.finos.legend.pure.generated.\" + unitClassName);\n" +
                        "\n" +
                        "                java.lang.Class paramClasses[] = new java.lang.Class[]{String.class, ExecutionSupport.class};\n" +
                        "                Method method = c.getMethod(\"_val\", Number.class);\n" +
                        "                Object classInstance = c.getConstructor(paramClasses).newInstance(\"Anonymous_NoCounter\", es);\n" +
                        "                method.invoke(classInstance, unitValue);\n" +
                        "                return (T) classInstance;\n" +
                        "            }\n" +
                        "        }));\n" +
                        "    }\n" +
                        "}\n", false));
    }

    @Override
    public List<Native> getExtraNatives()
    {
        return Lists.fixedSize.with(
                new EqualJsonStrings(),
                new Escape(),
                new FromJson(),
                new FromJsonDeprecated(),
                new ParseJSON(),
                new ToJsonBeta()
        );
    }

    @Override
    public String getRelatedRepository()
    {
        return "core_functions_json";
    }

    public static CompiledExtension extension()
    {
        return new JsonExtensionCompiled();
    }
}
