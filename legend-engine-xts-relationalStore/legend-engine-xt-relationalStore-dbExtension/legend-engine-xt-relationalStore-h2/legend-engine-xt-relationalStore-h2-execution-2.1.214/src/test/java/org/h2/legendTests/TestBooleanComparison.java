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