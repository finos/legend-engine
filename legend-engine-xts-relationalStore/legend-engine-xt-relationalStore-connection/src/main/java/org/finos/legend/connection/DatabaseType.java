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

package org.finos.legend.connection;

public enum DatabaseType implements Database
{
    H2("H2"),
    POSTGRES("Postgres"),
    BIG_QUERY("BigQuery"),
    SNOWFLAKE("Snowflake");

    private final String label;

    private DatabaseType(String label)
    {
        this.label = label;
    }

    @Override
    public String getLabel()
    {
        return this.label;
    }
}
