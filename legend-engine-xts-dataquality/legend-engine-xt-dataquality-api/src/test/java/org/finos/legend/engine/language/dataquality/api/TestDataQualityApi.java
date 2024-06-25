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

package org.finos.legend.engine.language.dataquality.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtensionLoader;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.graph.RootGraphFetchTree;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_DataQuality;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestDataQualityApi
{
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    @Test
    public void testDataQualityGetPropertyPathTree() throws IOException
    {
        URL url = Objects.requireNonNull(getClass().getClassLoader().getResource("inputs/modelWithDataQualityValidation.json"));
        PureModelContextData pureModelContextData = objectMapper.readValue(url, PureModelContextData.class);
        CompilerExtensionLoader.logExtensionList();
        PureModel model = Compiler.compile(pureModelContextData, DeploymentMode.TEST, Identity.getAnonymousIdentity().getName());
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement = model.getPackageableElement("meta::dataquality::PersonDataQualityValidation");
        assertTrue(DataQualityPropertyPathTreeGenerator.isDataQualityInstance(packageableElement));
        RootGraphFetchTree rootGraphFetchTree = DataQualityPropertyPathTreeGenerator.getPropertyPathTree(((Root_meta_external_dataquality_DataQuality)packageableElement)._validationTree(), "vX_X_X", model, null);
        assertEquals("{\"_type\":\"rootGraphFetchTree\",\"_type\":\"rootGraphFetchTree\",\"class\":\"meta::dataquality::Person\",\"subTrees\":[{\"_type\":\"propertyGraphFetchTree\",\"_type\":\"propertyGraphFetchTree\",\"parameters\":[],\"property\":\"name\",\"subTrees\":[],\"subTypeTrees\":[]},{\"_type\":\"propertyGraphFetchTree\",\"_type\":\"propertyGraphFetchTree\",\"parameters\":[],\"property\":\"age\",\"subTrees\":[],\"subTypeTrees\":[]}],\"subTypeTrees\":[]}", objectMapper.writeValueAsString(rootGraphFetchTree));

        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement2 = model.getPackageableElement("meta::dataquality::Person");
        assertFalse(DataQualityPropertyPathTreeGenerator.isDataQualityInstance(packageableElement2));
    }

    @Ignore
    @Test
    public void testDataQualityGetPropertyPathTree_WithNullDataQualityValidation() throws IOException
    {
        URL url = Objects.requireNonNull(getClass().getClassLoader().getResource("inputs/modelWithNullDataQualityValidation.json"));
        PureModelContextData pureModelContextData = objectMapper.readValue(url, PureModelContextData.class);
        CompilerExtensionLoader.logExtensionList();
        PureModel model = Compiler.compile(pureModelContextData, DeploymentMode.TEST, Identity.getAnonymousIdentity().getName());
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement = model.getPackageableElement("meta::dataquality::PersonDataQualityValidation");
        assertFalse(DataQualityPropertyPathTreeGenerator.isDataQualityInstance(packageableElement));
    }
}
