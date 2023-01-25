package org.finos.legend.engine;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public interface SessionHandler {

    PreparedStatement prepareStatement(String query) throws Exception;

    Statement createStatement() throws Exception;
}
