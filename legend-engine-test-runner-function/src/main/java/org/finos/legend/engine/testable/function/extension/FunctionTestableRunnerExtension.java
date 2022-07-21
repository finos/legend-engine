// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.testable.function.extension;

import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.CorePureProtocolExtension;
import org.finos.legend.engine.testable.extension.TestRunner;
import org.finos.legend.engine.testable.extension.TestableRunnerExtension;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.test.Testable;


public class FunctionTestableRunnerExtension implements TestableRunnerExtension
{
    private String pureVersion = PureClientVersions.production;

    @Override
    public String getSupportedClassifierPath()
    {
        return CorePureProtocolExtension.FUNCTION_CLASSIFIER_PATH;
    }

    @Override
    public TestRunner getTestRunner(Testable testable)
    {
        if (testable instanceof ConcreteFunctionDefinition<?>)
        {
            return new FunctionTestRunner((ConcreteFunctionDefinition<?>) testable, pureVersion);
        }
        return null;
    }

    public void setPureVersion(String pureVersion)
    {
        this.pureVersion = pureVersion;
    }
}