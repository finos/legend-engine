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

package org.finos.legend.engine.language.memsqlFunction.generator.test;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.memsqlFunction.generator.MemSqlFunctionGenerator;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.memsqlFunction.deployment.MemSqlFunctionArtifact;
import org.finos.legend.engine.protocol.memsqlFunction.deployment.MemSqlFunctionContent;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_memSqlFunction_MemSqlFunction;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.finos.legend.pure.generated.platform_pure_essential_meta_graph_pathToElement.Root_meta_pure_functions_meta_pathToElement_String_1__PackageableElement_1_;

public class TestMemSqlFunctionGenerator
{
    private final PureModelContextData contextData;
    private final PureModel pureModel;
    private final Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions = (PureModel pureModel) -> PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(pureModel.getExecutionSupport()));

    public  TestMemSqlFunctionGenerator()
    {
        this.contextData = PureGrammarParser.newInstance().parseModel(readModelContentFromResource(this.modelResourcePath()));
        this.pureModel = Compiler.compile(contextData, null, Identity.getAnonymousIdentity().getName());
    }

    public String modelResourcePath()
    {
        return "/org/finos/legend/engine/language/memsqlFunction/generator/memsqlFunctionTestModels.pure";
    }

    private String readModelContentFromResource(String resourcePath)
    {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(Objects.requireNonNull(TestMemSqlFunctionGenerator.class.getResourceAsStream(resourcePath)))))
        {
            return buffer.lines().collect(Collectors.joining("\n"));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private MemSqlFunctionArtifact generateForActivator(String activatorPath, PureModel pureModel)
    {
        Root_meta_external_function_activator_memSqlFunction_MemSqlFunction app = (Root_meta_external_function_activator_memSqlFunction_MemSqlFunction) Root_meta_pure_functions_meta_pathToElement_String_1__PackageableElement_1_(activatorPath, pureModel.getExecutionSupport());
        return MemSqlFunctionGenerator.generateArtifact(pureModel, app, this.contextData, routerExtensions);
    }

    @Test
    public void testNoParamActivator()
    {
        MemSqlFunctionArtifact artifact = generateForActivator("egress::alloy::memsqlTest::activators::DemoMemSqlFunction", this.pureModel);
        String expected = "DEMOFUNCTION1() RETURNS TABLE AS RETURN select `root`.APP_NAME as `App Name`, `root`.SQL_FRAGMENT as `Query`, `root`.OWNER as `Owner`, `root`.VERSION_NUMBER as `Version`, `root`.DESCRIPTION as `Doc` from LEGEND_GOVERNANCE.BUSINESS_OBJECTS as `root`;";
        Assert.assertEquals(expected, ((MemSqlFunctionContent)artifact.content).sqlExpressions.get(0));
    }

    @Test
    public void testParamActivator()
    {
        MemSqlFunctionArtifact artifact = generateForActivator("egress::alloy::memsqlTest::activators::DemoMemSqlFunctionWithParam", this.pureModel);
        String expected = "DEMOFUNCTION2(nameLength INT,nameStart VARCHAR(255)) RETURNS TABLE AS RETURN select `root`.APP_NAME as `App Name`, `root`.SQL_FRAGMENT as `Query`, `root`.OWNER as `Owner`, `root`.VERSION_NUMBER as `Version`, `root`.DESCRIPTION as `Doc` from LEGEND_GOVERNANCE.BUSINESS_OBJECTS as `root` where (char_length(`root`.APP_NAME) > nameLength and `root`.APP_NAME like 'nameStar%');";
        Assert.assertEquals(expected, ((MemSqlFunctionContent)artifact.content).sqlExpressions.get(0));
    }
}
