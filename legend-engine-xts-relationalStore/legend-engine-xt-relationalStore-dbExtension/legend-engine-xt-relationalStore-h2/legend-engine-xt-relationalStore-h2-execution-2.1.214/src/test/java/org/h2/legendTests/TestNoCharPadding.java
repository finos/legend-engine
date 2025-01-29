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
