// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.ds.specifications.keys;

import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecificationKey;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class DuckDBDataSourceSpecificationKey implements DataSourceSpecificationKey
{
    private final String path;
    private final List<String> testDataSetupSqls;
    private final long setupCheckSum;

    public DuckDBDataSourceSpecificationKey(String path)
    {
        this(path, Collections.emptyList());
    }

    public DuckDBDataSourceSpecificationKey(String path, List<String> testDataSetupSqls)
    {
        this.path = path;
        this.testDataSetupSqls = testDataSetupSqls != null ? testDataSetupSqls : Collections.emptyList();
        Checksum crc32 = new CRC32();
        byte[] bytes = String.join(";", this.testDataSetupSqls).getBytes();
        crc32.update(bytes, 0, bytes.length);
        this.setupCheckSum = crc32.getValue();
    }

    public String getPath()
    {
        return path;
    }

    public List<String> getTestDataSetupSqls()
    {
        return testDataSetupSqls;
    }

    @Override
    public String toString()
    {
        return "DuckDBDataSourceSpecificationKey{" +
                "path='" + path + '\'' +
                '}';
    }

    @Override
    public String shortId()
    {
        return "DuckDB_" +
                "path:" + path +
                (testDataSetupSqls.isEmpty() ? "" : "_sqlCS:" + setupCheckSum);
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
        DuckDBDataSourceSpecificationKey that = (DuckDBDataSourceSpecificationKey) o;
        return Objects.equals(path, that.path)
                && Objects.equals(testDataSetupSqls, that.testDataSetupSqls);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(path, testDataSetupSqls);
    }
}
