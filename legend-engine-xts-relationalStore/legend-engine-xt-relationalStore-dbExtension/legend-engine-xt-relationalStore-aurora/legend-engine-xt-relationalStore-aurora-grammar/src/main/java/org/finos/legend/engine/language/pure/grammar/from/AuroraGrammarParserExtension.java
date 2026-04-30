// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.from;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.AuroraLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.AuroraParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.authentication.AuthenticationStrategySourceCode;
import org.finos.legend.engine.language.pure.grammar.from.datasource.DataSourceSpecificationSourceCode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.AuroraDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.GlobalAuroraDatasourceSpecification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class AuroraGrammarParserExtension implements IRelationalGrammarParserExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Relational", "Aurora");
    }

    @Override
    public List<Function<AuthenticationStrategySourceCode, AuthenticationStrategy>> getExtraAuthenticationStrategyParsers()
    {
        return Collections.singletonList(code -> null);
    }

    @Override
    public List<Function<DataSourceSpecificationSourceCode, DatasourceSpecification>> getExtraDataSourceSpecificationParsers()
    {
        return Collections.singletonList(code ->
        {
            if ("Aurora".equals(code.getType()))
            {
                return IRelationalGrammarParserExtension.parse(code, AuroraLexerGrammar::new, AuroraParserGrammar::new,
                        p -> visitAuroraDsp(code, p.auroraDsp()));
            }
            if ("GlobalAurora".equals(code.getType()))
            {
                return IRelationalGrammarParserExtension.parse(code, AuroraLexerGrammar::new, AuroraParserGrammar::new,
                        p -> visitGlobalAuroraDsp(code, p.globalAuroraDsp()));
            }
            return null;
        });
    }

    private AuroraDatasourceSpecification visitAuroraDsp(DataSourceSpecificationSourceCode code, AuroraParserGrammar.AuroraDspContext ctx)
    {
        AuroraDatasourceSpecification dsSpec = new AuroraDatasourceSpecification();
        dsSpec.sourceInformation = code.getSourceInformation();

        AuroraParserGrammar.AuroraHostContext hostCtx = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.auroraHost(), "host", dsSpec.sourceInformation);
        dsSpec.host = PureGrammarParserUtility.fromGrammarString(hostCtx.STRING().getText(), true);

        AuroraParserGrammar.AuroraPortContext portCtx = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.auroraPort(), "port", dsSpec.sourceInformation);
        dsSpec.port = Integer.parseInt(portCtx.INTEGER().getText());

        AuroraParserGrammar.AuroraNameContext nameCtx = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.auroraName(), "name", dsSpec.sourceInformation);
        dsSpec.name = PureGrammarParserUtility.fromGrammarString(nameCtx.STRING().getText(), true);

        Optional.ofNullable(PureGrammarParserUtility.validateAndExtractOptionalField(ctx.auroraClusterInstanceHostPattern(), "clusterInstanceHostPattern", dsSpec.sourceInformation)).ifPresent(x ->
                dsSpec.clusterInstanceHostPattern = PureGrammarParserUtility.fromGrammarString(x.STRING().getText(), true));

        return dsSpec;
    }

    private GlobalAuroraDatasourceSpecification visitGlobalAuroraDsp(DataSourceSpecificationSourceCode code, AuroraParserGrammar.GlobalAuroraDspContext ctx)
    {
        GlobalAuroraDatasourceSpecification dsSpec = new GlobalAuroraDatasourceSpecification();
        dsSpec.sourceInformation = code.getSourceInformation();

        AuroraParserGrammar.GlobalAuroraHostContext hostCtx = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.globalAuroraHost(), "host", dsSpec.sourceInformation);
        dsSpec.host = PureGrammarParserUtility.fromGrammarString(hostCtx.STRING().getText(), true);

        AuroraParserGrammar.GlobalAuroraPortContext portCtx = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.globalAuroraPort(), "port", dsSpec.sourceInformation);
        dsSpec.port = Integer.parseInt(portCtx.INTEGER().getText());

        AuroraParserGrammar.GlobalAuroraNameContext nameCtx = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.globalAuroraName(), "name", dsSpec.sourceInformation);
        dsSpec.name = PureGrammarParserUtility.fromGrammarString(nameCtx.STRING().getText(), true);

        AuroraParserGrammar.GlobalAuroraRegionContext regionCtx = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.globalAuroraRegion(), "region", dsSpec.sourceInformation);
        dsSpec.region = PureGrammarParserUtility.fromGrammarString(regionCtx.STRING().getText(), true);

        AuroraParserGrammar.GlobalAuroraClusterInstanceHostPatternsContext patternsCtx = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.globalAuroraClusterInstanceHostPatterns(), "globalClusterInstanceHostPatterns", dsSpec.sourceInformation);
        dsSpec.globalClusterInstanceHostPatterns = new ArrayList<>();
        for (TerminalNode stringNode : patternsCtx.STRING())
        {
            dsSpec.globalClusterInstanceHostPatterns.add(PureGrammarParserUtility.fromGrammarString(stringNode.getText(), true));
        }

        return dsSpec;
    }
}
