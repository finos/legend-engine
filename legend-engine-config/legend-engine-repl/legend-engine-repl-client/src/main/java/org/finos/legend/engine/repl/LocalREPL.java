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

package org.finos.legend.engine.repl;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.repl.REPLInterface;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.*;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionPlan;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;

import java.net.URL;

public class LocalREPL implements REPLInterface
{
    @Override
    public PureModelContextData parse(String txt)
    {
//        txt = "#>{a::DB.test}#->filter(t|$t.name->startsWith('Dr'))->meta::pure::mapping::from(^meta::core::runtime::Runtime\n" +
//                "                                                      (\n" +
//                "                                                        connectionStores= ^meta::core::runtime::ConnectionStore(\n" +
//                "                                                                              connection=^meta::external::store::relational::runtime::TestDatabaseConnection(\n" +
//                "                                                                                              type = meta::relational::runtime::DatabaseType.H2,\n" +
//                "                                                                                              timeZone = 'GMT'\n" +
//                "                                                                                        ),\n" +
//                "                                                                              element = a::DB\n" +
//                "                                                                          )\n" +
//                "                                                      )\n" +
//                "                                                  )";
        return PureGrammarParser.newInstance().parseModel(txt);
//
//                "" +
//                "###Runtime\n" +
//                "Runtime test::test\n" +
//                "{\n" +
//                "   mappings : [];" +
//                "   connections:\n" +
//                "   [\n" +
//                "       test::TestDatabase : [connection: test::testConnection]\n" +
//                "   ];\n" +
//                "}\n" +
//                "###Connection\n" +
//                "RelationalDatabaseConnection test::testConnection\n" +
//                "{\n" +
//                "   store: test::TestDatabase;" +
//                "   specification: LocalH2{};" +
//                "   type: H2;" +
//                "   auth: DefaultH2;" +
//                "}\n" +
//                "###Relational\n" +
//                "Database test::TestDatabase (Table test (id VARCHAR(200), name VARCHAR(200)))\n" +
//                "###Pure\n" +
//                "function a::b::c::d():Any[*]{"+txt+"}");
    }

    @Override
    public PureModel compile(PureModelContextData pureModelContextData)
    {
        return Compiler.compile(pureModelContextData, DeploymentMode.PROD, IdentityFactoryProvider.getInstance().getAnonymousIdentity());
    }

    @Override
    public Root_meta_pure_executionPlan_ExecutionPlan generatePlan(PureModel pureModel, boolean debug)
    {
        RichIterable<? extends Root_meta_pure_extension_Extension> extensions =  PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(pureModel.getExecutionSupport()));
        Pair<Root_meta_pure_executionPlan_ExecutionPlan, String> res = PlanGenerator.generateExecutionPlanAsPure(pureModel.getConcreteFunctionDefinition_safe("a::b::c::d__Any_MANY_"), null, pureModel, PlanPlatform.JAVA, "", debug, extensions);
        if (debug)
        {
            System.out.println(res.getTwo());
        }
        return res.getOne();
    }

    @Override
    public String executePlan(String plan)
    {
        return null;
    }

    @Override
    public void loadCSV(URL content)
    {

    }
}
