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

        int initTasks = ListIterate.count(tasks, t -> t.getDisplayName().endsWith("Initialization (Init, Parse & Compile)"));
        Assertions.assertEquals(2, initTasks,
                () -> "Expected one Initialization task per discovered model (class-simple, m2m-passing); got names:\n" + names(tasks));
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
        Assertions.assertTrue(tasks.anySatisfy(t -> "[class-simple] Initialization (Init, Parse & Compile)".equals(t.getDisplayName())),
                () -> "Missing init task; got:\n" + names(tasks));
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
        Assertions.assertTrue(names.contains("[m2m-passing] Initialization (Init, Parse & Compile)"),
                () -> "Missing init task; got: " + names);
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
    void compileFailureYieldsSingleFailingInitTask()
    {
        List<DynamicTest> tasks = EMITTestSuiteBuilder.taskList("emit-models-failure/");

        Assertions.assertEquals(1, tasks.size(),
                () -> "compile-failure should yield exactly one task (failing Init); got:\n" + names(tasks));
        DynamicTest only = tasks.get(0);
        Assertions.assertEquals("[compile-failure] Initialization (Init, Parse & Compile)", only.getDisplayName());

        EMITAssertionError thrown = Assertions.assertThrows(EMITAssertionError.class, () -> only.getExecutable().execute(),
                "Expected the init task to throw because the model fails to compile");
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
