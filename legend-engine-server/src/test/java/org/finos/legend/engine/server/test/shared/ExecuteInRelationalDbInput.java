package org.finos.legend.engine.server.test.shared;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;

public class ExecuteInRelationalDbInput
{
    @JsonProperty
    RelationalDatabaseConnection connection;

    @JsonProperty
    List<String> sqls;
}
