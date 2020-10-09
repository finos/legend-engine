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

package org.finos.engine.shared.javaCompiler;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.LazyIterate;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;

public class JavaCompileException extends Exception
{
    private static final Predicate<Diagnostic<?>> IS_ERROR_DIAGNOSTIC = new Predicate<Diagnostic<?>>()
    {
        @Override
        public boolean accept(Diagnostic<?> diagnostic)
        {
            return (diagnostic != null) && (Diagnostic.Kind.ERROR == diagnostic.getKind());
        }
    };

    private static final Function<Diagnostic<? extends JavaFileObject>, String> SOURCE_NAME = new Function<Diagnostic<? extends JavaFileObject>, String>()
    {
        @Override
        public String valueOf(Diagnostic<? extends JavaFileObject> diagnostic)
        {
            JavaFileObject source = diagnostic.getSource();
            return (source == null) ? null : source.getName();
        }
    };

    private static final int MAX_SOURCES_FOR_MESSAGE = 5;
    private static final int MAX_LINES_FOR_MESSAGE = 250;
    private static final long SOURCE_CONTEXT_LINES = 20;

    private final ImmutableList<Diagnostic<? extends JavaFileObject>> diagnostics;

    public JavaCompileException(Iterable<Diagnostic<? extends JavaFileObject>> diagnostics)
    {
        super(createMessage(diagnostics));
        this.diagnostics = Lists.immutable.withAll(diagnostics);
    }

    public JavaCompileException(DiagnosticCollector<JavaFileObject> diagnosticCollector)
    {
        this(diagnosticCollector.getDiagnostics());
    }

    public RichIterable<Diagnostic<? extends JavaFileObject>> getDiagnostics()
    {
        return this.diagnostics;
    }

    public RichIterable<Diagnostic<? extends JavaFileObject>> getErrorDiagnostics()
    {
        return LazyIterate.select(this.diagnostics, IS_ERROR_DIAGNOSTIC);
    }

    private static String createMessage(Iterable<Diagnostic<? extends JavaFileObject>> diagnostics)
    {
        MutableList<Diagnostic<? extends JavaFileObject>> errorDiagnostics = Iterate.select(diagnostics, IS_ERROR_DIAGNOSTIC, Lists.mutable.<Diagnostic<? extends JavaFileObject>>with());
        return (errorDiagnostics.isEmpty())
                ? "Unknown error compiling generated Java code"
                : new JavaCompileException.MessageBuilder(errorDiagnostics).getMessage();
    }

    private static class MessageBuilder
    {
        private final MutableList<Diagnostic<? extends JavaFileObject>> diagnostics;
        private MutableList<String> lines;
        private String lastSourceName;
        private String[] sourceLines;
        private long sourceLinesHighwater;

        private MessageBuilder(MutableList<Diagnostic<? extends JavaFileObject>> diagnostics)
        {
            this.diagnostics = diagnostics;
        }

        String getMessage()
        {
            lines = Lists.mutable.empty();
            lastSourceName = null;

            addLine(createSummaryMessage());
            diagnostics.forEach(new Procedure<Diagnostic<? extends JavaFileObject>>()
            {
                @Override
                public void value(Diagnostic<? extends JavaFileObject> diagnostic)
                {
                    printLines(diagnostic.toString());
                }
            });
            diagnostics.groupBy(SOURCE_NAME)
                    .forEachKeyMultiValues(new Procedure2<String, Iterable<Diagnostic<? extends JavaFileObject>>>()
                    {
                        @Override
                        public void value(String sourceName, Iterable<Diagnostic<? extends JavaFileObject>> diagnostics)
                        {
                            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics)
                            {
                                printDiagnosticSource(diagnostic);
                            }
                        }
                    });

            return lines.makeString("\n");
        }

        private void addLine(String line)
        {
            if (lines.size() < MAX_LINES_FOR_MESSAGE)
            {
                lines.add(line);
            }
            else if (lines.size() == MAX_LINES_FOR_MESSAGE)
            {
                lines.add("... (max message size exceeded)");
            }
        }

        private void printLines(String text)
        {
            String[] lines = text.split("\\n");
            for (int i = 0; i < lines.length; i++)
            {
                addLine(lines[i]);
            }
        }

        private String createSummaryMessage()
        {
            StringBuilder builder = new StringBuilder();
            int count = diagnostics.size();
            builder.append(count);
            builder.append((count == 1) ? " error compiling " : " errors compiling ");
            MutableList<String> sourceNames = LazyIterate.collect(diagnostics, SOURCE_NAME).select(Predicates.notNull(), Sets.mutable.<String>with()).toSortedList();

            if (sourceNames.size() <= MAX_SOURCES_FOR_MESSAGE)
            {
                sourceNames.appendString(builder, ", ");
            }
            else
            {
                LazyIterate.take(sourceNames, MAX_SOURCES_FOR_MESSAGE).appendString(builder, "", ", ", ", ... (and ");
                builder.append(sourceNames.size() - MAX_SOURCES_FOR_MESSAGE);
                builder.append(" more)");
            }
            return builder.toString();
        }

        private void printDiagnosticSource(Diagnostic<? extends JavaFileObject> diagnostic)
        {
            String sourceName = SOURCE_NAME.valueOf(diagnostic);
            if (sourceName != lastSourceName)
            {
                JavaFileObject source = diagnostic.getSource();
                if (source instanceof StringJavaSource)
                {
                    sourceLines = ((StringJavaSource) source).getCode().split("\\n");
                    sourceLinesHighwater = 0;
                    addLine("");
                    addLine(sourceName);
                    lastSourceName = sourceName;
                }
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
