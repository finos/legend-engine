// Copyright 2021 Goldman Sachs
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

import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.AthenaLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.AthenaParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.authentication.AuthenticationStrategySourceCode;
import org.finos.legend.engine.language.pure.grammar.from.datasource.DataSourceSpecificationSourceCode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.AthenaDatasourceSpecification;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class AthenaGrammarParserExtension implements IRelationalGrammarParserExtension
{
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
            if ("Athena".equals(code.getType()))
            {
                return IRelationalGrammarParserExtension.parse(code, AthenaLexerGrammar::new, AthenaParserGrammar::new,
                        p -> visitAthenaDsp(code, p.athenaDsp()));
            }
            return null;
        });
    }

    private AthenaDatasourceSpecification visitAthenaDsp(DataSourceSpecificationSourceCode code, AthenaParserGrammar.AthenaDspContext dbSpecCtx)
    {
        AthenaDatasourceSpecification dsSpec = new AthenaDatasourceSpecification();
        dsSpec.sourceInformation = code.getSourceInformation();
        AthenaParserGrammar.AwsRegionContext awsRegionContext = PureGrammarParserUtility.validateAndExtractRequiredField(dbSpecCtx.awsRegion(), "awsRegion", dsSpec.sourceInformation);
        dsSpec.awsRegion = PureGrammarParserUtility.fromGrammarString(awsRegionContext.STRING().getText(), true);
        AthenaParserGrammar.S3OutputLocationContext s3OutputLocationContext = PureGrammarParserUtility.validateAndExtractRequiredField(dbSpecCtx.s3OutputLocation(), "s3OutputLocation", dsSpec.sourceInformation);
        dsSpec.s3OutputLocation = PureGrammarParserUtility.fromGrammarString(s3OutputLocationContext.STRING().getText(), true);
        AthenaParserGrammar.DbNameContext nameCtx = PureGrammarParserUtility.validateAndExtractRequiredField(dbSpecCtx.dbName(), "name", dsSpec.sourceInformation);
        dsSpec.databaseName = PureGrammarParserUtility.fromGrammarString(nameCtx.STRING().getText(), true);
        return dsSpec;
    }
}
