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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification;

public class BigQueryDatasourceSpecification extends DatasourceSpecification
{
    public String projectId;
    public String defaultDataset;
    public String proxyHost;
    public String proxyPort;

    public BigQueryDatasourceSpecification()
    {
    }

    public BigQueryDatasourceSpecification(String projectId, String defaultDataset)
    {
        this.projectId = projectId;
        this.defaultDataset = defaultDataset;
    }

    @Override
    public <T> T accept(DatasourceSpecificationVisitor<T> datasourceSpecificationVisitor)
    {
        return datasourceSpecificationVisitor.visit(this);
    }
}