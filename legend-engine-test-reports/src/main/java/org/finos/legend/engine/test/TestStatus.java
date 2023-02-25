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

package org.finos.legend.engine.test;

public enum TestStatus
{
    SUCCESS("Success", "Test passed", ":green_circle:"),
    ERROR("Error", "Test failed", ":red_circle:"),
    UNSUPPORTED_IN_LEGEND("Unsupported", "Feature has not been implemented in Legend. (The feature very well might be supported in the target database)", ":black_circle:"),
    DEVIATION("Deviation from standard", "Deviation in behavior. (TODO : The semantics of this status are not clear and needs to be refined)", ":diamond_shape_with_a_dot_inside:"),
    MISSING("Missing", "Test result not available. Most likely because of a systemic failure (e.g Github workflow for the database failed) or omission (e.g We have not implemented any tests for a database).", ":purple_circle:"),
    REPORT_GENERATION_ERROR("Report Generation Error", "An error/bug in the generation of this report", ":confused:");

    public final String friendlyName;
    public final String description;
    public final String emoji;

    TestStatus(String friendlyName, String description, String emoji)
    {
        this.friendlyName = friendlyName;
        this.description = description;
        this.emoji = emoji;
    }
}
