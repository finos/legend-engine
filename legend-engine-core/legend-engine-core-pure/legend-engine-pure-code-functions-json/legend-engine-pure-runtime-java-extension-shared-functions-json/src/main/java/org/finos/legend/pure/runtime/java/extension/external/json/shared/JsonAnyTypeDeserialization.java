// Copyright 2022 Goldman Sachs
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
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.Conversion;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.ConversionContext;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.PrimitiveConversion;

public class JsonAnyTypeDeserialization implements Conversion<Object, Object>
{
    static final JsonAnyTypeDeserialization JSON_ANY_TYPE_DESERIALIZATION = new JsonAnyTypeDeserialization();

    @Override
    public Object apply(Object value, ConversionContext context)
    {
        try
        {
            String primitiveTypeName = PrimitiveConversion.toPurePrimitiveName(value.getClass());
            PrimitiveType primitiveType = (PrimitiveType) context.getProcessorSupport().repository_getTopLevel(primitiveTypeName);
            Conversion conversion = context.getConversionCache().getConversion(primitiveType, context);
            return conversion.apply(value, context);
        }
        catch (IllegalArgumentException e)
        {
            throw new PureExecutionException("Deserialization of Any currently only supported on primitive values!", e);
        }
    }

    @Override
    public String pureTypeAsString()
    {
        return "Any";
    }
}
