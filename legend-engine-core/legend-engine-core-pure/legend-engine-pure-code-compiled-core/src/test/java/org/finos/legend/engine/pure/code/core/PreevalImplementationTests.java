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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Enumeration;

final class PreevalImplementationTests
{
    private static final String PROPERTY = "legend.engine.preeval.implementation";

    private PreevalImplementationTests()
    {
    }

    static TestSuite withImplementation(String implementation, TestSuite source)
    {
        TestSuite result = new TestSuite(source.getName() == null ? implementation : source.getName() + "[" + implementation + "]");
        for (Enumeration<Test> tests = source.tests(); tests.hasMoreElements(); )
        {
            Test test = tests.nextElement();
            if (test instanceof TestSuite)
            {
                result.addTest(withImplementation(implementation, (TestSuite) test));
            }
            else
            {
                result.addTest(new ImplementationTestCase(implementation, (TestCase) test));
            }
        }
        return result;
    }

    private static final class ImplementationTestCase extends TestCase
    {
        private final String implementation;
        private final TestCase delegate;

        private ImplementationTestCase(String implementation, TestCase delegate)
        {
            super(delegate.getName() + "[" + implementation + "]");
            this.implementation = implementation;
            this.delegate = delegate;
        }

        @Override
        public void runBare() throws Throwable
        {
            String previous = System.getProperty(PROPERTY);
            System.setProperty(PROPERTY, this.implementation);
            try
            {
                this.delegate.runBare();
            }
            finally
            {
                if (previous == null)
                {
                    System.clearProperty(PROPERTY);
                }
                else
                {
                    System.setProperty(PROPERTY, previous);
                }
            }
        }
    }
}
