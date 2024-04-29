//  Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.result;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.result.SQLResultColumn;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class TestSqlResultDBColumnMetaData
{

    private SQLResultDBColumnsMetaData metaData;
    private ResultSetMetaData resultSetMetaData;


    @Before
    public void setUp() throws SQLException
    {
        resultSetMetaData = Mockito.mock(ResultSetMetaData.class);
        try
        {
            when(resultSetMetaData.getColumnType(1)).thenReturn(Types.TIMESTAMP);
            when(resultSetMetaData.getColumnType(2)).thenReturn(Types.DATE);
            when(resultSetMetaData.getColumnType(3)).thenReturn(Types.VARCHAR);

        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        List<SQLResultColumn> resultColumns = Lists.mutable.of(
                new SQLResultColumn("Column1", "TIMESTAMP"),
                new SQLResultColumn("Column2", "DATE"),
                new SQLResultColumn("Column3", "STRING")

        );

        metaData = new SQLResultDBColumnsMetaData(resultColumns, resultSetMetaData);
    }

    @Test
    public void testIsTimestampColumn()
    {
        assertTrue(metaData.isTimestampColumn(1));
        assertFalse(metaData.isTimestampColumn(2));
        assertFalse(metaData.isTimestampColumn(3));

    }


    @Test
    public void testIsDateColumn()
    {
        assertFalse(metaData.isDateColumn(1));
        assertTrue(metaData.isDateColumn(2));
        assertFalse(metaData.isDateColumn(3));

    }

}
