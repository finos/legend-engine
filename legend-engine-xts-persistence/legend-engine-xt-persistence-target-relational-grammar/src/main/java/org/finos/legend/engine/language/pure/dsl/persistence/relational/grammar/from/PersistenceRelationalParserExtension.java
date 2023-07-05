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

package org.finos.legend.engine.language.pure.dsl.persistence.relational.grammar.from;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.finos.legend.engine.language.pure.dsl.persistence.grammar.from.IPersistenceParserExtension;
import org.finos.legend.engine.language.pure.dsl.persistence.grammar.from.PersistenceTargetSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.ParserErrorListener;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.sink.PersistenceTarget;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.grammar.from.antlr4.PersistenceRelationalLexerGrammar;
import org.finos.legend.pure.grammar.from.antlr4.PersistenceRelationalParserGrammar;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class PersistenceRelationalParserExtension implements IPersistenceParserExtension
{
    private static final String PERSISTENCE_TARGET_RELATIONAL = "Relational";

    @Override
    public List<Function<PersistenceTargetSourceCode, PersistenceTarget>> getExtraPersistenceTargetParsers()
    {
        return Collections.singletonList(code ->
        {
            PersistenceRelationalParseTreeWalker walker = new PersistenceRelationalParseTreeWalker(code.getWalkerSourceInformation());
            if (PERSISTENCE_TARGET_RELATIONAL.equals(code.getType()))
            {
                return parsePersistenceTarget(code, p -> walker.visitPersistenceTarget(p.definition()));
            }
            return null;
        });
    }

    private PersistenceTarget parsePersistenceTarget(PersistenceTargetSourceCode code, Function<PersistenceRelationalParserGrammar, PersistenceTarget> function)
    {
        if (code.getCode().isEmpty())
        {
            throw new EngineException("Persistence target '" + PERSISTENCE_TARGET_RELATIONAL + "' must have a non-empty body", code.getSourceInformation(), EngineErrorType.PARSER);
        }

        CharStream input = CharStreams.fromString(code.getCode());
        ParserErrorListener errorListener = new ParserErrorListener(code.getWalkerSourceInformation(), PersistenceRelationalLexerGrammar.VOCABULARY);
        PersistenceRelationalLexerGrammar lexer = new PersistenceRelationalLexerGrammar(input);
        PersistenceRelationalParserGrammar parser = new PersistenceRelationalParserGrammar(new CommonTokenStream(lexer));

        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        return function.apply(parser);
    }
}
