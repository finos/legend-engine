package org.finos.legend.engine.persistence.components.relational.executor;

import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.relational.sql.DataTypeMapping;
import org.finos.legend.engine.persistence.components.relational.sql.JdbcPropertiesToLogicalDataTypeMapping;

import java.util.List;
import java.util.Map;

public interface RelationalExecutionHelper {
    void beginTransaction();

    void commitTransaction();

    void revertTransaction();

    void closeTransactionManager();

    boolean doesTableExist(Dataset dataset);

    void validateDatasetSchema(Dataset dataset, DataTypeMapping datatypeMapping);

    Dataset constructDatasetFromDatabase(String tableName, String schemaName, String databaseName, JdbcPropertiesToLogicalDataTypeMapping mapping);

    void executeStatement(String sql);

    void executeStatements(List<String> sqls);

    List<Map<String, Object>> executeQuery(String sql);

    void close();
}
