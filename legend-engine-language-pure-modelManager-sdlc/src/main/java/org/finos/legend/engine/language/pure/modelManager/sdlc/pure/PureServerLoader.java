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

package org.finos.legend.engine.language.pure.modelManager.sdlc.pure;

import org.finos.legend.engine.language.pure.modelManager.sdlc.SDLCLoader;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.MetaDataServerConfiguration;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;

import javax.security.auth.Subject;

public class PureServerLoader
{
    private MetaDataServerConfiguration metaDataServerConfiguration;

    public PureServerLoader(MetaDataServerConfiguration metaDataServerConfiguration)
    {
        this.metaDataServerConfiguration = metaDataServerConfiguration;
    }

    private String buildPureMetadataURL(PackageableElementPointer pointer, String urlSegment, String clientVersion, String urlSuffix)
    {
        return metaDataServerConfiguration.getPure().getBaseUrl() + "/alloy/" + urlSegment + "/" + clientVersion + "/" + pointer.path + urlSuffix;
    }

    public PureModelContextData loadPurePackageableElementPointer(Subject subject, PackageableElementPointer pointer, String clientVersion, String urlSuffix)
    {
        switch (pointer.type)
        {
            case MAPPING:
                return SDLCLoader.loadMetadataFromHTTPURL(subject, LoggingEventType.METADATA_REQUEST_MAPPING_START, LoggingEventType.METADATA_REQUEST_MAPPING_STOP, buildPureMetadataURL(pointer, "pureModelFromMapping", clientVersion, urlSuffix));
            case STORE:
                return SDLCLoader.loadMetadataFromHTTPURL(subject, LoggingEventType.METADATA_REQUEST_STORE_START, LoggingEventType.METADATA_REQUEST_STORE_STOP, buildPureMetadataURL(pointer, "pureModelFromStore", clientVersion, urlSuffix));
            case SERVICE:
                return SDLCLoader.loadMetadataFromHTTPURL(subject, LoggingEventType.METADATA_REQUEST_SERVICE_START, LoggingEventType.METADATA_REQUEST_SERVICE_STOP, buildPureMetadataURL(pointer, "pureModelFromService", clientVersion, urlSuffix));
            default:
                throw new UnsupportedOperationException(pointer.type + " is not supported!");
        }
    }

}
