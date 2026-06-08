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
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

public class TestEMITTestSuiteBuilder
{
    @Test
    void discoversOneContainerPerModel()
    {
        MutableList<String> modelNames = Lists.mutable.fromStream(EMITTestSuiteBuilder.testContainers("emit-models/").map(DynamicNode::getDisplayName)).sortThis();

        // Every discovered yaml becomes exactly one top-level container, named after the model.
        Assertions.assertEquals(
                Lists.fixedSize.with("artifact-generation", "class-simple", "file-generation", "m2m-passing", "model-generation"),
                modelNames,
                () -> "Expected one container per discovered model (artifact-generation, class-simple, file-generation, m2m-passing, model-generation); got: " + modelNames);
    }

    @Test
    void testContainerGroupsByPhase() throws Throwable
    {
        // m2m-passing has both an Initialization phase and a Test phase, so it exercises grouping well.
        // A model is named by the location of its yaml relative to the root, without the .emit.yaml suffix.
        DynamicContainer container = EMITTestSuiteBuilder.testContainer("emit-models/", "basic/m2m-passing");
        Assertions.assertNotNull(container, "expected to find the m2m-passing model");
        Assertions.assertEquals("m2m-passing", container.getDisplayName(), "container should be named after the model descriptor");

        MutableList<DynamicNode> children = childrenOf(container);
        MutableList<String> childNames = children.collect(DynamicNode::getDisplayName);
        Assertions.assertEquals(
                Lists.mutable.with("Load Model Descriptor", "Initialization", "Test"),
                childNames,
                () -> "Expected the model container to group into 'Load Model Descriptor', 'Initialization' and 'Test'; got: " + childNames);

        // The Initialization phase nests the parse/compile/model-generation tasks.
        DynamicContainer initialization = (DynamicContainer) children.detect(n -> (n instanceof DynamicContainer) && "Initialization".equals(n.getDisplayName()));
        MutableList<DynamicNode> initChildren = childrenOf(initialization);
        Assertions.assertEquals(
                Lists.mutable.with("Parsing", "Compilation", "Model Generation"),
                initChildren.collect(DynamicNode::getDisplayName),
                "Initialization container should hold Parsing, Compilation and Model Generation, in order");

        // The Test phase nests one task per atomic test; running them exercises the model end-to-end.
        DynamicContainer testPhase = (DynamicContainer) children.detect(n -> (n instanceof DynamicContainer) && "Test".equals(n.getDisplayName()));
        MutableList<DynamicNode> testChildren = childrenOf(testPhase);
        Assertions.assertEquals(
                Lists.mutable.with("demo::PersonM2MMapping / fullNameSuite / johnSmith", "demo::PersonM2MMapping / fullNameSuite / janeDoe"),
                testChildren.collect(DynamicNode::getDisplayName),
                "Test container should hold one task per atomic test");
        executeAll(initChildren);
        executeAll(testChildren);
    }

    @Test
    void testContainerReturnsNullForUnknownModel()
    {
        Assertions.assertNull(EMITTestSuiteBuilder.testContainer("emit-models/", "basic/does-not-exist"),
                "testContainer should return null when the named model is absent");
    }

    @Test
    void testsForNamedModelMatchesFilteredDiscovery()
    {
        // Looking a model up by name should yield exactly the same flattened tasks (and order) as
        // discovering every model and filtering down to that one. This is the equivalence that lets
        // the rest of these tests address a single model by name rather than filtering discovery.
        MutableList<DynamicTest> named = tasksFor("emit-models/", "basic/m2m-passing");
        MutableList<DynamicTest> filtered = Lists.mutable.fromStream(
                EMITTestSuiteBuilder.tests("emit-models/").filter(t -> t.getDisplayName().startsWith("[m2m-passing] ")));

        Assertions.assertFalse(named.isEmpty(), "expected the named lookup to find m2m-passing");
        Assertions.assertEquals(
                filtered.collect(DynamicTest::getDisplayName),
                named.collect(DynamicTest::getDisplayName),
                "tests(root, name) should match the m2m-passing slice of full discovery");
    }

    @Test
    void classSimpleYieldsOnlyTheInitializationPhase() throws Throwable
    {
        MutableList<DynamicTest> tasks = tasksFor("emit-models/", "basic/class-simple");

        // class-simple has no Testable elements, no Services, and its demo::Person class is outside the
        // demo::artifactgen:: package the test artifact generator fires on, so it yields exactly the
        // descriptor load and the initialization phase — no Test, Plan, file- or artifact-generation tasks.
        Assertions.assertEquals(
                Lists.mutable.with(
                        "[class-simple] Load Model Descriptor",
                        "[class-simple] Initialization: Parsing",
                        "[class-simple] Initialization: Compilation",
                        "[class-simple] Initialization: Model Generation"),
                tasks.collect(DynamicTest::getDisplayName),
                () -> "class-simple should yield only the load + initialization-phase tasks; got:\n" + names(tasks));
        executeAll(tasks);
    }

    @Test
    void m2mPassingYieldsOneTaskPerAtomicTest() throws Throwable
    {
        MutableList<DynamicTest> tasks = tasksFor("emit-models/", "basic/m2m-passing");

        // The load + initialization phase, followed by one Test task per atomic test in the suite.
        Assertions.assertEquals(
                Lists.mutable.with(
                        "[m2m-passing] Load Model Descriptor",
                        "[m2m-passing] Initialization: Parsing",
                        "[m2m-passing] Initialization: Compilation",
                        "[m2m-passing] Initialization: Model Generation",
                        "[m2m-passing] Test: demo::PersonM2MMapping / fullNameSuite / johnSmith",
                        "[m2m-passing] Test: demo::PersonM2MMapping / fullNameSuite / janeDoe"),
                tasks.collect(DynamicTest::getDisplayName),
                () -> "m2m-passing should yield the load + init phase plus one task per atomic test; got:\n" + names(tasks));

        executeAll(tasks);
    }

    @Test
    void m2mPassingExecutingASingleAtomicTaskPasses() throws Throwable
    {
        // A single atomic-test DynamicTest must run cleanly on its own — the
        // per-group LazyTestSession has to open the session on first use rather
        // than relying on a previous task to have done so. Without that
        // invariant, selecting and running one atomic test from an IDE would
        // fail; this regression-guards the lazy-open path independently of the
        // bulk-execution path that {@link #m2mPassingYieldsOneTaskPerAtomicTest}
        // covers.
        MutableList<DynamicTest> tasks = tasksFor("emit-models/", "basic/m2m-passing");
        DynamicTest atomic = tasks.detect(t -> "[m2m-passing] Test: demo::PersonM2MMapping / fullNameSuite / johnSmith".equals(t.getDisplayName()));
        Assertions.assertNotNull(atomic, () -> "expected to find the johnSmith atomic-test task; got:\n" + names(tasks));
        atomic.getExecutable().execute();
    }

    @Test
    void m2mPassingAtomicTaskCanBeExecutedAcrossDistinctStreamConstructions() throws Throwable
    {
        // Re-discovering the m2m-passing model from scratch must yield a fresh
        // LazyTestSession each time, so executing the same atomic test against
        // two independently-built streams must both succeed — i.e. the close
        // on a prior session leaves no residual static state that breaks the
        // next open. Pairs with {@code TestEMITTaskSessions.reopeningASession…}
        // at the underlying TestSuiteSession layer.
        MutableList<DynamicTest> firstStream = tasksFor("emit-models/", "basic/m2m-passing");
        DynamicTest firstJohn = firstStream.detect(t -> t.getDisplayName().endsWith("johnSmith"));
        Assertions.assertNotNull(firstJohn, "expected johnSmith in the first stream");
        firstJohn.getExecutable().execute();

        MutableList<DynamicTest> secondStream = tasksFor("emit-models/", "basic/m2m-passing");
        DynamicTest secondJane = secondStream.detect(t -> t.getDisplayName().endsWith("janeDoe"));
        Assertions.assertNotNull(secondJane, "expected janeDoe in the second stream");
        secondJane.getExecutable().execute();
    }

    @Test
    void m2mPassingAtomicTasksAreGroupedAdjacentlyByTestableAndSuite()
    {
        // The session-grouped stream emits all atomic tests for a given
        // (testable, suite) consecutively so the per-group LazyTestSession can
        // amortize plan generation across them and so Stream#onClose can
        // release the session exactly once at the end of the group.
        // Interleaving tasks across suites would defeat both. m2m-passing has
        // only one suite, so this regression-guards by checking the two
        // atomic-test entries are immediate neighbors in the stream output.
        MutableList<DynamicTest> tasks = tasksFor("emit-models/", "basic/m2m-passing");
        MutableList<String> names = tasks.collect(DynamicTest::getDisplayName);
        int john = names.indexOf("[m2m-passing] Test: demo::PersonM2MMapping / fullNameSuite / johnSmith");
        int jane = names.indexOf("[m2m-passing] Test: demo::PersonM2MMapping / fullNameSuite / janeDoe");
        Assertions.assertTrue(john >= 0 && jane >= 0, () -> "expected both atomic-test tasks; got:\n" + names(tasks));
        Assertions.assertEquals(1, Math.abs(john - jane),
                () -> "atomic tests in the same suite must be emitted consecutively so the per-group session amortizes and closes correctly; got positions " + john + " and " + jane);
    }

    @Test
    void modelGenerationYieldsOnlyTheInitializationPhase() throws Throwable
    {
        MutableList<DynamicTest> tasks = tasksFor("emit-models/", "basic/model-generation");

        // The JUnit integration does not itself run model generation, so a model that only declares
        // a GenerationSpecification yields exactly the descriptor load plus the initialization phase.
        Assertions.assertEquals(
                Lists.mutable.with(
                        "[model-generation] Load Model Descriptor",
                        "[model-generation] Initialization: Parsing",
                        "[model-generation] Initialization: Compilation",
                        "[model-generation] Initialization: Model Generation"),
                tasks.collect(DynamicTest::getDisplayName),
                () -> "model-generation should yield only the load + initialization-phase tasks; got:\n" + names(tasks));

        executeAll(tasks);
    }

    @Test
    void fileGenerationYieldsOneFileGenTask() throws Throwable
    {
        MutableList<DynamicTest> tasks = tasksFor("emit-models/", "basic/file-generation");

        // The load + initialization phase, followed by exactly one file-generation task (and no
        // artifact-generation task, since the model is outside the demo::artifactgen:: package).
        Assertions.assertEquals(
                Lists.mutable.with(
                        "[file-generation] Load Model Descriptor",
                        "[file-generation] Initialization: Parsing",
                        "[file-generation] Initialization: Compilation",
                        "[file-generation] Initialization: Model Generation",
                        "[file-generation] File Generation: demo::filegen::PersonFileGen"),
                tasks.collect(DynamicTest::getDisplayName),
                () -> "file-generation should yield the load + init phase plus one file-generation task; got:\n" + names(tasks));

        executeAll(tasks);
    }

    @Test
    void artifactGenerationYieldsOneArtifactTaskPerClass() throws Throwable
    {
        MutableList<DynamicTest> tasks = tasksFor("emit-models/", "basic/artifact-generation");

        // The load + initialization phase, followed by one artifact-generation task per class (in
        // model-definition order: Person then Firm), and no file-generation task.
        Assertions.assertEquals(
                Lists.mutable.with(
                        "[artifact-generation] Load Model Descriptor",
                        "[artifact-generation] Initialization: Parsing",
                        "[artifact-generation] Initialization: Compilation",
                        "[artifact-generation] Initialization: Model Generation",
                        "[artifact-generation] Artifact Generation: demo::artifactgen::Person (emit-demo-artifact)",
                        "[artifact-generation] Artifact Generation: demo::artifactgen::Firm (emit-demo-artifact)"),
                tasks.collect(DynamicTest::getDisplayName),
                () -> "artifact-generation should yield the load + init phase plus one artifact task per class; got:\n" + names(tasks));

        executeAll(tasks);
    }

    @Test
    void compileFailureYieldsFailingCompilationTask() throws Throwable
    {
        MutableList<DynamicTest> tasks = tasksFor("emit-models-failure/", "compile-failure");

        // PARSE succeeds but COMPILE fails, so initialization stops at Compilation and no later
        // phases are emitted. The descriptor load and parse pass; compilation surfaces the failure.
        Assertions.assertEquals(
                Lists.mutable.with(
                        "[compile-failure] Load Model Descriptor",
                        "[compile-failure] Initialization: Parsing",
                        "[compile-failure] Initialization: Compilation"),
                tasks.collect(DynamicTest::getDisplayName),
                () -> "compile-failure should yield exactly the load, parse and failing-compile tasks; got:\n" + names(tasks));

        // Load Model Descriptor and Parsing must pass; Compilation is where the failure surfaces.
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
    void testsForNamedModelsPreservesGivenOrder() throws Throwable
    {
        // The varargs overload concatenates the named models' tasks in the order requested.
        MutableList<DynamicTest> tasks = Lists.mutable.fromStream(EMITTestSuiteBuilder.tests("emit-models/", "basic/model-generation", "basic/class-simple"));

        Assertions.assertEquals(
                Lists.mutable.with(
                        "[model-generation] Load Model Descriptor",
                        "[model-generation] Initialization: Parsing",
                        "[model-generation] Initialization: Compilation",
                        "[model-generation] Initialization: Model Generation",
                        "[class-simple] Load Model Descriptor",
                        "[class-simple] Initialization: Parsing",
                        "[class-simple] Initialization: Compilation",
                        "[class-simple] Initialization: Model Generation"),
                tasks.collect(DynamicTest::getDisplayName),
                () -> "tests(root, names...) should concatenate the named models' tasks in the order requested; got:\n" + names(tasks));
        executeAll(tasks);
    }

    @Test
    void testContainersForNamedModelsPreservesGivenOrder()
    {
        MutableList<String> names = Lists.mutable.fromStream(
                EMITTestSuiteBuilder.testContainers("emit-models/", "basic/class-simple", "basic/model-generation").map(DynamicNode::getDisplayName));
        Assertions.assertEquals(
                Lists.mutable.with("class-simple", "model-generation"),
                names,
                "testContainers(root, names...) should return one container per name, in order");
    }

    @Test
    void namedOverloadsAcceptIterables() throws Throwable
    {
        List<String> requested = Lists.mutable.with("basic/class-simple", "basic/model-generation");

        MutableList<String> containerNames = Lists.mutable.fromStream(
                EMITTestSuiteBuilder.testContainers("emit-models/", requested).map(DynamicNode::getDisplayName));
        Assertions.assertEquals(Lists.mutable.with("class-simple", "model-generation"), containerNames,
                "testContainers(root, Iterable) should return one container per name, in iteration order");

        MutableList<DynamicTest> tasks = Lists.mutable.fromStream(EMITTestSuiteBuilder.tests("emit-models/", requested));
        Assertions.assertEquals(
                Lists.mutable.with(
                        "[class-simple] Load Model Descriptor",
                        "[class-simple] Initialization: Parsing",
                        "[class-simple] Initialization: Compilation",
                        "[class-simple] Initialization: Model Generation",
                        "[model-generation] Load Model Descriptor",
                        "[model-generation] Initialization: Parsing",
                        "[model-generation] Initialization: Compilation",
                        "[model-generation] Initialization: Model Generation"),
                tasks.collect(DynamicTest::getDisplayName),
                () -> "tests(root, Iterable) should yield the named models' tasks in iteration order; got:\n" + names(tasks));
        executeAll(tasks);
    }

    @Test
    void namedLookupThrowsForUnknownModel()
    {
        // tests(root, name) resolves the model eagerly, so a missing name throws on the call itself.
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> EMITTestSuiteBuilder.tests("emit-models/", "basic/does-not-exist"),
                "tests(root, name) should throw for an unknown model");

        // The stream-producing overloads resolve lazily, so the failure surfaces on consumption.
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> EMITTestSuiteBuilder.testContainers("emit-models/", "basic/does-not-exist").forEach(c ->
                {
                    // do nothing
                }),
                "testContainers(root, names...) should throw for an unknown model when consumed");
    }

    @Test
    void deprecatedMethodsDelegateToTests()
    {
        // taskStream/taskList are retained only as deprecated shims over tests(...); confirm they
        // still produce the same flattened sequence so existing callers are not broken.
        List<String> viaTests = EMITTestSuiteBuilder.tests("emit-models-failure/").map(DynamicTest::getDisplayName).collect(Collectors.toList());
        List<String> viaTaskStream = EMITTestSuiteBuilder.taskStream("emit-models-failure/").map(DynamicTest::getDisplayName).collect(Collectors.toList());
        List<String> viaTaskList = ListIterate.collect(EMITTestSuiteBuilder.taskList("emit-models-failure/"), DynamicTest::getDisplayName);

        Assertions.assertEquals(viaTests, viaTaskStream, "taskStream should delegate to tests");
        Assertions.assertEquals(viaTests, viaTaskList, "taskList should delegate to tests");
    }

    private static MutableList<DynamicTest> tasksFor(String classpathRoot, String name)
    {
        return Lists.mutable.fromStream(EMITTestSuiteBuilder.tests(classpathRoot, name));
    }

    // DynamicContainer#getChildren may only be consumed once, so materialise it eagerly.
    private static MutableList<DynamicNode> childrenOf(DynamicContainer container)
    {
        return Lists.mutable.fromStream(container.getChildren());
    }

    private static void executeAll(Iterable<? extends DynamicNode> nodes) throws Throwable
    {
        Deque<DynamicNode> deque = new ArrayDeque<>();
        nodes.forEach(deque::addLast);
        while (!deque.isEmpty())
        {
            DynamicNode node = deque.removeFirst();
            if (node instanceof DynamicContainer)
            {
                ((DynamicContainer) node).getChildren().forEach(deque::addLast);
            }
            else
            {
                ((DynamicTest) node).getExecutable().execute();
            }
        }
    }

    private static String names(List<DynamicTest> tasks)
    {
        StringBuilder sb = new StringBuilder();
        tasks.forEach(t -> sb.append("  - ").append(t.getDisplayName()).append('\n'));
        return sb.toString();
    }
}
