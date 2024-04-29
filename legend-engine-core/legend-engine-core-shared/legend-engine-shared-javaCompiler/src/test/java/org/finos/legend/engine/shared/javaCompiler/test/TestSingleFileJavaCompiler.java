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

package org.finos.legend.engine.shared.javaCompiler.test;

import org.finos.legend.engine.shared.javaCompiler.EngineJavaCompiler;
import org.finos.legend.engine.shared.javaCompiler.JavaVersion;
import org.finos.legend.engine.shared.javaCompiler.SingleFileCompiler;
import org.finos.legend.engine.shared.javaCompiler.StringJavaSource;
import org.junit.Assert;
import org.junit.Test;

import java.util.Base64;

public class TestSingleFileJavaCompiler
{
    @Test
    public void testSingleFileCompilerCompilation() throws Exception
    {
        final String code = "package engine.generated;" +
                "public class Execute" +
                "{" +
                "    public static String execute()\n" +
                "    {\n" +
                "       return \"ok\";" +
                "    }\n" +
                "}";

        byte[] classBytes = SingleFileCompiler.compileFile(StringJavaSource.newStringJavaSource("engine.generated", "Execute", code)).get("engine.generated.Execute");
        Assert.assertTrue(classBytes.length > 0);
    }

    @Test
    public void testSingleFileCompilerLoadingAndExecution() throws Exception
    {
        final String code =
                "package engine.generated;" +
                        "public class Execute" +
                        "{" +
                        "    public static String execute()\n" +
                        "    {\n" +
                        "       return \"ok\";" +
                        "    }\n" +
                        "}";
        byte[] classBytes = SingleFileCompiler.compileFile(StringJavaSource.newStringJavaSource("engine.generated", "Execute", code)).get("engine.generated.Execute");
        EngineJavaCompiler engineJavaCompiler = new EngineJavaCompiler(JavaVersion.JAVA_8);
        engineJavaCompiler.load("engine.generated.Execute", Base64.getEncoder().encodeToString(classBytes));
        Assert.assertEquals("ok", engineJavaCompiler.getClassLoader().loadClass("engine.generated.Execute").getMethod("execute").invoke(null));
    }
}
