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

import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.PrimitiveType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.Conversion;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.ConversionContext;

public class JsonGenericAndAnyTypeSerialization<T> implements Conversion<T, Object>
{
    static final JsonGenericAndAnyTypeSerialization JSON_GENERIC_AND_ANY_TYPE_SERIALIZATION = new JsonGenericAndAnyTypeSerialization();

    @Override
    public Object apply(T pureObject, ConversionContext context)
    {
        JsonSerializationContext jsonSerializationContext = (JsonSerializationContext) context;
        if (pureObject instanceof CoreInstance)
        {
            Type type = (Type) jsonSerializationContext.getClassifier(pureObject);
            Conversion<T, Object> concreteConversion = (Conversion<T, Object>) jsonSerializationContext.getConversionCache().getConversion(type, jsonSerializationContext);
            if (type instanceof PrimitiveType)
            {
                return concreteConversion.apply((T) jsonSerializationContext.extractPrimitiveValue(pureObject), jsonSerializationContext);
            }
            return concreteConversion.apply(pureObject, jsonSerializationContext);
        }
        //JSON Simple library can handle stringifying and correct escaping of all other primitive (Java) types except PureDate as it's our custom one
        // so we need to explicitly convert it to String ourselves before handing over to JSON library
        if (pureObject instanceof PureDate)
        {
            return pureObject.toString();
        }
        return pureObject;
    }

    @Override
    public String pureTypeAsString()
    {
        return "T";
    }
}
