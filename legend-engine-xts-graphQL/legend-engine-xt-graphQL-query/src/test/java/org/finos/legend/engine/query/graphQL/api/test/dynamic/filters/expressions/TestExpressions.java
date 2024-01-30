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

package org.finos.legend.engine.query.graphQL.api.test.dynamic.filters.expressions;

import org.finos.legend.engine.query.graphQL.api.test.dynamic.filters.*;
import org.junit.*;

public class TestExpressions extends TestGraphQLDynamicFilters
{
    @Test
    public void testSimpleAnd() throws Exception
    {
        String query = "query Query {\n" +
                "  allEmployees(where: { _and : [ { firstName: { _eq: \"John\" } }, { lastName: { _eq: \"Hill\" } } ] }) {\n" +
                "      fullName" +
                "    }\n" +
                "  }";
        String expected = "{\"data\":{\"allEmployees\":{\"fullName()\":\"John Hill\"}}}";
        runTest(query, expected);
    }

    @Test
    public void testSimpleOr() throws Exception
    {
        String query = "query Query {\n" +
                "  allEmployees(where: { _or : [ { firstName: { _eq: \"John\" } }, { lastName: { _eq: \"Hill\" } } ] }) {\n" +
                "      fullName" +
                "    }\n" +
                "  }";
        String expected = "{\"data\":{\"allEmployees\":[{\"fullName()\":\"John Johnson\"},{\"fullName()\":\"John Hill\"},{\"fullName()\":\"Oliver Hill\"}]}}";
        runTest(query, expected);
    }
}
