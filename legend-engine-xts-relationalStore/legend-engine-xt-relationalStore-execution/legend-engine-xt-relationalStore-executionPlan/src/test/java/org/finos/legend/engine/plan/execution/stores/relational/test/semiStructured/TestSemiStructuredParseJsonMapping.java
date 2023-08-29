// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.test.semiStructured;

import org.junit.Assert;
import org.junit.Test;

public class TestSemiStructuredParseJsonMapping extends AbstractTestSemiStructured
{
    private static final String snowflakeMapping = "parseJson::mapping::SnowflakeMapping";
    private static final String h2Runtime = "parseJson::runtime::H2Runtime";

    @Test
    public void testParseJsonInMapping()
    {
        String h2Result = this.executeFunction("parseJson::parseJsonInMapping__TabularDataSet_1_", snowflakeMapping, h2Runtime);
        Assert.assertEquals("Peter,Firm X,Firm X,Firm X,Firm X,Firm X\n" +
                "John,Firm X,Firm X,,,\n" +
                "John,Firm X,Firm X,Firm X,Firm X,Firm X\n" +
                "Anthony,Firm X,,,,\n" +
                "Fabrice,Firm A,,,,\n" +
                "Oliver,Firm B,Firm B,,,\n" +
                "David,Firm B,,,,\n" +
                "UNKNOWN,,,,,\n", h2Result.replace("\r\n", "\n"));

        Assert.assertEquals("[PERSON_TABLE_VARCHAR.FIRM_DETAILS <DynaFunction>, PERSON_TABLE_VARCHAR.FIRSTNAME <TableAliasColumn>, PERSON_TABLE_VARCHAR.ID <JoinTreeNode>, PERSON_TABLE_VARCHAR.MANAGERID <JoinTreeNode>]", this.scanColumns("parseJson::parseJsonInMapping__TabularDataSet_1_", snowflakeMapping));
    }

    public String modelResourcePath()
    {
        return "/org/finos/legend/engine/plan/execution/stores/relational/test/semiStructured/semiStructuredParseJsonMapping.pure";
    }
}
