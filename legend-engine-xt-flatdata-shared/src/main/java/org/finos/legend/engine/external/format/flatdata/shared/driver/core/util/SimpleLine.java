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

package org.finos.legend.engine.external.format.flatdata.shared.driver.core.util;

import java.util.function.Predicate;
import java.util.regex.Pattern;

public class SimpleLine implements LineReader.Line
{
    private static final Predicate<String> BLANK_LINE = Pattern.compile("^\\s*$").asPredicate();

    private final long lineNumber;
    private final String text;

    public SimpleLine(long lineNumber, String text)
    {
        this.lineNumber = lineNumber;
        this.text = text;
    }

    @Override
    public long getLineNumber()
    {
        return lineNumber;
    }

    @Override
    public boolean isEmpty()
    {
        return BLANK_LINE.test(text);
    }

    @Override
    public String getText()
    {
        return text;
    }
}
