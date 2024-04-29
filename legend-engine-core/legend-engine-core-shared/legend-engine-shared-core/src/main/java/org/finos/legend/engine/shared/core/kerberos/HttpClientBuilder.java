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

package org.finos.legend.engine.shared.core.kerberos;

import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthSchemeFactory;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.KerberosCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.auth.SPNegoScheme;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;

import java.io.IOException;
import java.security.Principal;

public class HttpClientBuilder
{
    public static HttpClient getHttpClient(CookieStore cookieStore)
    {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        httpclient.getAuthSchemes().register(AuthSchemes.SPNEGO, new SPNegoWithDelegationSchemeFactory());
        Credentials credentials = new Credentials()
        {
            @Override
            public String getPassword()
            {
                return null;
            }

            @Override
            public Principal getUserPrincipal()
            {
                return null;
            }
        };
        httpclient.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM), credentials);
        httpclient.setCookieStore(cookieStore);
        return httpclient;
    }

    public static CookieStore buildCookieStore(String url) throws IOException
    {
        BasicCookieStore cookieStore = new BasicCookieStore();
        HttpClient httpclient = HttpClientBuilder.getHttpClient(cookieStore);
        HttpUriRequest request = new HttpGet(url);
        httpclient.execute(request);
        return cookieStore;
    }

    private static class SPNegoWithDelegationSchemeFactory implements AuthSchemeFactory
    {
        @Override
        public AuthScheme newInstance(HttpParams params)
        {
            return new SPNegoWithDelegationScheme();
        }
    }

    private static class SPNegoWithDelegationScheme extends SPNegoScheme
    {
        private SPNegoWithDelegationScheme()
        {
            super(true, false);
        }

        @Override
        protected byte[] generateGSSToken(byte[] input, Oid oid, String authServer, Credentials credentials) throws GSSException
        {
            GSSCredential gssCredential = (credentials instanceof KerberosCredentials) ? ((KerberosCredentials) credentials).getGSSCredential() : null;

            GSSManager manager = GSSManager.getInstance();
            GSSName serverName = manager.createName("HTTP@" + authServer, GSSName.NT_HOSTBASED_SERVICE);

            GSSContext gssContext = manager.createContext(serverName.canonicalize(oid), oid, gssCredential, GSSContext.DEFAULT_LIFETIME);
            gssContext.requestCredDeleg(true);
            gssContext.requestMutualAuth(true);

            return gssContext.initSecContext((input == null) ? new byte[0] : input, 0, (input == null) ? 0 : input.length);
        }
    }
}
