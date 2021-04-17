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

package org.finos.legend.engine.query.pure.api.test.inMemory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.api.model.ExecuteInput;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.url.EngineUrlStreamHandlerFactory;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pac4j.core.profile.CommonProfile;

public class TestM2MGrammarModel
{
    private static final String WALK_TREE = "main::walkTree_main::Person_MANY__String_MANY_";

    @BeforeClass
    public static void setUpUrls()
    {
        EngineUrlStreamHandlerFactory.initialize();
    }

    @Test
    public void testTwoParameterLambdaTypeInference()
    {
        ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

        try
        {
            ExecuteInput executeInput = objectMapper.readValue(TestM2MGrammarModel.class.getClassLoader().getResource( "org/finos/legend/engine/query/pure/api/test/inMemory/two-parameter-lambdas.json"), ExecuteInput.class);
            String clientVersion = executeInput.clientVersion == null ? PureClientVersions.latest : executeInput.clientVersion;
            ModelManager modelManager = new ModelManager(DeploymentMode.TEST);
            MutableList<CommonProfile> profiles = null;
            PureModel pureModel = modelManager.loadModel(executeInput.model, clientVersion, profiles, null);

            ConcreteFunctionDefinition walkTree = pureModel.getConcreteFunctionDefinition(WALK_TREE, null);
            SimpleFunctionExpression fold = (SimpleFunctionExpression) walkTree._expressionSequence().getFirst();
            InstanceValue iv = (InstanceValue) ((FastList) fold._parametersValues()).get(1);
            SimpleFunctionExpression concat = (SimpleFunctionExpression) ((LambdaFunction) iv._values().getFirst())._expressionSequence().getFirst();

            Assert.assertEquals(pureModel.getType("String"), fold._genericType()._rawType());
            Assert.assertEquals(pureModel.getType("String"), concat._genericType()._rawType());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
