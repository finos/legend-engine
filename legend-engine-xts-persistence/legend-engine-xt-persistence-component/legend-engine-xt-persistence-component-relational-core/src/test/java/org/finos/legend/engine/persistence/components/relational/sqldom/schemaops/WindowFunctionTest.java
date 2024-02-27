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

package org.finos.legend.engine.persistence.components.relational.sqldom.schemaops;

import org.finos.legend.engine.persistence.components.relational.sqldom.common.FunctionName;
import org.finos.legend.engine.persistence.components.relational.sqldom.common.Order;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.Table;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Field;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Function;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.OrderedField;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.WindowFunction;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WindowFunctionTest
{
    @Test
    void testWithPartitionFields()
    {
        Function rowNumber = new Function(FunctionName.ROW_NUMBER, null, BaseTest.QUOTE_IDENTIFIER);
        Table tableA = new Table("mydb", null, "mytable1", "stage", BaseTest.QUOTE_IDENTIFIER);
        Field field1 = new Field(tableA.getAlias(), "field1", BaseTest.QUOTE_IDENTIFIER, "field1");
        Field field2 = new Field(tableA.getAlias(), "field2", BaseTest.QUOTE_IDENTIFIER, "field2");
        List<Field> partitionFields = Arrays.asList(field1, field2);

        WindowFunction windowFunction = new WindowFunction("row_num", BaseTest.QUOTE_IDENTIFIER, rowNumber, partitionFields, null);

        String sql = BaseTest.genSql(windowFunction);
        assertEquals("ROW_NUMBER() OVER (PARTITION BY stage.\"field1\",stage.\"field2\") as \"row_num\"", sql);
    }

    @Test
    void testWithOrderByFields()
    {
        Function rowNumber = new Function(FunctionName.ROW_NUMBER, null, BaseTest.QUOTE_IDENTIFIER);
        Table tableA = new Table("mydb", null, "mytable1", "stage", BaseTest.QUOTE_IDENTIFIER);
        OrderedField field1 = new OrderedField(tableA.getAlias(), "field1", BaseTest.QUOTE_IDENTIFIER, "field1", Optional.of(Order.ASC));
        OrderedField field2 = new OrderedField(tableA.getAlias(), "field2", BaseTest.QUOTE_IDENTIFIER, "field2", Optional.empty());
        List<OrderedField> orderByFields = Arrays.asList(field1, field2);

        WindowFunction windowFunction = new WindowFunction(BaseTest.QUOTE_IDENTIFIER, rowNumber, null, orderByFields);

        String sql = BaseTest.genSql(windowFunction);
        assertEquals("ROW_NUMBER() OVER (ORDER BY stage.\"field1\" ASC,stage.\"field2\")", sql);
    }

    @Test
    void testWithPartitionFieldsAndOrderByFields()
    {
        Function rowNumber = new Function(FunctionName.ROW_NUMBER, null, BaseTest.QUOTE_IDENTIFIER);
        Table tableA = new Table("mydb", null, "mytable1", "stage", BaseTest.QUOTE_IDENTIFIER);
        Field field1 = new Field(tableA.getAlias(), "field1", BaseTest.QUOTE_IDENTIFIER, "field1");
        Field field2 = new Field(tableA.getAlias(), "field2", BaseTest.QUOTE_IDENTIFIER, "field2");
        List<Field> partitionFields = Arrays.asList(field1, field2);

        OrderedField field3 = new OrderedField(tableA.getAlias(), "field1", BaseTest.QUOTE_IDENTIFIER, "field1", Optional.of(Order.ASC));
        OrderedField field4 = new OrderedField(tableA.getAlias(), "field2", BaseTest.QUOTE_IDENTIFIER, "field2", Optional.empty());
        List<OrderedField> orderByFields = Arrays.asList(field3, field4);


        WindowFunction windowFunction = new WindowFunction(BaseTest.QUOTE_IDENTIFIER, rowNumber, partitionFields, orderByFields);

        String sql = BaseTest.genSql(windowFunction);
        assertEquals("ROW_NUMBER() OVER (PARTITION BY stage.\"field1\",stage.\"field2\" ORDER BY stage.\"field1\" ASC,stage.\"field2\")", sql);
    }

}
