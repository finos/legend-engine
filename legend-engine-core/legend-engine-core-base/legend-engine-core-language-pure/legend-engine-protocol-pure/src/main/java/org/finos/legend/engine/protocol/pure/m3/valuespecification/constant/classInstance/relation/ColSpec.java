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

package org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.classInstance.relation;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.finos.legend.engine.protocol.pure.m3.type.generics.GenericType;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.PackageableType;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction;

import java.io.IOException;

import static org.finos.legend.engine.protocol.pure.v1.ProcessHelper.processOne;

@JsonDeserialize(using = ColSpec.ColSpecDeserializer.class)
public class ColSpec
{
    public SourceInformation sourceInformation;
    public String name;
    public GenericType genericType;
    public LambdaFunction function1;
    public LambdaFunction function2;

    public static class ColSpecDeserializer extends JsonDeserializer<ColSpec>
    {
        @Override
        public ColSpec deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException
        {
            ObjectCodec codec = jsonParser.getCodec();
            JsonNode node = codec.readTree(jsonParser);
            ColSpec colSpec = new ColSpec();
            colSpec.name = node.get("name").asText();

            // Backward compatibility - old protocol -------------------------------------------------------------------
            if (node.get("type") != null)
            {
                String _class = node.get("type").asText();
                colSpec.genericType = new GenericType(new PackageableType(_class));
            }
            else
            {
                colSpec.genericType = processOne(node, "genericType", GenericType.class, codec);
            }
            // Backward compatibility - old protocol -------------------------------------------------------------------

            colSpec.function1 = processOne(node, "function1", LambdaFunction.class, codec);
            colSpec.function2 = processOne(node, "function2", LambdaFunction.class, codec);
            colSpec.sourceInformation = processOne(node, "sourceInformation", SourceInformation.class, codec);
            return colSpec;
        }
    }
}
