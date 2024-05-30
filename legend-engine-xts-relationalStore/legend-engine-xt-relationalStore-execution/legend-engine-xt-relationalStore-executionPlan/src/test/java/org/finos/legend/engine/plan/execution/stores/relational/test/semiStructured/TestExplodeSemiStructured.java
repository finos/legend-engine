// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.test.semiStructured;

import org.junit.Assert;
import org.junit.Test;

public class TestExplodeSemiStructured extends AbstractTestSemiStructured
{
    private static final String h2Mapping = "simple::mapping::semistructured";
    private static final String h2Runtime = "simple::runtime::runtime";
    private static final String h2ViewMapping = "view::mapping::semistructured";
    private static final String h2ViewRuntime = "view::runtime::runtime";

    @Test
    public void testSimplePrimitivePropertiesProjectExplodeSource()
    {
        String queryFunction = "simple::query::getOrdersForBlock__TabularDataSet_1_";
        String expected = "1,a1,o1,i1,100.0\n" +
                "2,a1,o2,i2,10.0\n" +
                "2,a1,o1,i1,100.0\n" +
                "3,a2,o3,i3,100.0\n" +
                "4,a2,o4,i1,100.0\n" +
                "5,a1,o5,i4,50.0\n" +
                "5,a1,o6,i4,50.0\n" +
                "6,a3,,,\n";

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals(expected, h2Result.replace("\r\n", "\n"));

        String h2ViewResult = this.executeFunction(queryFunction, h2ViewMapping, h2ViewRuntime);
        Assert.assertEquals(expected, h2ViewResult.replace("\r\n", "\n"));
    }

    @Test
    public void testSimplePrimitivePropertiesProjectExplodeTarget()
    {
        String queryFunction = "simple::query::getBlockForTrade__TabularDataSet_1_";
        String expected = "t1,accepted,1,a1\n" +
                "t2,rejected,2,a1\n" +
                "t3,accepted,3,a2\n" +
                "t4,accepted,3,a2\n" +
                "t5,accepted,4,a2\n" +
                "t6,rejected,4,a2\n" +
                "t7,accepted,5,a1\n" +
                "t8,invalid,,\n";

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals(expected, h2Result.replace("\r\n", "\n"));

        String h2ViewResult = this.executeFunction(queryFunction, h2ViewMapping, h2ViewRuntime);
        Assert.assertEquals(expected, h2ViewResult.replace("\r\n", "\n"));
    }

    @Test
    public void testComplexProjectFlattenedAndExplodedPropertiesInProject()
    {
        String queryFunction = "simple::query::getOrdersAndRelatedEntitiesForBlock__TabularDataSet_1_";
        String expected = "1,trade,t1,o1\n" +
                "1,order,o1,o1\n" +
                "2,trade,t2,o2\n" +
                "2,trade,t2,o1\n" +
                "2,order,o2,o2\n" +
                "2,order,o2,o1\n" +
                "2,order,o1,o2\n" +
                "2,order,o1,o1\n" +
                "3,trade,t3,o3\n" +
                "3,trade,t4,o3\n" +
                "3,order,o3,o3\n" +
                "4,trade,t5,o4\n" +
                "4,trade,t6,o4\n" +
                "4,order,o4,o4\n" +
                "5,trade,t7,o5\n" +
                "5,trade,t7,o6\n" +
                "5,order,o5,o5\n" +
                "5,order,o5,o6\n" +
                "5,order,o6,o5\n" +
                "5,order,o6,o6\n" +
                "6,,,\n";

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals(expected, h2Result.replace("\r\n", "\n"));

        String h2ViewResult = this.executeFunction(queryFunction, h2ViewMapping, h2ViewRuntime);
        Assert.assertEquals(expected, h2ViewResult.replace("\r\n", "\n"));
    }

    @Test
    public void testComplexProjectMultiplePropertiesToExplodeInProject()
    {
        String queryFunction = "simple::query::getTradesAndOrdersInBlock__TabularDataSet_1_";
        String expected = "1,a1,o1,i1,t1,accepted\n" +
                "2,a1,o2,i2,t2,rejected\n" +
                "2,a1,o1,i1,t2,rejected\n" +
                "3,a2,o3,i3,t3,accepted\n" +
                "3,a2,o3,i3,t4,accepted\n" +
                "4,a2,o4,i1,t5,accepted\n" +
                "4,a2,o4,i1,t6,rejected\n" +
                "5,a1,o5,i4,t7,accepted\n" +
                "5,a1,o6,i4,t7,accepted\n" +
                "6,a3,,,,\n";

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals(expected, h2Result.replace("\r\n", "\n"));

        String h2ViewResult = this.executeFunction(queryFunction, h2ViewMapping, h2ViewRuntime);
        Assert.assertEquals(expected, h2ViewResult.replace("\r\n", "\n"));
    }

    @Test
    public void testSimplePrimitivePropertiesProjectWithFilterOnSource()
    {
        String queryFunction = "simple::query::getTradesForNonCancelledBlocks__TabularDataSet_1_";
        String expected = "1,a1,t1,accepted\n" +
                "3,a2,t3,accepted\n" +
                "3,a2,t4,accepted\n" +
                "4,a2,t5,accepted\n" +
                "4,a2,t6,rejected\n" +
                "5,a1,t7,accepted\n" +
                "6,a3,,\n";

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals(expected, h2Result.replace("\r\n", "\n"));

        String h2ViewResult = this.executeFunction(queryFunction, h2ViewMapping, h2ViewRuntime);
        Assert.assertEquals(expected, h2ViewResult.replace("\r\n", "\n"));
    }

    @Test
    public void testSimplePrimitivePropertiesProjectWithFilterOnTarget()
    {
        String queryFunction = "simple::query::getNonCancelledBlocksForTrades__TabularDataSet_1_";
        String expected = "t1,accepted,1,a1\n" +
                "t3,accepted,3,a2\n" +
                "t4,accepted,3,a2\n" +
                "t5,accepted,4,a2\n" +
                "t6,rejected,4,a2\n" +
                "t7,accepted,5,a1\n" +
                "t8,invalid,,\n";

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals(expected, h2Result.replace("\r\n", "\n"));

        String h2ViewResult = this.executeFunction(queryFunction, h2ViewMapping, h2ViewRuntime);
        Assert.assertEquals(expected, h2ViewResult.replace("\r\n", "\n"));
    }

    @Test
    public void testProjectWithExplodedPropertyAccessOnlyInFilter()
    {
        String queryFunction = "simple::query::getNonCancelledBlocksForTradesNoProject__TabularDataSet_1_";
        String expected = "t1,accepted\n" +
                "t3,accepted\n" +
                "t4,accepted\n" +
                "t5,accepted\n" +
                "t6,rejected\n" +
                "t7,accepted\n" +
                "t8,invalid\n";

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals(expected, h2Result.replace("\r\n", "\n"));

        String h2ViewResult = this.executeFunction(queryFunction, h2ViewMapping, h2ViewRuntime);
        Assert.assertEquals(expected, h2ViewResult.replace("\r\n", "\n"));
    }

    @Test
    public void testFilterOnExplodedPropertyFilteringInsideProject()
    {
        String queryFunction = "simple::query::getBigBuyOrdersInBlock__TabularDataSet_1_";
        String expected = "1,a1,,o1\n" +
                "2,a1,o2,o2\n" +
                "2,a1,o2,o1\n" +
                "3,a2,o3,o3\n" +
                "4,a2,,o4\n" +
                "5,a1,,o5\n" +
                "5,a1,,o6\n" +
                "6,a3,,\n";

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals(expected, h2Result.replace("\r\n", "\n"));

        String h2ViewResult = this.executeFunction(queryFunction, h2ViewMapping, h2ViewRuntime);
        Assert.assertEquals(expected, h2ViewResult.replace("\r\n", "\n"));
    }

    @Test
    public void testAggregationAggregateExplodedPropertyUsingGroupBy()
    {
        String queryFunction = "simple::query::getTradeVolumeInBlock__TabularDataSet_1_";
        String expected = "1,a1,100\n" +
                "2,a1,100\n" +
                "3,a2,200\n" +
                "4,a2,150\n" +
                "5,a1,60\n" +
                "6,a3,\n";

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals(expected, h2Result.replace("\r\n", "\n"));

        String h2ViewResult = this.executeFunction(queryFunction, h2ViewMapping, h2ViewRuntime);
        Assert.assertEquals(expected, h2ViewResult.replace("\r\n", "\n"));
    }

    @Test
    public void testAggregationAggregateExplodedPropertyInsideProject()
    {
        String queryFunction = "simple::query::getTotalBuyOrderVolumeInBlock__TabularDataSet_1_";
        String expected = "1,a1,\n" +
                "2,a1,100\n" +
                "3,a2,200\n" +
                "4,a2,\n" +
                "5,a1,\n" +
                "6,a3,\n";

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals(expected, h2Result.replace("\r\n", "\n"));

        String h2ViewResult = this.executeFunction(queryFunction, h2ViewMapping, h2ViewRuntime);
        Assert.assertEquals(expected, h2ViewResult.replace("\r\n", "\n"));
    }

    @Test
    public void testSimpleJoinChainOneJoin()
    {
        String queryFunction = "simple::query::getAccountForOrders__TabularDataSet_1_";
        String expected = "o1,a1,1\n" +
                "o1,a1,2\n" +
                "o2,a1,2\n" +
                "o3,a2,3\n" +
                "o4,a2,4\n" +
                "o5,a1,5\n" +
                "o6,a1,5\n";

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals(expected, h2Result.replace("\r\n", "\n"));

        String h2ViewResult = this.executeFunction(queryFunction, h2ViewMapping, h2ViewRuntime);
        Assert.assertEquals(expected, h2ViewResult.replace("\r\n", "\n"));
    }

    @Test
    public void testJoinChainMultipleJoinsSingleExplode()
    {
        String queryFunction = "simple::query::getProductsForOrdersInBlock__TabularDataSet_1_";
        String expected = "p1,1\n" +
                "p2,2\n" +
                "p1,2\n" +
                "p3,3\n" +
                "p1,4\n" +
                "p1,5\n" +
                "p1,5\n" +
                ",6\n";

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals(expected, h2Result.replace("\r\n", "\n"));

        String h2ViewResult = this.executeFunction(queryFunction, h2ViewMapping, h2ViewRuntime);
        Assert.assertEquals(expected, h2ViewResult.replace("\r\n", "\n"));
    }

    @Test
    public void testJoinChainMultipleJoinsMultipleExplode()
    {
        String queryFunction = "simple::query::getRelatedTradesForOrder__TabularDataSet_1_";
        String expected = "o1,t1\n" +
                "o1,t2\n" +
                "o2,t2\n" +
                "o3,t3\n" +
                "o3,t4\n" +
                "o4,t5\n" +
                "o4,t6\n" +
                "o5,t7\n" +
                "o6,t7\n";

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals(expected, h2Result.replace("\r\n", "\n"));

        String h2ViewResult = this.executeFunction(queryFunction, h2ViewMapping, h2ViewRuntime);
        Assert.assertEquals(expected, h2ViewResult.replace("\r\n", "\n"));
    }

    @Override
    public String modelResourcePath()
    {
        return "/org/finos/legend/engine/plan/execution/stores/relational/test/semiStructured/explodeSemiStructuredMapping.pure";
    }
}
