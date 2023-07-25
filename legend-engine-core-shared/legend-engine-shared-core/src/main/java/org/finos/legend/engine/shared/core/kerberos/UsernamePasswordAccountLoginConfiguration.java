// Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.shared.core.kerberos;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import java.util.HashMap;
import java.util.Map;

public class UsernamePasswordAccountLoginConfiguration extends Configuration
{
    private String username;
    private String [] password;

    public UsernamePasswordAccountLoginConfiguration(String username)
    {
        this.username = username;
        this.password = password;
    }

    @Override
    public AppConfigurationEntry[] getAppConfigurationEntry(String name)
    {
        try
        {
            Map<String, String> options = new HashMap<>();
            options.put("principal", this.username);
            options.put("debug", "false");
            options.put("storeKey", "true");

            AppConfigurationEntry ace = new AppConfigurationEntry(
                    "com.sun.security.auth.module.Krb5LoginModule",
                    AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                    options
            );

            AppConfigurationEntry[] entryArray = new AppConfigurationEntry[1];
            entryArray[0] = ace;

            return entryArray;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}