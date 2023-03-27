// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.trino;

import com.google.common.base.Strings;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DriverWrapper;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.TrinoDatasourceSpecificationRuntime;

import java.util.List;
import java.util.Properties;

public class TrinoDriver extends DriverWrapper
{
    public static String DRIVER_CLASSNAME = "io.trino.jdbc.TrinoDriver";

    @Override
    protected String getClassName()
    {
        return DRIVER_CLASSNAME;
    }

    @Override
    protected Properties handlePropertiesPriorToJDBCDriverConnection(Properties properties)
    {
        Properties trinoDriverProperties = new Properties();
        List<String> propertiesForDriver = TrinoDatasourceSpecificationRuntime.propertiesForDriver;

        // Keep only the valid Trino JDBC properties
        properties.keySet().stream().filter(key -> propertiesForDriver.contains(key)).forEach(key -> trinoDriverProperties.put(key,properties.get(key)));

        // Also remove null or blank properties
        // This is useful for running container based tests where we don't
        // have support for SSL or Kerberos
        if (Strings.isNullOrEmpty((String)trinoDriverProperties.get("password")))
        {
            trinoDriverProperties.remove("password");
        }

        return trinoDriverProperties;
    }

}
