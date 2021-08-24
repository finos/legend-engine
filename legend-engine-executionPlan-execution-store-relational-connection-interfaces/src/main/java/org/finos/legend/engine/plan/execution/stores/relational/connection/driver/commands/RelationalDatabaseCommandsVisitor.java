package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands;

public interface RelationalDatabaseCommandsVisitor<T> {
    T apply();
}
