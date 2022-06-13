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

package org.finos.legend.engine.external.shared.format.model.test;

import org.finos.legend.engine.external.shared.format.model.fromModel.ModelToSchemaConfiguration;
import org.finos.legend.engine.external.shared.format.model.fromModel.ModelToSchemaGenerator;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.junit.Assert;

public class ModelToSchemaGenerationTest
{
    public static PureModelContextData generateSchema(String modelCode, ModelToSchemaConfiguration config)
    {
        PureModel pureModel = null;
        try
        {
            PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(modelCode);
            pureModel = Compiler.compile(modelData, DeploymentMode.TEST, null);

            PureModelContextData generated = new ModelToSchemaGenerator(pureModel).generate(config);
            PureModelContextData combined = modelData.combine(generated);
            try
            {
                Compiler.compile(combined, DeploymentMode.TEST, null);
            }
            catch (Exception e)
            {
                Assert.fail("The generated model cannot be compiled: " + e.getMessage());
            }
            return combined;
        }
        catch (Exception e)
        {
            Assert.fail("The model code cannot be compiled: " + e.getMessage());
        }

        return null;
    }
}
