package org.finos.legend.engine.pg.postgres.jdbc;

import org.finos.legend.engine.Session;
import org.finos.legend.engine.SessionHandler;
import org.finos.legend.engine.SessionsFactory;
import org.finos.legend.engine.pg.postgres.auth.User;
import org.jetbrains.annotations.Nullable;

import java.sql.*;

public class JDBCSessionFactory implements SessionsFactory {

    private Connection connection;
    private String connectionString;
    private String user;
    private String password;

    public JDBCSessionFactory(String connectionString, String user, String password) {
        this.connectionString = connectionString;
        this.user = user;
        this.password = password;
    }

    @Override
    public Session createSession(@Nullable String defaultSchema, User authenticatedUser) {
        return new Session(new SessionHandler() {
            @Override
            public PreparedStatement prepareStatement(String query) throws SQLException {
                return getConnection().prepareStatement(query);
            }

            @Override
            public Statement createStatement() throws SQLException {
                return getConnection().createStatement();
            }
        });
    }


    private Connection getConnection() throws SQLException {
        if (connection == null) {
            this.connection = DriverManager.getConnection(connectionString, user, password);
        }
        return connection;
    }

    public static void main(String[] args) {
        JDBCSessionFactory sessionFactory = new JDBCSessionFactory("jdbc:postgresql://localhost:5432/postgres", "postgres", "vika");
        Session session = sessionFactory.createSession(null, null);
        session.executeSimple("select * from public.demo");
    }
}   
