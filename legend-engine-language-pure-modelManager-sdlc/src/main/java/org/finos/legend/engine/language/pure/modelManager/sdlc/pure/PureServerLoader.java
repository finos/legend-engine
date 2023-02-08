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

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.modelManager.sdlc.SDLCLoader;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.MetaDataServerConfiguration;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.PureServerConnectionConfiguration;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureSDLC;
import org.finos.legend.engine.shared.core.kerberos.HttpClientBuilder;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.pac4j.core.profile.CommonProfile;

import javax.security.auth.Subject;
import java.util.List;

public class PureServerLoader
{
    private MetaDataServerConfiguration metaDataServerConfiguration;

    public PureServerLoader(MetaDataServerConfiguration metaDataServerConfiguration)
    {
        this.metaDataServerConfiguration = metaDataServerConfiguration;
    }

    public String getBaseUrl(String overrideUrl)
    {
        if (overrideUrl == null || overrideUrl.equals(metaDataServerConfiguration.getPure().getBaseUrl()) || !(metaDataServerConfiguration.pure instanceof PureServerConnectionConfiguration))
        {
            return metaDataServerConfiguration.getPure().getBaseUrl();
        }
        List<String> allowedOverrideUrls = ((PureServerConnectionConfiguration)metaDataServerConfiguration.getPure()).allowedOverrideUrls;
        if (allowedOverrideUrls.contains(overrideUrl))
        {
            return overrideUrl;
        }
        throw new RuntimeException(overrideUrl + " is not a valid metadata server url. Valid Urls are - " + allowedOverrideUrls);
    }

    public String buildPureMetadataVersionURL(String urlSuffix, String overrideUrl)
    {
        return getBaseUrl(overrideUrl) + "/alloy/pureServerBaseVersion" + urlSuffix;
    }

    @Deprecated
    protected String buildPureMetadataURL(PackageableElementPointer pointer, String urlSegment, String clientVersion, String urlSuffix)
    {
        return buildPureMetadataURL(pointer, urlSegment, clientVersion, urlSuffix, null);
    }

    protected String buildPureMetadataURL(PackageableElementPointer pointer, String urlSegment, String clientVersion, String urlSuffix, String overrideUrl)
    {
        return getBaseUrl(overrideUrl) + "/alloy/" + urlSegment + "/" + clientVersion + "/" + pointer.path + urlSuffix;
    }

    protected HttpUriRequest buildRequest(String url, MutableList<CommonProfile> profiles)
    {
        RequestBuilder builder = RequestBuilder.get(url);
        HttpUriRequest httpUriRequest = builder.build();
        return httpUriRequest;
    }

    public String getBaseServerVersion(MutableList<CommonProfile> profiles, Subject executionSubject, String overrideUrl)
    {
        CloseableHttpClient httpclient = (CloseableHttpClient) HttpClientBuilder.getHttpClient(new BasicCookieStore());
        HttpUriRequest request = buildRequest(buildPureMetadataVersionURL(executionSubject == null ? "" : "?auth=kerberos", overrideUrl), profiles);
        try (CloseableHttpResponse response = httpclient.execute(request))
        {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode < 200 || statusCode >= 300)
            {
                throw new EngineException("Error response with " + statusCode + "\n" + EntityUtils.toString(response.getEntity()));
            }
            return EntityUtils.toString(response.getEntity());
        }
        catch (Exception e)
        {
            throw new EngineException("Engine was unable to load information from the Pure SDLC :" + e.getMessage());
        }
    }

    public PureModelContext getCacheKey(PureModelContext context, MutableList<CommonProfile> profiles, Subject executionSubject)
    {
        PureModelContextPointer deepCopy = new PureModelContextPointer();
        PureSDLC sdlc = new PureSDLC();
        sdlc.packageableElementPointers = ((PureSDLC) ((PureModelContextPointer) context).sdlcInfo).packageableElementPointers;
        sdlc.overrideUrl = getBaseUrl(((PureSDLC) ((PureModelContextPointer) context).sdlcInfo).overrideUrl);
        deepCopy.sdlcInfo = sdlc;
        deepCopy.serializer = ((PureModelContextPointer) context).serializer;
        deepCopy.sdlcInfo.baseVersion = this.getBaseServerVersion(profiles, executionSubject, ((PureSDLC) ((PureModelContextPointer) context).sdlcInfo).overrideUrl);
        return deepCopy;
    }

    @Deprecated
    public PureModelContextData loadPurePackageableElementPointer(MutableList<CommonProfile> pm, PackageableElementPointer pointer, String clientVersion, String urlSuffix)
    {
        return loadPurePackageableElementPointer(pm, pointer, clientVersion, urlSuffix, null);
    }

    public PureModelContextData loadPurePackageableElementPointer(MutableList<CommonProfile> pm, PackageableElementPointer pointer, String clientVersion, String urlSuffix, String overrideUrl)
    {
        switch (pointer.type)
        {
            case MAPPING:
                return SDLCLoader.loadMetadataFromHTTPURL(pm, LoggingEventType.METADATA_REQUEST_MAPPING_START, LoggingEventType.METADATA_REQUEST_MAPPING_STOP, buildPureMetadataURL(pointer, "pureModelFromMapping", clientVersion, urlSuffix, overrideUrl));
            case STORE:
                return SDLCLoader.loadMetadataFromHTTPURL(pm, LoggingEventType.METADATA_REQUEST_STORE_START, LoggingEventType.METADATA_REQUEST_STORE_STOP, buildPureMetadataURL(pointer, "pureModelFromStore", clientVersion, urlSuffix, overrideUrl));
            case SERVICE:
                return SDLCLoader.loadMetadataFromHTTPURL(pm, LoggingEventType.METADATA_REQUEST_SERVICE_START, LoggingEventType.METADATA_REQUEST_SERVICE_STOP, buildPureMetadataURL(pointer, "pureModelFromService", clientVersion, urlSuffix, overrideUrl));
            default:
                throw new UnsupportedOperationException(pointer.type + " is not supported!");
        }
    }

}
