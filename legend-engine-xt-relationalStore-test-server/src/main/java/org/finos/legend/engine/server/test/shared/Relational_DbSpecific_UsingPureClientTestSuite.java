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

package org.finos.legend.engine.server.test.shared;

import com.fasterxml.jackson.databind.jsontype.NamedType;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.finos.legend.pure.code.core.compiled.test.PureTestBuilderHelper;
import org.finos.legend.pure.m3.exception.PureAssertFailException;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.junit.Ignore;

//Base classs for db specific tests - dont run this test directly
@Ignore
public abstract class Relational_DbSpecific_UsingPureClientTestSuite extends TestSuite
{
    public static Test createSuite(String pureTestCollectionPath, String testServerConfigFilePath, NamedType... extraConfigTypes) throws Exception
    {
        //Run test engine server - needs to be setup before as we need testParam(connection details) to create test suite
        boolean shouldCleanUp = PureWithEngineHelper.initClientVersionIfNotAlreadySet("vX_X_X");
        RelationalTestServer server = PureWithEngineHelper.initEngineServer(testServerConfigFilePath, () -> new RelationalTestServer(extraConfigTypes));
        CompiledExecutionSupport executionSupport = PureTestBuilderHelper.getClassLoaderExecutionSupport();

        TestSuite suite = wrapTestCases(PureTestBuilderHelper.buildSuite(TestCollection.collectTests(pureTestCollectionPath, executionSupport.getProcessorSupport(), fn -> PureTestBuilderHelper.generatePureTestCollection(fn, executionSupport), ci -> PureTestBuilderHelper.satisfiesConditions(ci, executionSupport.getProcessorSupport())), executionSupport));

        return new TestSetup(suite)
        {
            @Override
            protected void tearDown() throws Exception
            {
                super.tearDown();
                server.shutDown();
                if (shouldCleanUp)
                {
                    PureWithEngineHelper.cleanUp();
                }
                System.out.println("STOP");
            }
        };
    }

    private static final String PURE_NAME_RUN_DATA_ASSERTION_WHICH_DEVIATES_FROM_STANDARD = "runDataAssertionWhichDeviatesFromStandard";

    private static TestSuite wrapTestCases(TestSuite suite)
    {
        TestSuite wrappedSuite = new TestSuite(suite.getName());
        String dbSuiteName = wrappedSuite.getName().substring(wrappedSuite.getName().indexOf("sqlQueryTests::") + 15, wrappedSuite.getName().indexOf("["));
        for (int i = 0; i < suite.testCount(); i++)
        {
            Test test = suite.testAt(i);
            if (test instanceof TestSuite)
            {
                wrappedSuite.addTest(wrapTestCases((TestSuite) test));
            }
            else
            {
                TestCase testCase = (TestCase) test;
                wrappedSuite.addTest(new TestCase(testCase.getName())
                {
                    @Override
                    protected void runTest() throws Throwable
                    {
                        Throwable failureException = null;
                        String dbTestName = dbSuiteName + "::" + testCase.getName().substring(0, testCase.getName().indexOf("["));
                        try
                        {
                            System.out.println("Running db test " + dbTestName);
                            testCase.runBare();
                        }
                        catch (PureException e)
                        {
                            if (ArrayIterate.anySatisfy(e.getStackTrace(), x -> x.getMethodName().contains(PURE_NAME_RUN_DATA_ASSERTION_WHICH_DEVIATES_FROM_STANDARD)))
                            {
                                System.out.println("| " + dbTestName + " | deviates-from-standard :ballot_box_with_check: |");
                                if (e instanceof PureAssertFailException)
                                {
                                    throw new PureAssertFailException(e.getSourceInformation(), "[unsupported-api] [deviating-from-standard] " + e.getInfo(), (PureAssertFailException) e);
                                }
                                throw new PureExecutionException(e.getSourceInformation(), "[unsupported-api] [deviating-from-standard] " + e.getInfo(), e);
                            }
                            if ((e.getInfo() == null ? "" : e.getInfo()).toLowerCase().startsWith("[unsupported-api]"))
                            {
                                System.out.println("| " + dbTestName + " | not-implemented :eight_spoked_asterisk: |");
                                throw e;
                            }
                            failureException = e;
                        }
                        catch (Throwable e)
                        {
                            failureException = e;
                        }
                        finally
                        {
                            if (failureException != null)
                            {
                                System.out.println("| " + dbTestName + " | failing :x: |");
                                throw failureException;
                            }
                            System.out.println("| " + dbTestName + " | working :white_check_mark: |");
                        }
                    }
                });
            }
        }
        return wrappedSuite;
    }
}
