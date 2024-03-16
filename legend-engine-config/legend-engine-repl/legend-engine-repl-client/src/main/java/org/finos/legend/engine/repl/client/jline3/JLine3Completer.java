// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.repl.client.jline3;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.repl.core.Command;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.List;

public class JLine3Completer implements Completer
{
    private MutableList<Command> commands;

    public JLine3Completer(MutableList<Command> commands)
    {
        this.commands = commands;
    }

    @Override
    public void complete(LineReader lineReader, ParsedLine parsedLine, List<Candidate> list)
    {
        String inScope = parsedLine.line().substring(0, parsedLine.cursor());

        for (Command command : this.commands)
        {
            List<Candidate> candidates = command.complete(inScope, lineReader, parsedLine);
            if (candidates != null)
            {
                list.addAll(candidates);
                break;
            }
        }
    }
}
