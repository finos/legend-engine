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

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.janino.ClassLoaderIClassLoader;
import org.codehaus.janino.IClassLoader;
import org.codehaus.janino.Parser;
import org.codehaus.janino.Scanner;
import org.codehaus.janino.UnitCompiler;
import org.codehaus.janino.util.ClassFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class SingleFileCompiler
{
    public static Map<String, byte[]> compileFile(StringJavaSource source) throws IOException, CompileException
    {
        return compileFile(source, null);
    }

    public static Map<String, byte[]> compileFile(StringJavaSource source, ClassLoader parentClassLoader) throws IOException, CompileException
    {
        IClassLoader classLoader = parentClassLoader == null ?
                new ClassLoaderIClassLoader(Thread.currentThread().getContextClassLoader()) :
                new ClassLoaderIClassLoader(parentClassLoader);

        Scanner scanner = new Scanner(
                source.getName(),
                new InputStreamReader(source.openInputStream(), Charset.defaultCharset())
        );

        Parser parser = new Parser(scanner);
        UnitCompiler unitCompiler = new UnitCompiler(parser.parseAbstractCompilationUnit(), classLoader);
        ClassFile[] classFiles = unitCompiler.compileUnit(false, false, false);
        Map<String, byte[]> classes = new HashMap<>();
        for (ClassFile cf : classFiles)
        {
            classes.put(cf.getThisClassName(), cf.toByteArray());
        }

        return classes;
    }
}

