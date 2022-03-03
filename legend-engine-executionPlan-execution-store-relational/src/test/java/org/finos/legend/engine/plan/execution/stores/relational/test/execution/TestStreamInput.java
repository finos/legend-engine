// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.test.execution;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.ipc.ArrowStreamReader;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.execution.stores.relational.connection.AlloyTestServer;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.plan.execution.stores.relational.serialization.RelationalResultToJsonDefaultSerializer;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

public class TestStreamInput extends AlloyTestServer
{
    @Override
    protected void insertTestData(Statement statement) throws SQLException
    {
        statement.execute("drop table if exists employeeTable");
        statement.execute("create table employeeTable(id INT, name VARCHAR(200), firmId INT, doh TIMESTAMP, type VARCHAR(200), active INT , skills VARCHAR(200))");
        statement.execute("insert into employeeTable (id, name, firmId, doh, type, active,skills) values (1, 'Alice',  0, '1983-03-15', 'FTC', 1, null)");
        statement.execute("insert into employeeTable (id, name, firmId, doh, type, active,skills) values (2, 'Bob',    0, '2003-07-19', 'FTE', 0, ',1,2,')");
        statement.execute("insert into employeeTable (id, name, firmId, doh, type, active,skills) values (3, 'Curtis', 0, '2012-08-25', 'FTO', null, ',3,2,')");

    }

    @Test
    public void testExecutionPlanWithStreamInput()
    {
        String plan = "{\"serializer\":{\"name\":\"pure\",\"version\":\"vX_X_X\"},\"templateFunctions\":[\"<#function renderCollection collection separator><#return collection?join(separator)><\\/#function>\",\"<#function collectionSize collection> <#return collection?size> <\\/#function>\"],\"rootExecutionNode\":{\"_type\":\"relationalBlock\",\"resultType\":{\"tdsColumns\":[{\"name\":\"name\",\"type\":\"String\",\"relationalType\":\"VARCHAR(1000)\"}],\"_type\":\"tds\"},\"executionNodes\":[{\"functionParameters\":[{\"name\":\"id\",\"supportsStream\":false,\"multiplicity\":{\"lowerBound\":0},\"class\":\"String\",\"_type\":\"var\"},{\"name\":\"name\",\"supportsStream\":true,\"multiplicity\":{\"lowerBound\":0},\"class\":\"String\",\"_type\":\"var\"}],\"_type\":\"function-parameters-validation\",\"resultType\":{\"dataType\":\"Boolean\",\"_type\":\"dataType\"}},{\"varName\":\"inFilterClause_name\",\"_type\":\"allocation\",\"resultType\":{\"dataType\":\"String\",\"_type\":\"dataType\"},\"executionNodes\":[{\"freeMarkerBooleanExpression\":\"${(instanceOf(name, \\\"Stream\\\") || ((collectionSize(name)) > 50))?c}\",\"trueBlock\":{\"_type\":\"sequence\",\"resultType\":{\"dataType\":\"String\",\"_type\":\"dataType\"},\"executionNodes\":[{\"inputVarNames\":[\"name\"],\"tempTableName\":\"tempTableForIn_name\",\"tempTableColumnMetaData\":[{\"column\":{\"label\":\"ColumnForStoringInCollection\",\"dataType\":\"VARCHAR(200)\"}}],\"connection\": {\"_type\": \"RelationalDatabaseConnection\",\"type\": \"H2\",\"authenticationStrategy\" : {\"_type\" : \"test\"},\"datasourceSpecification\" : {\"_type\" : \"h2Local\"}},\"_type\":\"createAndPopulateTempTable\",\"resultType\":{\"_type\":\"void\"},\"implementation\":{\"executionClassFullName\":\"com.alloy.plan.root.n2.n1.trueBlock.n1.CreateAndPopulateTempTable\",\"_type\":\"java\"}},{\"values\":{\"values\":[\"select \\\"temptableforin_name_0\\\".ColumnForStoringInCollection as ColumnForStoringInCollection from tempTableForIn_name as \\\"temptableforin_name_0\\\"\"],\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"_type\":\"string\"},\"_type\":\"constant\",\"resultType\":{\"dataType\":\"String\",\"_type\":\"dataType\"}}]},\"falseBlock\":{\"values\":{\"values\":[\"'${renderCollection(name \\\"','\\\")}'\"],\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"_type\":\"string\"},\"_type\":\"constant\",\"resultType\":{\"dataType\":\"String\",\"_type\":\"dataType\"}},\"_type\":\"freeMarkerConditionalExecutionNode\",\"resultType\":{\"dataType\":\"String\",\"_type\":\"dataType\"}}]},{\"_type\":\"relationalTdsInstantiation\",\"resultType\":{\"tdsColumns\":[{\"name\":\"name\",\"type\":\"String\",\"relationalType\":\"VARCHAR(1000)\"}],\"_type\":\"tds\"},\"executionNodes\":[{\"sqlQuery\":\"select \\\"root\\\".name as \\\"name\\\" from employeeTable as \\\"root\\\" where (\\\"root\\\".name in (${inFilterClause_name}) or (${collectionSize(id)}) = 0)\",\"resultColumns\":[{\"label\":\"\\\"name\\\"\",\"dataType\":\"VARCHAR(1000)\"}],\"connection\": {\"_type\": \"RelationalDatabaseConnection\",\"type\": \"H2\",\"authenticationStrategy\" : {\"_type\" : \"test\"},\"datasourceSpecification\" : {\"_type\" : \"h2Local\"}},\"_type\":\"sql\",\"resultType\":{\"dataType\":\"meta::pure::metamodel::type::Any\",\"_type\":\"dataType\"}}]}]},\"authDependent\":false,\"globalImplementationSupport\":{\"classes\":[{\"package\":\"com.alloy.plan.root.n2.n1.trueBlock.n1\",\"name\":\"CreateAndPopulateTempTable\",\"source\":\"package com.alloy.plan.root.n2.n1.trueBlock.n1;\\n\\nimport org.finos.legend.engine.plan.dependencies.store.relational.IRelationalCreateAndPopulateTempTableExecutionNodeSpecifics;\\nimport org.finos.legend.engine.plan.dependencies.util.Library;\\nimport java.util.Arrays;\\nimport java.util.List;\\nimport java.util.Optional;\\nimport java.util.stream.Collector;\\nimport java.util.stream.Collectors;\\nimport java.util.stream.Stream;\\n\\npublic class CreateAndPopulateTempTable implements IRelationalCreateAndPopulateTempTableExecutionNodeSpecifics {\\n\\n    public String getGetterNameForProperty(String p) {\\n        return p == null ? null : Arrays.asList(\\\"get\\\", Library.toOne(Optional.ofNullable(p).map(Stream::of).orElseGet(Stream::empty).map(Library::toUpperFirstCharacter).collect(Collectors.toList()))).stream().collect(Collectors.joining(\\\"\\\"));\\n    }\\n}\\n\"}],\"_type\":\"java\"}}";

        PlanExecutor planExecutor = buildRelationalPlanExecutor();

        Map<String, ?> allInputsAsList = Maps.mutable.with("name", Lists.mutable.with("Alice", "Bob", "Curtis"), "id", Lists.mutable.with("A", "B", "C"));
        RelationalResult resultWithLists = (RelationalResult)planExecutor.execute(plan, allInputsAsList);
        Assert.assertEquals("{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"name\",\"type\":\"String\",\"relationalType\":\"VARCHAR(1000)\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".name as \\\"name\\\" from employeeTable as \\\"root\\\" where (\\\"root\\\".name in ('Alice','Bob','Curtis') or (3) = 0)\"}], \"result\" : {\"columns\" : [\"name\"], \"rows\" : [{\"values\": [\"Alice\"]},{\"values\": [\"Bob\"]},{\"values\": [\"Curtis\"]}]}}", resultWithLists.flush(new RelationalResultToJsonDefaultSerializer(resultWithLists)));

        Map<String, ?> allInputsAsStream = Maps.mutable.with("name", Lists.mutable.with("Alice", "Bob", "Curtis").stream(), "id", Lists.mutable.with("A", "B", "C").stream());
        RelationalResult resultWithStreams = (RelationalResult)planExecutor.execute(plan, allInputsAsStream);
        Assert.assertEquals("{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"name\",\"type\":\"String\",\"relationalType\":\"VARCHAR(1000)\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".name as \\\"name\\\" from employeeTable as \\\"root\\\" where (\\\"root\\\".name in (select \\\"temptableforin_name_0\\\".ColumnForStoringInCollection as ColumnForStoringInCollection from tempTableForIn_name as \\\"temptableforin_name_0\\\") or (3) = 0)\"}], \"result\" : {\"columns\" : [\"name\"], \"rows\" : [{\"values\": [\"Alice\"]},{\"values\": [\"Bob\"]},{\"values\": [\"Curtis\"]}]}}", resultWithStreams.flush(new RelationalResultToJsonDefaultSerializer(resultWithStreams)));

        Map<String, ?> inputsAsListAndStream1 = Maps.mutable.with("name", Lists.mutable.with("Alice", "Bob", "Curtis").stream(), "id", Lists.mutable.with("A", "B", "C"));
        RelationalResult resultWithListAndStream1 = (RelationalResult)planExecutor.execute(plan, inputsAsListAndStream1);
        Assert.assertEquals("{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"name\",\"type\":\"String\",\"relationalType\":\"VARCHAR(1000)\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".name as \\\"name\\\" from employeeTable as \\\"root\\\" where (\\\"root\\\".name in (select \\\"temptableforin_name_0\\\".ColumnForStoringInCollection as ColumnForStoringInCollection from tempTableForIn_name as \\\"temptableforin_name_0\\\") or (3) = 0)\"}], \"result\" : {\"columns\" : [\"name\"], \"rows\" : [{\"values\": [\"Alice\"]},{\"values\": [\"Bob\"]},{\"values\": [\"Curtis\"]}]}}", resultWithListAndStream1.flush(new RelationalResultToJsonDefaultSerializer(resultWithListAndStream1)));

        Map<String, ?> inputsAsListAndStream2 = Maps.mutable.with("name", Lists.mutable.with("Alice", "Bob", "Curtis"), "id", Lists.mutable.with("A", "B", "C").stream());
        RelationalResult resultWithListAndStream2 = (RelationalResult)planExecutor.execute(plan, inputsAsListAndStream2);
        Assert.assertEquals("{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"name\",\"type\":\"String\",\"relationalType\":\"VARCHAR(1000)\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".name as \\\"name\\\" from employeeTable as \\\"root\\\" where (\\\"root\\\".name in ('Alice','Bob','Curtis') or (3) = 0)\"}], \"result\" : {\"columns\" : [\"name\"], \"rows\" : [{\"values\": [\"Alice\"]},{\"values\": [\"Bob\"]},{\"values\": [\"Curtis\"]}]}}", resultWithListAndStream2.flush(new RelationalResultToJsonDefaultSerializer(resultWithListAndStream2)));
    }

    @Test
    public void testExecutionPlanWithArrowStreamInput() throws IOException {
        String plan = "{\"serializer\":{\"name\":\"pure\",\"version\":\"vX_X_X\"},\"templateFunctions\":[\"<#function renderCollection collection separator><#return collection?join(separator)><\\/#function>\",\"<#function collectionSize collection> <#return collection?size> <\\/#function>\"],\"rootExecutionNode\":{\"_type\":\"relationalBlock\",\"resultType\":{\"tdsColumns\":[{\"name\":\"name\",\"type\":\"String\",\"relationalType\":\"VARCHAR(1000)\"}],\"_type\":\"tds\"},\"executionNodes\":[{\"functionParameters\":[{\"name\":\"id\",\"supportsStream\":false,\"multiplicity\":{\"lowerBound\":0},\"class\":\"String\",\"_type\":\"var\"},{\"name\":\"name\",\"supportsStream\":true,\"multiplicity\":{\"lowerBound\":0},\"class\":\"String\",\"_type\":\"var\"}],\"_type\":\"function-parameters-validation\",\"resultType\":{\"dataType\":\"Boolean\",\"_type\":\"dataType\"}},{\"varName\":\"inFilterClause_name\",\"_type\":\"allocation\",\"resultType\":{\"dataType\":\"String\",\"_type\":\"dataType\"},\"executionNodes\":[{\"freeMarkerBooleanExpression\":\"${(instanceOf(name, \\\"Stream\\\") || ((collectionSize(name)) > 50))?c}\",\"trueBlock\":{\"_type\":\"sequence\",\"resultType\":{\"dataType\":\"String\",\"_type\":\"dataType\"},\"executionNodes\":[{\"inputVarNames\":[\"name\"],\"tempTableName\":\"tempTableForIn_name\",\"tempTableColumnMetaData\":[{\"column\":{\"label\":\"ColumnForStoringInCollection\",\"dataType\":\"VARCHAR(200)\"}}],\"connection\": {\"_type\": \"RelationalDatabaseConnection\",\"type\": \"H2\",\"authenticationStrategy\" : {\"_type\" : \"test\"},\"datasourceSpecification\" : {\"_type\" : \"h2Local\"}},\"_type\":\"createAndPopulateTempTable\",\"resultType\":{\"_type\":\"void\"},\"implementation\":{\"executionClassFullName\":\"com.alloy.plan.root.n2.n1.trueBlock.n1.CreateAndPopulateTempTable\",\"_type\":\"java\"}},{\"values\":{\"values\":[\"select \\\"temptableforin_name_0\\\".ColumnForStoringInCollection as ColumnForStoringInCollection from tempTableForIn_name as \\\"temptableforin_name_0\\\"\"],\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"_type\":\"string\"},\"_type\":\"constant\",\"resultType\":{\"dataType\":\"String\",\"_type\":\"dataType\"}}]},\"falseBlock\":{\"values\":{\"values\":[\"'${renderCollection(name \\\"','\\\")}'\"],\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"_type\":\"string\"},\"_type\":\"constant\",\"resultType\":{\"dataType\":\"String\",\"_type\":\"dataType\"}},\"_type\":\"freeMarkerConditionalExecutionNode\",\"resultType\":{\"dataType\":\"String\",\"_type\":\"dataType\"}}]},{\"_type\":\"relationalTdsInstantiation\",\"resultType\":{\"tdsColumns\":[{\"name\":\"name\",\"type\":\"String\",\"relationalType\":\"VARCHAR(1000)\"}],\"_type\":\"tds\"},\"executionNodes\":[{\"sqlQuery\":\"select \\\"root\\\".name as \\\"name\\\" from employeeTable as \\\"root\\\" where (\\\"root\\\".name in (${inFilterClause_name}) or (${collectionSize(id)}) = 0)\",\"resultColumns\":[{\"label\":\"\\\"name\\\"\",\"dataType\":\"VARCHAR(1000)\"}],\"connection\": {\"_type\": \"RelationalDatabaseConnection\",\"type\": \"H2\",\"authenticationStrategy\" : {\"_type\" : \"test\"},\"datasourceSpecification\" : {\"_type\" : \"h2Local\"}},\"_type\":\"sql\",\"resultType\":{\"dataType\":\"meta::pure::metamodel::type::Any\",\"_type\":\"dataType\"}}]}]},\"authDependent\":false,\"globalImplementationSupport\":{\"classes\":[{\"package\":\"com.alloy.plan.root.n2.n1.trueBlock.n1\",\"name\":\"CreateAndPopulateTempTable\",\"source\":\"package com.alloy.plan.root.n2.n1.trueBlock.n1;\\n\\nimport org.finos.legend.engine.plan.dependencies.store.relational.IRelationalCreateAndPopulateTempTableExecutionNodeSpecifics;\\nimport org.finos.legend.engine.plan.dependencies.util.Library;\\nimport java.util.Arrays;\\nimport java.util.List;\\nimport java.util.Optional;\\nimport java.util.stream.Collector;\\nimport java.util.stream.Collectors;\\nimport java.util.stream.Stream;\\n\\npublic class CreateAndPopulateTempTable implements IRelationalCreateAndPopulateTempTableExecutionNodeSpecifics {\\n\\n    public String getGetterNameForProperty(String p) {\\n        return p == null ? null : Arrays.asList(\\\"get\\\", Library.toOne(Optional.ofNullable(p).map(Stream::of).orElseGet(Stream::empty).map(Library::toUpperFirstCharacter).collect(Collectors.toList()))).stream().collect(Collectors.joining(\\\"\\\"));\\n    }\\n}\\n\"}],\"_type\":\"java\"}}";

        PlanExecutor planExecutor = buildRelationalPlanExecutor();

        Map<String, ?> allInputsAsList = Maps.mutable.with("name", Lists.mutable.with("Alice", "Bob", "Curtis"), "id", Lists.mutable.with("A", "B", "C"));
        RelationalResult resultWithLists = (RelationalResult) planExecutor.execute(plan, allInputsAsList);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        resultWithLists.stream(out, SerializationFormat.ARROW);
        BufferAllocator allocator = new RootAllocator(Integer.MAX_VALUE);
        ArrowStreamReader reader = new ArrowStreamReader(new BufferedInputStream(new ByteArrayInputStream(out.toByteArray())), allocator);

        StringBuilder text = new StringBuilder();
        while(reader.loadNextBatch()) {
            VectorSchemaRoot readBatch = reader.getVectorSchemaRoot();
            text.append(readBatch.contentToTSVString());
        }
        Assert.assertEquals("name\n" +
                "Alice\n" +
                "Bob\n" +
                "Curtis\n", text.toString());
    }
}
