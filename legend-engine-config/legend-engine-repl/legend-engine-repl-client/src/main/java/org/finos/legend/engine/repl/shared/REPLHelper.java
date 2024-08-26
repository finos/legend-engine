// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.repl.shared;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.repl.core.Command;

import java.util.Comparator;

import static org.jline.jansi.Ansi.ansi;

public class REPLHelper
{
    private static final int COMMANDS_HELP_INDENTATION = 2;
    private static final int COMMANDS_HELP_SPACING = 4;
    private static final int LINE_WIDTH = 80;

    public static String generateCommandsHelp(MutableList<Command> commands)
    {
        int maxDocLength = commands.maxBy(c -> c.documentation().length()).documentation().length();
        return commands
                .toSortedList(Comparator.comparing(Command::documentation))
                // pad right to align the command description
                .collect(c ->
                {
                    // TODO: if needed, we can also wrap the description
                    StringBuilder line = new StringBuilder();
                    String[] descriptionLines = c.description().split("\n");
                    for (int i = 0; i < descriptionLines.length; i++)
                    {
                        if (i == 0)
                        {
                            line.append(StringUtils.leftPad("", COMMANDS_HELP_INDENTATION)).append(StringUtils.rightPad(c.documentation(), maxDocLength + COMMANDS_HELP_SPACING)).append(descriptionLines[0]);
                        }
                        else
                        {
                            line.append("\n").append(StringUtils.leftPad("", COMMANDS_HELP_INDENTATION)).append(StringUtils.rightPad("", maxDocLength + COMMANDS_HELP_SPACING)).append(descriptionLines[i]);
                        }
                    }
                    return line.toString();
                })
                .makeString("\n");
    }

    public static String horizontalRule(String label)
    {
        return StringUtils.rightPad(label != null ? ("-- " + label + " ") : "", LINE_WIDTH, "-");
    }

    public static String wrap(String text)
    {
        return WordUtils.wrap(text, LINE_WIDTH);
    }

    public static String ansiGreen(String text)
    {
        return ansi().fgGreen().a(text).reset().toString();
    }

    public static String ansiYellow(String text)
    {
        return ansi().fgYellow().a(text).reset().toString();
    }

    public static String ansiBlue(String text)
    {
        return ansi().fgBlue().a(text).reset().toString();
    }

    public static String ansiRed(String text)
    {
        return ansi().fgRed().a(text).reset().toString();
    }

    public static String ansiDim(String text)
    {
        return ansi().fgBrightBlack().a(text).reset().toString();
    }
}
