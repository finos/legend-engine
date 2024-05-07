// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.query.graphQL.api;

import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.MetaDataServerConfiguration;
import org.finos.legend.engine.protocol.graphQL.metamodel.Document;
import org.finos.legend.engine.protocol.graphQL.metamodel.ProtocolToMetamodelTranslator;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.AlloySDLC;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.WorkspaceSDLC;
import org.finos.legend.engine.shared.core.identity.Identity;


public abstract class GraphQL
{
    protected ModelManager modelManager;

    public GraphQL(ModelManager modelManager, MetaDataServerConfiguration metadataserver)
    {
        this.modelManager = modelManager;
    }

    public static org.finos.legend.pure.generated.Root_meta_external_query_graphQL_metamodel_sdl_Document toPureModel(Document document, PureModel pureModel)
    {
        return new ProtocolToMetamodelTranslator().translate(document, pureModel);
    }

    protected PureModel loadSDLCProjectModel(Identity identity, HttpServletRequest request, String projectId, String workspaceId, boolean isGroupWorkspace) throws PrivilegedActionException
    {
        WorkspaceSDLC sdlcInfo = new WorkspaceSDLC();
        sdlcInfo.project = projectId;
        sdlcInfo.version = workspaceId;
        sdlcInfo.isGroupWorkspace = isGroupWorkspace;

        PureModelContextPointer pointer = new PureModelContextPointer();
        pointer.sdlcInfo = sdlcInfo;

        Subject subject = identity.getSubjectFromIdentity();
        return subject == null ?
                this.modelManager.loadModel(pointer, PureClientVersions.production, identity, "") :
                Subject.doAs(subject, (PrivilegedExceptionAction<PureModel>) () -> this.modelManager.loadModel(pointer, PureClientVersions.production, identity, ""));
    }

    protected PureModel loadProjectModel(Identity identity, String groupId, String artifactId, String versionId) throws PrivilegedActionException
    {
        Subject subject = identity.getSubjectFromIdentity();
        PureModelContextPointer pointer = new PureModelContextPointer();
        AlloySDLC sdlcInfo = new AlloySDLC();
        sdlcInfo.groupId = groupId;
        sdlcInfo.artifactId = artifactId;
        sdlcInfo.version = versionId;
        pointer.sdlcInfo = sdlcInfo;
        return subject == null ?
                this.modelManager.loadModel(pointer, PureClientVersions.production, identity, "") :
                Subject.doAs(subject, (PrivilegedExceptionAction<PureModel>) () -> this.modelManager.loadModel(pointer, PureClientVersions.production, identity, ""));
    }
}
