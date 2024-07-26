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
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.PrimitiveType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.measure.Measure;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.Conversion;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

import java.util.Iterator;

public class JsonSerializer
{
    private JsonSerializer()
    {
    }

    public static String toJson(RichIterable<?> pureObjectCollection, ProcessorSupport processorSupport, JsonSerializationContext jsonSerializationContext, SourceInformation sourceInformation)
    {
        if (pureObjectCollection.isEmpty())
        {
            return new JSONArray().toString();
        }
        //Using iterator to avoid asserting collection size which would break DB stream if we try to serialize result streamed from DB to json
        Iterator<?> collectionIterator = pureObjectCollection.iterator();
        Object firstInstance = collectionIterator.next();
        Object serializedFirstInstance = toJson(firstInstance, processorSupport, jsonSerializationContext, sourceInformation);
        if (!collectionIterator.hasNext())
        {
            return JSONValue.toJSONString(serializedFirstInstance);
        }

        JSONArray jsonArray = new JSONArray();
        jsonArray.add(serializedFirstInstance);
        while (collectionIterator.hasNext())
        {
            jsonArray.add(toJson(collectionIterator.next(), processorSupport, jsonSerializationContext, sourceInformation));
        }
        return jsonArray.toString();
    }

    private static Object toJson(Object pureObject, ProcessorSupport processorSupport, JsonSerializationContext jsonSerializationContext, SourceInformation sourceInformation)
    {
        if (pureObject instanceof CoreInstance)
        {
            Type baseClassType = (pureObject instanceof InstanceValue && Measure.isUnitOrMeasureInstance((CoreInstance) pureObject, processorSupport)) ? (Type) Instance.getValueForMetaPropertyToOneResolved((CoreInstance) pureObject, M3Properties.genericType, M3Properties.rawType, processorSupport) : (Type) processorSupport.getClassifier((CoreInstance) pureObject);
            Conversion conversion = jsonSerializationContext.getConversionCache().getConversion(baseClassType, jsonSerializationContext);
            return conversion.apply((baseClassType instanceof PrimitiveType) ? jsonSerializationContext.extractPrimitiveValue(pureObject) : pureObject, jsonSerializationContext);
        }
        Object res = JsonParser.processor.process(pureObject, jsonSerializationContext);
        if (res != null)
        {
            return res;
        }
        return pureObject;
    }
}
