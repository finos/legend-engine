// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.persistence.components.executor;

import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;

import java.util.List;
import java.util.Map;

public interface RelationalExecutionHelper
{

    String COLUMN_NAME = "COLUMN_NAME";

    void beginTransaction();

    void commitTransaction();

    void revertTransaction();

    void closeTransactionManager();

    boolean doesTableExist(Dataset dataset);

    void validateDatasetSchema(Dataset dataset, TypeMapping datatypeMapping);

    Dataset constructDatasetFromDatabase(Dataset dataset, TypeMapping mapping, boolean escape);

    void executeStatement(String sql);

    void executeStatements(List<String> sqls);

    List<Map<String, Object>> executeQuery(String sql, int rows);

    List<Map<String, Object>> executeQuery(String sql);

    TabularData executeQueryAsTabularData(String sql);

    TabularData executeQueryAsTabularData(String sql, int rows);

    void close();
}