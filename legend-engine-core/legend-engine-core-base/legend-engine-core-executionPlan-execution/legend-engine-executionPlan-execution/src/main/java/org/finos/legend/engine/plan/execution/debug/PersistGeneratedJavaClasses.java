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

package org.finos.legend.engine.plan.execution.debug;

import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.JavaClass;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.JavaPlatformImplementation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class PersistGeneratedJavaClasses
{
    public static void persistForExecutionPlan(SingleExecutionPlan plan) throws IOException
    {
        List<JavaClass> javaClassesInScope = ((JavaPlatformImplementation) plan.globalImplementationSupport).classes;

        for (JavaClass javaClass : javaClassesInScope)
        {
            Path javaClassPath = Paths.get("src", "test", "java").resolve(javaClass._package.replaceAll("\\.", "/")).resolve(javaClass.name + ".java");
            File javaClassFile = javaClassPath.toFile();
            if (!javaClassFile.getParentFile().exists())
            {
                javaClassFile.getParentFile().mkdirs();
            }
            javaClassFile.createNewFile();
            try (FileOutputStream fileOutputStream = new FileOutputStream(javaClassFile))
            {
                fileOutputStream.write(javaClass.source.getBytes(StandardCharsets.UTF_8));
            }
        }
    }
}