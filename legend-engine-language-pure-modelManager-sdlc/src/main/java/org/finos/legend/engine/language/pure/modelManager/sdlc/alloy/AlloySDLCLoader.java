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

package org.finos.legend.engine.language.pure.modelManager.sdlc.alloy;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.modelManager.sdlc.SDLCLoader;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.MetaDataServerConfiguration;
import org.finos.legend.engine.protocol.pure.v1.model.context.AlloySDLC;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;

import javax.security.auth.Subject;
import java.util.List;
import java.util.stream.Collectors;

public class AlloySDLCLoader
{
    private final MetaDataServerConfiguration metaDataServerConfiguration;

    public AlloySDLCLoader(MetaDataServerConfiguration metaDataServerConfiguration)
    {
        this.metaDataServerConfiguration = metaDataServerConfiguration;
    }

    public PureModelContextData loadAlloyProject(Subject subject, AlloySDLC alloySDLC, String clientVersion)
    {
        String url = (alloySDLC.version == null || alloySDLC.version.equals("none") || alloySDLC.version.equals("master-SNAPSHOT")) ?
                metaDataServerConfiguration.getAlloy().getBaseUrl() + "/metadata/api/projects/" + alloySDLC.project + "/revisions/latest/pureModelContextData/" + clientVersion  :
                metaDataServerConfiguration.getAlloy().getBaseUrl() + "/metadata/api/projects/" + alloySDLC.project + "/versions/" + alloySDLC.version + "/pureModelContextData/" + clientVersion;
        return SDLCLoader.loadMetadataFromHTTPURL(subject, LoggingEventType.METADATA_REQUEST_ALLOY_PROJECT_START, LoggingEventType.METADATA_REQUEST_ALLOY_PROJECT_STOP, url);
    }

    public List<String> checkAllPathsExist(PureModelContextData data, AlloySDLC alloySDLC) {
        List<String> pathsFromPointer = alloySDLC.packageableElementPointers.stream().map(s -> s.path).collect(Collectors.toList());
        List<String> entities = data.getElements().stream().map(s -> s.getPath()).collect(Collectors.toList());

        pathsFromPointer.removeAll(entities);
        return pathsFromPointer;
    }

}
