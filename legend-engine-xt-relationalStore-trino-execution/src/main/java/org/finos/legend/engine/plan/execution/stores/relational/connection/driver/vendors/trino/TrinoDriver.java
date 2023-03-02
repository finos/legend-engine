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
        properties.keySet().stream().filter(key -> propertiesForDriver.contains(key)).forEach(key -> trinoDriverProperties.put(key,properties.get(key)));
        if (!Boolean.parseBoolean(properties.getProperty("SSL")))
        {
            trinoDriverProperties.remove("password");
        }

        return trinoDriverProperties;
    }

}
