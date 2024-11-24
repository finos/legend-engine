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

package org.finos.legend.engine.protocol.pure.v1.model.valueSpecification;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.type.GenericType;
import org.finos.legend.engine.protocol.pure.v1.model.type.PackageableType;

import java.io.IOException;

@JsonDeserialize(using = Variable.VariableDeserializer.class)
public class Variable extends ValueSpecification
{
    private static ObjectMapper om = PureProtocolObjectMapperFactory.getNewObjectMapper();

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
            JsonNode node = jsonParser.getCodec().readTree(jsonParser);
            Variable variable = new Variable();
            variable.name = node.get("name").asText();

            // Backward compatibility - old protocol -------------------------------------------------------------------
            if (node.get("class") != null)
            {
                String _class = node.get("class").asText();
                GenericType genericType = new GenericType(new PackageableType(_class));
                if ("meta::pure::mapping::Result".equals(_class) || "Result".equals(_class))
                {
                    genericType.typeArguments = Lists.mutable.of(new GenericType(new PackageableType("meta::pure::metamodel::type::Any")));
                    genericType.multiplicityArguments = Lists.mutable.of(Multiplicity.PURE_MANY);
                }
                variable.genericType = genericType;
            }
            // Backward compatibility - old protocol -------------------------------------------------------------------

            else if (node.get("genericType") != null)
            {
                variable.genericType = om.treeToValue(node.get("genericType"), GenericType.class);

                // Backward compatibility - old grammar -------------------------------------------------------------------
                if (variable.genericType != null && variable.genericType.rawType instanceof PackageableType)
                {
                    String _class = ((PackageableType) variable.genericType.rawType).fullPath;
                    if (("meta::pure::mapping::Result".equals(_class) || "Result".equals(_class)) && variable.genericType.typeArguments.size() == 0)
                    {
                        variable.genericType.typeArguments = Lists.mutable.of(new GenericType(new PackageableType("meta::pure::metamodel::type::Any")));
                        variable.genericType.multiplicityArguments = Lists.mutable.of(Multiplicity.PURE_MANY);
                    }
                }
                // Backward compatibility - old grammar -------------------------------------------------------------------

            }
            if (node.get("multiplicity") != null)
            {
                variable.multiplicity = om.treeToValue(node.get("multiplicity"), Multiplicity.class);
            }
            if (node.get("sourceInformation") != null)
            {
                variable.sourceInformation = om.treeToValue(node.get("sourceInformation"), SourceInformation.class);
            }
            if (node.get("supportsStream") != null)
            {
                variable.supportsStream = om.treeToValue(node.get("supportsStream"), Boolean.class);
            }
            return variable;
        }
    }
}


