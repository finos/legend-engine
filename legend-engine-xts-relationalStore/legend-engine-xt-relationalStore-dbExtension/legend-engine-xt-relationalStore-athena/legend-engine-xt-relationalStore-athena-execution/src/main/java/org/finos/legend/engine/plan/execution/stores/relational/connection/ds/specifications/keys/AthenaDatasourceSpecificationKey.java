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
    public String region;
    public String catalog;
    public String database;
    public String workGroup;
    public String outputLocation;
    public String athenaEndpoint;

    public AthenaDatasourceSpecificationKey(String region, String catalog, String database, String workGroup, String outputLocation, String athenaEndpoint)
    {
        this.region = region;
        this.catalog = catalog;
        this.database = database;
        this.workGroup = workGroup;
        this.outputLocation = outputLocation;
        this.athenaEndpoint = athenaEndpoint;
    }

    public String getRegion()
    {
        return region;
    }

    public String getCatalog()
    {
        return catalog;
    }

    public String getDatabase()
    {
        return database;
    }

    public String getWorkGroup()
    {
        return workGroup;
    }

    public String getOutputLocation()
    {
        return outputLocation;
    }

    public String getAthenaEndpoint()
    {
        return athenaEndpoint;
    }

    @Override
    public String toString()
    {
        return "AthenaDatasourceSpecificationKey{" +
                "region='" + region + '\'' +
                ", catalog='" + catalog + '\'' +
                ", database='" + database + '\'' +
                ", workGroup='" + workGroup + '\'' +
                ", outputLocation='" + outputLocation + '\'' +
                ", athenaEndpoint='" + athenaEndpoint + '\'' +
                '}';
    }

    @Override
    public String shortId()
    {
        return "Athena_" +
                "region:" + region + "_" +
                "catalog:" + catalog + "_" +
                "workGroup:" + workGroup + "_" +
                "outputLocation:" + outputLocation + "_" +
                "athenaEndpoint:" + athenaEndpoint + "_" +
                "db:" + database;
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof AthenaDatasourceSpecificationKey))
        {
            return false;
        }
        AthenaDatasourceSpecificationKey that = (AthenaDatasourceSpecificationKey) o;
        return Objects.equals(region, that.region) && Objects.equals(athenaEndpoint, that.athenaEndpoint) && Objects.equals(catalog, that.catalog) && Objects.equals(database, that.database) && Objects.equals(workGroup, that.workGroup) && Objects.equals(outputLocation, that.outputLocation);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(region, athenaEndpoint, catalog, database, workGroup, outputLocation);
    }
}
