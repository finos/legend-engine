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
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.factory.Bags;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.ImmutableSet;
import org.finos.legend.pure.runtime.java.interpreted.testHelper.PureTestBuilderInterpreted;

import java.util.Enumeration;

public class Test_Interpreted_Preeval
{
    private static final String INCLUDE_KNOWN_DIVERGENT = "legend.engine.preeval.test.includeKnownDivergent";

    static final ImmutableSet<String> KNOWN_INTERPRETED_FAILURES = Sets.immutable.empty();

    public static TestSuite suite()
    {
        TestSuite all = PureTestBuilderInterpreted.buildSuite("meta::pure::router::preeval::tests");
        MutableBag<String> collected = Bags.mutable.empty();
        TestSuite selected = filter(all, Boolean.getBoolean(INCLUDE_KNOWN_DIVERGENT), collected);
        ImmutableSet<String> unmatched = KNOWN_INTERPRETED_FAILURES.reject(name -> collected.occurrencesOf(name) == 1);
        if (unmatched.notEmpty())
        {
            throw new IllegalStateException("KNOWN_INTERPRETED_FAILURES names that do not match exactly one collected preeval test: " + unmatched.toSortedList().makeString(", "));
        }
        TestSuite suite = new TestSuite();
        suite.addTest(PreevalImplementationTests.withImplementation("PURE", selected));
        suite.addTest(PreevalImplementationTests.withImplementation("JAVA", selected));
        suite.addTest(PreevalImplementationTests.withImplementation("SHADOW", selected));
        return suite;
    }

    private static TestSuite filter(TestSuite source, boolean includeKnown, MutableBag<String> collected)
    {
        TestSuite result = new TestSuite(source.getName());
        for (Enumeration<Test> tests = source.tests(); tests.hasMoreElements(); )
        {
            Test test = tests.nextElement();
            if (test instanceof TestSuite)
            {
                result.addTest(filter((TestSuite) test, includeKnown, collected));
            }
            else
            {
                String name = ((TestCase) test).getName();
                collected.add(name);
                if (includeKnown || !KNOWN_INTERPRETED_FAILURES.contains(name))
                {
                    result.addTest(test);
                }
            }
        }
        return result;
    }
}
