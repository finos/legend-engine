// Copyright 2026 Databricks
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

package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.databricks;

import org.junit.Assert;
import org.junit.Test;

public class TestDatabricksManager
{
    private final DatabricksManager manager = new DatabricksManager();

    // A trimmed but structurally faithful capture of a real response observed against a live Databricks cluster
    // for a SQL statement guarded by a `raise_error(...)` call (see databricksExtension.pure's bitShiftLeft
    // registration) - the full response also carries dozens more "at ..." stack trace lines, omitted here since
    // the extraction doesn't depend on their number.
    private static final String RAISED_EXCEPTION_SAMPLE =
            "Operation failed with error: [no error details from server] for statement [01f1b095-6fe9-11f6-aa6c-578051885f72], "
                    + "with response [TGetOperationStatusResp(status:TStatus(statusCode:SUCCESS_STATUS), operationState:ERROR_STATE, "
                    + "sqlState:P0001, errorCode:0, errorMessage:org.apache.hive.service.cli.HiveSQLException: Error running query: "
                    + "[USER_RAISED_EXCEPTION] org.apache.spark.SparkRuntimeException: [USER_RAISED_EXCEPTION] Unsupported number of "
                    + "bits to shift - max bits allowed is 62 SQLSTATE: P0001\n"
                    + "\tat org.apache.spark.sql.hive.thriftserver.HiveThriftServerErrors$.runningQueryError(HiveThriftServerErrors.scala:57)\n"
                    + "\tat org.apache.spark.sql.hive.thriftserver.SparkExecuteStatementOperation.$anonfun$execute$1(SparkExecuteStatementOperation.scala:1229)\n"
                    + "\t... 62 more\n"
                    + ")]";

    @Test
    public void extractsRaisedMessageFromHiveThriftWrapper()
    {
        Assert.assertEquals("Unsupported number of bits to shift - max bits allowed is 62", this.manager.cleanErrorMessage(RAISED_EXCEPTION_SAMPLE));
    }

    @Test
    public void leavesMessagesWithoutTheWrapperUnchanged()
    {
        String plainMessage = "Table or view not found: does_not_exist";
        Assert.assertEquals(plainMessage, this.manager.cleanErrorMessage(plainMessage));
    }

    @Test
    public void toleratesANullMessage()
    {
        Assert.assertNull(this.manager.cleanErrorMessage(null));
    }
}
