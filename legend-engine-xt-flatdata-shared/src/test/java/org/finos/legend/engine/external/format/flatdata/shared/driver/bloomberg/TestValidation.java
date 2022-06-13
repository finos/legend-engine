//  Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.external.format.flatdata.shared.driver.bloomberg;

import org.finos.legend.engine.external.format.flatdata.shared.AbstractValidationTest;
import org.junit.Test;

public class TestValidation extends AbstractValidationTest
{
    @Test
    public void valid()
    {
        test(BloombergFixtures.FLT_CPN_HIST_SECTION_GRAMMAR);

        test(BloombergFixtures.FLT_CPN_HIST_SECTION_GRAMMAR + "\n" + BloombergFixtures.metadataGrammar("md"));

        test("section actions: BloombergActions\n" +
                "{\n" +
                "  Record\n" +
                "  {\n" +
                "    SECURITY                  : STRING;\n" +
                "    ID_BB_COMPANY             : INTEGER;\n" +
                "  }\n" +
                "}\n" +
                "section deletes: BloombergActionDetails\n" +
                "{\n" +
                "  actionFlags: 'D';\n" +
                "\n" +
                "  Record\n" +
                "  {\n" +
                "    CP_DELETE_REASON : STRING(optional);\n" +
                "  }\n" +
                "}\n" +
                "section dvdCash: BloombergActionDetails\n" +
                "{\n" +
                "  actionFlags: ['N', 'U'];\n" +
                "  mnemonics: 'DVD_CASH';\n" +
                "\n" +
                "  Record\n" +
                "  {\n" +
                "    CP_RECORD_DT              : DATE(optional);\n" +
                "    CP_PAY_DT                 : DATE(optional);\n" +
                "  }\n" +
                "}\n" +
                "section metadata: BloombergMetadata\n" +
                "{\n" +
                "  Record\n" +
                "  {\n" +
                "    TIMESTARTED  : DATETIME(format='EEE MMM d HH:mm:ss z yyyy');\n" +
                "    TIMEFINISHED : DATETIME(format='EEE MMM d HH:mm:ss z yyyy');\n" +
                "  }\n" +
                "}"
        );

        test("section actions: BloombergActions\n" +
                "{\n" +
                "  Record\n" +
                "  {\n" +
                "    SECURITY                  : STRING;\n" +
                "    ID_BB_COMPANY             : INTEGER;\n" +
                "  }\n" +
                "}\n" +
                "section metadata: BloombergMetadata\n" +
                "{\n" +
                "  Record\n" +
                "  {\n" +
                "    TIMESTARTED  : DATETIME(format='EEE MMM d HH:mm:ss z yyyy');\n" +
                "    TIMEFINISHED : DATETIME(format='EEE MMM d HH:mm:ss z yyyy');\n" +
                "  }\n" +
                "}\n" +
                "section deletes: BloombergActionDetails\n" +
                "{\n" +
                "  actionFlags: 'D';\n" +
                "\n" +
                "  Record\n" +
                "  {\n" +
                "    CP_DELETE_REASON : STRING(optional);\n" +
                "  }\n" +
                "}\n" +
                "section dvdCash: BloombergActionDetails\n" +
                "{\n" +
                "  actionFlags: ['N', 'U'];\n" +
                "  mnemonics: 'DVD_CASH';\n" +
                "\n" +
                "  Record\n" +
                "  {\n" +
                "    CP_RECORD_DT              : DATE(optional);\n" +
                "    CP_PAY_DT                 : DATE(optional);\n" +
                "  }\n" +
                "}"
        );
    }

    @Test
    public void metadataMustFollowBloombergData()
    {
        test(
                BloombergFixtures.metadataGrammar("md"),
                "BloombergMetadata section must follow a Bloomberg section in section 'md'");
        test(
                BloombergFixtures.FLT_CPN_HIST_SECTION_GRAMMAR + "\n" +
                        BloombergFixtures.metadataGrammar("md1") + "\n" +
                        BloombergFixtures.metadataGrammar("md2"),
                "BloombergMetadata section must follow a Bloomberg section in section 'md2'");

    }

    @Test
    public void bloombergShouldNotBeMixedWithNonBloomberg()
    {
        test(
                "section x: ImmaterialLines\n" +
                        "{\n" +
                        "  scope.untilEof;\n" +
                        "}\n" +
                        BloombergFixtures.FLT_CPN_HIST_SECTION_GRAMMAR,
                "Bloomberg section must not be combined with non-Bloomberg sections in section 'fltCpnHist'");

    }

    @Test
    public void invalidFilters()
    {
        test(
                "section fltCpnHist: BloombergData\n" +
                        "{\n" +
                        "  filter: ['DATA', 'DATA=', '=', '=VALUE', 'DATA=VALUE'];\n" +
                        "\n" +
                        "  Record\n" +
                        "  {\n" +
                        "    F1 : DATE;\n" +
                        "  }\n" +
                        "}",
                "Invalid filter value 'DATA' (Expected KEY=VALUE) in section 'fltCpnHist'",
                "Invalid filter value 'DATA=' (Expected KEY=VALUE) in section 'fltCpnHist'",
                "Invalid filter value '=' (Expected KEY=VALUE) in section 'fltCpnHist'",
                "Invalid filter value '=VALUE' (Expected KEY=VALUE) in section 'fltCpnHist'");
    }

    @Test
    public void actionDetailsMustFollowBloombergActions()
    {
        test(
                "section deletes: BloombergActionDetails\n" +
                        "{\n" +
                        "  actionFlags: 'D';\n" +
                        "\n" +
                        "  Record\n" +
                        "  {\n" +
                        "    CP_DELETE_REASON : STRING(optional);\n" +
                        "  }\n" +
                        "}",
                "BloombergActionDetails sections must follow a BloombergActions section in section 'deletes'");
        test(
                BloombergFixtures.FLT_CPN_HIST_SECTION_GRAMMAR + "\n" +
                        "section deletes: BloombergActionDetails\n" +
                        "{\n" +
                        "  actionFlags: 'D';\n" +
                        "\n" +
                        "  Record\n" +
                        "  {\n" +
                        "    CP_DELETE_REASON : STRING(optional);\n" +
                        "  }\n" +
                        "}",
                "BloombergActionDetails sections must follow a BloombergActions section in section 'deletes'");

    }
}
