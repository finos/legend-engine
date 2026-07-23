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

package org.finos.legend.engine.postgres.e2e.coverage;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Auto-detects an error category from a simplified error message.
 * Used to classify ERROR/FAIL results in parity reports.
 */
public class ErrorCategorizer
{
    public static final String FUNCTION_NOT_SUPPORTED = "FUNCTION_NOT_SUPPORTED";
    public static final String FUNCTION_NO_SQL_TRANSLATION = "FUNCTION_NO_SQL_TRANSLATION";
    public static final String ALIAS_NOT_FOUND = "ALIAS_NOT_FOUND";
    public static final String PARSE_ERROR = "PARSE_ERROR";
    public static final String UNSUPPORTED_SYNTAX = "UNSUPPORTED_SYNTAX";
    public static final String REGEX_LIMITATION = "REGEX_LIMITATION";
    public static final String TYPE_ERROR = "TYPE_ERROR";
    public static final String RESULT_MISMATCH = "RESULT_MISMATCH";
    public static final String UNSUPPORTED = "UNSUPPORTED";
    public static final String MISC = "MISC";

    private static final LinkedHashMap<Pattern, String> PATTERNS = new LinkedHashMap<>();

    static
    {
        PATTERNS.put(Pattern.compile("No function matches the given name"), FUNCTION_NOT_SUPPORTED);
        PATTERNS.put(Pattern.compile("No SQL translation exists for the PURE function"), FUNCTION_NO_SQL_TRANSLATION);
        PATTERNS.put(Pattern.compile("function \\S+ is not yet supported"), FUNCTION_NO_SQL_TRANSLATION);
        PATTERNS.put(Pattern.compile("can't be found|column .* not found|Could not find column", Pattern.CASE_INSENSITIVE), ALIAS_NOT_FOUND);
        PATTERNS.put(Pattern.compile("Parsing error|no viable alternative|Unexpected token"), PARSE_ERROR);
        PATTERNS.put(Pattern.compile("not currently supported|not yet supported|not supported|only .* supported"), UNSUPPORTED_SYNTAX);
        PATTERNS.put(Pattern.compile("regex|REGEX"), REGEX_LIMITATION);
        PATTERNS.put(Pattern.compile("type mismatch|No value found for|integer out of range|cannot cast|cannot be cast"), TYPE_ERROR);
    }

    /**
     * Categorize an error based on the simplified error message and state.
     *
     * @param state   the test state ("ERROR", "FAIL", "BUG", etc.)
     * @param error   the simplified error message (may be null for FAIL state)
     * @return the error category string
     */
    public static String categorize(String state, String error)
    {
        if ("FAIL".equals(state))
        {
            return RESULT_MISMATCH;
        }
        if (error == null || error.isEmpty())
        {
            return MISC;
        }
        for (Map.Entry<Pattern, String> entry : PATTERNS.entrySet())
        {
            if (entry.getKey().matcher(error).find())
            {
                return entry.getValue();
            }
        }
        return MISC;
    }

    /**
     * Returns a human-readable description of the error category.
     */
    public static String description(String category)
    {
        switch (category)
        {
            case FUNCTION_NOT_SUPPORTED:
                return "Function name not recognized by Legend SQL";
            case FUNCTION_NO_SQL_TRANSLATION:
                return "Pure function exists but has no SQL translation";
            case ALIAS_NOT_FOUND:
                return "Column or alias reference cannot be resolved";
            case PARSE_ERROR:
                return "SQL syntax not parseable by Legend SQL parser";
            case UNSUPPORTED_SYNTAX:
                return "SQL construct recognized but not yet implemented";
            case REGEX_LIMITATION:
                return "Regex feature limitation (only exact match supported)";
            case TYPE_ERROR:
                return "Type mismatch or cast error";
            case RESULT_MISMATCH:
                return "Query executes but results differ from Postgres";
            case UNSUPPORTED:
                return "Category not applicable to Legend (system/network functions)";
            case MISC:
                return "Other/uncategorized error";
            default:
                return category;
        }
    }
}

