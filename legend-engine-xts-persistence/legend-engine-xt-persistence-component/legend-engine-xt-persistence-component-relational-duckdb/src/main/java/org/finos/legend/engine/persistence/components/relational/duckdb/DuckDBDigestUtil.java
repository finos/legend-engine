// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.relational.duckdb;

import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcHelper;

public class DuckDBDigestUtil
{
    private static final String CUSTOM_MD5_UDF = "MD5(SUBSTRING(HEX(IFNULL(NULLIF(A, FROM_HEX('')), FROM_HEX('DADB2C00'))), 9))";
    private static final String CUSTOM_COLUMN_UDF = "IFNULL((FROM_HEX('DADB2C00') || ENCODE(COLUMN_NAME) || FROM_HEX('DADB2C00') || ENCODE(COLUMN_VALUE)), FROM_HEX(''))";

    public static void registerMD5Udf(JdbcHelper sink, String UdfName)
    {
        sink.executeStatement("CREATE FUNCTION " + UdfName + "(A) AS " + CUSTOM_MD5_UDF);
    }

    public static void registerColumnUdf(JdbcHelper sink, String UdfName)
    {
        sink.executeStatement("CREATE FUNCTION " + UdfName + "(COLUMN_NAME, COLUMN_VALUE) AS " + CUSTOM_COLUMN_UDF);
    }
}
