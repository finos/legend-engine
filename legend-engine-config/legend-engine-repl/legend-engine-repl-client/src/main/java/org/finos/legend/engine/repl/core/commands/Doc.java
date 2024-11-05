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

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.client.jline3.JLine3Parser;
import org.finos.legend.engine.repl.core.Command;
import org.finos.legend.engine.repl.shared.DocumentationHelper;
import org.finos.legend.pure.m3.pct.aggregate.model.FunctionDocumentation;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.List;
import java.util.Map;

public class Doc implements Command
{
    private final Client client;
    private final DocGroupCompleter completer;
    private final Map<String, FunctionDocumentation> functionDocIndex = Maps.mutable.empty();

    public Doc(Client client)
    {
        this.client = client;
        this.client.getDocumentedFunctions().forEach(key ->
        {
            FunctionDocumentation doc = this.client.getFunctionDocumentation(key);
            String path = doc.functionDefinition.sourceId.substring(doc.reportScope.filePath.length(), doc.functionDefinition.sourceId.lastIndexOf(".pure"));
            functionDocIndex.put(path, doc);
        });
        this.completer = new DocGroupCompleter(functionDocIndex);
    }

    @Override
    public String documentation()
    {
        return "doc (<function directory path>) | docFn <function path>";
    }

    @Override
    public String description()
    {
        return "'doc' command supports browsing all supported Pure functions\n" +
                "'docFn' command shows documentation of the specified Pure function";
    }

    @Override
    public boolean process(String line) throws Exception
    {
        if (line.startsWith("docFn"))
        {
            String[] tokens = line.split(" ");
            String path = tokens.length > 1 ? tokens[1] : "";
            if (path.isEmpty())
            {
                client.printError("Function path is required");
                return true;
            }
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
        else if (line.startsWith("doc"))
        {
            String[] tokens = line.split(" ");
            String path = tokens.length > 1 ? tokens[1] : "";
            if (path.isEmpty())
            {
                client.printError("Function path is required");
                return true;
            }
            FunctionDocumentation functionDocumentation = this.functionDocIndex.get(path);
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
        if (inScope.startsWith("docFn"))
        {
            return ListIterate.collect(client.getDocumentedFunctions(), Candidate::new);
        }
        else if (inScope.startsWith("doc"))
        {
            MutableList<String> words = Lists.mutable.withAll(parsedLine.words()).drop(2);
            String path = words.makeString("");
            MutableList<Candidate> list = Lists.mutable.empty();
            completer.complete(lineReader, new JLine3Parser.MyParsedLine(new JLine3Parser.ParserResult(parsedLine.line(), Lists.mutable.with("doc", " ", path))), list);
            return list;
        }
        return null;
    }

    private static class DocGroupCompleter implements org.jline.reader.Completer
    {
        private final Map<String, FunctionDocumentation> functionDocIndex;

        public DocGroupCompleter(Map<String, FunctionDocumentation> functionDocIndex)
        {
            this.functionDocIndex = functionDocIndex;
        }

        public void complete(LineReader reader, ParsedLine commandLine, final List<Candidate> candidates)
        {
            String buffer = commandLine.word().substring(0, commandLine.wordCursor());
            String current;
            String sep = "/";
            int lastSep = buffer.lastIndexOf(sep);
            try
            {
                current = lastSep >= 0 ? buffer.substring(0, lastSep + 1) : "";
                Sets.mutable.withAll(functionDocIndex.keySet())
                        .select(path -> path.startsWith(current))
                        .collect(path ->
                        {
                            String childNode = path.substring(current.length());
                            return current + childNode.substring(0, !childNode.contains(sep) ? childNode.length() : childNode.indexOf(sep) + 1);
                        })
                        .toSortedList()
                        .forEach(value ->
                        {
                            candidates.add(value.endsWith(sep)
                                    ? new Candidate(value, sep + value, null, null, "/", null, false)
                                    : new Candidate(value, value, null, null, null, null, true));
                        });
            }
            catch (Exception e)
            {
                // Ignore
            }
        }
    }
}
