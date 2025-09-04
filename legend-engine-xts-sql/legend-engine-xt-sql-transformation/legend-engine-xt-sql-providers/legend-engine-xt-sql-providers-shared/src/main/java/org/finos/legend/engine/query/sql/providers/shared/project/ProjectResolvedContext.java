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

package org.finos.legend.engine.query.sql.providers.shared.project;

import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;

/**
 * This class acts as a holder for a ProjectCoordinateWrapper resolved data
 */
public class ProjectResolvedContext
{
    /** this will be the smallest unit possible, eg. a pointer instead of the full pmcd if available*/
    private final PureModelContext context;
    private final PureModelContextData data;

    public ProjectResolvedContext(PureModelContext context, PureModelContextData data)
    {
        this.context = context;
        this.data = data;
    }

    public PureModelContext getContext()
    {
        return context;
    }

    public PureModelContextData getData()
    {
        return data;
    }
}
