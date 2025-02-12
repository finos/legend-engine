/*
 * //  Copyright 2023 Goldman Sachs
 * //
 * //  Licensed under the Apache License, Version 2.0 (the "License");
 * //  you may not use this file except in compliance with the License.
 * //  You may obtain a copy of the License at
 * //
 * //       http://www.apache.org/licenses/LICENSE-2.0
 * //
 * //  Unless required by applicable law or agreed to in writing, software
 * //  distributed under the License is distributed on an "AS IS" BASIS,
 * //  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * //  See the License for the specific language governing permissions and
 * //  limitations under the License.
 */

package org.finos.legend.engine.test.fct;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import static org.finos.legend.engine.test.fct.FCTTestSuitBuilder.isFCTTestCollection;

public class FCTTestCollection
{
    private final String runtimeFunction;


    private final String mockRuntime;
    private final String setupFunction;

    private final MutableList<CoreInstance> testFunctions = Lists.mutable.with();
    private final CoreInstance pkg;
    private final MutableList<FCTTestCollection> subCollections = Lists.mutable.with();

    public String getRuntimeFunction(boolean useMock)
    {
        return  useMock ? mockRuntime : runtimeFunction;
    }

    public String getSetupFunction()
    {
        return setupFunction;
    }

    public String getMockRuntime()
    {
        return mockRuntime;
    }


    public FCTTestCollection(CoreInstance pkg, String runtimeFunction, String setupFunction, ProcessorSupport processorSupport, String mockRuntime)
    {
        this.pkg = pkg;
        this.runtimeFunction = runtimeFunction;
        this.setupFunction = setupFunction;
        this.mockRuntime = mockRuntime;
        findPackageTests(processorSupport);
    }

    public MutableList<CoreInstance> getAllTestFunctions()
    {
        MutableList<CoreInstance> tests = Lists.mutable.with();
        collectAllTests(tests);
        return tests;
    }

    public MutableList<CoreInstance> getTestFunctions()
    {
        return this.testFunctions;
    }

    public CoreInstance getPackage()
    {
        return this.pkg;

    }

    public boolean hasTestContent()
    {
        return this.testFunctions.notEmpty() ||
                this.subCollections.anySatisfy(FCTTestCollection::hasTestContent);
    }

    private void collectAllTests(MutableList<CoreInstance> tests)
    {
        {
            tests.addAll(this.testFunctions);
        }

        for (FCTTestCollection subCollection : this.subCollections)
        {
            subCollection.collectAllTests(tests);
        }
    }

    public RichIterable<FCTTestCollection> getSubCollections()
    {
        return this.subCollections;
    }

    private void findPackageTests(ProcessorSupport processorSupport)
    {
        for (CoreInstance child : Instance.getValueForMetaPropertyToManyResolved(this.pkg, M3Properties.children, processorSupport))
        {
            if (Instance.instanceOf(child, M3Paths.FunctionDefinition, processorSupport))
            {

                   if (isFCTTestCollection(child, processorSupport))
                    {
                        this.testFunctions.add(child);
                    }

            }
            else if (Instance.instanceOf(child, M3Paths.Package, processorSupport))
            {
                FCTTestCollection subCollection = new FCTTestCollection(child,this.getRuntimeFunction(false),this.getSetupFunction(), processorSupport,  this.getMockRuntime());
                // only add the sub-collection if it has some test content
                if (subCollection.hasTestContent())
                {
                    this.subCollections.add(subCollection);
                }
            }
        }
    }

}
