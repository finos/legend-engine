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

package org.finos.legend.engine.language.pure.dsl.persistence.cloud.grammar.from;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.finos.legend.engine.language.pure.dsl.persistence.grammar.from.IPersistenceParserExtension;
import org.finos.legend.engine.language.pure.dsl.persistence.grammar.from.context.PersistencePlatformSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.ParserErrorListener;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.PersistenceCloudLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.PersistenceCloudParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.context.PersistencePlatform;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class PersistenceCloudParserExtension implements IPersistenceParserExtension
{
    private static final String PERSISTENCE_PLATFORM_AWS_GLUE = "AwsGlue";

    @Override
    public List<Function<PersistencePlatformSourceCode, PersistencePlatform>> getExtraPersistencePlatformParsers()
    {
        return Collections.singletonList(code ->
        {
            PersistenceCloudParseTreeWalker walker = new PersistenceCloudParseTreeWalker(code.getWalkerSourceInformation());
            if (PERSISTENCE_PLATFORM_AWS_GLUE.equals(code.getType()))
            {
                return parsePersistencePlatform(code, p -> walker.visitPersistencePlatform(p.definition()));
            }
            return null;
        });
    }

    private PersistencePlatform parsePersistencePlatform(PersistencePlatformSourceCode code, Function<PersistenceCloudParserGrammar, PersistencePlatform> function)
    {
        if (code.getCode().isEmpty())
        {
            throw new EngineException("Persistence platform '" + PERSISTENCE_PLATFORM_AWS_GLUE + "' must have a non-empty body", code.getSourceInformation(), EngineErrorType.PARSER);
        }

        CharStream input = CharStreams.fromString(code.getCode());
        ParserErrorListener errorListener = new PersistenceCloudParserErrorListener(code.getWalkerSourceInformation());
        PersistenceCloudLexerGrammar lexer = new PersistenceCloudLexerGrammar(input);
        PersistenceCloudParserGrammar parser = new PersistenceCloudParserGrammar(new CommonTokenStream(lexer));

        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        return function.apply(parser);
    }
}
