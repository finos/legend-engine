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

package org.finos.legend.engine.test.emit.error;

import org.finos.legend.engine.test.emit.EMITPhase;

class EMITErrorTools
{
    static String formatMessage(String description, EMITPhase phase, String message)
    {
        StringBuilder builder = new StringBuilder(description);
        if (phase != null)
        {
            builder.append(" [").append(getPhaseDescription(phase)).append(']');
        }
        if (message != null)
        {
            builder.append(": ").append(message);
        }
        return builder.toString();
    }

    private static String getPhaseDescription(EMITPhase phase)
    {
        switch (phase)
        {
            case INITIALIZATION:
            {
                return "Initialization";
            }
            case PARSE:
            {
                return "Parsing";
            }
            case COMPILE:
            {
                return "Compilation";
            }
            case MODEL_GENERATION:
            {
                return "Model Generation";
            }
            case FILE_GENERATION:
            {
                return "File Generation";
            }
            case TEST_EXECUTION:
            {
                return "Test Execution";
            }
            case PLAN_GENERATION:
            {
                return "Plan Generation";
            }
            default:
            {
                return phase.name().replace('_', ' ');
            }
        }
    }
}
