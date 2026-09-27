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

package org.finos.legend.engine.pure.preeval;

import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public final class PrevalResults
{
    private static final String PREVAL_RESULT = "meta::pure::functions::preeval::PrevalResult";

    private PrevalResults()
    {
    }

    public static CoreInstance toPure(PrevalResult result, ProcessorSupport processorSupport)
    {
        CoreInstance pureResult = processorSupport.newCoreInstance(null, PREVAL_RESULT, null);
        Instance.setValueForProperty(pureResult, "value", result.getValue(), processorSupport);
        Instance.setValueForProperty(pureResult, "canPreval", newBoolean(result.canPreval(), processorSupport), processorSupport);
        Instance.setValuesForProperty(pureResult, "openVars", result.getOpenVars().collect(name -> processorSupport.newCoreInstance(name, M3Paths.String, null)), processorSupport);
        Instance.setValueForProperty(pureResult, "modified", newBoolean(result.isModified(), processorSupport), processorSupport);
        return pureResult;
    }

    private static CoreInstance newBoolean(boolean value, ProcessorSupport processorSupport)
    {
        return processorSupport.newCoreInstance(Boolean.toString(value), M3Paths.Boolean, null);
    }
}
