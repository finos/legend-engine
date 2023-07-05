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

package org.finos.legend.engine.language.pure.dsl.persistence.platform;

import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_PersistenceContext;

public class PersistencePlatformActionRequest
{
    private final Root_meta_pure_persistence_metamodel_PersistenceContext persistenceContext;
    private final PureModelContext originalPmc;
    private final PureModelContextData pmcd;
    private final PureModel model;
    private final Identity userIdentity;
    private final Identity systemIdentity;

    public PersistencePlatformActionRequest(Root_meta_pure_persistence_metamodel_PersistenceContext persistenceContext, PureModelContextData pmcd, PureModel model, PureModelContext originalPmc, Identity userIdentity, Identity systemIdentity)
    {
        this.persistenceContext = persistenceContext;
        this.pmcd = pmcd;
        this.model = model;
        this.originalPmc = originalPmc;
        this.userIdentity = userIdentity;
        this.systemIdentity = systemIdentity;
    }

    public Root_meta_pure_persistence_metamodel_PersistenceContext getPersistenceContext()
    {
        return this.persistenceContext;
    }

    public PureModelContextData getPureModelContextData()
    {
        return this.pmcd;
    }

    public PureModel getPureModel()
    {
        return this.model;
    }

    public PureModelContext getOriginalPureModelContext()
    {
        return this.originalPmc;
    }

    public Identity getUserIdentity()
    {
        return this.userIdentity;
    }

    public Identity getSystemIdentity()
    {
        return this.systemIdentity;
    }
}
