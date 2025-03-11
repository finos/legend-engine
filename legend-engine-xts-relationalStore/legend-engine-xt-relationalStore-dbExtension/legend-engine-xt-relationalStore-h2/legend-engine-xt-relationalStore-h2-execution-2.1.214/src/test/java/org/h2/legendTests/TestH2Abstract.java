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
        String url = "jdbc:h2:~/test" + defaultH2Properties;
        h2Conn = DriverManager.getConnection(url);
        System.out.println("H2 Connection Acquired....");
        h2ConnStatement = h2Conn.createStatement();
    }

    @AfterClass
    public static void tearDownClass() throws Exception
    {
        h2ConnStatement.execute("DROP ALL OBJECTS");
        h2ConnStatement.close();
        h2Conn.close();
        System.out.println("H2 Connection Closed....");
    }

}
