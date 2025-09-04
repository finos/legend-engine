// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.query.sql.providers.shared.project;

import java.util.Optional;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.eclipse.collections.api.block.function.Function;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.ServerConnectionConfiguration;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.AlloySDLC;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.WorkspaceSDLC;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.kerberos.HttpClientBuilder;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;


public class ProjectCoordinateLoader
{
    private final ModelManager modelManager;

    public ProjectCoordinateLoader(ModelManager modelManager, ServerConnectionConfiguration sdlcServerConfig)
    {
        this(modelManager, sdlcServerConfig, identity -> (CloseableHttpClient) HttpClientBuilder.getHttpClient(new BasicCookieStore()));
    }

    public ProjectCoordinateLoader(ModelManager modelManager, ServerConnectionConfiguration sdlcServerConfig, Function<Identity, CloseableHttpClient> httpClientProvider)
    {
        this.modelManager = modelManager;
    }

    public ProjectResolvedContext resolve(ProjectCoordinateWrapper projectCoordinateWrapper, Identity identity)
    {
        return resolve(projectCoordinateWrapper, true, identity);
    }

    public ProjectResolvedContext resolve(ProjectCoordinateWrapper projectCoordinateWrapper, boolean required, Identity identity)
    {
        Optional<String> coordinates = projectCoordinateWrapper.getCoordinates();
        if (coordinates.isPresent())
        {
            PureModelContextPointer pointer = pointerFromCoordinates(coordinates.get());

            PureModelContextData pmcd = modelManager.loadData(pointer, PureClientVersions.production, identity);

            return new ProjectResolvedContext(pointer, pmcd);
        }
        Optional<String> project = projectCoordinateWrapper.getProject();
        if (project.isPresent())
        {
            Optional<String> workspace = projectCoordinateWrapper.getWorkspace();
            Optional<String> groupWorkspace = projectCoordinateWrapper.getGroupWorkspace();
            String workspaceId = workspace.orElseGet(groupWorkspace::get);
            boolean isGroup = groupWorkspace.isPresent();
            String projectId = project.get();

            PureModelContextData pmcd = loadProjectPureModelContextData(projectId, workspaceId, isGroup, identity);

            return new ProjectResolvedContext(pmcd, pmcd);
        }

        if (required)
        {
            throw new EngineException("project/workspace or coordinates must be supplied");
        }

        return null;
    }

    private PureModelContextPointer pointerFromCoordinates(String coordinates)
    {
        AlloySDLC sdlc = new AlloySDLC();
        enrichCoordinates(sdlc, coordinates);
        PureModelContextPointer pointer = new PureModelContextPointer();
        pointer.sdlcInfo = sdlc;
        return pointer;
    }

    private void enrichCoordinates(AlloySDLC alloySDLC, String coordinates)
    {
        String[] parts = coordinates.split(":");
        if (parts.length != 3)
        {
            throw new IllegalArgumentException("Invalid coordinates on service " + coordinates);
        }

        alloySDLC.groupId = parts[0];
        alloySDLC.artifactId = parts[1];
        alloySDLC.version = parts[2];
    }

    private PureModelContextData loadProjectPureModelContextData(String project, String workspace, boolean isGroup, Identity identity)
    {
        WorkspaceSDLC sdlcInfo = new WorkspaceSDLC();
        sdlcInfo.project = project;
        sdlcInfo.version = workspace;
        sdlcInfo.isGroupWorkspace = isGroup;

        PureModelContextPointer pointer = new PureModelContextPointer();
        pointer.sdlcInfo = sdlcInfo;

        return this.modelManager.loadData(pointer, PureClientVersions.production, identity);
    }
}