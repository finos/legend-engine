//  Copyright 2025 Goldman Sachs
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

package org.h2.legendTests;

import org.junit.Assert;
import org.junit.Test;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TestNoCharPadding extends TestH2Abstract
{
    @Test
    public void testBooleanToVarcharComparison() throws SQLException
    {
        h2ConnStatement.execute("create table myTable (charCol char(10));");
        h2ConnStatement.execute("insert into myTable (charCol) values ('val');");

        ResultSet rs = h2ConnStatement.executeQuery("select charCol from myTable");
        rs.next();
        Assert.assertEquals("val", rs.getString(1));     // instead  'val       '   ( padded 10 length)
    }
}
