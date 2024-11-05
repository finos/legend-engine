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

package org.finos.legend.engine.pure.runtime.external.relational.postgresSql.parser.compiled.natives;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.factory.Stacks;
import org.finos.legend.engine.language.sql.grammar.from.SQLGrammarParser;
import org.finos.legend.engine.protocol.sql.metamodel.Statement;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNativeFunctionGeneric;

public class ParseSqlStatementToJson extends AbstractNativeFunctionGeneric
{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public ParseSqlStatementToJson()
    {
        super(
                ParseSqlStatementToJson.class.getCanonicalName() + "." + "parseSqlStatementToJson",
                AbstractNativeFunctionGeneric.getMethod(ParseSqlStatementToJson.class, "parseSqlStatementToJson").getParameterTypes(),
                "parseSqlStatementToJson_String_1__String_1_"
        );
    }

    public static String parseSqlStatementToJson(String sql) throws PureExecutionException
    {
        try
        {
            Statement statement = SQLGrammarParser.newInstance().parseStatement(sql);
            return OBJECT_MAPPER.writeValueAsString(statement);
        }
        catch (JsonProcessingException e)
        {
            throw new PureExecutionException(e, Stacks.mutable.empty());
        }
    }
}
