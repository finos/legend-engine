package org.finos.legend.h2;

import java.sql.DriverManager;
import java.sql.SQLException;

public class H2Defaults
{
    public static int getMajorVersion()
    {
        try
        {
            return DriverManager.getDriver("jdbc:h2:").getMajorVersion();
        }
        catch (SQLException e)
        {
            throw new RuntimeException("cannot identify H2 driver major version", e);
        }
    }
    public static String getDefaultH2Properties()
    {
        String defaultH2Properties;
        if (getMajorVersion() == 2)
        {
            defaultH2Properties = System.getProperty("legend.test.h2.properties",
                    ";NON_KEYWORDS=ANY,ASYMMETRIC,AUTHORIZATION,CAST,CURRENT_PATH,CURRENT_ROLE,DAY,DEFAULT,ELSE,END,HOUR,KEY,MINUTE,MONTH,SECOND,SESSION_USER,SET,SOME,SYMMETRIC,SYSTEM_USER,TO,UESCAPE,USER,VALUE,WHEN,YEAR,OVER;MODE=LEGACY");
        }
        else
        {
            defaultH2Properties = "";
        }
        return defaultH2Properties;
    }

}
