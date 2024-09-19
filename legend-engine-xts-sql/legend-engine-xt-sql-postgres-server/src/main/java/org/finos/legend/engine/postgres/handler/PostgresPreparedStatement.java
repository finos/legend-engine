// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.postgres.handler;

import java.sql.ParameterMetaData;

public interface PostgresPreparedStatement
{

    void setObject(int i, Object o) throws Exception;

    PostgresResultSetMetaData getMetaData() throws Exception;

    ParameterMetaData getParameterMetaData() throws Exception;

    void close() throws Exception;

    void setMaxRows(int maxRows) throws Exception;

    int getMaxRows() throws Exception;

    boolean isExecuted();

    boolean execute() throws Exception;

    PostgresResultSet getResultSet() throws Exception;

    static ParameterMetaData emptyParameterMetaData()
    {
        return new ParameterMetaData()
        {
            @Override
            public int getParameterCount()
            {
                return 0;
            }

            @Override
            public int isNullable(int param)
            {
                return ParameterMetaData.parameterNullableUnknown;
            }

            @Override
            public boolean isSigned(int param)
            {
                return false;
            }

            @Override
            public int getPrecision(int param)
            {
                return 0;
            }

            @Override
            public int getScale(int param)
            {
                return 0;
            }

            @Override
            public int getParameterType(int param)
            {
                return 0;
            }

            @Override
            public String getParameterTypeName(int param)
            {
                return null;
            }

            @Override
            public String getParameterClassName(int param)
            {
                return null;
            }

            @Override
            public int getParameterMode(int param)
            {
                return ParameterMetaData.parameterModeUnknown;
            }

            @Override
            public <T> T unwrap(Class<T> iface)
            {
                return null;
            }

            @Override
            public boolean isWrapperFor(Class<?> iface)
            {
                return false;
            }
        };
    }
}
