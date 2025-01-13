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

package org.finos.legend.engine.protocol.pure.m3.function;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.m3.constraint.Constraint;
import org.finos.legend.engine.protocol.pure.m3.extension.StereotypePtr;
import org.finos.legend.engine.protocol.pure.m3.extension.TaggedValue;
import org.finos.legend.engine.protocol.pure.m3.multiplicity.Multiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElementVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.function.FunctionTestSuite;
import org.finos.legend.engine.protocol.pure.m3.type.generics.GenericType;
import org.finos.legend.engine.protocol.pure.v1.model.type.PackageableType;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.Variable;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.finos.legend.engine.protocol.pure.v1.ProcessHelper.processMany;
import static org.finos.legend.engine.protocol.pure.v1.ProcessHelper.processOne;

@JsonDeserialize(using = Function.FunctionDeserializer.class)
public class Function extends PackageableElement
{
    public List<Constraint> preConstraints = Collections.emptyList();
    public List<Constraint> postConstraints = Collections.emptyList();
    public List<Variable> parameters = Collections.emptyList();
    public GenericType returnGenericType;
    public Multiplicity returnMultiplicity;
    public List<StereotypePtr> stereotypes = Collections.emptyList();
    public List<TaggedValue> taggedValues = Collections.emptyList();
    public List<ValueSpecification> body = Collections.emptyList();
    public List<FunctionTestSuite> tests = Collections.emptyList();

    @Override
    public <T> T accept(PackageableElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

    public static class FunctionDeserializer extends JsonDeserializer<Function>
    {
        @Override
        public Function deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException
        {
            Function result = new Function();

            ObjectCodec codec = jsonParser.getCodec();
            JsonNode node = codec.readTree(jsonParser);

            result.name = processOne(node, "name", String.class, codec);
            result._package = processOne(node, "package", String.class, codec);
            result.sourceInformation = processOne(node, "sourceInformation", SourceInformation.class, codec);

            result.preConstraints = processMany(node, "preConstraints", Constraint.class, codec);
            result.postConstraints = processMany(node, "postConstraints", Constraint.class, codec);
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
            result.tests = processMany(node, "tests", FunctionTestSuite.class, codec);

            return result;
        }
    }
}
