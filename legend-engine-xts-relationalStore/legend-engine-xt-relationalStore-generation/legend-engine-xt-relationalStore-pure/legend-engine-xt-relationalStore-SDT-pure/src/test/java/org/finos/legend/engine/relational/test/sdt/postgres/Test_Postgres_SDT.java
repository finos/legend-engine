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

package org.finos.legend.engine.relational.test.sdt.postgres;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.relational.test.sdt.SdtTestSuiteBuilder;

import static org.finos.legend.pure.generated.core_external_store_relational_sql_dialect_translation_postgres_postgresSqlDialect.*;

public class Test_Postgres_SDT extends TestSuite
{
    public static Test suite()
    {
        return SdtTestSuiteBuilder.buildSdtTestSuite(
                "Postgres",
                es -> Lists.immutable.of(Root_meta_external_store_relational_sqlDialectTranslation_postgres_postgresSqlDialectExtension__Extension_1_(es)),
                Maps.mutable.empty()
        );
    }
}
