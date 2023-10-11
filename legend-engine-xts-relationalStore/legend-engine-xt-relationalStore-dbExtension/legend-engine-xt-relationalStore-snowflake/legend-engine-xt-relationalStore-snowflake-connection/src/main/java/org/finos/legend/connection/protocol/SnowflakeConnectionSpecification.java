//  Copyright 2023 Goldman Sachs
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

package org.finos.legend.connection.protocol;

public class SnowflakeConnectionSpecification extends ConnectionSpecification
{
    public String accountName;
    public String region;
    public String warehouseName;
    public String databaseName;
    public String cloudType;

    public Boolean quotedIdentifiersIgnoreCase;
    public Boolean enableQueryTags;
    public String proxyHost;
    public String proxyPort;
    public String nonProxyHosts;
    public String organization;
    public String accountType;

    public String role;

    @Override
    public String shortId()
    {
        return "Snowflake" +
                "--account=" + accountName +
                "--region=" + region +
                "--warehouse=" + warehouseName +
                "--db=" + databaseName +
                "--cloudType=" + cloudType +
                "--proxyHost=" + proxyHost +
                "--proxyPort=" + proxyPort +
                "--nonProxyHosts=" + nonProxyHosts +
                "--accountType=" + accountType +
                "--organisation=" + organization +
                "--quoteIdentifiers=" + quotedIdentifiersIgnoreCase +
                "--role=" + role +
                "--enableQueryTags=" + enableQueryTags;
    }
}
