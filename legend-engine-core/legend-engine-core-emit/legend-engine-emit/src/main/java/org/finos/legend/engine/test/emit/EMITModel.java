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

package org.finos.legend.engine.test.emit;

import org.eclipse.collections.api.set.SetIterable;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;

/**
 * The compiled-and-loaded state of one EMIT model: the original source set, the
 * combined {@link PureModelContextData} (primary + dependency files), the
 * compiled {@link PureModel}, and the set of source IDs that came from the
 * primary {@code model:} section of the {@code *.emit.yaml} (used to scope
 * downstream phases away from dependency-loaded elements).
 */
public final class EMITModel
{
    private final EMITSourceSet sourceSet;
    private final PureModelContextData pmcd;
    private final PureModel pureModel;
    private final SetIterable<String> primarySourceIds;

    public EMITModel(EMITSourceSet sourceSet, PureModelContextData pmcd, PureModel pureModel, SetIterable<String> primarySourceIds)
    {
        this.sourceSet = sourceSet;
        this.pmcd = pmcd;
        this.pureModel = pureModel;
        this.primarySourceIds = primarySourceIds;
    }

    public EMITSourceSet getSourceSet()
    {
        return this.sourceSet;
    }

    public PureModelContextData getPmcd()
    {
        return this.pmcd;
    }

    public PureModel getPureModel()
    {
        return this.pureModel;
    }

    public SetIterable<String> getPrimarySourceIds()
    {
        return this.primarySourceIds;
    }

    public boolean isPrimary(PackageableElement element)
    {
        return (element.sourceInformation == null) ||
                (element.sourceInformation.sourceId == null) ||
                this.primarySourceIds.contains(element.sourceInformation.sourceId);
    }

    /**
     * Return a new {@code EMITModel} sharing the source set and primary scope,
     * but pointing at a different (post-model-generation) PMCD and PureModel.
     */
    public EMITModel withModel(PureModelContextData newPmcd, PureModel newPureModel)
    {
        return new EMITModel(this.sourceSet, newPmcd, newPureModel, this.primarySourceIds);
    }
}