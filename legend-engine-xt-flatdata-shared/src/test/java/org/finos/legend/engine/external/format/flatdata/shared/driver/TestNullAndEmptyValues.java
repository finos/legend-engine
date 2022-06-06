// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.external.format.flatdata.shared.driver;

import org.finos.legend.engine.external.format.flatdata.shared.model.FlatData;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class TestNullAndEmptyValues extends AbstractDriverTest
{
    @Test
    public void emptyFieldsWithNoNullString()
    {
        runTest("", "B", "C", ",B,C", "");
        runTest("A", "", "C", "A,,C", "");
        runTest("A", "B", "", "A,B,", "");
    }

    @Test
    public void emptyFieldsWithEmptyNullString()
    {
        runTest(null, "B", "C", ",B,C", "nullString: '';");
        runTest("A", null, "C", "A,,C", "nullString: '';");
        runTest("A", "B", null, "A,B,", "nullString: '';");
    }

    @Test
    public void missingFieldsAreEmptyString()
    {
        runTest("A", "B", null, "A,B", "");
        runTest("A", null, null, "A", "");
        runTest(null, null, null, "", "");
    }

    @Test
    public void canUseMultipleNullStrings()
    {
        runTest(null, null, "C", ",null,C", "nullString: ['', 'null'];");
    }

    private void runTest(String expectedField1, String expectedField2, String expectedField3, String rawLine, String nullStringGrammar)
    {
        FlatData flatData = parseFlatData("section default: DelimitedWithHeadings\n" +
                "{\n" +
                "  scope.untilEof;\n" +
                "  delimiter       : ',';\n" +
                nullStringGrammar + "\n" +
                "\n" +
                "  Record\n" +
                "  {\n" +
                "    FIELD1 : STRING(optional);\n" +
                "    FIELD2 : STRING(optional);\n" +
                "    FIELD3 : STRING(optional);\n" +
                "  }\n" +
                "}\n");

        List<IChecked<Data>> records = deserialize(Data.class, flatData, data("\n", "FIELD1,FIELD2,FIELD3", rawLine));
        if (expectedField1 == null && expectedField2 == null && expectedField3 == null)
        {
            Assert.assertEquals(0, records.size());
        }
        else
        {
            Assert.assertEquals(1, records.size());
            IChecked<Data> record = records.get(0);

            assertNoDefects(record);
            Data data = record.getValue();
            Assert.assertEquals(expectedField1, data.field1);
            Assert.assertEquals(expectedField2, data.field2);
            Assert.assertEquals(expectedField3, data.field3);
        }
    }

    public static class Data
    {
        public String field1;
        public String field2;
        public String field3;
    }
}
