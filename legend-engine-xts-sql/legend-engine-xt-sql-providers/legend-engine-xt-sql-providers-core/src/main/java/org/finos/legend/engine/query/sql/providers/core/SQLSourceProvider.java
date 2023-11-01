// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.finos.legend.engine.query.sql.providers.core;

import org.eclipse.collections.api.list.MutableList;
import org.pac4j.core.profile.CommonProfile;

import java.util.List;

public interface SQLSourceProvider
{
    String getType();

    //TODO remove default impl
    default SQLSourceResolvedContext resolve(List<TableSource> sources, SQLContext context, MutableList<CommonProfile> profiles)
    {
        return null;
    }

    //TODO remove default impl
    @Deprecated
    default org.finos.legend.engine.query.sql.api.sources.SQLSourceResolvedContext resolve(List<org.finos.legend.engine.query.sql.api.sources.TableSource> sources, org.finos.legend.engine.query.sql.api.sources.SQLContext context, MutableList<CommonProfile> profiles)
    {
        return null;
    }
}