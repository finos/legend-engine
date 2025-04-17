// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.language.dataquality.api;

import org.finos.legend.engine.shared.core.operational.logs.ILoggingEventType;

public enum DataQualityExecutionLoggingEventType implements ILoggingEventType
{
    DATAQUALITY_PLAN_EXECUTION_START,
    DATAQUALITY_PLAN_EXECUTION_END,
    DATAQUALITY_PLAN_EXECUTION_TRIAL_ERROR,
    DATAQUALITY_ARTIFACT_PLAN_EXECUTION_START,
    DATAQUALITY_ARTIFACT_PLAN_EXECUTION_END,
    DATAQUALITY_PLAN_EXECUTION_ERROR,
    DATAQUALITY_GENERATE_PLAN_START,
    DATAQUALITY_GENERATE_PLAN_END,
    DATAQUALITY_EXECUTE_INTERACTIVE_STOP,
    DATAQUALITY_EXECUTE_INTERACTIVE_ERROR,
    DATAQUALITY_PROPERTY_PATH_TREE_START,
    DATAQUALITY_PROPERTY_PATH_TREE_END,
    DATAQUALITY_GENERATE_PLAN_QUERY_COUNT_START,
    DATAQUALITY_GENERATE_PLAN_QUERY_COUNT_END,
    DATAQUALITY_RELATION_QUERY_COUNT_PLAN_EXECUTION_START,
    DATAQUALITY_RELATION_QUERY_COUNT_PLAN_EXECUTION_END
}
