// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.finos.legend.engine.query.sql.api.grammar;

import io.dropwizard.testing.junit.ResourceTestRule;
import org.finos.legend.engine.protocol.sql.metamodel.AllColumns;
import org.finos.legend.engine.protocol.sql.metamodel.Node;
import org.finos.legend.engine.protocol.sql.metamodel.Query;
import org.finos.legend.engine.protocol.sql.metamodel.QuerySpecification;
import org.finos.legend.engine.protocol.sql.metamodel.Relation;
import org.finos.legend.engine.protocol.sql.metamodel.SelectItem;
import org.finos.legend.engine.protocol.sql.metamodel.Table;
import org.finos.legend.engine.query.sql.api.MockPac4jFeature;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import java.io.IOException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SqlGrammarTest
{

    @ClassRule
    public static final ResourceTestRule resources = ResourceTestRule.builder()
            .addResource(new SqlGrammar())
            .addResource(new MockPac4jFeature())
            .build();

    @Test
    public void grammarToJson()
    {
        Node node = resources.target("sql/v1/grammar/grammarToJson").request().post(Entity.text("SELECT * FROM service.\"/personService\"")).readEntity(Node.class);
        assertTrue(node instanceof Query);
        Query query = (Query) node;
        assertTrue(query.queryBody instanceof QuerySpecification);
        QuerySpecification querySpecification = (QuerySpecification) query.queryBody;
        assertEquals(1, querySpecification.select.selectItems.size());
        SelectItem selectItem = querySpecification.select.selectItems.get(0);
        assertTrue(selectItem instanceof AllColumns);
        assertEquals(1, querySpecification.from.size());
        Relation relation = querySpecification.from.get(0);
        assertTrue(relation instanceof Table);
        Table table = (Table) relation;
        assertTrue(table.name.parts.contains("service"));
        assertTrue(table.name.parts.contains("/personService"));
    }

    @Test
    public void jsonToGrammar()
    {
        String query = resources.target("sql/v1/grammar/jsonToGrammar").request()
                .post(Entity.json("{\"_type\":\"query\",\"orderBy\":[],\"queryBody\":{\"_type\":\"querySpecification\",\"from\":[{\"_type\":\"table\",\"name\":{\"parts\":[\"service\",\"/personService\"]}}],\"groupBy\":[],\"orderBy\":[],\"select\":{\"_type\":\"select\",\"distinct\":false,\"selectItems\":[{\"_type\":\"allColumns\"}]}}}")).readEntity(String.class);
        assertEquals("select * from service./personService", query);

    }
}