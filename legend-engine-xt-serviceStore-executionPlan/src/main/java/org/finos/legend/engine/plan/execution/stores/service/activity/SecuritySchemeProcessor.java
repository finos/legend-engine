package org.finos.legend.engine.plan.execution.stores.service.activity;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.AuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.SecurityScheme;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.SimpleHttpSecurityScheme;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.UsernamePasswordSpecification;
import org.pac4j.core.profile.CommonProfile;

public class SecuritySchemeProcessor
{
    private HttpClientBuilder httpClientBuilder;
    private AuthenticationSpecification authSpecification;
    private RequestBuilder requestBuilder;
    private MutableList<CommonProfile> profiles;

    public SecuritySchemeProcessor(AuthenticationSpecification authSpecification, HttpClientBuilder httpClientBuilder, RequestBuilder requestBuilder, MutableList<CommonProfile> profiles)
    {
        this.authSpecification = authSpecification;
        this.httpClientBuilder = httpClientBuilder;
        this.requestBuilder = requestBuilder;
        this.profiles = profiles;
    }

    public Boolean visit(SecurityScheme securityScheme)
    {
        if (securityScheme instanceof SimpleHttpSecurityScheme)
        {
            try
            {
                UsernamePasswordSpecification spec = (UsernamePasswordSpecification) this.authSpecification;
                String encoding = Base64.encodeBase64String((spec.username+ ":" + spec.password).getBytes());
                requestBuilder.addHeader("Authorization", "Basic " + encoding);
                return true;
            }
            catch (Exception e)
            {
                return false;
            }
        }
        return null;
    }
}
