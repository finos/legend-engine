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

package org.finos.legend.engine.external.format.flatdata.shared.driver.core.valueParser;

import org.eclipse.collections.api.factory.Maps;

import java.text.ParseException;
import java.util.Map;

public abstract class BooleanParser implements ValueParser
{
    private static Map<String, String> COMMON_MAPPINGS = Maps.mutable.empty();
    static
    {
        COMMON_MAPPINGS.put("true", "false");
        COMMON_MAPPINGS.put("True", "False");
        COMMON_MAPPINGS.put("TRUE", "FALSE");
        COMMON_MAPPINGS.put("y", "n");
        COMMON_MAPPINGS.put("Y", "N");
        COMMON_MAPPINGS.put("yes", "no");
        COMMON_MAPPINGS.put("Yes", "No");
        COMMON_MAPPINGS.put("YES", "NO");
        COMMON_MAPPINGS.put("1", "0");
    }

    public abstract boolean parse(String s) throws ParseException;
    public abstract String toString(boolean b);

    public static BooleanParser of(String trueString, String falseString)
    {
        if (trueString == null && falseString == null)
        {
            return new TrueStringBooleanParser("true");
        }
        else if (trueString != null && falseString == null)
        {
            return new TrueStringBooleanParser(trueString);
        }
        else if (trueString == null)
        {
            return new FalseStringBooleanParser(falseString);
        }
        else
        {
            return new TrueAndFalseStringBooleanParser(trueString, falseString);
        }
    }

    private static class TrueStringBooleanParser extends BooleanParser
    {
        private final String trueString;
        private String inferredFalseString = "";

        TrueStringBooleanParser(String trueString)
        {
            this.trueString = trueString;
            COMMON_MAPPINGS.forEach((t, f) -> inferredFalseString = t.equals(trueString) ? f : inferredFalseString);
        }

        @Override
        public boolean parse(String s)
        {
            return trueString.equalsIgnoreCase(s);
        }

        @Override
        public String validate(String s)
        {
            return null;
        }

        @Override
        public String toString(boolean b)
        {
            return b ? trueString : inferredFalseString;
        }
    }

    private static class FalseStringBooleanParser extends BooleanParser
    {
        private final String falseString;
        private String inferredTrueString = "";

        FalseStringBooleanParser(String falseString)
        {
            this.falseString = falseString;
            COMMON_MAPPINGS.forEach((t, f) -> inferredTrueString = f.equals(falseString) ? t : inferredTrueString);
        }

        @Override
        public boolean parse(String s)
        {
            return !falseString.equalsIgnoreCase(s);
        }

        @Override
        public String validate(String s)
        {
            return null;
        }

        @Override
        public String toString(boolean b)
        {
            return b ? inferredTrueString : falseString;
        }
    }

    private static class TrueAndFalseStringBooleanParser extends BooleanParser
    {
        private final String trueString;
        private final String falseString;

        TrueAndFalseStringBooleanParser(String trueString, String falseString)
        {
            this.trueString = trueString;
            this.falseString = falseString;
        }

        @Override
        public boolean parse(String s) throws ParseException
        {
            if (trueString.equalsIgnoreCase(s))
            {
                return true;
            }
            else if (falseString.equalsIgnoreCase(s))
            {
                return false;
            }
            else
            {
                throw new ParseException("Invalid boolean: neither '" + trueString + "' nor '" + falseString + "'", 0);
            }
        }

        @Override
        public String validate(String s)
        {
            return trueString.equalsIgnoreCase(s) || falseString.equalsIgnoreCase(s)
                   ? null
                   : "Invalid boolean: neither '" + trueString + "' nor '" + falseString + "'";
        }

        @Override
        public String toString(boolean b)
        {
            return b ? trueString : falseString;
        }
    }
}
