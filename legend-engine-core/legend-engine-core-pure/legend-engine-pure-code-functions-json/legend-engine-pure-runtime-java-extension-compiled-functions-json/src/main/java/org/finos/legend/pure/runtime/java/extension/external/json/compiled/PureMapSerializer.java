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

import org.finos.legend.pure.runtime.java.extension.external.json.shared.JsonSerializationContext;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.Conversion;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.TypeCoreInstanceWrapper;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class PureMapSerializer
{
    public static JSONObject toJson(PureMap pureMap, final JsonSerializationContext context)
    {
        final JSONObject result = new JSONObject();
        pureMap.getMap().forEachKeyValue(new Procedure2()
        {
            @Override
            public void value(Object key, Object value)
            {
                if (!(key instanceof String))
                {
                    throw new PureExecutionException("Only String-key map conversions are supported, found key: " + key);
                }
                result.put(key, toJson(value, context));
            }
        });
        return result;
    }

    private static Object toJson(Object value, JsonSerializationContext context)
    {
        if (value instanceof CoreInstance)
        {
            CoreInstance valueCoreInstance = (CoreInstance)value;
            CoreInstance valueClassifier = context.getProcessorSupport().getClassifier(valueCoreInstance);
            Conversion<Object, ?> valConcreteConversion = (Conversion<Object, ?>)context.getConversionCache().getConversion(TypeCoreInstanceWrapper.toType(valueClassifier), context);
            return valConcreteConversion.apply(value, context);
        }
        if (value instanceof PureMap)
        {
            return toJson((PureMap)value, context);
        }
        if (value instanceof String)
        {
            return JSONValue.escape((String)value);
        }
        // non-string primitive type (hopefully)
        return value;
    }
}
