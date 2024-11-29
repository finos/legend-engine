//  Copyright 2024 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.pure.code.core;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.test.fct.FCTTestSuitBuilder;
import org.finos.legend.pure.code.core.relational.Test_Pure_Relational_FCT_Collection;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import static org.finos.legend.engine.test.fct.FCTTestSuitBuilder.LINEAGE_FUNCTION;
import static org.finos.legend.engine.test.fct.FCTTestSuitBuilder.addCollectionToSuite;
import static org.finos.legend.engine.test.fct.FCTTestSuitBuilder.buildFCTTestCollection;
import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.*;

public class Test_Analytics_Lineage_FCT
{
    public static Test suite()
    {

        CompiledExecutionSupport support = getClassLoaderExecutionSupport();
        MutableMap<String, String> exclusions = Maps.mutable.empty();

       TestCollection collectionRelational = Test_Pure_Relational_FCT_Collection.buildCollection(support);
       TestCollection  collection = buildFCTTestCollection("meta::analytics::lineage::tests", support.getProcessorSupport());
       TestSuite suite = FCTTestSuitBuilder.buildFCTTestSuiteWithExecutorFunction(collection, exclusions, LINEAGE_FUNCTION, false, support);
       addCollectionToSuite(suite,collectionRelational,exclusions,LINEAGE_FUNCTION, false,support);
        return wrapSuite(
                () -> true,
                () -> suite,
                () -> false,
                Lists.mutable.empty()
        );
    }

}
