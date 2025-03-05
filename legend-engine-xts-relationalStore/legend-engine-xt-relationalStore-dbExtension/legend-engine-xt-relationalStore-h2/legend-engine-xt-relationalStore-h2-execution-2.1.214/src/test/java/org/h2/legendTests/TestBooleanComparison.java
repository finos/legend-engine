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

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import java.sql.*;

 public class TestBooleanComparison extends TestH2Abstract
{
    @Test
    public void testBooleanToVarcharComparison() throws SQLException
    {
        String query = "Select CASE\n" +
                "    WHEN false = 'false' THEN 'Ok'\n" +
                "    ELSE 'Error'\n" +
                "END AS BooleanVarcharComparison";
        ResultSet rs = h2ConnStatement.executeQuery(query);
        rs.next();
        Assert.assertEquals("Ok", rs.getString("BooleanVarcharComparison"));
    }

    @Test
    public void testBooleanToIntegerComparison() throws SQLException
    {
        String query = "Select CASE\n" +
                "    WHEN false = 0 THEN 'Ok'\n" +
                "    ELSE 'Error'\n" +
                "END AS BooleanIntegerComparison";
        ResultSet rs = h2ConnStatement.executeQuery(query);
        rs.next();
        Assert.assertEquals("Ok", rs.getString("BooleanIntegerComparison"));
    }

    @Test
    public void testBooleanToFloatComparison() throws SQLException
    {
        String query = "Select CASE\n" +
                "    WHEN false = 0.0 THEN 'Ok'\n" +
                "    ELSE 'Error'\n" +
                "END AS BooleanFloatComparison";
        ResultSet rs = h2ConnStatement.executeQuery(query);
        rs.next();
        Assert.assertEquals("Ok", rs.getString("BooleanFloatComparison"));
    }
}