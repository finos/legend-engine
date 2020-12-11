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

import org.finos.legend.engine.language.pure.modelManager.sdlc.SDLCLoader;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.MetaDataServerConfiguration;
import org.finos.legend.engine.protocol.pure.v1.model.context.AlloySDLC;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.pac4j.core.profile.ProfileManager;

import javax.security.auth.Subject;

public class AlloySDLCLoader
{
    private final MetaDataServerConfiguration metaDataServerConfiguration;

    public AlloySDLCLoader(MetaDataServerConfiguration metaDataServerConfiguration)
    {
        this.metaDataServerConfiguration = metaDataServerConfiguration;
    }

    public PureModelContextData loadAlloyProject(ProfileManager pm, AlloySDLC alloySDLC, String clientVersion)
    {
        String url = (alloySDLC.version == null || alloySDLC.version.equals("none")) ?
                metaDataServerConfiguration.getAlloy().getBaseUrl() + "/metadata/api/projects/" + alloySDLC.project + "/revisions/latest/pureModelContextData/" + clientVersion  :
                metaDataServerConfiguration.getAlloy().getBaseUrl() + "/metadata/api/projects/" + alloySDLC.project + "/versions/" + alloySDLC.version + "/pureModelContextData/" + clientVersion;
        return SDLCLoader.loadMetadataFromHTTPURL(pm, LoggingEventType.METADATA_REQUEST_ALLOY_PROJECT_START, LoggingEventType.METADATA_REQUEST_ALLOY_PROJECT_STOP, url);
    }
}
