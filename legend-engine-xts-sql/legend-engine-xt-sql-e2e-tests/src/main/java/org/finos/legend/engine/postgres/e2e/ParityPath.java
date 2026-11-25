// Copyright 2026 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.postgres.e2e;

import java.util.function.Function;

public enum ParityPath
{
    TDS("TDS", "tds", name -> "e2e::tds_" + name),
    RELATION("Relation", "rel", name -> "e2e::rel_" + name),
    GRAMMAR("Grammar", "grammar", name -> "e2e::rel_" + name + "__Relation_1_");

    private final String label;
    private final String statusKey;
    private final Function<String, String> functionPath;

    ParityPath(String label, String statusKey, Function<String, String> functionPath)
    {
        this.label = label;
        this.statusKey = statusKey;
        this.functionPath = functionPath;
    }

    public String label()
    {
        return this.label;
    }

    public String reportKey()
    {
        return this.label.toLowerCase();
    }

    public String functionPathFor(String tableName)
    {
        return this.functionPath.apply(tableName);
    }

    public String tableFunctionCall(String tableName)
    {
        return "func('" + functionPathFor(tableName) + "')";
    }

    public String joinFunctionCall()
    {
        return tableFunctionCall("person_with_dept");
    }

    public String statusField()
    {
        return "expected_" + this.statusKey + "_status";
    }

    public String expectedStatus(TestCaseLoader.TestCase testCase)
    {
        switch (this)
        {
            case TDS:
                return testCase.expected_tds_status;
            case RELATION:
                return testCase.expected_rel_status;
            default:
                return testCase.expected_grammar_status;
        }
    }

    public static ParityPath fromLabel(String label)
    {
        for (ParityPath path : values())
        {
            if (path.label.equals(label))
            {
                return path;
            }
        }
        throw new IllegalArgumentException("Unknown parity path: " + label);
    }
}
