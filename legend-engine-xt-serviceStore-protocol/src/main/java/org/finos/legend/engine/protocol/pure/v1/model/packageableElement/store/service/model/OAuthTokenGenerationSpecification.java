package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model;

public class OAuthTokenGenerationSpecification extends AuthenticationSpecification
{
    public OauthGrantType grantType;
    public String clientId;
    public String clientSecretVaultReference;
    public String authServerUrl;
}
