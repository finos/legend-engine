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
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import static org.finos.legend.engine.test.fct.FCTTestSuitBuilder.isFCTTestCollection;

public class FCTTestCollection
{

    private final MutableList<CoreInstance> testFunctions = Lists.mutable.with();

    private final String testCollectionName;
    private final CoreInstance pkg;

    public String getStoreType()
    {
        return storeType;
    }

    private final String storeType;

    private final MutableList<FCTTestCollection> subCollections = Lists.mutable.with();


    public String getTestCollectionName()
    {
        return testCollectionName;
    }

    public FCTTestCollection(String testCollectionName, CoreInstance pkg, String storeType, ProcessorSupport processorSupport)
    {
        this.pkg = pkg;
        this.storeType = storeType;
        this.testCollectionName = testCollectionName;
        findPackageTests(processorSupport);

    }


    public FCTTestCollection(CoreInstance pkg, String storeType, ProcessorSupport processorSupport)
    {
        this.pkg = pkg;
        this.storeType = storeType;
        this.testCollectionName = PackageableElement.getUserPathForPackageableElement(pkg);
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
                        FCTTestCollection subCollection = new FCTTestCollection(child.getName(), child, storeType, processorSupport);
                        subCollection.testFunctions.add(child);
                        this.subCollections.add(subCollection);
                    }

            }
            else if (Instance.instanceOf(child, M3Paths.Package, processorSupport))
            {
                FCTTestCollection subCollection = new FCTTestCollection(child, storeType, processorSupport);
                // only add the sub-collection if it has some test content
                if (subCollection.hasTestContent())
                {
                    this.subCollections.add(subCollection);
                }
            }
        }
    }


}
