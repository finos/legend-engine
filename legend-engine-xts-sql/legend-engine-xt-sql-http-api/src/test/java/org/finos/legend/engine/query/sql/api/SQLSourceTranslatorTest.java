// Copyright 2026 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.finos.legend.engine.query.sql.api;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.language.pure.grammar.test.GrammarParseTestUtils;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureSingleExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.executionContext.BaseExecutionContext;
import org.finos.legend.engine.query.sql.providers.core.SQLSource;
import org.finos.legend.engine.query.sql.providers.core.SQLSourceArgument;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_external_query_sql_transformation_queryToPure_SQLSource;
import org.junit.Assert;
import org.junit.Test;

public class SQLSourceTranslatorTest
{
    @Test
    public void testTranslateWithBaseExecutionContext()
    {
        PureModelContextData pmcd = GrammarParseTestUtils.loadPureModelContextFromResource("proj-1.pure", TestSQLSourceProvider.class);
        ModelManager modelManager = new ModelManager(DeploymentMode.TEST);
        PureModel pureModel = modelManager.loadModel(pmcd, PureClientVersions.production, Identity.getAnonymousIdentity(), "");

        Service service = pmcd.getElementsOfType(Service.class).stream()
                .filter(s -> s.execution instanceof PureSingleExecution)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No single execution service found"));

        PureSingleExecution execution = (PureSingleExecution) service.execution;

        BaseExecutionContext baseCtx = new BaseExecutionContext();

        SQLSource source = new SQLSource(
                "service",
                execution.func,
                execution.mapping,
                execution.runtime,
                execution.executionOptions,
                baseCtx,
                FastList.newListWith(new SQLSourceArgument("pattern", 0, service.pattern))
        );

        SQLSourceTranslator translator = new SQLSourceTranslator();

        RichIterable<Root_meta_external_query_sql_transformation_queryToPure_SQLSource> compiled =
                translator.translate(FastList.newListWith(source).toImmutable(), pureModel);

        Assert.assertEquals(1, compiled.size());
        Assert.assertNotNull(compiled.getFirst()._executionContext());
    }
}