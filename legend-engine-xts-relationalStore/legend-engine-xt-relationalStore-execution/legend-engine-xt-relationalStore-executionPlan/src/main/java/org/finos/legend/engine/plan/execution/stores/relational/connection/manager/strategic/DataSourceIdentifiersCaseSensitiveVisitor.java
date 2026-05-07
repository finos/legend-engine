//  Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.connection.manager.strategic;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecificationVisitor;

import java.util.List;
import java.util.Objects;

public class DataSourceIdentifiersCaseSensitiveVisitor implements DatasourceSpecificationVisitor<Boolean>
{
    public Boolean visit(DatasourceSpecification datasourceSpecification)
    {
        List<StrategicConnectionExtension> extensions = StrategicConnectionExtensionLoader.extensions();
        return ListIterate
                .collect(extensions, extension -> extension.getIdentifiersCaseSensitivity(datasourceSpecification))
                .select(Objects::nonNull)
                .getFirstOptional()
                .orElse(null);
    }
}
