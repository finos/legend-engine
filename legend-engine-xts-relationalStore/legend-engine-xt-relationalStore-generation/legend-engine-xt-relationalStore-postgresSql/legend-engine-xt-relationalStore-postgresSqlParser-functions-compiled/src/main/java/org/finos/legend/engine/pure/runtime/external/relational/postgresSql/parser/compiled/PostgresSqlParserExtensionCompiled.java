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

package org.finos.legend.engine.pure.runtime.external.relational.postgresSql.parser.compiled;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.runtime.java.compiled.extension.AbstractCompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.Native;
import org.finos.legend.engine.pure.runtime.external.relational.postgresSql.parser.compiled.natives.ParseSqlStatementToJson;

import java.util.List;

public class PostgresSqlParserExtensionCompiled extends AbstractCompiledExtension
{
    @Override
    public List<Native> getExtraNatives()
    {
        return Lists.fixedSize.with(
                new ParseSqlStatementToJson()
        );
    }

    @Override
    public String getRelatedRepository()
    {
        return "core_external_store_relational_postgres_sql_parser";
    }

    public static CompiledExtension extension()
    {
        return new PostgresSqlParserExtensionCompiled();
    }
}
