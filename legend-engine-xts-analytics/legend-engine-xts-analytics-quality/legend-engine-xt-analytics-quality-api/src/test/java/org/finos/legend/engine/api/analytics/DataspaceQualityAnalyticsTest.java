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

package org.finos.legend.engine.api.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpace;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Objects;

public class DataspaceQualityAnalyticsTest
{
    private final DataspaceQualityAnalytics api = new DataspaceQualityAnalytics(new ModelManager(DeploymentMode.TEST));
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    @Test
    public void testValidCheckDataSpaceConstraints() throws IOException
    {
        objectMapper.registerSubtypes(DataSpace.class);
        PureModelContextData modelContextData = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResource("ValidDataspaceTestPMCD.json")), PureModelContextData.class);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), api.checkDataSpaceConstraints(modelContextData).getStatus());
    }

    @Test
    public void testDataspaceWithBadClasses() throws IOException
    {
        objectMapper.registerSubtypes(DataSpace.class);
        PureModelContextData modelContextData = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResource("InvalidClassesPMCD.json")), PureModelContextData.class);
        Response response = api.checkDataSpaceConstraints(modelContextData);
        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String message = "Dataspace does not match the constraints - [Class name (targetCompany) does not match required standards: should start with upper case, Provide documentation for class and its properties targetCompany]";
        Assert.assertEquals(message, response.getEntity());
    }

    @Test
    public void testDataspaceWithBadAssociations() throws IOException
    {
        objectMapper.registerSubtypes(DataSpace.class);
        PureModelContextData modelContextData = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResource("InvalidAssociationsPMCD.json")), PureModelContextData.class);
        Response response = api.checkDataSpaceConstraints(modelContextData);
        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String message = "Dataspace does not match the constraints - [Check name of association targetCompany_TargetPerson]";
        Assert.assertEquals(message, response.getEntity());
    }

    @Test
    public void testDataspaceWithBadEnumerations() throws IOException
    {
        objectMapper.registerSubtypes(DataSpace.class);
        PureModelContextData modelContextData = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResource("InvalidEnumerationsPMCD.json")), PureModelContextData.class);
        Response response = api.checkDataSpaceConstraints(modelContextData);
        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String message = "Dataspace does not match the constraints - [Enumeration name (enum_TestForMyDataspace) does not match required standards: should start with upper case;should not contain '_', Enumeration name (enum_TestForMyDataspace) does not match required standards: should not contain '_';should start with upper case]";
        Assert.assertEquals(message, response.getEntity());
    }

    @Test
    public void testDataspaceWithBadFunctions() throws IOException
    {
        objectMapper.registerSubtypes(DataSpace.class);
        PureModelContextData modelContextData = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResource("InvalidFunctionsPMCD.json")), PureModelContextData.class);
        Response response = api.checkDataSpaceConstraints(modelContextData);
        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String message = "Dataspace does not match the constraints - [Possible invalid equal check (type mismatch, String vs Integer)]";
        Assert.assertEquals(message, response.getEntity());
    }

    @Test
    public void testDataspaceWithBadClassProperties() throws IOException
    {
        objectMapper.registerSubtypes(DataSpace.class);
        PureModelContextData modelContextData = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResource("InvalidClassPropertiesPMCD.json")), PureModelContextData.class);
        Response response = api.checkDataSpaceConstraints(modelContextData);
        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String message = "Dataspace does not match the constraints - [Property name (TargetCompanyObject) does not match required standards: should start with lower case, Property name (id_TargetCompany) does not match required standards: should not contain '_']";
        Assert.assertEquals(message, response.getEntity());
    }
}
