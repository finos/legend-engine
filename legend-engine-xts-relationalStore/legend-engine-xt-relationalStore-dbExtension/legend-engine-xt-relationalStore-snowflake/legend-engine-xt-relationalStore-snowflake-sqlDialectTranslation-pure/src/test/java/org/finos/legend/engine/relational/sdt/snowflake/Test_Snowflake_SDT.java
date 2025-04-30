// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.relational.sdt.snowflake;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.relational.test.sdt.SdtTestSuiteBuilder;
import org.finos.legend.pure.generated.core_external_store_relational_sql_dialect_translation_snowflake_snowflakeSqlDialect;

import static org.finos.legend.pure.generated.core_external_store_relational_sql_dialect_translation_snowflake_snowflakeSqlDialect.Root_meta_external_store_relational_sqlDialectTranslation_snowflake_snowflakeSqlDialectExtension__Extension_1_;

public class Test_Snowflake_SDT extends TestSuite
{
    public static Test suite()
    {
        return SdtTestSuiteBuilder.buildSdtTestSuite(
                "Snowflake",
                es -> Lists.immutable.of(Root_meta_external_store_relational_sqlDialectTranslation_snowflake_snowflakeSqlDialectExtension__Extension_1_(es)),
                core_external_store_relational_sql_dialect_translation_snowflake_snowflakeSqlDialect::Root_meta_external_store_relational_sqlDialectTranslation_snowflake_snowflakeFunctionTestsExpectedErrors__Map_1_
        );
    }
}
