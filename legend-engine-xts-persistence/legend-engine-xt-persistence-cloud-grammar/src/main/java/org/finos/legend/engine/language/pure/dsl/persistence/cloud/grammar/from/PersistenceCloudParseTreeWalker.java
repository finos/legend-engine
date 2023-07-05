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

import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.PersistenceCloudParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.cloud.context.AwsGluePersistencePlatform;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.context.PersistencePlatform;

public class PersistenceCloudParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;

    public PersistenceCloudParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation)
    {
        this.walkerSourceInformation = walkerSourceInformation;
    }

    /**********
     * persistence platform
     **********/

    public PersistencePlatform visitPersistencePlatform(PersistenceCloudParserGrammar.DefinitionContext ctx)
    {
        AwsGluePersistencePlatform platform = new AwsGluePersistencePlatform();
        platform.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // data processing units
        PersistenceCloudParserGrammar.AwsGlueDpuCountContext dpuCountContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.awsGlueDpuCount(), "dataProcessingUnits", platform.sourceInformation);
        platform.dataProcessingUnits = Integer.parseInt(dpuCountContext.INTEGER().getText());

        return platform;
    }
}
