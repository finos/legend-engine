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

package org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.function.Function;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.DataTypeValueSpecification;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.Collection;

public abstract class PrimitiveValueSpecification extends DataTypeValueSpecification
{
    public static ValueSpecification customParsePrimitive(JsonParser parser, Function<JsonNode, ValueSpecification> func) throws IOException
    {
        return customParsePrimitive(parser.getCodec(), parser.readValueAsTree(), func);
    }

    public static ValueSpecification customParsePrimitive(ObjectCodec oc, JsonNode node, Function<JsonNode, ValueSpecification> func) throws JsonProcessingException
    {
        JsonNode values = node.get("values");
        ValueSpecification result;
        if (values != null)
        {
            if (values.size() == 0)
            {
                return new Collection();
            }
            else if (values.size() == 1)
            {
                result = func.apply(values.get(0));
            }
            else
            {
                result = new Collection(Lists.mutable.withAll(Iterate.collect(values, func::apply)));
            }
        }
        else
        {
            result = func.apply(node.get("value"));
        }
        JsonNode sourceInformation = node.get("sourceInformation");
        if (sourceInformation != null)
        {
            result.sourceInformation = oc.treeToValue(sourceInformation, SourceInformation.class);
        }
        return result;
    }
}
