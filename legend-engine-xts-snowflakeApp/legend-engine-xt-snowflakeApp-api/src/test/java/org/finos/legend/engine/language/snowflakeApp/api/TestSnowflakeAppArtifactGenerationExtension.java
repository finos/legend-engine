//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.language.snowflakeApp.api;

import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.generation.extension.Artifact;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.snowflakeApp.deployment.SnowflakeAppArtifactGenerationExtension;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.junit.Test;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestSnowflakeAppArtifactGenerationExtension
{

    @Test
    public void testSnowflakeAppArtifactGenerationExtension()
    {
        String val =
                "Class a::Person {name : String[1];}\n" +
                        "function a::f():a::Person[*]{a::Person.all()->from(a::m, ^meta::pure::runtime::Runtime(connections=^meta::relational::runtime::TestDatabaseConnection(element=a::db, type=meta::relational::runtime::DatabaseType.H2)))}\n" +
                        "###Mapping\n" +
                        "Mapping a::m(a::Person:Relational{name : [a::db]tb.name})\n" +
                        "###Relational\n" +
                        "Database a::db(Table tb(name VARCHAR(100)))\n" +
                        "###Snowflake\n" +
                        "SnowflakeApp a::myApp{" +
                        "   applicationName: 'name';" +
                        "   description: 'ee';" +
                        "   function: a::f():Person[*];" +
                        "}";
        PureModelContextData pureModelContextData = PureGrammarParser.newInstance().parseModel(val);
        PureModel model = Compiler.compile(pureModelContextData, DeploymentMode.TEST, null);
        SnowflakeAppArtifactGenerationExtension extension = new SnowflakeAppArtifactGenerationExtension();
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement = model.getPackageableElement("a::myApp");
        assertTrue(extension.canGenerate(packageableElement));

        List<Artifact> outputs = extension.generate(packageableElement, model, pureModelContextData, PureClientVersions.production);
        assertEquals(1, outputs.size());
        Artifact snowflakeAppResult = outputs.get(0);
        Assert.assertTrue(false); //test should be failing right now. 
    }
}
