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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.pure.generated.Root_meta_pure_test_TestSuite;

import java.util.function.Consumer;
import java.util.function.Function;

public abstract class AbstractTestSuiteSessionWithResources<S, T, R> extends AbstractTestSuiteSession<S, T, R>
{
    private static final int UNINITIALIZED = 0;
    private static final int INITIALIZED = 1;
    private static final int INIT_ERROR = 2;
    private static final int CLOSED = 3;

    private int state = UNINITIALIZED;
    private RuntimeException initError;
    private final MutableList<AutoCloseable> closeables = Lists.mutable.empty();

    protected AbstractTestSuiteSessionWithResources(Root_meta_pure_test_TestSuite pureSuite, S protocolSuite, PureModel pureModel, PureModelContextData pmcd, Function<? super S, ? extends Iterable<? extends T>> getAtomicTests, Function<? super T, ? extends String> getAtomicTestId)
    {
        super(pureSuite, protocolSuite, pureModel, pmcd, getAtomicTests, getAtomicTestId);
    }

    @Override
    public void initialize()
    {
        initialize(true);
    }

    @Override
    public R runAtomicTest(String atomicTestId)
    {
        T atomicTest = getAtomicTest(atomicTestId);
        if (atomicTest == null)
        {
            return null;
        }
        if (initialize(false) == INIT_ERROR)
        {
            return buildErrorResult(atomicTestId, this.initError);
        }
        try
        {
            return runAtomicTest(atomicTest);
        }
        catch (Exception e)
        {
            return buildErrorResult(atomicTestId, e);
        }
    }

    @Override
    public synchronized void close()
    {
        if (this.state != CLOSED)
        {
            MutableList<Exception> exceptions = Lists.mutable.empty();
            this.closeables.forEach(c ->
            {
                try
                {
                    c.close();
                }
                catch (Exception e)
                {
                    exceptions.add(e);
                }
            });
            this.state = CLOSED;
            if ((exceptions.size() == 1) && (exceptions.get(0) instanceof RuntimeException))
            {
                throw (RuntimeException) exceptions.get(0);
            }
            if (exceptions.notEmpty())
            {
                RuntimeException e = new RuntimeException("Multiple exceptions closing test suite session for '" + getTestSuiteId() + "': " + exceptions.size() + " exceptions");
                exceptions.forEach(e::addSuppressed);
                throw e;
            }
        }
    }

    private synchronized int initialize(boolean throwOnInitError)
    {
        switch (this.state)
        {
            case UNINITIALIZED:
            {
                try
                {
                    initialize(this.closeables::add);
                    this.state = INITIALIZED;
                }
                catch (Throwable t)
                {
                    this.initError = new RuntimeException("Error initializing test suite session for '" + getTestSuiteId() + "'", t);
                    this.state = INIT_ERROR;
                    if (throwOnInitError)
                    {
                        if (t instanceof Error)
                        {
                            throw (Error) t;
                        }
                        throw this.initError;
                    }
                }
                break;
            }
            case INIT_ERROR:
            {
                if (throwOnInitError)
                {
                    throw this.initError;
                }
                break;
            }
            case CLOSED:
            {
                throw new IllegalStateException("Session is closed");
            }
        }
        return this.state;
    }

    protected abstract void initialize(Consumer<? super AutoCloseable> closeableConsumer) throws Exception;

    protected abstract R runAtomicTest(T atomicTest);

    protected abstract R buildErrorResult(String atomicTestId, Throwable t);
}
