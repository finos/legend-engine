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

package org.finos.legend.engine.ide.api.execution.test;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.eclipse.collections.impl.utility.StringIterate;
import org.finos.legend.engine.ide.api.execution.test.TestNode;
import org.finos.legend.engine.ide.session.PureSession;
import org.finos.legend.engine.ide.session.SimpleFunction;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.m3.execution.test.TestRunner;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorageNode;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.vcs.MutableVersionControlledCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.tools.ListHelper;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class TestRun implements SimpleFunction
{
    private ExecutorService executorService;
    private final AtomicInteger nextTestId = new AtomicInteger(1);

    public TestRun(ExecutorService executorService)
    {
        this.executorService = executorService;
    }

    @Override
    public void run(PureSession pureSession, JSONObject extraParams, JSONArray modifiedFiles, HttpServletResponse response, OutputStream outputStream) throws Exception
    {
        PureRuntime runtime = pureSession.getPureRuntime();
        runtime.compile();

        final int newId = this.nextTestId.getAndIncrement();

        String path = getPath(extraParams);
        String[] filterPaths = getFilterPaths(extraParams);
        boolean relevantTestsOnly = getRelevantTestsOnly(extraParams);
        Predicate<? super CoreInstance> filterPredicate = getFilterPredicate(runtime, relevantTestsOnly);

        TestCollection collection = getTestCollection(pureSession, path, filterPaths, filterPredicate);
        TestRunner runner = pureSession.newTestRunner(newId, collection);

        this.executorService.execute(runner);

        org.finos.legend.engine.ide.api.execution.test.TestNode root = new org.finos.legend.engine.ide.api.execution.test.TestNode(runtime.getCoreInstance("::"));

        MutableList<Pair<CoreInstance, String>> tests = runner.getTestCollection().getAllTestFunctionsWithParameterizations(false);
        tests.sortThisBy(t -> PackageableElement.getUserPathForPackageableElement(t.getOne()));

        for (Pair<CoreInstance, String> test : tests)
        {
            ListIterable<CoreInstance> nodePath = PackageableElement.getUserObjectPathForPackageableElement(test.getOne());
            org.finos.legend.engine.ide.api.execution.test.TestNode node = root;
            for (CoreInstance element : ListHelper.tail(nodePath))
            {
                node = node.getOrCreateChild(element, element == nodePath.getLast() ? test.getTwo() : null);
            }
        }

        outputStream.write("{\"runnerId\":".getBytes());
        outputStream.write(Integer.toString(newId).getBytes());
        outputStream.write((",\"path\":\"" + path + "\"").getBytes());
        outputStream.write((",\"filterPaths\":" + (filterPaths.length == 0 ? "[]" : ArrayIterate.makeString(filterPaths, "[\"", "\",\"", "\"]"))).getBytes());
        outputStream.write((",\"relevantTestsOnly\":" + relevantTestsOnly).getBytes());
        outputStream.write(",\"count\":".getBytes());
        outputStream.write(Integer.toString(tests.size()).getBytes());
        outputStream.write(",\"tests\":".getBytes());
        outputStream.write(root.getChildren().asLazy().collect(new Function<org.finos.legend.engine.ide.api.execution.test.TestNode, String>()
        {
            @Override
            public String valueOf(TestNode testNode)
            {
                return testNode.toJson(newId);
            }
        }).makeString("[", ",", "]").getBytes());
        outputStream.write((",\"cached\":" + runtime.getCache().getCacheState().isCached()).getBytes());
        outputStream.write("}".getBytes());
        outputStream.close();
    }

    private String getPath(JSONObject extraParams)
    {
        String path = (String)extraParams.get("path");
        return StringIterate.isEmpty(path) ? "::" : path;
    }

    private String[] getFilterPaths(JSONObject extraParams)
    {
        JSONArray filterPaths = (JSONArray)extraParams.get("filterPaths");
        if (filterPaths != null)
        {
            String[] paths = new String[filterPaths.size()];
            filterPaths.toArray(paths);
            return paths;
        }
        return new String[0];
    }

    private boolean getRelevantTestsOnly(JSONObject extraParams)
    {
        Boolean relevantTestsOnly = (Boolean)extraParams.get("relevantTestsOnly");
        return (relevantTestsOnly == null) ? false : relevantTestsOnly;
    }

    private Predicate<? super CoreInstance> getFilterPredicate(PureRuntime runtime, boolean relevantTestsOnly)
    {
        if (!relevantTestsOnly)
        {
            return null;
        }

        final MutableVersionControlledCodeStorage codeStorage = (MutableVersionControlledCodeStorage)runtime.getCodeStorage();
        RichIterable<CodeStorageNode> modifiedUserFiles = codeStorage.getModifiedUserFiles();
        if (modifiedUserFiles.isEmpty())
        {
            return Predicates.alwaysFalse();
        }

        MutableSet<String> repos = Sets.mutable.empty();
        for (CodeStorageNode node : modifiedUserFiles)
        {
            repos.add(codeStorage.getRepositoryForPath(node.getPath()).getName());
        }
        if (repos.isEmpty())
        {
            return Predicates.alwaysFalse();
        }

        final SetIterable<String> reposPlusDependents = CompositeCodeStorage.getRepositoriesDependendingOnByName(codeStorage.getAllRepositories(), repos);
        return new Predicate<CoreInstance>()
        {
            @Override
            public boolean accept(CoreInstance test)
            {
                return reposPlusDependents.contains(codeStorage.getRepositoryForPath(test.getSourceInformation().getSourceId()).getName());
            }
        };
    }

    private TestCollection getTestCollection(PureSession session, String path, String[] filterPaths, Predicate<? super CoreInstance> filterPredicate)
    {
        PureRuntime runtime = session.getPureRuntime();
        FunctionExecution functionExecution = session.getFunctionExecution();
        CoreInstance coreInstance = runtime.getCoreInstance(path);
        ProcessorSupport processorSupport = runtime.getProcessorSupport();

        CoreInstance pkg;
        Predicate<? super CoreInstance> singleTestFilter;
        Predicate<? super CoreInstance> pathFilter = Predicates.alwaysTrue();

        if (Instance.instanceOf(coreInstance, M3Paths.ConcreteFunctionDefinition, processorSupport))
        {
            pkg = Instance.getValueForMetaPropertyToOneResolved(coreInstance, M3Properties._package, processorSupport);
            singleTestFilter = Predicates.sameAs(coreInstance);
        }
        else
        {
            pkg = coreInstance;
            singleTestFilter = Predicates.alwaysTrue();
        }

        if (filterPaths.length > 0)
        {
            pathFilter = Predicates.alwaysFalse();
            for (String filterPath : filterPaths)
            {
                pathFilter = Predicates.or(pathFilter, Predicates.attributeEqual(e -> e.getValueForMetaPropertyToOne(M3Properties._package), runtime.getCoreInstance(filterPath)));
            }
        }

        Predicate<CoreInstance> funcExecPredicate = TestCollection.getFilterPredicateForExecutionPlatformClass(session.getFunctionExecution().getClass(), processorSupport);
        Predicate<? super CoreInstance> predicate = (filterPredicate == null) ? funcExecPredicate : Predicates.and(funcExecPredicate, filterPredicate);
        Predicate<? super CoreInstance> alloyTextModeExclusionPredicate = TestCollection.getFilterPredicateForAlloyTextModeExclusion(processorSupport);
        predicate = Predicates.and(predicate, singleTestFilter, pathFilter, alloyTextModeExclusionPredicate);
        return TestCollection.collectTests(pkg, processorSupport, fn -> TestCollection.collectTestsFromPure(fn, functionExecution), predicate);
    }
}
