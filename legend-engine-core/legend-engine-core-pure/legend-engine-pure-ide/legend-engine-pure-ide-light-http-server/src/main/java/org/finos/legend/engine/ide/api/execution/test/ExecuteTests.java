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

import io.swagger.annotations.Api;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.tuple.primitive.BooleanObjectPair;
import org.finos.legend.engine.ide.session.PureSession;
import org.finos.legend.pure.m3.execution.test.TestExceptionStatus;
import org.finos.legend.pure.m3.execution.test.TestRunner;
import org.finos.legend.pure.m3.execution.test.TestStatus;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.json.simple.JSONValue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Api(tags = "Execute Tests")
@Path("/")
public class ExecuteTests
{
    private PureSession pureSession;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public ExecuteTests(PureSession pureSession)
    {
        this.pureSession = pureSession;
    }

    @POST
    @Path("executeTests")
    public Response executeTests(@Context HttpServletRequest request, @Context HttpServletResponse response)
    {
        return Response.ok((StreamingOutput) outputStream ->
        {
            this.pureSession.saveFilesAndExecute(request, response, outputStream, new TestRun(this.executorService));
        }).build();
    }

    @GET
    @Path("testRunnerCancel")
    public Response testRunnerCancel(@Context HttpServletRequest request, @Context HttpServletResponse response)
    {
        return Response.ok((StreamingOutput) outputStream ->
        {
            int testRunnerId = getTestRunnerId(request);
            TestRunner runner = pureSession.removeTestRunner(testRunnerId);
            if (runner == null)
            {
                outputStream.write("{\"text\":\"Unknown test runner: ".getBytes());
                outputStream.write(Integer.toString(testRunnerId).getBytes());
                outputStream.write("\"}".getBytes());
                outputStream.close();
            }
            else
            {
                runner.stop();
                outputStream.write("{\"text\":\"Test run ".getBytes());
                outputStream.write(Integer.toString(testRunnerId).getBytes());
                outputStream.write(" canceled\"}".getBytes());
                outputStream.close();
            }
        }).build();
    }

    @GET
    @Path("testRunnerCheck")
    public Response testRunnerCheck(@Context HttpServletRequest request, @Context HttpServletResponse response)
    {
        return Response.ok((StreamingOutput) outputStream ->
        {
            int testRunnerId = getTestRunnerId(request);
            CallBack callBack = pureSession.getTestCallBack(testRunnerId);
            if (callBack == null)
            {
                outputStream.write("{\"error\":true,\"text\":\"Unknown test runner: ".getBytes());
                outputStream.write(Integer.toString(testRunnerId).getBytes());
                outputStream.write("\"}".getBytes());
                outputStream.close();
            }
            else
            {
                BooleanObjectPair<ListIterable<org.finos.legend.engine.ide.api.execution.test.TestResult>> pullResult = callBack.pullNewResults();
                boolean finished = pullResult.getOne();
                ListIterable<org.finos.legend.engine.ide.api.execution.test.TestResult> testResults = pullResult.getTwo();

                if (finished)
                {
                    pureSession.removeTestRunner(testRunnerId);
                }

                outputStream.write("{\"finished\":".getBytes());
                outputStream.write(Boolean.toString(finished).getBytes());
                outputStream.write(",\"tests\":[".getBytes());
                if (testResults.notEmpty())
                {
                    Iterator<org.finos.legend.engine.ide.api.execution.test.TestResult> iterator = testResults.iterator();
                    writeTestResult(outputStream, iterator.next(), pureSession);
                    while (iterator.hasNext())
                    {
                        outputStream.write(',');
                        writeTestResult(outputStream, iterator.next(), pureSession);
                    }
                }
                outputStream.write("]}".getBytes());
                outputStream.close();
            }
        }).build();
    }

    private void writeTestResult(OutputStream outStream, TestResult result, PureSession session) throws IOException
    {
        outStream.write("{\"test\":[".getBytes());
        String parameterizationSuffix = result.getTestParameterizationId() == null ? "" : "[" + result.getTestParameterizationId() + "]";
        outStream.write(("\"" + PackageableElement.getUserPathForPackageableElement(result.getTestFunction(), "\",\"") + parameterizationSuffix + "\"").getBytes());
        outStream.write("],\"console\":\"".getBytes());
        outStream.write(JSONValue.escape(result.getConsoleOutput()).getBytes());
        outStream.write("\",\"status\":\"".getBytes());
        outStream.write(result.getStatus().toString().getBytes());
        outStream.write('"');
        if (!TestStatus.SUCCESS.equals(result.getStatus()))
        {
            outStream.write(",\"error\":".getBytes());
            outStream.write(PureSession.exceptionToJson(session, ((TestExceptionStatus) result.getStatus()).getException(), null).getBytes());
        }
        outStream.write('}');
    }


    private static final String TEST_RUNNER_ID_PARAM = "testRunnerId";

    private int getTestRunnerId(HttpServletRequest request)
    {
        String idString = request.getParameter(TEST_RUNNER_ID_PARAM);
        if (idString == null)
        {
            throw new RuntimeException("No test runner id");
        }
        try
        {
            return Integer.parseInt(idString);
        }
        catch (NumberFormatException e)
        {
            throw new RuntimeException("Invalid test runner id: " + idString);
        }
    }
}