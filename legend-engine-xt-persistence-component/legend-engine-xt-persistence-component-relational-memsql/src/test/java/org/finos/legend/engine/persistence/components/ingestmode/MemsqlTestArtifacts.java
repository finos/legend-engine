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

package org.finos.legend.engine.persistence.components.ingestmode;

public class MemsqlTestArtifacts
{
    public static String expectedBaseTableCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`(" +
            "`id` INTEGER," +
            "`name` VARCHAR(256)," +
            "`amount` DOUBLE," +
            "`biz_date` DATE," +
            "PRIMARY KEY (`id`, `name`))";

    public static String expectedBaseTableCreateQueryWithUpperCase = "CREATE TABLE IF NOT EXISTS `MYDB`.`MAIN`" +
            "(`ID` INTEGER," +
            "`NAME` VARCHAR(256)," +
            "`AMOUNT` DOUBLE," +
            "`BIZ_DATE` DATE," +
            "PRIMARY KEY (`ID`, `NAME`))";

    public static String expectedBaseTablePlusDigestCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`(" +
            "`id` INTEGER," +
            "`name` VARCHAR(256)," +
            "`amount` DOUBLE," +
            "`biz_date` DATE," +
            "`digest` VARCHAR(256)," +
            "PRIMARY KEY (`id`, `name`))";

    public static String expectedBaseTablePlusDigestCreateQueryWithUpperCase = "CREATE TABLE IF NOT EXISTS `MYDB`.`MAIN`(" +
            "`ID` INTEGER," +
            "`NAME` VARCHAR(256)," +
            "`AMOUNT` DOUBLE," +
            "`BIZ_DATE` DATE," +
            "`DIGEST` VARCHAR(256)," +
            "PRIMARY KEY (`ID`, `NAME`))";

    public static String expectedBaseTableCreateQueryWithNoPKs = "CREATE TABLE IF NOT EXISTS `mydb`.`main`(" +
            "`id` INTEGER," +
            "`name` VARCHAR(256)," +
            "`amount` DOUBLE," +
            "`biz_date` DATE," +
            "`digest` VARCHAR(256))";

    public static String expectedBaseTableCreateQueryWithAuditAndNoPKs = "CREATE TABLE IF NOT EXISTS `mydb`.`main`" +
            "(`id` INTEGER,`name` VARCHAR(256),`amount` DOUBLE,`biz_date` DATE,`digest` VARCHAR(256),`batch_update_time` DATETIME)";

    public static String expectedBaseTablePlusDigestPlusUpdateTimestampCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`(" +
            "`id` INTEGER," +
            "`name` VARCHAR(256)," +
            "`amount` DOUBLE," +
            "`biz_date` DATE," +
            "`digest` VARCHAR(256)," +
            "`batch_update_time` DATETIME," +
            "PRIMARY KEY (`id`, `name`, `batch_update_time`))";

    public static String expectedStagingCleanupQuery = "DELETE FROM `mydb`.`staging` as stage";

    public static String expectedDropTableQuery = "DROP TABLE IF EXISTS `mydb`.`staging` CASCADE";

    public static String cleanUpMainTableSql = "DELETE FROM `mydb`.`main` as sink";
    public static String cleanupMainTableSqlUpperCase = "DELETE FROM `MYDB`.`MAIN` as sink";


}
