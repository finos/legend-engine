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

package org.finos.legend.engine.protocol.pure.m3.function.property;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.m3.extension.StereotypePtr;
import org.finos.legend.engine.protocol.pure.m3.extension.TaggedValue;
import org.finos.legend.engine.protocol.pure.m3.multiplicity.Multiplicity;
import org.finos.legend.engine.protocol.pure.m3.type.generics.GenericType;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.PackageableType;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.Variable;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.finos.legend.engine.protocol.pure.v1.ProcessHelper.processMany;
import static org.finos.legend.engine.protocol.pure.v1.ProcessHelper.processOne;

@JsonDeserialize(using = QualifiedProperty.QualifiedPropertyDeserializer.class)
public class QualifiedProperty
{
    public String name;
    public List<Variable> parameters = Collections.emptyList();
    public GenericType returnGenericType;
    public Multiplicity returnMultiplicity;
    public List<StereotypePtr> stereotypes = Collections.emptyList();
    public List<TaggedValue> taggedValues = Collections.emptyList();
    public List<ValueSpecification> body = Collections.emptyList();
    public SourceInformation sourceInformation;

    public static class QualifiedPropertyDeserializer extends JsonDeserializer<QualifiedProperty>
    {
        @Override
        public QualifiedProperty deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException
        {
            QualifiedProperty result = new QualifiedProperty();

            ObjectCodec codec = jsonParser.getCodec();
            JsonNode node = codec.readTree(jsonParser);

            result.name = node.get("name").asText();
            result.parameters = processMany(node, "parameters", Variable.class, codec);

            result.returnGenericType = processOne(node, "returnGenericType", GenericType.class, codec);
            // Backward compatibility --------------
            if (node.get("returnType") != null)
            {
                String fullPath = node.get("returnType").asText();
                result.returnGenericType = new GenericType(new PackageableType(fullPath));
            }
            // Backward compatibility --------------

            result.returnMultiplicity = processOne(node, "returnMultiplicity", Multiplicity.class, codec);
            result.stereotypes = processMany(node, "stereotypes", StereotypePtr.class, codec);
            result.taggedValues = processMany(node, "taggedValues", TaggedValue.class, codec);
            result.body = processMany(node, "body", ValueSpecification.class, codec);
            result.sourceInformation = processOne(node, "sourceInformation", SourceInformation.class, codec);

            return result;
        }


    }
}
