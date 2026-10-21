//  Copyright 2026 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.external.format.flatdata.grammar.driver;

import org.finos.legend.engine.external.format.flatdata.metamodel.FlatData;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Reproduces a bug in the delimited line reader: when the delimiter is itself a
 * whitespace character (e.g. TAB) and a field's value starts with whitespace
 * (e.g. a single space), the parser's {@code whitespaceAtStartOfValue} state
 * tests {@code isWhitespace()} before it tests the delimiter. Because
 * {@code Character.isWhitespace('\t')} is {@code true}, the TAB delimiter that
 * should terminate the field is instead absorbed into the value, swallowing the
 * delimiter and merging the following fields into this one.
 *
 * Real-world case: a vendor sends a TAB-delimited record where one field is a
 * single space " " followed by two more (empty) fields:
 *   ...FRA \t " " \t "" \t SID...
 * which is parsed as a single value " \t\tSID" (reported as
 * { "address": 7, "rawValue": " \t\tSID" }), collapsing three fields into one.
 */
public class TestDelimitedWhitespaceDelimiter extends AbstractDriverTest
{
    @Test
    public void singleSpaceFieldDoesNotSwallowTabDelimiters()
    {
        FlatData flatData = parseFlatData("section default: DelimitedWithoutHeadings\n" +
                "{\n" +
                "  scope.untilEof;\n" +
                "  delimiter       : '\\t';\n" +
                "\n" +
                "  Record\n" +
                "  {\n" +
                "    COL1 {1} : INTEGER;\n" +
                "    COL2 {2} : STRING;\n" +
                "    COL3 {3} : STRING(optional);\n" +
                "    COL4 {4} : STRING(optional);\n" +
                "    COL5 {5} : STRING(optional);\n" +
                "    COL6 {6} : STRING(optional);\n" +
                "  }\n" +
                "}\n");

        // Field 3 is a single space; field 4 is empty; fields 5 and 6 are "SID".
        String data = data("\n", "16\tWLCX\t \t\tSID\tSID");

        List<IChecked<Row>> records = deserialize(Row.class, flatData, data);
        records.forEach(this::assertNoDefects);

        Row row = records.get(0).getValue();
        Assert.assertEquals(16L, row.COL1);
        Assert.assertEquals("WLCX", row.COL2);
        // The bug (now fixed): COL3 used to become " \t\tSID" because the TAB
        // delimiters after the leading space were swallowed as whitespace instead
        // of terminating the field, which shifted every following field left.
        Assert.assertEquals("A single-space field must not swallow the following TAB delimiters", " ", row.COL3);
        Assert.assertEquals("Field 4 (empty) must remain its own field", "", row.COL4);
        Assert.assertEquals("Field 5 must still be parsed as its own field", "SID", row.COL5);
        Assert.assertEquals("Field 6 must still be parsed as its own field", "SID", row.COL6);
    }

    public static class Row
    {
        public long COL1;
        public String COL2;
        public String COL3;
        public String COL4;
        public String COL5;
        public String COL6;
    }
}
