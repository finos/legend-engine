// Copyright 2023 Goldman Sachs
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

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.RedshiftLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.RedshiftParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.authentication.AuthenticationStrategySourceCode;
import org.finos.legend.engine.language.pure.grammar.from.datasource.DataSourceSpecificationSourceCode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.RedshiftDatasourceSpecification;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class RedshiftGrammarParserExtension implements IRelationalGrammarParserExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Relational", "Redshift");
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
            if ("Redshift".equals(code.getType()))
            {
                return IRelationalGrammarParserExtension.parse(code, RedshiftLexerGrammar::new, RedshiftParserGrammar::new,
                        p -> visitRedshiftDatasourceSpecification(code, p.redshiftDatasourceSpecification()));
            }
            return null;
        });
    }

    public RedshiftDatasourceSpecification visitRedshiftDatasourceSpecification(DataSourceSpecificationSourceCode code, RedshiftParserGrammar.RedshiftDatasourceSpecificationContext ctx)
    {
        RedshiftDatasourceSpecification redshiftSpec = new RedshiftDatasourceSpecification();
        RedshiftParserGrammar.ClusterIDContext clusterID = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.clusterID(), "clusterID", redshiftSpec.sourceInformation);
        RedshiftParserGrammar.DbHostContext host = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.dbHost(), "dbHost", redshiftSpec.sourceInformation);
        RedshiftParserGrammar.DbPortContext port = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.dbPort(), "port", redshiftSpec.sourceInformation);
        RedshiftParserGrammar.RegionContext region = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.region(), "region", redshiftSpec.sourceInformation);
        RedshiftParserGrammar.DbNameContext database = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.dbName(), "name", redshiftSpec.sourceInformation);
        RedshiftParserGrammar.EndpointURLContext endpoint = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.endpointURL(), "endpointURL", redshiftSpec.sourceInformation);

        redshiftSpec.clusterID = PureGrammarParserUtility.fromGrammarString(clusterID.STRING().getText(), true);
        redshiftSpec.host = PureGrammarParserUtility.fromGrammarString(host.STRING().getText(), true);
        redshiftSpec.port = Integer.parseInt(port.INTEGER().getText());
        redshiftSpec.region = PureGrammarParserUtility.fromGrammarString(region.STRING().getText(), true);
        redshiftSpec.databaseName = PureGrammarParserUtility.fromGrammarString(database.STRING().getText(), true);
        redshiftSpec.endpointURL = PureGrammarParserUtility.fromGrammarString(endpoint.STRING().getText(), true);

        return redshiftSpec;
    }
}
