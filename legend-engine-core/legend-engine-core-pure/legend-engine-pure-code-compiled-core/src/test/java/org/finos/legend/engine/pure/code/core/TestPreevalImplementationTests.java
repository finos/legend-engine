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

package org.finos.legend.engine.pure.code.core;

import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class TestPreevalImplementationTests
{
    private static final String PROPERTY = "legend.engine.preeval.implementation";

    @After
    public void clearProperty()
    {
        System.clearProperty(PROPERTY);
    }

    @Test
    public void testLeavesAreRenamedAfterTheImplementation()
    {
        TestSuite source = new TestSuite();
        source.addTest(new Recording("testSomething"));
        TestSuite named = PreevalImplementationTests.withImplementation("JAVA", source);
        Assert.assertEquals("testSomething[JAVA]", ((TestCase) named.testAt(0)).getName());
    }

    @Test
    public void testImplementationIsSetDuringTheRunAndRestoredAfter()
    {
        System.setProperty(PROPERTY, "PURE");
        Recording recording = new Recording("testSomething");
        TestSuite source = new TestSuite();
        source.addTest(recording);
        TestResult result = new TestResult();
        PreevalImplementationTests.withImplementation("SHADOW", source).run(result);
        Assert.assertTrue(result.wasSuccessful());
        Assert.assertEquals("SHADOW", recording.seen);
        Assert.assertEquals("PURE", System.getProperty(PROPERTY));
    }

    @Test
    public void testImplementationIsClearedAfterAFailingRunWhenPreviouslyUnset()
    {
        TestSuite source = new TestSuite();
        source.addTest(new Failing("testFails"));
        TestResult result = new TestResult();
        PreevalImplementationTests.withImplementation("JAVA", source).run(result);
        Assert.assertEquals(1, result.failureCount());
        Assert.assertNull(System.getProperty(PROPERTY));
    }

    private static final class Recording extends TestCase
    {
        private String seen;

        private Recording(String name)
        {
            super(name);
        }

        @Override
        protected void runTest()
        {
            this.seen = System.getProperty(PROPERTY);
        }
    }

    private static final class Failing extends TestCase
    {
        private Failing(String name)
        {
            super(name);
        }

        @Override
        protected void runTest()
        {
            fail("expected");
        }
    }
}
