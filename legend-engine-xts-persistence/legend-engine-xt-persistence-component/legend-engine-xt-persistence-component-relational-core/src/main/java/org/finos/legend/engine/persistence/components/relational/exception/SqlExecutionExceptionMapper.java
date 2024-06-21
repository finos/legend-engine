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

package org.finos.legend.engine.persistence.components.relational.exception;

import java.sql.SQLException;

public class SqlExecutionExceptionMapper
{
    public static SqlExecutionException from(SQLException sqlException)
    {
        String SQLState = sqlException.getSQLState();
        int errorCode = sqlException.getErrorCode();
        String classCode = getClassCode(sqlException);

        switch (classCode)
        {
            case "07" : return new DynamicSqlErrorException(sqlException.getMessage(), SQLState, errorCode);
            case "08" : return new ConnectionException(sqlException.getMessage(), SQLState, errorCode);
            case "20" :
            case "21" :
            case "22" : return new DataRelatedException(sqlException.getMessage(), SQLState, errorCode);
            case "23" : return new ConstraintViolationException(sqlException.getMessage(), SQLState, errorCode);
            case "40" : return new TransactionRollbackException(sqlException.getMessage(), SQLState, errorCode);
            case "42" : return new SqlSyntaxErrorOrAccessRuleViolationException(sqlException.getMessage(), SQLState, errorCode);
            default: return new SqlExecutionException(sqlException.getMessage(), SQLState, errorCode);
        }
    }

    private static String getClassCode(SQLException sqlException)
    {
        try
        {
            String classCode = sqlException.getSQLState().substring(0, 2);
            return classCode;
        }
        catch (Exception e)
        {
            throw new SqlExecutionException(sqlException.getMessage(), sqlException.getSQLState(), sqlException.getErrorCode());
        }
    }
}
