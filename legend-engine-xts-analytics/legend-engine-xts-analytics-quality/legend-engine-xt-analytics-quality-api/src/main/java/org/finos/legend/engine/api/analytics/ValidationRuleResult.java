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

import java.util.HashMap;
import java.util.Map;

public class ValidationRuleResult
{
    private Map<String, String> resultMap = new HashMap<>();

    public ValidationRuleResult(boolean ruleResult, String packageableElementName, String violationType, String errorMessage, String ruleDescription)
    {
        this.resultMap.put("isElementValid", String.valueOf(ruleResult));
        this.resultMap.put("packageableElementName", packageableElementName);
        this.resultMap.put("violationType", violationType);
        this.resultMap.put("errorMessage", errorMessage);
        this.resultMap.put("ruleDescription", ruleDescription);
    }

    public Map<String, String> getResultMap()
    {
        return this.resultMap;
    }
}
