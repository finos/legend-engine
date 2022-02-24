package org.finos.legend.engine.server.test.pureClient.stores;

import junit.framework.Test;
import junit.framework.TestSuite;

public class Test_Relational_UsingPureClientTestSuite_TextMode extends TestSuite
{
    public static Test suite() throws Exception
    {
        System.setProperty("legend.test.serializationKind", "text");
        return Test_Relational_UsingPureClientTestSuite.suite();
    }
}
