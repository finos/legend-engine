// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.extension.functions.shared.string;

import java.util.regex.Pattern;

public enum RegexpParameter
{
    CASE_SENSITIVE,
    CASE_INSENSITIVE,
    MULTILINE,
    NON_NEWLINE_SENSITIVE;

    public static int toPatternFlag(RegexpParameter param)
    {
        switch (param)
        {
            case CASE_SENSITIVE:
                return 0;

            case CASE_INSENSITIVE:
                return Pattern.CASE_INSENSITIVE;

            case MULTILINE:
                return Pattern.MULTILINE;

            case NON_NEWLINE_SENSITIVE:
                return Pattern.DOTALL;

            default:
                throw new IllegalArgumentException("Unsupported parameter: " + param);
        }
    }
}
