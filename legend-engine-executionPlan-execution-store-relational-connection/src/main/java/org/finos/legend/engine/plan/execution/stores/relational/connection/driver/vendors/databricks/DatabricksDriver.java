// Copyright 2021 Databricks
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

import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DriverWrapper;

public class DatabricksDriver extends DriverWrapper
{
    public static String DRIVER_CLASSNAME = "org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.databricks.DatabricksDriverWrapper";

    @Override
    protected String getClassName()
    {
        return DRIVER_CLASSNAME;
    }
}