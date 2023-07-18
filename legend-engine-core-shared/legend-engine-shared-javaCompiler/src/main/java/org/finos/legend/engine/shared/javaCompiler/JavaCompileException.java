// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.shared.javaCompiler;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.LazyIterate;

import java.util.regex.Pattern;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;

public class JavaCompileException extends Exception
{
    private static final int MAX_SOURCES_FOR_MESSAGE = 5;
    private static final int MAX_LINES_FOR_MESSAGE = 250;
    private static final long SOURCE_CONTEXT_LINES = 20;

    private final ImmutableList<Diagnostic<? extends JavaFileObject>> diagnostics;
    private final ImmutableList<String> options;

    public JavaCompileException(Iterable<Diagnostic<? extends JavaFileObject>> diagnostics, Iterable<String> options)
    {
        super(createMessage(diagnostics));
        this.diagnostics = Lists.immutable.withAll(diagnostics);
        this.options = Lists.immutable.withAll(options);
    }

    public JavaCompileException(DiagnosticCollector<JavaFileObject> diagnosticCollector, Iterable<String> options)
    {
        this(diagnosticCollector.getDiagnostics(), options);
    }

    public JavaCompileException(Iterable<Diagnostic<? extends JavaFileObject>> diagnostics)
    {
        this(diagnostics, null);
    }

    public JavaCompileException(DiagnosticCollector<JavaFileObject> diagnosticCollector)
    {
        this(diagnosticCollector, null);
    }

    public RichIterable<Diagnostic<? extends JavaFileObject>> getDiagnostics()
    {
        return this.diagnostics;
    }

    public RichIterable<Diagnostic<? extends JavaFileObject>> getErrorDiagnostics()
    {
        return LazyIterate.select(this.diagnostics, JavaCompileException::isErrorDiagnostic);
    }

    public RichIterable<String> getCompileOptions()
    {
        return this.options;
    }

    private static String createMessage(Iterable<Diagnostic<? extends JavaFileObject>> diagnostics)
    {
        MutableList<Diagnostic<? extends JavaFileObject>> errorDiagnostics = Iterate.select(diagnostics, JavaCompileException::isErrorDiagnostic, Lists.mutable.empty());
        return errorDiagnostics.isEmpty() ? "Unknown error compiling generated Java code" : new MessageBuilder(errorDiagnostics).getMessage();
    }

    private static boolean isErrorDiagnostic(Diagnostic<?> diagnostic)
    {
        return (diagnostic != null) && (Diagnostic.Kind.ERROR == diagnostic.getKind());
    }

    private static String getSourceName(Diagnostic<? extends JavaFileObject> diagnostic)
    {
        JavaFileObject source = diagnostic.getSource();
        return (source == null) ? null : source.getName();
    }

    private static class MessageBuilder
    {
        private final ListIterable<Diagnostic<? extends JavaFileObject>> diagnostics;
        private final Pattern lineSplitter = Pattern.compile("\\R");
        private final MutableList<String> lines = Lists.mutable.empty();

        private MessageBuilder(MutableList<Diagnostic<? extends JavaFileObject>> diagnostics)
        {
            this.diagnostics = diagnostics;
        }

        String getMessage()
        {
            addLine(createSummaryMessage());
            this.diagnostics.forEach(diagnostic -> printLines(diagnostic.toString()));
            this.diagnostics.groupBy(JavaCompileException::getSourceName).forEachKeyMultiValues(this::printDiagnosticSources);
            return this.lines.makeString("\n");
        }

        private void addLine(String line)
        {
            if (this.lines.size() < MAX_LINES_FOR_MESSAGE)
            {
                this.lines.add(line);
            }
            else if (this.lines.size() == MAX_LINES_FOR_MESSAGE)
            {
                this.lines.add("... (max message size exceeded)");
            }
        }

        private boolean maxMessageSizeReached()
        {
            return this.lines.size() >= MAX_LINES_FOR_MESSAGE;
        }

        private void printLines(String text)
        {
            String[] lines = this.lineSplitter.split(text);
            ArrayIterate.forEach(lines, this::addLine);
        }

        private String createSummaryMessage()
        {
            StringBuilder builder = new StringBuilder();
            builder.append(this.diagnostics.size()).append((this.diagnostics.size() == 1) ? " error compiling " : " errors compiling ");
            MutableList<String> sourceNames = this.diagnostics.collect(JavaCompileException::getSourceName, Sets.mutable.empty()).without(null).toSortedList();
            if (sourceNames.size() <= MAX_SOURCES_FOR_MESSAGE)
            {
                sourceNames.appendString(builder, ", ");
            }
            else
            {
                sourceNames.subList(0, MAX_SOURCES_FOR_MESSAGE).appendString(builder, "", ", ", ", ... (and ");
                builder.append(sourceNames.size() - MAX_SOURCES_FOR_MESSAGE).append(" more)");
            }
            return builder.toString();
        }

        private void printDiagnosticSources(String sourceName, Iterable<? extends Diagnostic<? extends JavaFileObject>> diagnostics)
        {
            if (maxMessageSizeReached())
            {
                return;
            }

            Diagnostic<? extends JavaFileObject> firstDiagnostic = Iterate.getFirst(diagnostics);
            if (firstDiagnostic == null)
            {
                return;
            }

            JavaFileObject source = firstDiagnostic.getSource();
            String[] sourceLines;
            if (source instanceof StringJavaSource)
            {
                sourceLines = this.lineSplitter.split(((StringJavaSource) source).getCode());
                addLine("");
                addLine(sourceName);
            }
            else
            {
                sourceLines = new String[0];
            }

            long sourceLinesHighwater = 0;
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics)
            {
                if (maxMessageSizeReached())
                {
                    return;
                }

                long lineNo = Math.max(diagnostic.getLineNumber() - SOURCE_CONTEXT_LINES, sourceLinesHighwater + 1);
                long end = Math.min(diagnostic.getLineNumber() + SOURCE_CONTEXT_LINES, sourceLines.length);
                if (lineNo - sourceLinesHighwater > 2)
                {
                    addLine(String.format("%04d:%04d ...", sourceLinesHighwater + 1, lineNo - 1));
                    sourceLinesHighwater = lineNo - 1;
                }
                else
                {
                    lineNo = sourceLinesHighwater + 1;
                }

                while (lineNo < end)
                {
                    if (sourceLinesHighwater < lineNo)
                    {
                        addLine(String.format("%04d %s", lineNo, sourceLines[(int) (lineNo - 1)]));
                        sourceLinesHighwater = lineNo;
                    }
                    lineNo++;
                }
            }
        }
    }
}
