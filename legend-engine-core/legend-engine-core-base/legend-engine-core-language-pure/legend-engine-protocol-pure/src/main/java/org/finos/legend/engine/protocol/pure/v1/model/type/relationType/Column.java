// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.protocol.pure.v1.model.type.relationType;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.type.GenericType;
import org.finos.legend.engine.protocol.pure.v1.model.type.PackageableType;

import java.io.IOException;
import java.util.Objects;

import static org.finos.legend.engine.protocol.pure.v1.ProcessHelper.processOne;

@JsonDeserialize(using = Column.ColumnDeserializer.class)
public class Column
{
    public SourceInformation sourceInformation;
    public String name;
    public GenericType genericType;
    public Multiplicity multiplicity;

    public Column()
    {
    }

    public Column(String name, GenericType genericType, Multiplicity multiplicity)
    {
        this.name = name;
        this.genericType = genericType;
        this.multiplicity = multiplicity;
    }

    public static class ColumnDeserializer extends JsonDeserializer<Column>
    {
        @Override
        public Column deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException
        {
            Column result = new Column();

            ObjectCodec codec = jsonParser.getCodec();
            JsonNode node = codec.readTree(jsonParser);

            result.name = processOne(node, "name", String.class, codec);
            result.sourceInformation = processOne(node, "sourceInformation", SourceInformation.class, codec);
            result.genericType = processOne(node, "genericType", GenericType.class, codec);
            result.multiplicity = processOne(node, "multiplicity", Multiplicity.class, codec);
            // Backward compatibility --------------
            if (node.get("type") != null)
            {
                String fullPath = node.get("type").asText();
                result.genericType = new GenericType(new PackageableType(fullPath));
            }
            // Backward compatibility --------------
            return result;
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof Column))
        {
            return false;
        }
        Column column = (Column) o;
        return Objects.equals(name, column.name) && Objects.equals(genericType, column.genericType) && Objects.equals(multiplicity, column.multiplicity);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(name, genericType, multiplicity);
    }
}
