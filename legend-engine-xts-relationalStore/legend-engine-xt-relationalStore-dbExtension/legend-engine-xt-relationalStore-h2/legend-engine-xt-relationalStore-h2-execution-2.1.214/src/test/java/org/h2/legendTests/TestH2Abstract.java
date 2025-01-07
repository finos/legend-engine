package org.h2.legendTests;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class TestH2Abstract
{
    static Connection h2Conn;
    static Statement h2ConnStatement;

    @BeforeClass
    public static void setupClass() throws Exception
    {
        Class.forName("org.h2.Driver"); // Driver name
        String defaultH2Properties = ";NON_KEYWORDS=ANY,ASYMMETRIC,AUTHORIZATION,CAST,CURRENT_PATH,CURRENT_ROLE,DAY,DEFAULT,ELSE,END,HOUR,KEY,MINUTE,MONTH,SECOND,SESSION_USER,SET,SOME,SYMMETRIC,SYSTEM_USER,TO,UESCAPE,USER,VALUE,WHEN,YEAR;MODE=LEGACY";
        String url = "jdbc:h2:~/test"+ defaultH2Properties;
        h2Conn = DriverManager.getConnection(url);
        System.out.println("H2 Connection Acquired....");
        h2ConnStatement = h2Conn.createStatement();
    }

    @AfterClass
    public static void tearDownClass() throws Exception
    {
        h2ConnStatement.close();
        h2Conn.close();
        System.out.println("H2 Connection Closed....");
    }

}
