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

package org.finos.legend.engine.query.graphQL.api.cache;

import org.junit.Assert;
import org.junit.Test;

public class TestGraphQLCacheKeys
{
    @Test
    public void testGraphQLDevCacheKeyExcludesVariablesFromEquality()
    {
        GraphQLDevCacheKey key1 = new GraphQLDevCacheKey("project1", "workspace1", "simple::model::Query", "simple::mapping::Map", "simple::runtime::Runtime", "simple::binding::Binding", "query { allFirms { legalName } }", "{\"id\": 1}");
        GraphQLDevCacheKey key2 = new GraphQLDevCacheKey("project1", "workspace1", "simple::model::Query", "simple::mapping::Map", "simple::runtime::Runtime", "simple::binding::Binding", "query { allFirms { legalName } }", "{\"id\": 2}");

        Assert.assertEquals(key1, key2);
        Assert.assertEquals(key1.hashCode(), key2.hashCode());
    }

    @Test
    public void testGraphQLDevCacheKeyStillDistinguishesDifferentQueryText()
    {
        GraphQLDevCacheKey key1 = new GraphQLDevCacheKey("project1", "workspace1", "simple::model::Query", "simple::mapping::Map", "simple::runtime::Runtime", "query { allFirms { legalName } }");
        GraphQLDevCacheKey key2 = new GraphQLDevCacheKey("project1", "workspace1", "simple::model::Query", "simple::mapping::Map", "simple::runtime::Runtime", "query { allFirms { id } }");

        Assert.assertNotEquals(key1, key2);
    }

    @Test
    public void testGraphQLProdMappingRuntimeCacheKeyExcludesVariablesFromEquality()
    {
        GraphQLProdMappingRuntimeCacheKey key1 = new GraphQLProdMappingRuntimeCacheKey("org.finos.legend.graphql", "model.one", "1.0.0", "simple::mapping::Map", "simple::runtime::Runtime", "simple::binding::Binding", "simple::model::Query", "query { allFirms { legalName } }", "{\"id\": 1}");
        GraphQLProdMappingRuntimeCacheKey key2 = new GraphQLProdMappingRuntimeCacheKey("org.finos.legend.graphql", "model.one", "1.0.0", "simple::mapping::Map", "simple::runtime::Runtime", "simple::binding::Binding", "simple::model::Query", "query { allFirms { legalName } }", "{\"id\": 2}");

        Assert.assertEquals(key1, key2);
        Assert.assertEquals(key1.hashCode(), key2.hashCode());
    }
}
