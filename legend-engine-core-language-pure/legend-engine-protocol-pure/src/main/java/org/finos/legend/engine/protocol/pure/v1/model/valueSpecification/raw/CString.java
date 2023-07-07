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

package org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecificationVisitor;

import java.io.IOException;

@JsonDeserialize(using = CString.CStringDeserializer.class)
public class CString extends PrimitiveValueSpecification
{
    public String value = "";

    public CString()
    {
    }

    public CString(String value)
    {
        this.value = value;
    }

    @Override
    public <T> T accept(ValueSpecificationVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

    public static class CStringDeserializer extends JsonDeserializer<ValueSpecification>
    {
        @Override
        public ValueSpecification deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException
        {
            JsonNode node = jsonParser.getCodec().readTree(jsonParser);
            // Fix Empty Set Bug
            JsonNode values = node.get("values");
            JsonNode multiplicities = node.get("multiplicity");
            if (values != null && values.isEmpty() && multiplicities != null && multiplicities.get("upperBound") != null && multiplicities.get("upperBound").asLong() == 1)
            {
                return new CString("");
            }
            return customParsePrimitive(node, x -> new CString(x.asText()));
        }
    }
}
