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

package org.finos.legend.engine.plan.execution.parameterization;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.stream.IntStream;
import org.finos.legend.engine.plan.execution.planHelper.PrimitiveValueSpecificationToObjectVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.ParameterValue;
import org.finos.legend.engine.protocol.pure.v1.model.type.PackageableType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.Variable;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.CFloat;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.CInteger;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.CStrictDate;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.CString;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TestParameterizedValueSpecification
{

    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    @Test
    public void testLambdaIsParameterized() throws IOException
    {

        ValueSpecification input = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResource("parameterization/lambdaWithFilter.json")), ValueSpecification.class);

        ParameterizedValueSpecification spec = new ParameterizedValueSpecification(input, "GENERATED");

        List<ParameterValue> expectedParameters = new ArrayList<>();

        expectedParameters.add(createParameterValue("GENERATEDL0L0L0L1L0L0L1", new CFloat(10.0)));
        expectedParameters.add(createParameterValue("GENERATEDL0L0L0L1L0L1L0L1", new CString("abc")));
        expectedParameters.add(createParameterValue("GENERATEDL0L0L0L1L0L1L1L1L1", new CStrictDate("2023-06-02")));
        expectedParameters.add(createParameterValue("GENERATEDL0L1", new CInteger(1000L)));


        List<Variable> expectedVariables = new ArrayList<>();
        expectedVariables.add(new Variable("GENERATEDL0L0L0L1L0L0L1", "Float", Multiplicity.PURE_ONE));
        expectedVariables.add(new Variable("GENERATEDL0L0L0L1L0L1L0L1", "String", Multiplicity.PURE_ONE));
        expectedVariables.add(new Variable("GENERATEDL0L0L0L1L0L1L1L1L1", "StrictDate", Multiplicity.PURE_ONE));
        expectedVariables.add(new Variable("GENERATEDL0L1", "Integer", Multiplicity.PURE_ONE));

        String actualSpec = objectMapper.writeValueAsString(spec.getValueSpecification());
        Assert.assertEquals("{\"_type\":\"lambda\",\"body\":[{\"_type\":\"func\",\"function\":\"take\",\"parameters\":[{\"_type\":\"func\",\"function\":\"project\",\"parameters\":[{\"_type\":\"func\",\"function\":\"filter\",\"parameters\":[{\"_type\":\"func\",\"function\":\"getAll\",\"parameters\":[{\"_type\":\"packageableElementPtr\",\"fullPath\":\"domain::Example\"}]},{\"_type\":\"lambda\",\"body\":[{\"_type\":\"func\",\"function\":\"and\",\"parameters\":[{\"_type\":\"func\",\"function\":\"equal\",\"parameters\":[{\"_type\":\"property\",\"parameters\":[{\"_type\":\"var\",\"name\":\"x\"}],\"property\":\"cases\"},{\"_type\":\"var\",\"genericType\":{\"multiplicityArguments\":[],\"rawType\":{\"_type\":\"packageableType\",\"fullPath\":\"Float\"},\"typeArguments\":[],\"typeVariableValues\":[]},\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"GENERATEDL0L0L0L1L0L0L1\"}]},{\"_type\":\"func\",\"function\":\"and\",\"parameters\":[{\"_type\":\"func\",\"function\":\"equal\",\"parameters\":[{\"_type\":\"property\",\"parameters\":[{\"_type\":\"var\",\"name\":\"x\"}],\"property\":\"caseType\"},{\"_type\":\"var\",\"genericType\":{\"multiplicityArguments\":[],\"rawType\":{\"_type\":\"packageableType\",\"fullPath\":\"String\"},\"typeArguments\":[],\"typeVariableValues\":[]},\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"GENERATEDL0L0L0L1L0L1L0L1\"}]},{\"_type\":\"func\",\"function\":\"and\",\"parameters\":[{\"_type\":\"func\",\"function\":\"equal\",\"parameters\":[{\"_type\":\"property\",\"parameters\":[{\"_type\":\"var\",\"name\":\"x\"}],\"property\":\"caseType\"},{\"_type\":\"packageableElementPtr\",\"fullPath\":\"test::pure::mapping::modelToModel::test::shared::dest::Person\"}]},{\"_type\":\"func\",\"function\":\"equal\",\"parameters\":[{\"_type\":\"property\",\"parameters\":[{\"_type\":\"var\",\"name\":\"x\"}],\"property\":\"date\"},{\"_type\":\"var\",\"genericType\":{\"multiplicityArguments\":[],\"rawType\":{\"_type\":\"packageableType\",\"fullPath\":\"StrictDate\"},\"typeArguments\":[],\"typeVariableValues\":[]},\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"GENERATEDL0L0L0L1L0L1L1L1L1\"}]}]}]}]}],\"parameters\":[{\"_type\":\"var\",\"name\":\"x\"}]}]},{\"_type\":\"collection\",\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"values\":[{\"_type\":\"lambda\",\"body\":[{\"_type\":\"property\",\"parameters\":[{\"_type\":\"var\",\"name\":\"x\"}],\"property\":\"cases\"}],\"parameters\":[{\"_type\":\"var\",\"name\":\"x\"}]}]},{\"_type\":\"collection\",\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"values\":[{\"_type\":\"string\",\"value\":\"Cases\"}]}]},{\"_type\":\"var\",\"genericType\":{\"multiplicityArguments\":[],\"rawType\":{\"_type\":\"packageableType\",\"fullPath\":\"Integer\"},\"typeArguments\":[],\"typeVariableValues\":[]},\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"GENERATEDL0L1\"}]}],\"parameters\":[]}", actualSpec);

        assert (IntStream.range(0, spec.getParameterValues().size()).allMatch(index -> parameterValueCompare(spec.getParameterValues().get(index), expectedParameters.get(index))));
        assert (IntStream.range(0, spec.getVariables().size()).allMatch(index -> variableCompare(spec.getVariables().get(index), expectedVariables.get(index))));

    }

    @Test
    public void testStringListIsNotParameterized() throws IOException
    {

        ValueSpecification input = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResource("parameterization/lambdaWithInFilter.json")), ValueSpecification.class);

        ParameterizedValueSpecification spec = new ParameterizedValueSpecification(input, "GENERATED");

        List<ParameterValue> expectedParameters = new ArrayList<>();

        expectedParameters.add(createParameterValue("GENERATEDL0L1", new CInteger(1000L)));


        List<Variable> expectedVariables = new ArrayList<>();
        expectedVariables.add(new Variable("GENERATEDL0L1", "Integer", Multiplicity.PURE_ONE));

        String actualSpec = objectMapper.writeValueAsString(spec.getValueSpecification());
        Assert.assertEquals("{\"_type\":\"lambda\",\"body\":[{\"_type\":\"func\",\"function\":\"take\",\"parameters\":[{\"_type\":\"func\",\"function\":\"project\",\"parameters\":[{\"_type\":\"func\",\"function\":\"filter\",\"parameters\":[{\"_type\":\"func\",\"function\":\"getAll\",\"parameters\":[{\"_type\":\"packageableElementPtr\",\"fullPath\":\"domain::Example\"}]},{\"_type\":\"lambda\",\"body\":[{\"_type\":\"func\",\"function\":\"in\",\"parameters\":[{\"_type\":\"property\",\"parameters\":[{\"_type\":\"var\",\"name\":\"x\"}],\"property\":\"caseType\"},{\"_type\":\"collection\",\"multiplicity\":{\"lowerBound\":3,\"upperBound\":3},\"values\":[{\"_type\":\"string\",\"value\":\"Case 3\"},{\"_type\":\"string\",\"value\":\"Case 2\"},{\"_type\":\"string\",\"value\":\"Case 1\"}]}]}],\"parameters\":[{\"_type\":\"var\",\"name\":\"x\"}]}]},{\"_type\":\"collection\",\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"values\":[{\"_type\":\"lambda\",\"body\":[{\"_type\":\"property\",\"parameters\":[{\"_type\":\"var\",\"name\":\"x\"}],\"property\":\"cases\"}],\"parameters\":[{\"_type\":\"var\",\"name\":\"x\"}]}]},{\"_type\":\"collection\",\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"values\":[{\"_type\":\"string\",\"value\":\"Cases\"}]}]},{\"_type\":\"var\",\"genericType\":{\"multiplicityArguments\":[],\"rawType\":{\"_type\":\"packageableType\",\"fullPath\":\"Integer\"},\"typeArguments\":[],\"typeVariableValues\":[]},\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"GENERATEDL0L1\"}]}],\"parameters\":[]}", actualSpec);

        assert (IntStream.range(0, spec.getParameterValues().size()).allMatch(index -> parameterValueCompare(spec.getParameterValues().get(index), expectedParameters.get(index))));
        assert (IntStream.range(0, spec.getVariables().size()).allMatch(index -> variableCompare(spec.getVariables().get(index), expectedVariables.get(index))));

    }

    private Boolean parameterValueCompare(ParameterValue a, ParameterValue b)
    {
        PrimitiveValueSpecificationToObjectVisitor visitor = new PrimitiveValueSpecificationToObjectVisitor();
        return a.value.accept(visitor).equals(b.value.accept(visitor)) && a.name.equals(b.name);

    }

    private Boolean variableCompare(Variable a, Variable b)
    {
        return a.name.equals(b.name) && ((PackageableType) a.genericType.rawType).fullPath.equals(((PackageableType) a.genericType.rawType).fullPath) && a.multiplicity.equals(b.multiplicity);
    }

    private ParameterValue createParameterValue(String name, ValueSpecification value)
    {
        ParameterValue param = new ParameterValue();
        param.value = value;
        param.name = name;
        return param;
    }

}
