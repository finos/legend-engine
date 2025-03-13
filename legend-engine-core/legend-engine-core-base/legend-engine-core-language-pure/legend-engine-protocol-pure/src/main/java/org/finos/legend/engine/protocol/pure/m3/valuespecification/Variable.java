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

package org.finos.legend.engine.protocol.pure.m3.valuespecification;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.IOException;
import java.util.Objects;

import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.m3.multiplicity.Multiplicity;
import org.finos.legend.engine.protocol.pure.m3.type.generics.GenericType;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.PackageableType;

@JsonDeserialize(using = Variable.VariableDeserializer.class)
public class Variable extends ValueSpecification
{
    public String name;
    public GenericType genericType;
    public Multiplicity multiplicity;
    public Boolean supportsStream;

    public Variable()
    {
        // DO NOT DELETE: this resets the default constructor for Jackson
    }

    public Variable(String name, String _class, Multiplicity multiplicity)
    {
        this.name = name;
        this.genericType = new GenericType(new PackageableType(_class));
        this.multiplicity = multiplicity;
    }

    public Variable(String name, GenericType genericType, Multiplicity multiplicity)
    {
        this.name = name;
        this.genericType = genericType;
        this.multiplicity = multiplicity;
    }

    @Override
    public <T> T accept(ValueSpecificationVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

    public static class VariableDeserializer extends JsonDeserializer<ValueSpecification>
    {
        @Override
        public ValueSpecification deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException
        {
            ObjectCodec codec = jsonParser.getCodec();
            JsonNode node = codec.readTree(jsonParser);
            Variable variable = new Variable();
            variable.name = node.get("name").asText();

            // Backward compatibility - old protocol -------------------------------------------------------------------
            if (node.get("class") != null)
            {
                String _class = node.get("class").asText();
                variable.genericType = new GenericType(new PackageableType(_class));
            }
            // Backward compatibility - old protocol -------------------------------------------------------------------

            else if (node.get("genericType") != null)
            {
                variable.genericType = codec.treeToValue(node.get("genericType"), GenericType.class);
            }
            if (node.get("multiplicity") != null)
            {
                variable.multiplicity = codec.treeToValue(node.get("multiplicity"), Multiplicity.class);
            }
            if (node.get("sourceInformation") != null)
            {
                variable.sourceInformation = codec.treeToValue(node.get("sourceInformation"), SourceInformation.class);
            }
            if (node.get("supportsStream") != null)
            {
                variable.supportsStream = codec.treeToValue(node.get("supportsStream"), Boolean.class);
            }
            return variable;
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof Variable))
        {
            return false;
        }
        Variable variable = (Variable) o;
        return Objects.equals(name, variable.name) && Objects.equals(genericType, variable.genericType) && Objects.equals(multiplicity, variable.multiplicity) && Objects.equals(supportsStream, variable.supportsStream);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(name, genericType, multiplicity, supportsStream);
    }
}


