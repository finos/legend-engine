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

package org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys;

import java.util.Objects;

public class TrinoDelegatedKerberosAuthenticationStrategyKey implements AuthenticationStrategyKey
{

    public String serverPrincipal;
    public String kerberosRemoteServiceName;
    public Boolean kerberosUseCanonicalHostname;


    @Override
    public String shortId()
    {
        return "type:" + type() +
                "_serverPrincipal:" + serverPrincipal +
                "_kerberosRemoteServiceName:" + kerberosRemoteServiceName +
                "_kerberosUseCanonicalHostname:" + kerberosUseCanonicalHostname;
    }

    @Override
    public String type()
    {
        return "TrinoDelegatedKerberosAuth";
    }

    @Override
    public String toString()
    {
        return "TrinoDelegatedKerberosAuthenticationStrategyKey{" +
                "serverPrincipal='" + serverPrincipal + '\'' +
                ", kerberosRemoteServiceName='" + kerberosRemoteServiceName + '\'' +
                ", kerberosUseCanonicalHostname=" + kerberosUseCanonicalHostname +
                '}';
    }

    public TrinoDelegatedKerberosAuthenticationStrategyKey(String serverPrincipal, String kerberosRemoteServiceName, Boolean kerberosUseCanonicalHostname)
    {
        this.serverPrincipal = serverPrincipal;
        this.kerberosRemoteServiceName = kerberosRemoteServiceName;
        this.kerberosUseCanonicalHostname = kerberosUseCanonicalHostname;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        TrinoDelegatedKerberosAuthenticationStrategyKey that = (TrinoDelegatedKerberosAuthenticationStrategyKey) o;
        return serverPrincipal.equals(that.serverPrincipal) && kerberosRemoteServiceName.equals(that.kerberosRemoteServiceName) && kerberosUseCanonicalHostname.equals(that.kerberosUseCanonicalHostname);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(serverPrincipal, kerberosRemoteServiceName, kerberosUseCanonicalHostname);
    }

    public String getServerPrincipal()
    {
        return serverPrincipal;
    }

    public String getKerberosRemoteServiceName()
    {
        return kerberosRemoteServiceName;
    }

    public Boolean getKerberosUseCanonicalHostname()
    {
        return kerberosUseCanonicalHostname;
    }
}
