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

package org.finos.legend.engine.external.language.java.runtime.compiler.shared;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;

import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;

public class CompilationResult
{
    private final MutableList<Diagnostic<? extends JavaFileObject>> diagnostics;
    private final ClassLoader classLoader;

    private CompilationResult(ClassLoader classLoader)
    {
        this.classLoader = classLoader;
        this.diagnostics = Lists.fixedSize.empty();
    }

    private CompilationResult(DiagnosticCollector<JavaFileObject> diagnostics)
    {
        this.classLoader = null;
        this.diagnostics = ListIterate.select(diagnostics.getDiagnostics(), d -> d.getKind() == Diagnostic.Kind.ERROR).asUnmodifiable();
    }

    public boolean isSuccess()
    {
        return this.classLoader != null;
    }

    public ClassLoader getClassLoader()
    {
        return this.classLoader;
    }

    public List<Diagnostic<? extends JavaFileObject>> getDiagnostics()
    {
        return this.diagnostics;
    }

    public ListIterable<String> getErrorMessages()
    {
        return this.diagnostics.collect(CompilationResult::getErrorMessage);
    }

    static CompilationResult success(ClassLoader classLoader)
    {
        return new CompilationResult(classLoader);
    }

    static CompilationResult error(DiagnosticCollector<JavaFileObject> diagnostics)
    {
        return new CompilationResult(diagnostics);
    }

    private static String getErrorMessage(Diagnostic<? extends JavaFileObject> diagnostic)
    {
        String separator = System.lineSeparator();
        return diagnostic.getSource().getName() + ":" + diagnostic.getLineNumber() + ":" + diagnostic.getColumnNumber() + separator + diagnostic.getMessage(null);
    }
}
