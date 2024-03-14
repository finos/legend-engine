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
import org.junit.Ignore;
import org.junit.Test;

public class TestSemiStructuredFlattening extends AbstractTestSemiStructured
{
    private static final String h2Mapping = "flatten::mapping::H2Mapping";
    private static final String h2Runtime = "flatten::runtime::H2Runtime";

    @Test
    public void testSemiStructuredPrimitivePropertyFlattening()
    {
        String queryFunction = "flatten::semiStructuredPrimitivePropertyFlattening__TabularDataSet_1_";

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("Peter,Firm X,O1\n" +
                "Peter,Firm X,O2\n" +
                "John,Firm X,O1\n" +
                "John,Firm X,O2\n" +
                "John,Firm X,O1\n" +
                "John,Firm X,O2\n" +
                "Anthony,Firm X,O1\n" +
                "Anthony,Firm X,O2\n" +
                "Fabrice,Firm A,O3\n" +
                "Fabrice,Firm A,O4\n" +
                "Oliver,Firm B,O5\n" +
                "Oliver,Firm B,O6\n" +
                "David,Firm B,O5\n" +
                "David,Firm B,O6\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testSemiStructuredComplexPropertyFlattening()
    {
        String queryFunction = "flatten::semiStructuredComplexPropertyFlattening__TabularDataSet_1_";

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("Peter,Firm X,A1\n" +
                "Peter,Firm X,A11\n" +
                "John,Firm X,A2\n" +
                "John,Firm X,A22\n" +
                "John,Firm X,A3\n" +
                "John,Firm X,A32\n" +
                "Anthony,Firm X,A4\n" +
                "Fabrice,Firm A,A5\n" +
                "Fabrice,Firm A,A52\n" +
                "Oliver,Firm B,A6\n" +
                "David,Firm B,A7\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testSemiStructuredPrimitivePropertyArrayIndexing()
    {
        String queryFunction = "flatten::semiStructuredPrimitivePropertyArrayIndexing__TabularDataSet_1_";

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("Peter,Firm X,O1,\n" +
                "John,Firm X,O1,\n" +
                "John,Firm X,O1,\n" +
                "Anthony,Firm X,O1,\n" +
                "Fabrice,Firm A,O3,\n" +
                "Oliver,Firm B,O5,\n" +
                "David,Firm B,O5,\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testSemiStructuredComplexPropertyArrayIndexing()
    {
        String queryFunction = "flatten::semiStructuredComplexPropertyArrayIndexing__TabularDataSet_1_";

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("Peter,Firm X,A1,\n" +
                "John,Firm X,A2,\n" +
                "John,Firm X,A3,\n" +
                "Anthony,Firm X,A4,\n" +
                "Fabrice,Firm A,A5,\n" +
                "Oliver,Firm B,A6,\n" +
                "David,Firm B,A7,\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testSemiStructuredComplexPropertyFlatteningFollowedBySubType()
    {
        String queryFunction = "flatten::semiStructuredComplexPropertyFlatteningFollowedBySubType__TabularDataSet_1_";

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("Peter,Firm X,1\n" +
                "Peter,Firm X,1\n" +
                "John,Firm X,1\n" +
                "John,Firm X,1\n" +
                "John,Firm X,1\n" +
                "John,Firm X,1\n" +
                "Anthony,Firm X,1\n" +
                "Fabrice,Firm A,1\n" +
                "Fabrice,Firm A,1\n" +
                "Oliver,Firm B,1\n" +
                "David,Firm B,1\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testSemiStructuredComplexPropertyArrayIndexingFollowedBySubType()
    {
        String queryFunction = "flatten::semiStructuredComplexPropertyArrayIndexingFollowedBySubType__TabularDataSet_1_";

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("Peter,Firm X,1\n" +
                "John,Firm X,1\n" +
                "John,Firm X,1\n" +
                "Anthony,Firm X,1\n" +
                "Fabrice,Firm A,1\n" +
                "Oliver,Firm B,1\n" +
                "David,Firm B,1\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testSemiStructuredPrimitivePropertyFiltering()
    {
        String queryFunction = "flatten::semiStructuredPrimitivePropertyFiltering__TabularDataSet_1_";

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("", h2Result.replace("\r\n", "\n"));
    }

    @Test
    @Ignore
    public void testSemiStructuredPrimitivePropertyFilteringInProject()
    {
        String queryFunction = "flatten::semiStructuredPrimitivePropertyFilteringInProject__TabularDataSet_1_";

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testSemiStructuredSubAggregation()
    {
        String queryFunction = "flatten::semiStructuredSubAggregation__TabularDataSet_1_";

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("Peter,Firm X,A1;A11\n" +
                "John,Firm X,A2;A22\n" +
                "John,Firm X,A3;A32\n" +
                "Anthony,Firm X,A4\n" +
                "Fabrice,Firm A,A5;A52\n" +
                "Oliver,Firm B,A6\n" +
                "David,Firm B,A7\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testSemiStructuredSubAggregationDeep()
    {
        String queryFunction = "flatten::semiStructuredSubAggregationDeep__TabularDataSet_1_";

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("Peter,Firm X,6\n" +
                "John,Firm X,6\n" +
                "John,Firm X,6\n" +
                "Anthony,Firm X,3\n" +
                "Fabrice,Firm A,6\n" +
                "Oliver,Firm B,3\n" +
                "David,Firm B,3\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testSemiStructuredMultiLevelFlatten()
    {
        String queryFunction = "flatten::semiStructuredMultiLevelFlattening__TabularDataSet_1_";

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("Peter,Firm X,1\n" +
                "Peter,Firm X,2\n" +
                "Peter,Firm X,1\n" +
                "Peter,Firm X,2\n" +
                "John,Firm X,1\n" +
                "John,Firm X,2\n" +
                "John,Firm X,1\n" +
                "John,Firm X,2\n" +
                "John,Firm X,1\n" +
                "John,Firm X,2\n" +
                "John,Firm X,1\n" +
                "John,Firm X,2\n" +
                "Anthony,Firm X,1\n" +
                "Anthony,Firm X,2\n" +
                "Fabrice,Firm A,1\n" +
                "Fabrice,Firm A,2\n" +
                "Fabrice,Firm A,1\n" +
                "Fabrice,Firm A,2\n" +
                "Oliver,Firm B,1\n" +
                "Oliver,Firm B,2\n" +
                "David,Firm B,1\n" +
                "David,Firm B,2\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testSemiStructuredMultiFlatten()
    {
        String queryFunction = "flatten::semiStructuredMultiFlatten__TabularDataSet_1_";

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("Peter,A1,1,O1\n" +
                "Peter,A1,1,O2\n" +
                "Peter,A11,1,O1\n" +
                "Peter,A11,1,O2\n" +
                "John,A2,1,O1\n" +
                "John,A2,1,O2\n" +
                "John,A22,1,O1\n" +
                "John,A22,1,O2\n" +
                "John,A3,1,O1\n" +
                "John,A3,1,O2\n" +
                "John,A32,1,O1\n" +
                "John,A32,1,O2\n" +
                "Anthony,A4,1,O1\n" +
                "Anthony,A4,1,O2\n" +
                "Fabrice,A5,1,O3\n" +
                "Fabrice,A5,1,O4\n" +
                "Fabrice,A52,1,O3\n" +
                "Fabrice,A52,1,O4\n" +
                "Oliver,A6,1,O5\n" +
                "Oliver,A6,1,O6\n" +
                "David,A7,1,O5\n" +
                "David,A7,1,O6\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testSemiStructuredMultiLevelFlattenMerging()
    {
        String queryFunction = "flatten::semiStructuredMultiLevelFlattenMerging__TabularDataSet_1_";

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("Peter,A1,1,S1,\n" +
                "Peter,A1,2,,C1\n" +
                "Peter,A11,1,S1,\n" +
                "Peter,A11,2,,C2\n" +
                "John,A2,1,S2,\n" +
                "John,A2,2,,C2\n" +
                "John,A22,1,S1,\n" +
                "John,A22,2,,C3\n" +
                "John,A3,1,S3,\n" +
                "John,A3,2,,C1\n" +
                "John,A32,1,S1,\n" +
                "John,A32,2,,C1\n" +
                "Anthony,A4,1,S1,\n" +
                "Anthony,A4,2,,C3\n" +
                "Fabrice,A5,1,S4,\n" +
                "Fabrice,A5,2,,C2\n" +
                "Fabrice,A52,1,S1,\n" +
                "Fabrice,A52,2,,C4\n" +
                "Oliver,A6,1,S5,\n" +
                "Oliver,A6,2,,C4\n" +
                "David,A7,1,S1,\n" +
                "David,A7,2,,C1\n", h2Result.replace("\r\n", "\n"));
    }

    @Override
    public String modelResourcePath()
    {
        return "/org/finos/legend/engine/plan/execution/stores/relational/test/semiStructured/semiStructuredFlattening.pure";
    }
}
