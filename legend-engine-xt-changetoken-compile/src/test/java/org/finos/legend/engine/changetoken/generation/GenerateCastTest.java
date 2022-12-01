//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.changetoken.generation;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Path;

public class GenerateCastTest
{

    private static Class<?> compiledClass;
    public static Path generatedSourcesDirectory;
    public static Path classesDirectory;

    @ClassRule
    public static TemporaryFolder tmpFolder = new TemporaryFolder();

    @BeforeClass
    public static void setupSuite() throws IOException
    {
        generatedSourcesDirectory = tmpFolder.newFolder("generated-sources", "java").toPath();
        classesDirectory = tmpFolder.newFolder("classes").toPath();

        //JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    }

    @Test
    public void testUpcast()
    {
        GenerateCast.main(generatedSourcesDirectory.toString(), "meta::pure::changetoken::tests::getVersions", "TestCastFunction");
    }
}
