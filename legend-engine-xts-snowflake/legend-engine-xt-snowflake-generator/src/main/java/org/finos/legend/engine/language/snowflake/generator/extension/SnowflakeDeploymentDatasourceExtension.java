// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.language.snowflake.generator.extension;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecificationVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SnowflakeDatasourceSpecification;
import org.finos.legend.engine.shared.core.extension.LegendConnectionExtension;
import org.finos.legend.engine.shared.core.identity.Identity;

/**
 * Allows a downstream extension to resolve a {@code DatasourceSpecification} type that this
 * module does not know about (e.g. a store-specific Snowflake connection variant defined
 * outside this repository) into an equivalent {@code SnowflakeDatasourceSpecification}. The
 * returned visitor must return {@code null} for any specification it does not own, so that
 * resolvers can try multiple registered extensions in turn.
 * <p>
 * The caller's {@code Identity} is passed through because resolving such a type may require
 * an authenticated call to a downstream metadata/catalog service (e.g. to look up the physical
 * account/region behind a logical environment identifier).
 */
public interface SnowflakeDeploymentDatasourceExtension extends LegendConnectionExtension
{
    DatasourceSpecificationVisitor<SnowflakeDatasourceSpecification> getExtraSnowflakeDatasourceSpecificationVisitor(Identity identity);
}
