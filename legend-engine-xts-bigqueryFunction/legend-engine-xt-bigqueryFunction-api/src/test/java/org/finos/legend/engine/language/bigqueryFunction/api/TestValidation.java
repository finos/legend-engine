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

package org.finos.legend.engine.language.bigqueryFunction.api;

import com.fasterxml.jackson.core.type.TypeReference;
import org.finos.legend.engine.functionActivator.api.FunctionActivatorAPI;
import org.finos.legend.engine.functionActivator.api.output.FunctionActivatorInfo;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.List;

public class TestValidation
{
    private final FunctionActivatorAPI api = new FunctionActivatorAPI(new ModelManager(DeploymentMode.TEST), (PureModel pureModel) -> PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(pureModel.getExecutionSupport())));

    @Test
    public void testList() throws Exception
    {
        Response response = api.list(null);
        System.out.println(response.getEntity().toString());
        List<FunctionActivatorInfo> info = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(response.getEntity().toString(), new TypeReference<List<FunctionActivatorInfo>>(){});
        Assert.assertEquals(1, info.size());
        Assert.assertEquals("BigQuery Function", info.get(0).name);
        Assert.assertEquals("Create a BigQuery Function that can activate in BigQuery.", info.get(0).description);
        Assert.assertEquals("meta::protocols::pure::vX_X_X::metamodel::function::activator::bigQueryFunction::BigQueryFunction", info.get(0).configuration.topElement);
        Assert.assertEquals(8, info.get(0).configuration.model.size());
    }
}
