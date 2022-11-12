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

package org.finos.legend.pure.code.core.compiled.test;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestListener;
import org.finos.legend.pure.m4.exception.PureException;
import org.junit.internal.runners.SuiteMethod;
import org.junit.runner.Describable;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

public class IgnoreUnsupportedApiPureTestSuiteRunner extends SuiteMethod
{
    public IgnoreUnsupportedApiPureTestSuiteRunner(Class<?> testClass) throws Throwable
    {
        super(testClass);
    }

    @Override
    public TestListener createAdaptingListener(RunNotifier notifier)
    {
        return new Listener(notifier);
    }

    private static final class Listener implements TestListener
    {
        private final RunNotifier notifier;

        private Listener(RunNotifier notifier)
        {
            this.notifier = notifier;
        }

        public void endTest(Test test)
        {
            notifier.fireTestFinished(asDescription(test));
        }

        public void startTest(Test test)
        {
            notifier.fireTestStarted(asDescription(test));
        }

        public void addError(Test test, Throwable e)
        {
            Failure failure = new Failure(asDescription(test), e);
            if (isPureUnsupportedApiException(e))
            {
                notifier.fireTestAssumptionFailed(failure);
            }
            else
            {
                notifier.fireTestFailure(failure);
            }
        }

        private boolean isPureUnsupportedApiException(Throwable e)
        {
            if (e instanceof PureException)
            {
                String info = ((PureException) e).getInfo();
                if (info != null)
                {
                    String infoLowercase = info.toLowerCase();
                    return infoLowercase.startsWith("[unsupported-api]");
                }
            }
            return false;
        }

        private Description asDescription(Test test)
        {
            if (test instanceof Describable)
            {
                Describable facade = (Describable) test;
                return facade.getDescription();
            }
            return Description.createTestDescription(getEffectiveClass(test), getName(test));
        }

        private Class<? extends Test> getEffectiveClass(Test test)
        {
            return test.getClass();
        }

        private String getName(Test test)
        {
            if (test instanceof TestCase)
            {
                return ((TestCase) test).getName();
            }
            else
            {
                return test.toString();
            }
        }

        public void addFailure(Test test, AssertionFailedError t)
        {
            addError(test, t);
        }
    }
}
