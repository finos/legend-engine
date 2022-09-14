// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.relational.sqldom.common;

public enum Clause
{

    DROP("DROP"),
    TRUNCATE("TRUNCATE"),
    SELECT("SELECT"),
    INSERT("INSERT"),
    INSERT_INTO("INSERT INTO"),
    CREATE("CREATE"),
    ALTER("ALTER"),
    VALUES("VALUES"),
    FROM("FROM"),
    UPDATE("UPDATE"),
    MERGE_INTO("MERGE INTO"),
    USING("USING"),
    WHERE("WHERE"),
    SET("SET"),
    EXISTS("EXISTS"),
    WHEN_MATCHED("WHEN MATCHED"),
    WHEN_NOT_MATCHED("WHEN NOT MATCHED"),
    THEN("THEN"),
    AND("AND"),
    IN("IN"),
    ON("ON"),
    NOT("NOT"),
    DELETE_FROM("DELETE FROM"),
    TABLE("TABLE"),
    SHOW("SHOW"),
    LIKE("LIKE"),
    COLUMN("COLUMN"),
    INDEX("INDEX"),
    GROUP_BY("GROUP BY"),
    CASE("CASE"),
    WHEN("WHEN"),
    ELSE("ELSE"),
    END("END");

    private final String clause;

    Clause(String clause)
    {
        this.clause = clause;
    }

    public String get()
    {
        return clause;
    }
}