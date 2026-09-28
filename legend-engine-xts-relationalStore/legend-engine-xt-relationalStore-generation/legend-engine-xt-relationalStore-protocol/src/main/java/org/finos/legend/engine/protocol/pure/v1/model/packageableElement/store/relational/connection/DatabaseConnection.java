// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.legacy.PostProcessorWithParameter;

import java.util.Collections;
import java.util.List;

public abstract class DatabaseConnection extends Connection
{
    public DatabaseType type;
    public String timeZone;
    public Boolean quoteIdentifiers;
    public Integer queryTimeOutInSeconds;

    public List<PostProcessorWithParameter> postProcessorWithParameter = Collections.emptyList();
    public List<RelationalQueryGenerationConfig> queryGenerationConfigs;

    public String getTimeZone()
    {
        return this.timeZone;
    }

    public void setTimeZone(String timeZone)
    {
        this.timeZone = fixTimeZoneId(timeZone);
    }

    /**
     * Compatibility only: before the connection parser stripped them, the grammar's quotes around a zone id were
     * kept as part of the value, so protocol JSON written then carries 'US/Arizona', quotes and all, which names no
     * zone. The grammar only ever quoted with single quotes.
     */
    private static String fixTimeZoneId(String timeZone)
    {
        if ((timeZone != null) &&
                (timeZone.length() >= 2) &&
                (timeZone.charAt(0) == '\'') &&
                (timeZone.charAt(timeZone.length() - 1) == '\''))
        {
            return timeZone.substring(1, timeZone.length() - 1);
        }
        return timeZone;
    }
}
