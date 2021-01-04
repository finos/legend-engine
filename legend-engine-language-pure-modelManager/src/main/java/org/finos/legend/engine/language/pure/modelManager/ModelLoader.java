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

package org.finos.legend.engine.language.pure.modelManager;

import io.opentracing.Span;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;

public interface ModelLoader
{
    boolean supports(PureModelContext context);

    PureModelContextData load(MutableList<CommonProfile> profiles, PureModelContext context, String clientVersion, Span parentSpan);

    void setModelManager(ModelManager modelManager);

    // Caching
    boolean shouldCache(PureModelContext context);

    PureModelContext cacheKey(PureModelContext context, MutableList<CommonProfile> pm);
}
