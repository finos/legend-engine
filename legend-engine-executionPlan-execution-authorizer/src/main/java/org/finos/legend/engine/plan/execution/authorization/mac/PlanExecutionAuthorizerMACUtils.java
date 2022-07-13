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

package org.finos.legend.engine.plan.execution.authorization.mac;

import org.finos.legend.engine.shared.core.vault.Vault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/*
    A helper class to generate and verify message authentication code(MAC)s. See https://en.wikipedia.org/wiki/Message_authentication_code

    In the context of authorizing an execution plan we use a MAC in a very limited context.

    Middle tier plan execution involves two steps : (1) Authorizing the plan and (2) Executing the plan which requires acquiring database connections.
    These two operations are performed by different pieces of code at different points in the call stack.
    This means that the connection acquisition code has to blindly assume that code higher in the call stack has performed the authorization check.
    Depending on the execution environment this might be a safe assumption to make.
    However, we want to protect against accidental incorrect use of the authorization protocol. For e.g a developer might add an execution code path to the platform that executes a middle tier plan without performing an authorization check.

    Therefore, we implement a simple protocol where the authorizing code generates and passes a MAC to the connection acquisition code.
    The connection acquisition code acquires a connection only if the incoming MAC is valid.

    For now, the message used to generate the MAC is a static message. i.e the connection acquisition code only verifies that an authorization check has been performed. Not that the authorization check has been performed for the connection being requested.
    This can be added in the future.

    Another way to solve this problem of creating a connection without authorization is to move the authorization code into the connection code.
    However, this has a couple of drawbacks :
    1/ The use of connection pooling can result in the authorization check being bypassed. (At time t0 we create and cache a connection for Alice. At time t1, authz is revoked. At time t2, the connection still exists in the pool and we use the connection without authz).
    2/ If a plan uses multiple middle tier connections, the authorizer which has full access to the plan can perform a batch authorization check. This is not possible by the connection code which does not see the full plan.

 */
public class PlanExecutionAuthorizerMACUtils
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PlanExecutionAuthorizerMACUtils.class);

    public String generateMAC(String message, String keyVaultReference) throws Exception
    {
        Mac mac = Mac.getInstance("HmacSHA512");
        byte[] rawKeyBytes = this.getKeyFromVault(keyVaultReference);
        mac.init(new SecretKeySpec(rawKeyBytes, "HmacSHA512"));
        mac.update(message.getBytes(StandardCharsets.UTF_8));
        return new String(Base64.getEncoder().encode(mac.doFinal()), StandardCharsets.UTF_8);
    }

    public MACValidationResult isValidMAC(String message, String receivedMac, String keyVaultReference) throws Exception
    {
        String generatedMac = this.generateMAC(message, keyVaultReference);
        return new MACValidationResult(receivedMac.equals(generatedMac), receivedMac, generatedMac);
    }

    private byte[] getKeyFromVault(String keyVaultReference) throws Exception
    {
        String base64EncodedKeyValue = Vault.INSTANCE.getValue(keyVaultReference);
        byte[] rawKeyBytes = Base64.getDecoder().decode(base64EncodedKeyValue);
        return rawKeyBytes;
    }

    public static class MACValidationResult
    {
        private boolean isValidMAC;
        private String receivedMAC;
        private String generatedMAC;

        public MACValidationResult(boolean isValidMAC, String receivedMAC, String generatedMAC)
        {
            this.isValidMAC = isValidMAC;
            this.receivedMAC = receivedMAC;
            this.generatedMAC = generatedMAC;
        }

        public boolean isValidMAC()
        {
            return isValidMAC;
        }

        public String getReceivedMAC()
        {
            return receivedMAC;
        }

        public String getGeneratedMAC()
        {
            return generatedMAC;
        }

        @Override
        public String toString()
        {
            return "MACValidationResult{" +
                    "isValidMAC=" + isValidMAC +
                    ", receivedMAC='" + receivedMAC + '\'' +
                    ", generatedMAC='" + generatedMAC + '\'' +
                    '}';
        }
    }
}
