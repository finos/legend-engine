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

package org.finos.legend.engine.query.sql.api.sources;

import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;

import java.util.List;

public class SQLSourceResolvedContext
{
    private final PureModelContext pureModelContext;
    private final List<SQLSource> sources;

    public SQLSourceResolvedContext(PureModelContext pureModelContext, List<SQLSource> sources)
    {
        this.pureModelContext = pureModelContext;
        this.sources = sources;
    }

    public PureModelContext getPureModelContext()
    {
        return pureModelContext;
    }

    public List<SQLSource> getSources()
    {
        return sources;
    }
}
