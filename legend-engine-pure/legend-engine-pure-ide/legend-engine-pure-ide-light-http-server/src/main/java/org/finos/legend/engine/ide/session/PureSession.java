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

package org.finos.legend.engine.ide.session;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.ide.SourceLocationConfiguration;
import org.finos.legend.engine.ide.api.execution.test.CallBack;
import org.finos.legend.engine.ide.helpers.response.IDEResponse;
import org.finos.legend.engine.pure.ide.interpreted.debug.FunctionExecutionInterpretedWithDebugSupport;
import org.finos.legend.pure.m3.SourceMutation;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.m3.execution.test.TestRunner;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.welcome.WelcomeCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.*;
import org.finos.legend.pure.m3.statelistener.VoidExecutionActivityListener;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static org.finos.legend.engine.ide.helpers.response.ExceptionTranslation.buildExceptionMessage;

public class PureSession
{
    private final PureRuntime pureRuntime;
    public MutableRepositoryCodeStorage codeStorage;
    private final FunctionExecution functionExecution;
    private final SourceLocationConfiguration sourceLocationConfiguration;
    private final ConcurrentMutableMap<Integer, TestRunnerWrapper> testRunnersById = ConcurrentHashMap.newMap();
    private final AtomicInteger executionCount = new AtomicInteger(0);
    private static final Pattern LINE_SPLITTER = Pattern.compile("^", Pattern.MULTILINE);

    public Message message = new Message("");

    public MutableList<RepositoryCodeStorage> repos;
    private final Map<String, Boolean> pureRuntimeOptions = new ConcurrentHashMap<>();

    private final String PURE_OPTION_PREFIX = "pure.option.";


    public PureSession(SourceLocationConfiguration sourceLocationConfiguration, MutableList<RepositoryCodeStorage> repos)
    {
        this.sourceLocationConfiguration = sourceLocationConfiguration;

        String rootPath = Optional.ofNullable(sourceLocationConfiguration)
                .flatMap(s -> Optional.ofNullable(s.welcomeFileDirectory))
                .orElse(System.getProperty("java.io.tmpdir"));

        this.repos = Lists.mutable.withAll(repos).with(new WelcomeCodeStorage(Paths.get(rootPath)));

        this.functionExecution = new FunctionExecutionInterpretedWithDebugSupport();

        for (String property : System.getProperties().stringPropertyNames())
        {
            if (property.startsWith(PURE_OPTION_PREFIX))
            {
                setPureRuntimeOption(property.substring(PURE_OPTION_PREFIX.length()), Boolean.getBoolean(property));
            }
        }

        this.codeStorage = new CompositeCodeStorage(this.repos.toArray(new RepositoryCodeStorage[0]));
        this.pureRuntime = new PureRuntimeBuilder(this.codeStorage)
                .withMessage(this.message)
                .setUseFastCompiler(true)
                .withOptions(new RuntimeOptions()
                {
                    @Override
                    public boolean isOptionSet(String name)
                    {
                        return getPureRuntimeOption(name);
                    }
                })
                .build();

        this.functionExecution.init(this.pureRuntime, this.message);
        this.codeStorage.initialize(this.message);
    }

    public MutableRepositoryCodeStorage getCodeStorage()
    {
        return this.codeStorage;
    }

    public PureRuntime getPureRuntime()
    {
        return this.pureRuntime;
    }

    public FunctionExecution getFunctionExecution()
    {
        return this.functionExecution;
    }

    public boolean getPureRuntimeOption(String optionName)
    {
        Boolean value = this.pureRuntimeOptions.get(optionName);
        return value != null && value;
    }

    public Map<String, Boolean> getAllPureRuntimeOptions()
    {
        return this.pureRuntimeOptions;
    }

    public void setPureRuntimeOption(String optionName, boolean value)
    {
        this.pureRuntimeOptions.put(optionName, value);
    }

    public TestRunner newTestRunner(int testRunId, TestCollection collection, String pctAdapter)
    {
        TestRunnerWrapper testRunnerWrapper = new TestRunnerWrapper(collection, this.getPureRuntime().executedTestTracker, pctAdapter);
        this.testRunnersById.put(testRunId, testRunnerWrapper);
        return testRunnerWrapper.testRunner;
    }

    public CallBack getTestCallBack(int testRunId)
    {
        TestRunnerWrapper testRunnerWrapper = this.testRunnersById.get(testRunId);
        return (testRunnerWrapper == null) ? null : testRunnerWrapper.callBack;
    }

    public int getTestRunCount()
    {
        return this.testRunnersById.size();
    }

    public TestRunner removeTestRunner(int testRunId)
    {
        TestRunnerWrapper testRunnerWrapper = this.testRunnersById.remove(testRunId);
        return (null == testRunnerWrapper) ? null : testRunnerWrapper.testRunner;
    }

    public void saveFilesAndExecute(HttpServletRequest request, HttpServletResponse response, OutputStream outputStream, SimpleFunction func) throws IOException
    {
        try
        {
            this.executionCount.incrementAndGet();
            JSONObject mainObject = this.saveFiles(request, response);
            SourceMutation sourceMutation = this.getPureRuntime().compile();
            JSONArray array = (mainObject.get("modifiedFiles") != null) ? (JSONArray) mainObject.get("modifiedFiles") : new JSONArray();
            Iterate.addAllIterable(sourceMutation.getModifiedFiles(), array);
            mainObject.put("modifiedFiles", array);
            func.run(this, (JSONObject) mainObject.get("extraParams"), (JSONArray) mainObject.get("modifiedFiles"), response, outputStream);
        }
        catch (Throwable t)
        {
            //todo: refactor this to not need the ByteArrayOutputStream
            ByteArrayOutputStream pureResponse = new ByteArrayOutputStream();
            outputStream.write(exceptionToJson(this, t, pureResponse).getBytes());
            if (t instanceof Error)
            {
                throw (Error) t;
            }
        }
        finally
        {
            this.executionCount.decrementAndGet();
        }
    }

    public void saveOnly(HttpServletRequest request, HttpServletResponse response, OutputStream outputStream, SimpleFunction func) throws IOException
    {
        JSONObject mainObj = null;
        ByteArrayOutputStream pureResponse = new ByteArrayOutputStream();
        try
        {
            this.executionCount.incrementAndGet();
            try
            {
                mainObj = this.saveFiles(request, response);
                if (null != mainObj)
                {
                    //file has been saved
                    JSONArray array = (null != mainObj.get("modifiedFiles")) ? (JSONArray) mainObj.get("modifiedFiles") : new JSONArray();
                    mainObj.put("modifiedFiles", array);

                    JSONObject extraParams = (JSONObject) mainObj.get("extraParams");
                    extraParams.put("saveOutcome", "saved");
                    func.run(this, extraParams, (JSONArray) mainObj.get("modifiedFiles"), response, outputStream);
                }
                else
                {
                    //Encountered Error trying to save
                    JSONObject extraParams = new JSONObject();
                    extraParams.put("saveOutcome", "Error");
                    func.run(this, extraParams, new JSONArray(), response, outputStream);
                }
            }
            catch (Exception e)
            {
                outputStream.write(exceptionToJson(this, e, pureResponse).getBytes());
            }

        }
        catch (Exception e)
        {
            outputStream.write(exceptionToJson(this, e, pureResponse).getBytes());
        }
        finally
        {
            this.executionCount.decrementAndGet();
        }
    }

    public JSONObject saveFiles(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        JSONObject mainObject = null;
        try
        {
            mainObject = (JSONObject) new JSONParser().parse(new InputStreamReader(request.getInputStream()));
            JSONArray openFiles = (JSONArray) mainObject.get("openFiles");
            if ((null != openFiles) && !openFiles.isEmpty())
            {
                PureRuntime pureRuntime = this.getPureRuntime();

                MutableList<JSONObject> diagrams = Lists.mutable.empty();
                for (Object rawOpenFile : openFiles)
                {
                    JSONObject openFile = (JSONObject) rawOpenFile;
                    if (null == openFile.get("diagram"))
                    {
                        String path = (String) openFile.get("path");
                        String code = (String) openFile.get("code");
                        if (null == pureRuntime.getSourceById(path))
                        {
                            pureRuntime.loadSourceIfLoadable(path);
                        }
                        pureRuntime.modify(path, code);
                    }
                    else
                    {
                        diagrams.add(openFile);
                    }
                }

                if (diagrams.notEmpty())
                {
                    JSONArray modifiedFiles = new JSONArray();

                    for (JSONObject d : diagrams)
                    {
                        SourceMutation mutation = pureRuntime.compile();
                        Iterate.addAllIterable(mutation.getModifiedFiles(), modifiedFiles);
                        CoreInstance diagram = pureRuntime.getProcessorSupport().package_getByUserPath(d.get("diagram").toString());
                        if (null != diagram)
                        {
                            SourceInformation sourceInformation = diagram.getSourceInformation();
                            Source source = pureRuntime.getSourceById(sourceInformation.getSourceId());
                            String content = source.getContent();
                            String[] lines = LINE_SPLITTER.split(content);
                            StringBuilder buffer = new StringBuilder(content.length());
                            for (int i = 0; i < sourceInformation.getStartLine() - 1; i++)
                            {
                                buffer.append(lines[i]);
                            }
                            buffer.append(lines[sourceInformation.getStartLine() - 1], 0, diagram.getSourceInformation().getStartColumn() - 1);
                            buffer.append(d.get("code"));
                            buffer.append(lines[sourceInformation.getEndLine() - 1].substring(sourceInformation.getEndColumn()));
                            for (int i = sourceInformation.getEndLine(); i < lines.length; i++)
                            {
                                buffer.append(lines[i]);
                            }
                            pureRuntime.modify(sourceInformation.getSourceId(), buffer.toString());
                            modifiedFiles.add(diagram.getSourceInformation().getSourceId());
                        }
                    }
                    mainObject.put("modifiedFiles", modifiedFiles);
                }
            }
        }
        catch (Exception e)
        {
            try (OutputStream outputStream = response.getOutputStream())
            {
                outputStream.write(exceptionToJson(this, e, null).getBytes());
            }
        }
        return mainObject;
    }

    public int getCurrentExecutionCount()
    {
        return this.executionCount.get();
    }

    public static String exceptionToJson(PureSession session, Throwable t, ByteArrayOutputStream pureResponse)
    {
        IDEResponse response = buildExceptionMessage(session, t, pureResponse);
        return response.toJSONString();
    }

    private class TestRunnerWrapper
    {
        private TestRunner testRunner;
        private final CallBack callBack;

        private TestRunnerWrapper(TestCollection collection, CallBack callBack, final ExecutedTestTracker executedTestTracker, String pctAdapter)
        {
            this.testRunner = new TestRunner(collection, false, PureSession.this.getFunctionExecution(), callBack, pctAdapter)
            {
                @Override
                public void run()
                {
                    super.run();
                    if (null != executedTestTracker)
                    {
                        executedTestTracker.notePassingTests(this.passedTests);
                        executedTestTracker.noteFailingTests(this.failedTests);
                    }
                }
            };
            this.callBack = callBack;
        }

        private TestRunnerWrapper(TestCollection collection, ExecutedTestTracker executedTestTracker, String pctAdapter)
        {
            this(collection, new CallBack(), executedTestTracker, pctAdapter);
        }

        void stopAndClear()
        {
            TestRunner tr = this.testRunner;
            if (null != tr)
            {
                tr.stop();
                this.testRunner = null;
            }
            this.callBack.clear();
        }
    }
}
