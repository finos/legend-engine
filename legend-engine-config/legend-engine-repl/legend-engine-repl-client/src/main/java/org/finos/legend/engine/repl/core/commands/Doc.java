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

package org.finos.legend.engine.repl.core.commands;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.core.Command;
import org.finos.legend.engine.repl.shared.DocumentationHelper;
import org.finos.legend.pure.m3.pct.aggregate.model.FunctionDocumentation;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

public class Doc implements Command
{
    private final Client client;

    public Doc(Client client)
    {
        this.client = client;
    }

    @Override
    public String documentation()
    {
        return "doc <function>";
    }

    @Override
    public String description()
    {
        return "show documentation of the specified Pure function";
    }

    @Override
    public boolean process(String line) throws Exception
    {
        if (line.startsWith("doc"))
        {
            String[] tokens = line.split(" ");
            String path = tokens[1];
            FunctionDocumentation functionDocumentation = this.client.getFunctionDocumentation(path);
            if (functionDocumentation != null)
            {
                client.println(DocumentationHelper.generateANSIFunctionDocumentation(functionDocumentation, client.getDocumentationAdapterKeys()));
            }
            else
            {
                client.printError("No documentation found for function: " + path);
            }
            return true;
        }
        return false;
    }

    @Override
    public MutableList<Candidate> complete(String inScope, LineReader lineReader, ParsedLine parsedLine)
    {
        if (inScope.startsWith("doc"))
        {
            return ListIterate.collect(client.getDocumentedFunctions(), Candidate::new);
        }
        return null;
    }
}
