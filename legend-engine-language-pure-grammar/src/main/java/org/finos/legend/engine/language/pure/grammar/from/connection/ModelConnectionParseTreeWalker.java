// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.from.connection;

import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.modelConnection.ModelConnectionParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.JsonModelConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.XmlModelConnection;

import java.util.Collections;

public class ModelConnectionParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;

    public ModelConnectionParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation)
    {
        this.walkerSourceInformation = walkerSourceInformation;
    }

    public void visitJsonModelConnectionValue(ModelConnectionParserGrammar.DefinitionContext ctx, JsonModelConnection connectionValue, boolean isEmbedded)
    {
        if (!isEmbedded)
        {
            connectionValue.element = "ModelStore"; // stub value since we don't use an actual store for this type of connection
        }
        ModelConnectionParserGrammar.ModelConnectionClassContext classContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.modelConnectionClass(), "class", connectionValue.sourceInformation);
        // class
        connectionValue._class = PureGrammarParserUtility.fromQualifiedName(classContext.qualifiedName().packagePath() == null ? Collections.emptyList() : classContext.qualifiedName().packagePath().identifier(), classContext.qualifiedName().identifier());
        connectionValue.classSourceInformation = this.walkerSourceInformation.getSourceInformation(classContext.qualifiedName());
        // url
        ModelConnectionParserGrammar.ConnectionUrlContext urlContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.connectionUrl(), "url", connectionValue.sourceInformation);
        connectionValue.url = PureGrammarParserUtility.fromGrammarString(urlContext.STRING().getText(), true);
    }

    public void visitXmlModelConnectionValue(ModelConnectionParserGrammar.DefinitionContext ctx, XmlModelConnection connectionValue, boolean isEmbedded)
    {
        if (!isEmbedded)
        {
            connectionValue.element = "ModelStore"; // stub value since we don't use an actual store for this type of connection
        }
        ModelConnectionParserGrammar.ModelConnectionClassContext classContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.modelConnectionClass(), "class", connectionValue.sourceInformation);
        // class
        connectionValue._class = PureGrammarParserUtility.fromQualifiedName(classContext.qualifiedName().packagePath() == null ? Collections.emptyList() : classContext.qualifiedName().packagePath().identifier(), classContext.qualifiedName().identifier());
        connectionValue.classSourceInformation = this.walkerSourceInformation.getSourceInformation(classContext.qualifiedName());
        // url
        ModelConnectionParserGrammar.ConnectionUrlContext urlContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.connectionUrl(), "url", connectionValue.sourceInformation);
        connectionValue.url = PureGrammarParserUtility.fromGrammarString(urlContext.STRING().getText(), true);
    }
}
