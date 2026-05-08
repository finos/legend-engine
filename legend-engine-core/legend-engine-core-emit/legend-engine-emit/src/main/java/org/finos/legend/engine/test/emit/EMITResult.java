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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;

import java.util.Formatter;
import java.util.List;

public class EMITResult
{
    private final MutableList<EMITPhaseResult> phaseResults = Lists.mutable.empty();
    private final MutableMap<EMITPhase, EMITPhaseResult> byPhase = Maps.mutable.empty();

    public List<EMITPhaseResult> getPhaseResults()
    {
        return this.phaseResults.asUnmodifiable();
    }

    public EMITPhaseResult getPhase(EMITPhase phase)
    {
        return this.byPhase.get(phase);
    }

    public void add(EMITPhaseResult result)
    {
        this.phaseResults.add(result);
        this.byPhase.put(result.getPhase(), result);
    }

    public boolean isSuccess()
    {
        return this.phaseResults.noneSatisfy(EMITPhaseResult::isFailure);
    }

    public String getSummary()
    {
        StringBuilder builder = new StringBuilder("EMIT Result: ").append(isSuccess() ? "PASSED" : "FAILED").append('\n');
        Formatter formatter = new Formatter(builder);
        for (EMITPhase phase : EMITPhase.values())
        {
            EMITPhaseResult r = this.byPhase.get(phase);
            if (r != null)
            {
                formatter.format("%s%-18s (%dms)", getStatusMarker(r), r.getPhase().name(), r.getDurationMs());
                if (r.getMessage() != null && !r.getMessage().isEmpty())
                {
                    builder.append(" - ").append(r.getMessage());
                }
                builder.append('\n');
                if (r.getThrowable() != null)
                {
                    builder.append("        ").append(r.getThrowable().getClass().getSimpleName())
                            .append(": ").append(r.getThrowable().getMessage()).append('\n');
                }
            }
        }
        return builder.toString();
    }

    private String getStatusMarker(EMITPhaseResult result)
    {
        switch (result.getStatus())
        {
            case SUCCESS:
            {
                return "  OK   ";
            }
            case FAILURE:
            {
                return "  FAIL ";
            }
            case SKIPPED:
            {
                return "  SKIP ";
            }
            case NOT_RUN:
            {
                return "  --   ";
            }
            default:
            {
                return "  ?    ";
            }
        }
    }
}
