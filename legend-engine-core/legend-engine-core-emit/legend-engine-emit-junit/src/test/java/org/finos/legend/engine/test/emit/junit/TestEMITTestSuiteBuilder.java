// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.test.emit.junit;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.test.emit.error.EMITAssertionError;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

public class TestEMITTestSuiteBuilder
{
    @Test
    void buildTasksDiscoversAllYamlsUnderRoot()
    {
        List<DynamicTest> tasks = EMITTestSuiteBuilder.taskList("emit-models/");

        // An Initialization task is added for every discovered yaml regardless of whether
        // its downstream phases succeed, so it is the most accurate per-model marker.
        int initTasks = ListIterate.count(tasks, t -> t.getDisplayName().endsWith("] Initialization"));
        Assertions.assertEquals(5, initTasks,
                () -> "Expected one Initialization task per discovered model (artifact-generation, class-simple, file-generation, m2m-passing, model-generation); got names:\n" + names(tasks));
    }

    @Test
    void classSimpleYieldsOnlyAnInitTask() throws Throwable
    {
        MutableList<DynamicTest> tasks = tasksForLabel("emit-models/", "class-simple");

        // class-simple has no Testable elements and no Services; the only mandatory
        // task is Initialization. Artifact generators registered on the classpath
        // could in principle yield additional Artifact Generation tasks for the
        // demo::Person class — assert the init task exists, and assert the
        // ones that do exist all execute successfully.
        Assertions.assertTrue(tasks.anySatisfy(t -> "[class-simple] Initialization".equals(t.getDisplayName())),
                () -> "Missing Initialization task; got:\n" + names(tasks));
        Assertions.assertTrue(tasks.anySatisfy(t -> "[class-simple] Parsing".equals(t.getDisplayName())),
                () -> "Missing Parsing task; got:\n" + names(tasks));
        Assertions.assertTrue(tasks.anySatisfy(t -> "[class-simple] Compilation".equals(t.getDisplayName())),
                () -> "Missing Compilation task; got:\n" + names(tasks));
        Assertions.assertTrue(tasks.anySatisfy(t -> "[class-simple] Model Generation".equals(t.getDisplayName())),
                () -> "Missing Model Generation task; got:\n" + names(tasks));
        Assertions.assertTrue(tasks.noneSatisfy(t -> t.getDisplayName().contains("] Test:")),
                () -> "class-simple should not produce any Test tasks; got:\n" + names(tasks));
        Assertions.assertTrue(tasks.noneSatisfy(t -> t.getDisplayName().contains("] Plan:")),
                () -> "class-simple should not produce any Plan tasks; got:\n" + names(tasks));
        for (DynamicTest task : tasks)
        {
            task.getExecutable().execute();
        }
    }

    @Test
    void m2mPassingYieldsInitPlusOneTaskPerAtomicTest() throws Throwable
    {
        MutableList<DynamicTest> tasks = tasksForLabel("emit-models/", "m2m-passing");

        MutableList<String> names = tasks.collect(DynamicTest::getDisplayName);
        Assertions.assertTrue(names.contains("[m2m-passing] Initialization"),
                () -> "Missing Initialization task; got: " + names);
        Assertions.assertTrue(names.contains("[m2m-passing] Parsing"),
                () -> "Missing Parsing task; got: " + names);
        Assertions.assertTrue(names.contains("[m2m-passing] Compilation"),
                () -> "Missing Compilation task; got: " + names);
        Assertions.assertTrue(names.contains("[m2m-passing] Model Generation"),
                () -> "Missing Model Generation task; got: " + names);
        Assertions.assertTrue(names.contains("[m2m-passing] Test: demo::PersonM2MMapping / fullNameSuite / johnSmith"),
                () -> "Missing johnSmith test task; got: " + names);
        Assertions.assertTrue(names.contains("[m2m-passing] Test: demo::PersonM2MMapping / fullNameSuite / janeDoe"),
                () -> "Missing janeDoe test task; got: " + names);

        int testTasks = tasks.count(t -> t.getDisplayName().contains("] Test:"));
        Assertions.assertEquals(2, testTasks,
                () -> "Expected exactly 2 Test tasks for m2m-passing; got:\n" + names);

        for (DynamicTest task : tasks)
        {
            task.getExecutable().execute();
        }
    }

    @Test
    void modelGenerationYieldsInitPlusModelGenTask() throws Throwable
    {
        MutableList<DynamicTest> tasks = tasksForLabel("emit-models/", "model-generation");

        Assertions.assertEquals(4, tasks.size(),
                () -> "model-generation should yield exactly four tasks (Initialization, Parsing, Compilation, and Model Generation); got:\n" + names(tasks));
        Assertions.assertEquals("[model-generation] Initialization", tasks.get(0).getDisplayName());
        Assertions.assertEquals("[model-generation] Parsing", tasks.get(1).getDisplayName());
        Assertions.assertEquals("[model-generation] Compilation", tasks.get(2).getDisplayName());
        Assertions.assertEquals("[model-generation] Model Generation", tasks.get(3).getDisplayName());

        for (DynamicTest task : tasks)
        {
            task.getExecutable().execute();
        }
    }

    @Test
    void fileGenerationYieldsInitPlusOneFileGenTask() throws Throwable
    {
        MutableList<DynamicTest> tasks = tasksForLabel("emit-models/", "file-generation");

        MutableList<String> taskNames = tasks.collect(DynamicTest::getDisplayName);
        Assertions.assertTrue(taskNames.contains("[file-generation] Initialization"),
                () -> "Missing Initialization task; got: " + taskNames);
        Assertions.assertTrue(taskNames.contains("[file-generation] Parsing"),
                () -> "Missing Parsing task; got: " + taskNames);
        Assertions.assertTrue(taskNames.contains("[file-generation] Compilation"),
                () -> "Missing Compilation task; got: " + taskNames);
        Assertions.assertTrue(taskNames.contains("[file-generation] Model Generation"),
                () -> "Missing Model Generation task; got: " + taskNames);
        Assertions.assertTrue(taskNames.contains("[file-generation] File Generation: demo::filegen::PersonFileGen"),
                () -> "Missing File Generation task; got: " + taskNames);

        int fileGenTasks = tasks.count(t -> t.getDisplayName().contains("] File Generation:"));
        Assertions.assertEquals(1, fileGenTasks,
                () -> "Expected exactly 1 File Generation task; got:\n" + taskNames);
        Assertions.assertTrue(tasks.noneSatisfy(t -> t.getDisplayName().contains("] Artifact Generation:")),
                () -> "file-generation should not produce any Artifact Generation tasks; got:\n" + taskNames);

        for (DynamicTest task : tasks)
        {
            task.getExecutable().execute();
        }
    }

    @Test
    void artifactGenerationYieldsInitPlusOneArtifactTaskPerClass() throws Throwable
    {
        MutableList<DynamicTest> tasks = tasksForLabel("emit-models/", "artifact-generation");

        MutableList<String> taskNames = tasks.collect(DynamicTest::getDisplayName);
        Assertions.assertTrue(taskNames.contains("[artifact-generation] Initialization"),
                () -> "Missing Initialization task; got: " + taskNames);
        Assertions.assertTrue(taskNames.contains("[artifact-generation] Parsing"),
                () -> "Missing Parsing task; got: " + taskNames);
        Assertions.assertTrue(taskNames.contains("[artifact-generation] Compilation"),
                () -> "Missing Compilation task; got: " + taskNames);
        Assertions.assertTrue(taskNames.contains("[artifact-generation] Model Generation"),
                () -> "Missing Model Generation task; got: " + taskNames);
        Assertions.assertTrue(taskNames.contains("[artifact-generation] Artifact Generation: demo::artifactgen::Person (emit-demo-artifact)"),
                () -> "Missing Person Artifact Generation task; got: " + taskNames);
        Assertions.assertTrue(taskNames.contains("[artifact-generation] Artifact Generation: demo::artifactgen::Firm (emit-demo-artifact)"),
                () -> "Missing Firm Artifact Generation task; got: " + taskNames);

        int artifactTasks = tasks.count(t -> t.getDisplayName().contains("] Artifact Generation:"));
        Assertions.assertEquals(2, artifactTasks,
                () -> "Expected exactly 2 Artifact Generation tasks; got:\n" + taskNames);
        Assertions.assertTrue(tasks.noneSatisfy(t -> t.getDisplayName().contains("] File Generation:")),
                () -> "artifact-generation should not produce any File Generation tasks; got:\n" + taskNames);

        for (DynamicTest task : tasks)
        {
            task.getExecutable().execute();
        }
    }

    @Test
    void compileFailureYieldsFailingCompilationTask() throws Throwable
    {
        List<DynamicTest> tasks = EMITTestSuiteBuilder.taskList("emit-models-failure/");

        Assertions.assertEquals(3, tasks.size(),
                () -> "compile-failure should yield exactly three tasks (Initialization, Parsing, failing Compilation); got:\n" + names(tasks));
        Assertions.assertEquals("[compile-failure] Initialization", tasks.get(0).getDisplayName());
        Assertions.assertEquals("[compile-failure] Parsing", tasks.get(1).getDisplayName());
        Assertions.assertEquals("[compile-failure] Compilation", tasks.get(2).getDisplayName());

        // Initialization and Parsing must pass; Compilation is where the failure surfaces.
        tasks.get(0).getExecutable().execute();
        tasks.get(1).getExecutable().execute();

        DynamicTest compilation = tasks.get(2);
        EMITAssertionError thrown = Assertions.assertThrows(EMITAssertionError.class, () -> compilation.getExecutable().execute(),
                "Expected the Compilation task to throw because the model fails to compile");
        Assertions.assertEquals("FAILURE [Compilation]: COMPILATION error at model.pure[20:12-29]: Can't find type 'demo::DoesNotExist'", thrown.getMessage());
        Assertions.assertInstanceOf(EngineException.class, thrown.getCause());
        Assertions.assertSame(EngineErrorType.COMPILATION, ((EngineException) thrown.getCause()).getErrorType());
    }

    @Test
    void taskStreamEqualsTaskList()
    {
        List<DynamicTest> listTasks = EMITTestSuiteBuilder.taskList("emit-models-failure/");
        List<DynamicTest> streamTasks = EMITTestSuiteBuilder.taskStream("emit-models-failure/").collect(Collectors.toList());
        Assertions.assertEquals(ListIterate.collect(listTasks, DynamicTest::getDisplayName), ListIterate.collect(streamTasks, DynamicTest::getDisplayName));
    }

    private static MutableList<DynamicTest> tasksForLabel(String classpathRoot, String label)
    {
        String prefix = "[" + label + "] ";
        return Lists.mutable.fromStream(EMITTestSuiteBuilder.taskStream(classpathRoot).filter(t -> t.getDisplayName().startsWith(prefix)));
    }

    private static String names(List<DynamicTest> tasks)
    {
        StringBuilder sb = new StringBuilder();
        tasks.forEach(t -> sb.append("  - ").append(t.getDisplayName()).append('\n'));
        return sb.toString();
    }
}
