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

package org.finos.legend.engine.testable.extension;

import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.test.AtomicTest;
import org.finos.legend.engine.protocol.pure.v1.model.test.TestSuite;
import org.finos.legend.pure.generated.Root_meta_pure_test_TestSuite;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public abstract class AbstractTestSuiteSession<S, T, R> implements TestSuiteSession<R>
{
    protected final Root_meta_pure_test_TestSuite pureSuite;
    protected final S protocolSuite;
    protected final PureModel pureModel;
    protected final PureModelContextData pmcd;
    private final Map<String, T> testsById;

    protected AbstractTestSuiteSession(Root_meta_pure_test_TestSuite pureSuite, S protocolSuite, PureModel pureModel, PureModelContextData pmcd, Function<? super S, ? extends Iterable<? extends T>> getAtomicTests, Function<? super T, ? extends String> getAtomicTestId)
    {
        this.pureSuite = Objects.requireNonNull(pureSuite);
        this.protocolSuite = Objects.requireNonNull(protocolSuite);
        this.pureModel = Objects.requireNonNull(pureModel);
        this.pmcd = Objects.requireNonNull(pmcd);
        this.testsById = indexTests(pureSuite._id(), this.protocolSuite, getAtomicTests, getAtomicTestId);
    }

    @Override
    public String getTestSuiteId()
    {
        return this.pureSuite._id();
    }

    @Override
    public Collection<String> getAtomicTestIds()
    {
        return this.testsById.keySet();
    }

    @Override
    public boolean isAtomicTestId(String id)
    {
        return this.testsById.containsKey(id);
    }

    protected T getAtomicTest(String id)
    {
        return this.testsById.get(id);
    }

    private static <S, T> Map<String, T> indexTests(String suiteId, S suite, Function<? super S, ? extends Iterable<? extends T>> getTests, Function<? super T, ? extends String> getTestId)
    {
        Iterable<? extends T> atomicTests = getTests.apply(suite);
        if (atomicTests == null)
        {
            return Collections.emptyMap();
        }
        Map<String, T> map = new LinkedHashMap<>();
        atomicTests.forEach(t ->
        {
            String testId = getTestId.apply(t);
            if (map.put(testId, t) != null)
            {
                throw new IllegalStateException("Duplicate atomic tests in suite " + suiteId + " for id: " + testId);
            }
        });
        return map.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(map);
    }

    @SuppressWarnings("unchecked")
    protected static <T extends AtomicTest> Iterable<T> getTestSuiteTests(TestSuite testSuite)
    {
        return LazyIterate.collect(testSuite.tests, t -> (T) t);
    }

    protected static String getAtomicTestId(AtomicTest atomicTest)
    {
        return atomicTest.id;
    }
}
