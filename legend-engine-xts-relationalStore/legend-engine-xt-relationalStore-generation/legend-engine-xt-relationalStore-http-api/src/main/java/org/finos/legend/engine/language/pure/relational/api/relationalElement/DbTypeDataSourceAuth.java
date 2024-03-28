//  Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.language.pure.relational.api.relationalElement;

import com.fasterxml.jackson.annotation.JsonGetter;

import java.util.Objects;

public class DbTypeDataSourceAuth
{
    private final String dbType;
    private final String dataSource;
    private final String authStrategy;

    public DbTypeDataSourceAuth(String dbType, String dataSource, String authStrategy)
    {
        this.dbType = dbType;
        this.dataSource = dataSource;
        this.authStrategy = authStrategy;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        DbTypeDataSourceAuth that = (DbTypeDataSourceAuth) o;
        return Objects.equals(dbType, that.dbType) && Objects.equals(dataSource, that.dataSource) && Objects.equals(authStrategy, that.authStrategy);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(dbType, dataSource, authStrategy);
    }

    @JsonGetter
    public String getDbType()
    {
        return this.dbType;
    }

    @JsonGetter
    public String getDataSource()
    {
        return this.dataSource;
    }

    @JsonGetter
    public String getAuthStrategy()
    {
        return this.authStrategy;
    }
}
