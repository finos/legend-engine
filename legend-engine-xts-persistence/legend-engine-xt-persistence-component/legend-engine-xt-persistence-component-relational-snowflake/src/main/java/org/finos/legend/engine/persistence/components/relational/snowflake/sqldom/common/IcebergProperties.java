// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.relational.snowflake.sqldom.common;

import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils;

import java.util.Optional;

public class IcebergProperties implements SqlGen
{
    private final String catalog;
    private final String externalVolume;
    private final String baseLocation;
    private final Optional<String> catalogSync;

    private static final String CATALOG = "CATALOG";
    private static final String EXTERNAL_VOLUME = "EXTERNAL_VOLUME";
    private static final String BASE_LOCATION = "BASE_LOCATION";
    private static final String CATALOG_SYNC = "CATALOG_SYNC";

    public IcebergProperties(String catalog, String externalVolume, String baseLocation, Optional<String> catalogSync)
    {
        this.catalog = catalog;
        this.externalVolume = externalVolume;
        this.baseLocation = baseLocation;
        this.catalogSync = catalogSync;
    }

    @Override
    public void genSql(StringBuilder builder)
    {
        builder.append(CATALOG);
        builder.append(SqlGenUtils.ASSIGNMENT_OPERATOR);
        builder.append(SqlGenUtils.singleQuote(catalog));
        builder.append(SqlGenUtils.COMMA + SqlGenUtils.WHITE_SPACE);

        builder.append(EXTERNAL_VOLUME);
        builder.append(SqlGenUtils.ASSIGNMENT_OPERATOR);
        builder.append(SqlGenUtils.singleQuote(externalVolume));
        builder.append(SqlGenUtils.COMMA + SqlGenUtils.WHITE_SPACE);

        builder.append(BASE_LOCATION);
        builder.append(SqlGenUtils.ASSIGNMENT_OPERATOR);
        builder.append(SqlGenUtils.singleQuote(baseLocation));

        if (catalogSync.isPresent())
        {
            builder.append(SqlGenUtils.COMMA + SqlGenUtils.WHITE_SPACE);
            builder.append(CATALOG_SYNC);
            builder.append(SqlGenUtils.ASSIGNMENT_OPERATOR);
            builder.append(SqlGenUtils.singleQuote(catalogSync.get()));
        }
    }
}
