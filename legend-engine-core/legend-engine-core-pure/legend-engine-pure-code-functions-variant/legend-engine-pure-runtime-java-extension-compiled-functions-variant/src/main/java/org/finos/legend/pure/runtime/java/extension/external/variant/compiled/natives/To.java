// Copyright 2025 Goldman Sachs
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


package org.finos.legend.pure.runtime.java.extension.external.variant.compiled.natives;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Iterator;
import java.util.Map;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.generated.JsonGen;
import org.finos.legend.pure.generated.Root_meta_json_JSONDeserializationConfig;
import org.finos.legend.pure.generated.Root_meta_json_JSONDeserializationConfig_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_functions_collection_Pair_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.variant.Variant;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateTime;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.m4.coreinstance.primitive.date.StrictDate;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.GenericTypeSerializationInCode;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.SourceInfoProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.FullJavaPaths;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;
import org.finos.legend.pure.runtime.java.extension.external.variant.VariantInstanceImpl;

public class To extends AbstractNative
{
    public To()
    {
        super("to_Variant_$0_1$__T_$0_1$__T_$0_1$_", "to_Variant_$0_1$__T_$0_1$__String_1__Pair_MANY__T_$0_1$_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        return getString(topLevelElement, functionExpression, transformedParams, processorContext);
    }

    @Override
    public String buildBody()
    {
        return buildBody(true);
    }

    protected static String buildBody(boolean isZeroOne)
    {
        return "new SharedPureFunction<Object>()\n" +
                "{\n" +
                "        @Override\n" +
                "        public Object execute(ListIterable<?> vars, final ExecutionSupport es)\n" +
                "        {\n" +
                "           org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.variant.Variant instances = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.variant.Variant) vars.get(0);\n" +
                "           " + FullJavaPaths.GenericType + " type = (" + FullJavaPaths.GenericType + ") vars.get(1);\n" +
                "           String typeName = vars.size() > 2 ? (String) vars.get(2) : null;\n" +
                "           ListIterable typeLookup = vars.size() > 2 ? (ListIterable) vars.get(3) : null;\n" +
                "           return " + To.class.getCanonicalName() + ".to(instances, typeName, typeLookup, " + isZeroOne + ", type, null, es);\n" +
                "        }\n" +
                "}";
    }

    protected static String getString(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        ProcessorSupport processorSupport = processorContext.getSupport();
        CoreInstance targetTypeParameter = Instance.getValueForMetaPropertyToManyResolved(functionExpression, M3Properties.parametersValues, processorSupport).get(1);

        CoreInstance targetType = Instance.getValueForMetaPropertyToOneResolved(targetTypeParameter, M3Properties.genericType, processorSupport);
        if (!GenericType.isGenericTypeFullyConcrete(targetType, processorSupport))
        {
            throw new PureCompilationException(functionExpression.getSourceInformation(), "Type parameter needs to be concreate: " + targetType);
        }

        String sourceInformation = SourceInfoProcessor.sourceInfoToString(functionExpression.getSourceInformation());
        CoreInstance returnMultiplicity = Instance.getValueForMetaPropertyToOneResolved(functionExpression, M3Properties.multiplicity, processorSupport);
        boolean toZeroOrOne = Multiplicity.isToZeroOrOne(returnMultiplicity);
        String pureJavaType = toZeroOrOne ? TypeProcessor.typeToJavaObjectSingle(targetType, true, processorSupport) : TypeProcessor.typeToJavaObjectWithMul(targetType, returnMultiplicity, processorSupport);

        String pureGenericType;

        if (targetType instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType)
        {
            pureGenericType = GenericTypeSerializationInCode.generateGenericTypeBuilder((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType) targetType, processorContext);
        }
        else
        {
            // todo what to do here?
            pureGenericType = "null";
        }

        String typeName = "null";
        String typeLookup = "null";

        if (transformedParams.size() > 2)
        {
            typeName = transformedParams.get(2);
            typeLookup = transformedParams.get(3);
        }

        return "(" + pureJavaType + ")" + To.class.getCanonicalName() + ".to(" + transformedParams.get(0) + "," + typeName + "," + typeLookup + "," + toZeroOrOne + ',' + pureGenericType + ',' + sourceInformation + ",es)";
    }

    public static Object to(Variant variant, String iTypeKeyName, ListIterable itypeLookup, boolean isZeroOrOne, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType pureGenericType, SourceInformation sourceInformation, ExecutionSupport es)
    {
        JsonNode jsonNode = null;

        String typeKeyName = iTypeKeyName == null ? "_type" : iTypeKeyName;

        if (itypeLookup == null)
        {
            itypeLookup = Lists.fixedSize.empty();
        }

        ListIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<String,String>> typeLookup = itypeLookup.collect(
                p -> new Root_meta_pure_functions_collection_Pair_Impl("")
                        ._first((String)((Pair) p)._first())
                        ._second(PackageableElement.getUserPathForPackageableElement((Class)((Pair) p)._second()))
        );

        if (variant != null)
        {
            jsonNode = ((VariantInstanceImpl) variant).getJsonNode();
        }

        if (isZeroOrOne)
        {
            return to(jsonNode, typeKeyName, typeLookup, pureGenericType, sourceInformation, (CompiledExecutionSupport) es);
        }
        else
        {
            if (jsonNode == null)
            {
                return Lists.fixedSize.empty();
            }
            else
            {
                if (jsonNode.isArray())
                {
                    return Iterate.collect(jsonNode, x -> to(x, typeKeyName, typeLookup, pureGenericType, sourceInformation, (CompiledExecutionSupport) es));
                }
                else
                {
                    throw new PureExecutionException(sourceInformation, "Expect variant that contains an 'ARRAY', but got '" + jsonNode.getNodeType() + "'");
                }
            }
        }
    }

    private static Object to(JsonNode jsonNode, String typeKeyName, ListIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<String,String>> typeLookup, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType pureGenericType, SourceInformation sourceInformation, CompiledExecutionSupport es)
    {
        boolean supportedTypeButWrongJson = false;

        try
        {
            if (jsonNode == null || jsonNode.isNull())
            {
                return null;
            }
            else if (pureGenericType._rawType() == es.getProcessorSupport().package_getByUserPath(M3Paths.Map))
            {
                MutableList<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType> arguments = pureGenericType._typeArguments().toList();

                org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType keyType = arguments.get(0);

                if (keyType._rawType() == es.getProcessorSupport().package_getByUserPath(M3Paths.String))
                {
                    if (jsonNode.isObject())
                    {
                        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType valueType = arguments.get(1);

                        MutableMap<Object, Object> map = Maps.mutable.empty();

                        for (Iterator<Map.Entry<String, JsonNode>> it = jsonNode.fields(); it.hasNext(); )
                        {
                            Map.Entry<String, JsonNode> jsonNodeEntry = it.next();
                            map.put(jsonNodeEntry.getKey(), to(jsonNodeEntry.getValue(), typeKeyName, typeLookup, valueType, sourceInformation, es));
                        }

                        return new PureMap(map);
                    }
                    supportedTypeButWrongJson = true;
                }
            }
            else if (pureGenericType._rawType() == es.getProcessorSupport().package_getByUserPath(M3Paths.List))
            {
                if (jsonNode.isArray())
                {
                    org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType elementType = pureGenericType._typeArguments().getOnly();
                    RichIterable<Object> values = Iterate.collect(jsonNode, x -> to(x, typeKeyName, typeLookup, elementType, sourceInformation, es), Lists.mutable.empty());
                    List<Object> list = (List<Object>) es.getProcessorSupport().newEphemeralAnonymousCoreInstance(M3Paths.List);
                    list._values(values);
                    return list;
                }
                supportedTypeButWrongJson = true;
            }
            else if (pureGenericType._rawType() == es.getProcessorSupport().package_getByUserPath(M3Paths.Variant))
            {
                return VariantInstanceImpl.newVariant(jsonNode, es.getProcessorSupport());
            }
            else if (pureGenericType._rawType() == es.getProcessorSupport().package_getByUserPath(M3Paths.StrictDate))
            {
                PureDate pureDate = DateFunctions.parsePureDate(jsonNode.asText());
                if (pureDate instanceof StrictDate)
                {
                    return pureDate;
                }

                throw new PureExecutionException(sourceInformation, "StrictDate must be a calendar day, got: " + jsonNode.asText());
            }
            else if (pureGenericType._rawType() == es.getProcessorSupport().package_getByUserPath(M3Paths.DateTime))
            {
                PureDate pureDate = DateFunctions.parsePureDate(jsonNode.asText());
                if (pureDate instanceof DateTime)
                {
                    return pureDate;
                }

                throw new PureExecutionException(sourceInformation, "DateTime must include time information, got: " + jsonNode.asText());
            }
            else if (pureGenericType._rawType() == es.getProcessorSupport().package_getByUserPath(M3Paths.Integer))
            {
                if (jsonNode.isIntegralNumber())
                {
                    return jsonNode.longValue();
                }
                else if (jsonNode.isTextual())
                {
                    return Long.parseLong(jsonNode.asText());
                }
                supportedTypeButWrongJson = true;
            }
            else if (pureGenericType._rawType() == es.getProcessorSupport().package_getByUserPath(M3Paths.Float))
            {
                if (jsonNode.isNumber())
                {
                    return jsonNode.doubleValue();
                }
                else if (jsonNode.isTextual())
                {
                    return Double.parseDouble(jsonNode.asText());
                }
                supportedTypeButWrongJson = true;
            }
            else if (pureGenericType._rawType() == es.getProcessorSupport().package_getByUserPath(M3Paths.Boolean))
            {
                if (jsonNode.isBoolean())
                {
                    return jsonNode.booleanValue();
                }
                else if (jsonNode.isTextual())
                {
                    return ModelRepository.getBooleanValue(jsonNode.asText());
                }
                supportedTypeButWrongJson = true;
            }
            else if (pureGenericType._rawType() == es.getProcessorSupport().package_getByUserPath(M3Paths.String))
            {
                return jsonNode.asText();
            }
            else if (pureGenericType._rawType() instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class)
            {
                if (jsonNode.isObject())
                {
                    Root_meta_json_JSONDeserializationConfig config = new Root_meta_json_JSONDeserializationConfig_Impl("")
                            ._typeKeyName(typeKeyName)
                            ._typeLookup(typeLookup)
                            ._failOnUnknownProperties(false);

                    return JsonGen.fromJson(jsonNode.toString(), (Class<Object>) pureGenericType._rawType(), config, sourceInformation, es);
                }

                supportedTypeButWrongJson = true;
            }
        }
        catch (IllegalArgumentException e)
        {
            throw new PureExecutionException(sourceInformation, e.getMessage(), e);
        }

        if (supportedTypeButWrongJson)
        {
            throw new PureExecutionException(sourceInformation, "Variant of type '" + jsonNode.getNodeType() + "' cannot be converted to " + GenericType.print(pureGenericType, es.getProcessorSupport()));
        }
        throw new PureExecutionException(sourceInformation, GenericType.print(pureGenericType, es.getProcessorSupport()) + " is not managed yet!");
    }
}
