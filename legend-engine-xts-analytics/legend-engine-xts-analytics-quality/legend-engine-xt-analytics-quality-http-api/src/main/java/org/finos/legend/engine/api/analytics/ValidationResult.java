//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.api.analytics;

import java.util.List;

public class ValidationResult
{
    private List<ValidationRuleResult> validationRuleResults;
    private int healthScore;

    public ValidationResult(List<ValidationRuleResult> validationRuleResults, int healthScore)
    {
        this.validationRuleResults = validationRuleResults;
        this.healthScore = healthScore;
    }

    public static int calculateHealthScore(int violations, int totalElements)
    {
        //TODO - weighted values to different constraints
        int diff = totalElements - violations;
        return (int) (Math.round((diff * 100.0 / totalElements)) / 10);
    }

    public int getHealthScore()
    {
        return this.healthScore;
    }

    public List<ValidationRuleResult> getValidationRuleResultsList()
    {
        return this.validationRuleResults;
    }
}
