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

public abstract class AbstractTestSnowflakeSemiStructured extends AbstractTestSemiStructured
{
    protected String wrapPreAndFinallyExecutionSqlQuery(String TDSType, String expectedRelational)
    {
        return  "RelationalBlockExecutionNode\n" +
                "(\n" +
                TDSType +
                "  (\n" +
                "    SQL\n" +
                "    (\n" +
                "      type = Void\n" +
                "      resultColumns = []\n" +
                "      sql = ALTER SESSION SET QUERY_TAG = '{\"executionTraceID\" : \"${execID}\", \"engineUser\" : \"${userId}\", \"referer\" : \"${referer}\"}';\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n" +
                expectedRelational +
                "  ) \n" +
                "  finallyExecutionNodes = \n" +
                "  (\n" +
                "    SQL\n" +
                "    (\n" +
                "      type = Void\n" +
                "      resultColumns = []\n" +
                "      sql = ALTER SESSION UNSET QUERY_TAG;\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n" +
                "  )\n" +
                ")\n";
    }

}
