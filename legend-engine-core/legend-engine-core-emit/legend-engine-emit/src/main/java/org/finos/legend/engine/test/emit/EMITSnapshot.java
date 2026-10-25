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

package org.finos.legend.engine.test.emit;

import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedClassResolver;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtensionLoader;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestDebug;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestExecutionPlanDebug;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.testable.extension.TestRunner;
import org.finos.legend.engine.testable.extension.TestableRunnerExtensionLoader;
import org.finos.legend.pure.generated.Root_meta_pure_test_AtomicTest;
import org.finos.legend.pure.generated.Root_meta_pure_test_TestSuite;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.testable.Testable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Opt-in record of what the engine produced for one EMIT model, so that two engine revisions can be
 * compared over the same corpus. Enabled by the {@value #DIRECTORY_PROPERTY} system property; every
 * file lands under {@code <dir>/<suite>/<model>/}:
 * <ul>
 *   <li>{@code model.json}: parse and compile outcome, and the primary source ids</li>
 *   <li>{@code pmcd.json}: the parsed {@link PureModelContextData}, as the engine serializes it</li>
 *   <li>{@code pmcd.roundtrip.json}: that JSON read back and re-serialized by the same engine, the
 *       baseline a cross-read is compared against</li>
 *   <li>{@code crossread.json} and {@code pmcd.crossread.json}: when {@value #CROSS_READ_PROPERTY}
 *       names another snapshot directory, that snapshot's {@code pmcd.json} deserialized,
 *       re-serialized and compiled by this engine</li>
 *   <li>{@code plans/service/<service>.json} and {@code plans/test/<testable>/<suite>/<test>.json}:
 *       the execution plan of each service and of each test its runner can debug (for a test that
 *       runs no plan, such as persistence, the statements it would issue), or the error that
 *       prevented it</li>
 * </ul>
 * Errors are recorded without stack frames, so that line numbers do not differ between revisions.
 */
public final class EMITSnapshot
{
    public static final String DIRECTORY_PROPERTY = "legend.emit.snapshot.dir";
    public static final String CROSS_READ_PROPERTY = "legend.emit.snapshot.crossRead";

    private static final ObjectMapper MAPPER = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    private static final Set<Path> WRITTEN_SUBTYPE_FILES = ConcurrentHashMap.newKeySet();

    private final Path directory;
    private final Path crossReadDirectory;

    private EMITSnapshot(Path directory, Path crossReadDirectory)
    {
        this.directory = directory;
        this.crossReadDirectory = crossReadDirectory;
    }

    /**
     * @return the snapshot of the given model, or {@code null} when snapshots are not enabled
     */
    public static EMITSnapshot forModel(String suite, String model)
    {
        String root = System.getProperty(DIRECTORY_PROPERTY);
        if ((root == null) || root.isEmpty())
        {
            return null;
        }
        Path relative = Paths.get(fileName(suite.replaceAll("^/+|/+$", "")), fileName(model));
        String crossReadRoot = System.getProperty(CROSS_READ_PROPERTY);
        return new EMITSnapshot(Paths.get(root).resolve(relative), ((crossReadRoot == null) || crossReadRoot.isEmpty()) ? null : Paths.get(crossReadRoot).resolve(relative));
    }

    public boolean hasCrossRead()
    {
        return this.crossReadDirectory != null;
    }

    /**
     * Writes {@code <dir>/<suite>/_subtypes.json} once per suite: every {@code _type} name each
     * polymorphic protocol base resolves, from annotations and extension registrations alike.
     */
    public void writeProtocolSubtypes()
    {
        Path file = this.directory.getParent().resolve("_subtypes.json");
        if (!WRITTEN_SUBTYPE_FILES.add(file))
        {
            return;
        }
        Set<Class<?>> bases = new LinkedHashSet<>(Arrays.asList(PureModelContext.class, PackageableElement.class, ValueSpecification.class, ExecutionNode.class));
        PureProtocolExtensionLoader.extensions().forEach(extension ->
                extension.getExtraProtocolSubTypeInfoCollectors().forEach(collector ->
                        collector.value().forEach(info -> bases.add(info.getSuperType()))));
        DeserializationConfig config = MAPPER.getDeserializationConfig();
        Map<String, Map<String, Set<String>>> registry = new TreeMap<>();
        bases.forEach(base ->
        {
            Map<String, Set<String>> names = registry.computeIfAbsent(base.getName(), k -> new TreeMap<>());
            MAPPER.getSubtypeResolver()
                    .collectAndResolveSubtypesByTypeId(config, AnnotatedClassResolver.resolveWithoutSuperTypes(config, base))
                    .stream()
                    .filter(NamedType::hasName)
                    .forEach(named -> names.computeIfAbsent(named.getName(), k -> new TreeSet<>()).add(named.getType().getName()));
        });
        write(this.directory.relativize(file), registry);
    }

    public void writeModel(EMITTasks.ParseResult parseResult, Throwable parseFailure, Throwable compileFailure)
    {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("parse", outcome(parseFailure));
        if (parseResult != null)
        {
            model.put("compile", outcome(compileFailure));
            model.put("primarySourceIds", parseResult.getPrimarySourceIds().toSortedList());
            write(Paths.get("pmcd.json"), parseResult.getPmcd());
            Throwable roundTripFailure = null;
            try
            {
                write(Paths.get("pmcd.roundtrip.json"), MAPPER.readValue(MAPPER.writeValueAsBytes(parseResult.getPmcd()), PureModelContextData.class));
            }
            catch (IOException e)
            {
                roundTripFailure = e;
            }
            model.put("roundTrip", outcome(roundTripFailure));
        }
        write(Paths.get("model.json"), model);
    }

    public void crossRead()
    {
        Map<String, Object> result = new LinkedHashMap<>();
        Path source = this.crossReadDirectory.resolve("pmcd.json");
        if (Files.notExists(source))
        {
            result.put("deserialize", error("missing", "no pmcd.json in " + this.crossReadDirectory));
            write(Paths.get("crossread.json"), result);
            return;
        }
        PureModelContextData pmcd;
        try
        {
            pmcd = MAPPER.readValue(source.toFile(), PureModelContextData.class);
        }
        catch (Exception e)
        {
            result.put("deserialize", outcome(e));
            write(Paths.get("crossread.json"), result);
            return;
        }
        result.put("deserialize", outcome(null));
        write(Paths.get("pmcd.crossread.json"), pmcd);
        Throwable compileFailure = null;
        try
        {
            EMITTasks.compile(pmcd);
        }
        catch (AssertionError | Exception e)
        {
            compileFailure = e;
        }
        result.put("compile", outcome(compileFailure));
        write(Paths.get("crossread.json"), result);
    }

    public void writeServicePlan(Service service, PureModel pureModel)
    {
        Map<String, Object> entry;
        try
        {
            entry = planEntry(EMITTasks.runPlan(service, pureModel));
        }
        catch (AssertionError | Exception e)
        {
            entry = outcome(e);
        }
        write(Paths.get("plans", "service", fileName(service.getPath()) + ".json"), entry);
    }

    public void writeTestPlans(String testablePath, String suiteId, List<String> atomicTestIds, PureModelContextData pmcd, PureModel pureModel)
    {
        Testable testable = (Testable) pureModel.getPackageableElement(testablePath);
        TestRunner runner = TestableRunnerExtensionLoader.forTestable(testable);
        List<TestDebug> debugs = new ArrayList<>();
        try
        {
            if (suiteId == null)
            {
                testable._tests()
                        .select(t -> (t instanceof Root_meta_pure_test_AtomicTest) && atomicTestIds.contains(t._id()))
                        .forEach(t -> debugs.add(runner.debugAtomicTest((Root_meta_pure_test_AtomicTest) t, pureModel, pmcd)));
            }
            else
            {
                Root_meta_pure_test_TestSuite suite = (Root_meta_pure_test_TestSuite) testable._tests().detect(t -> (t instanceof Root_meta_pure_test_TestSuite) && suiteId.equals(t._id()));
                debugs.addAll(runner.debugTestSuite(suite, atomicTestIds, pureModel, pmcd));
            }
        }
        catch (Exception e)
        {
            Map<String, Object> failure = outcome(e);
            atomicTestIds.forEach(id -> write(testPlanPath(testablePath, suiteId, id), failure));
            return;
        }
        debugs.stream()
                .filter(debug -> debug != null)
                .forEach(debug -> write(testPlanPath(testablePath, suiteId, debug.atomicTestId), debugEntry(debug)));
    }

    private static Path testPlanPath(String testablePath, String suiteId, String atomicTestId)
    {
        return Paths.get("plans", "test", fileName(testablePath), (suiteId == null) ? "_" : fileName(suiteId), fileName(atomicTestId) + ".json");
    }

    private static Map<String, Object> debugEntry(TestDebug debug)
    {
        if (debug.error != null)
        {
            return error("error", withoutStackFrames(debug.error));
        }
        if (debug instanceof TestExecutionPlanDebug)
        {
            TestExecutionPlanDebug planDebug = (TestExecutionPlanDebug) debug;
            if (planDebug.executionPlan != null)
            {
                return planEntry(planDebug.executionPlan);
            }
            if ((planDebug.debug != null) && !planDebug.debug.isEmpty())
            {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("status", "ok");
                entry.put("statements", planDebug.debug);
                return entry;
            }
        }
        return error("unsupported", debug.getClass().getName());
    }

    private static Map<String, Object> planEntry(ExecutionPlan plan)
    {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("status", "ok");
        entry.put("plan", plan);
        return entry;
    }

    private static Map<String, Object> outcome(Throwable failure)
    {
        if (failure == null)
        {
            Map<String, Object> ok = new LinkedHashMap<>();
            ok.put("status", "ok");
            return ok;
        }
        StringBuilder message = new StringBuilder();
        for (Throwable t = failure; t != null; t = (t.getCause() == t) ? null : t.getCause())
        {
            message.append((message.length() == 0) ? "" : "\nCaused by: ").append(t);
        }
        return error("error", message.toString());
    }

    private static Map<String, Object> error(String status, String message)
    {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("status", status);
        entry.put("message", message);
        return entry;
    }

    private static String withoutStackFrames(String stackTrace)
    {
        return Arrays.stream(stackTrace.split("\\R")).filter(line -> !line.startsWith("\t")).collect(Collectors.joining("\n"));
    }

    private static String fileName(String name)
    {
        return name.replace("::", ".").replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private void write(Path relative, Object value)
    {
        Path file = this.directory.resolve(relative);
        try
        {
            Files.createDirectories(file.getParent());
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), value);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("Failed to write EMIT snapshot file " + file, e);
        }
    }
}
