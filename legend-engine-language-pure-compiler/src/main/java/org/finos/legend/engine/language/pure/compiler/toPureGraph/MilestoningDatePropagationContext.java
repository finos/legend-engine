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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

public class MilestoningDatePropagationContext {
    private org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification businessDate;
    private org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification processingDate;
    private org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification topLevelProcessedParameter;
    private org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification lastLevelProcessedParameter;

    org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification getBusinessDate()
    {
        return this.businessDate;
    }

    org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification getProcessingDate()
    {
        return this.processingDate;
    }

    org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification getTopLevelParameter()
    {
        return this.topLevelProcessedParameter;
    }

    org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification getLastLevelParameter()
    {
        return this.lastLevelProcessedParameter;
    }

    void setBusinessDate(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification businessDate)
    {
        this.businessDate = businessDate;
    }

    void setProcessingDate(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification processingDate)
    {
        this.processingDate = processingDate;
    }

    void setTopLevelParameter(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification topLevelParameter)
    {
        this.topLevelProcessedParameter = topLevelParameter;
    }

    void setLastLevelParameter(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification lastLevelParameter)
    {
        this.lastLevelProcessedParameter = lastLevelParameter;
    }
}
