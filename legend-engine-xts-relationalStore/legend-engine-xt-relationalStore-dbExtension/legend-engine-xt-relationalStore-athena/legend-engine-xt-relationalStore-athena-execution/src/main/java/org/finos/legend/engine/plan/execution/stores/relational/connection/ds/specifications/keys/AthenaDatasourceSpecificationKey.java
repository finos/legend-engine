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

import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecificationKey;

import java.util.Objects;

public class AthenaDatasourceSpecificationKey implements DataSourceSpecificationKey
{
    private final String awsRegion;
    private final String s3OutputLocation;
    private final String databaseName;
    private final String workgroup;

    public AthenaDatasourceSpecificationKey(String awsRegion, String s3OutputLocation, String databaseName, String workgroup)
    {
        this.awsRegion = awsRegion;
        this.s3OutputLocation = s3OutputLocation;
        this.databaseName = databaseName;
        this.workgroup = workgroup;
    }

    public String getAwsRegion()
    {
        return awsRegion;
    }

    public String getS3OutputLocation()
    {
        return s3OutputLocation;
    }

    public String getDatabaseName()
    {
        return databaseName;
    }

    public String getWorkgroup()
    {
        return workgroup;
    }

    @Override
    public String toString()
    {
        return "AthenaDatasourceSpecificationKey{" +
                "awsRegion='" + awsRegion + '\'' +
                ", s3OutputLocation=" + s3OutputLocation +
                ", databaseName='" + databaseName + '\'' +
                (workgroup == null ? "" : ", workgroup='" + workgroup + '\'') +
                '}';
    }

    @Override
    public String shortId()
    {
        return "Athena_" +
                "awsRegion:" + awsRegion + "_" +
                "s3OutputLocation:" + s3OutputLocation + "_" +
                "db:" + databaseName +
                (workgroup == null ? "" : "_workgroup:" + workgroup);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof AthenaDatasourceSpecificationKey))
        {
            return false;
        }
        AthenaDatasourceSpecificationKey that = (AthenaDatasourceSpecificationKey) o;
        return Objects.equals(s3OutputLocation, that.s3OutputLocation) &&
                Objects.equals(awsRegion, that.awsRegion) &&
                Objects.equals(databaseName, that.databaseName) &&
                Objects.equals(workgroup, that.workgroup);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(awsRegion, s3OutputLocation, databaseName, workgroup);
    }
}
