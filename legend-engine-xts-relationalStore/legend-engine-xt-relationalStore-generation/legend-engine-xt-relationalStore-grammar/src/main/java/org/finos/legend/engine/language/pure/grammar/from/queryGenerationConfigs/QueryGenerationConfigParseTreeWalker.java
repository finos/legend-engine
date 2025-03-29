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

package org.finos.legend.engine.language.pure.grammar.from.queryGenerationConfigs;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParserErrorListener;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.queryGenerationConfigs.QueryGenerationConfigsLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.queryGenerationConfigs.QueryGenerationConfigsParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.GenerationFeaturesConfig;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalQueryGenerationConfig;

public class QueryGenerationConfigParseTreeWalker
{
    public static <T extends RelationalQueryGenerationConfig> T parseQueryGenerationConfig(QueryGenerationConfigSourceCode code, Function<QueryGenerationConfigsParserGrammar, T> func)
    {
        CharStream input = CharStreams.fromString(code.getCode());
        ParserErrorListener errorListener = new ParserErrorListener(code.getWalkerSourceInformation());
        QueryGenerationConfigsLexerGrammar lexer = new QueryGenerationConfigsLexerGrammar(input);
        QueryGenerationConfigsParserGrammar parser = new QueryGenerationConfigsParserGrammar(new CommonTokenStream(lexer));

        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        return func.apply(parser);
    }

    public GenerationFeaturesConfig visitGenerationFeaturesConfig(QueryGenerationConfigSourceCode code, QueryGenerationConfigsParserGrammar.GenerationFeaturesConfigContext ctx)
    {
        GenerationFeaturesConfig generationFeaturesConfig = new GenerationFeaturesConfig();
        QueryGenerationConfigsParserGrammar.EnabledFeaturesContext enabledFeaturesContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.enabledFeatures(), "enabled", code.getSourceInformation());
        if (enabledFeaturesContext != null && enabledFeaturesContext.STRING() != null)
        {
            generationFeaturesConfig.enabled = ListIterate.collect(enabledFeaturesContext.STRING(), s -> PureGrammarParserUtility.fromGrammarString(s.getText(), true));
        }
        QueryGenerationConfigsParserGrammar.DisabledFeaturesContext disabledFeaturesContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.disabledFeatures(), "disabled", code.getSourceInformation());
        if (disabledFeaturesContext != null && disabledFeaturesContext.STRING() != null)
        {
            generationFeaturesConfig.disabled = ListIterate.collect(disabledFeaturesContext.STRING(), s -> PureGrammarParserUtility.fromGrammarString(s.getText(), true));
        }
        return generationFeaturesConfig;
    }
}
