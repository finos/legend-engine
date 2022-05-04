// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys;

import org.finos.legend.engine.plan.execution.stores.relational.H2LocalServer;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class LocalH2DataSourceSpecificationKey extends StaticDataSourceSpecificationKey
{
    private static final String LOCAL_HOST = "127.0.0.1";
    private static final String LOCAL_H2_DB_NAME = "";
    private final List<String> testDataSetupSqls;
    private final long setupCheckSum;
    private final int port;
    private final Checksum crc32 = new CRC32();


    public LocalH2DataSourceSpecificationKey( List<String> testDataSetupSqls)
    {
        super(LOCAL_HOST, H2LocalServer.getInstance().getPort(), LOCAL_H2_DB_NAME);
        this.testDataSetupSqls = testDataSetupSqls;
        this.port = H2LocalServer.getInstance().getPort();
        byte[] bytes = this.testDataSetupSqls.stream().collect(Collectors.joining(";")).getBytes();
        crc32.update(bytes, 0, bytes.length);
        this.setupCheckSum = crc32.getValue();
    }

    public List<String> getTestDataSetupSqls()
    {
        return this.testDataSetupSqls;
    }

    @Override
    public String shortId()
    {
        return "LocalH2_" +
                "port:" + port + "_" +
                "sqlCS:" + setupCheckSum;
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
        LocalH2DataSourceSpecificationKey that = (LocalH2DataSourceSpecificationKey)o;
        return Objects.equals(testDataSetupSqls, that.testDataSetupSqls)
                && Objects.equals(port, that.port);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(testDataSetupSqls, port);
    }
}
