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

package org.finos.legend.pure.runtime.java.extension.external.json.shared;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.PrimitiveType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.TypeCoreInstanceWrapper;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.Conversion;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.ConversionContext;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.MapConversion;

import java.util.Map;

public class JsonMapSerialization<T extends CoreInstance> extends MapConversion<T, Map>
{
    public JsonMapSerialization()
    {
    }

    // Apply conversion and transform Pure Map to Java Map.
    @Override
    public Map<String, Object> apply(T pureObject, ConversionContext context)
    {
        Map<String, Object> resultMap = UnifiedMap.newMap();
        JsonSerializationContext jsonSerializationContext = (JsonSerializationContext)context;
        RichIterable<CoreInstance> keyValuePairs = jsonSerializationContext.getMapKeyValues(pureObject);
        for (CoreInstance keyValuePair : keyValuePairs)
        {
            resultMap.put(getKey(keyValuePair, jsonSerializationContext), getValue(keyValuePair, jsonSerializationContext));
        }
        return resultMap;
    }

    private static String getKey(CoreInstance keyValuePair, JsonSerializationContext context)
    {
        CoreInstance key = Instance.getValueForMetaPropertyToOneResolved(keyValuePair, M3Properties.first, context.getProcessorSupport());
        CoreInstance type = context.getClassifier(key);
        if (!((type instanceof PrimitiveType) && ModelRepository.STRING_TYPE_NAME.equals(type.getName())))
        {
            StringBuilder builder = new StringBuilder("Only String-key map conversions are supported, found key type: ");
            PackageableElement.writeUserPathForPackageableElement(builder, type);
            throw new PureExecutionException(builder.toString());
        }
        Conversion<Object, String> keyConcreteConversion = (Conversion<Object, String>)context.getConversionCache().getConversion(TypeCoreInstanceWrapper.toType(type), context);
        return keyConcreteConversion.apply(context.extractPrimitiveValue(key), context);
    }

    private static Object getValue(CoreInstance keyValuePair, JsonSerializationContext context)
    {
        CoreInstance value = Instance.getValueForMetaPropertyToOneResolved(keyValuePair, M3Properties.second, context.getProcessorSupport());
        CoreInstance type = context.getClassifier(value);
        Conversion<Object, Object> valConcreteConversion = (Conversion<Object, Object>)context.getConversionCache().getConversion(TypeCoreInstanceWrapper.toType(type), context);
        return (type instanceof PrimitiveType) ? valConcreteConversion.apply(context.extractPrimitiveValue(value), context) : valConcreteConversion.apply(value, context);
    }
}
