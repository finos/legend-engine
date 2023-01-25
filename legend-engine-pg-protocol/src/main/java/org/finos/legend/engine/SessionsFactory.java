package org.finos.legend.engine;

import org.finos.legend.engine.pg.postgres.auth.User;

import javax.annotation.Nullable;

public interface SessionsFactory {
    public Session createSession(@Nullable String defaultSchema, User authenticatedUser);

}
