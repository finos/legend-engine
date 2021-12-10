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

package org.finos.legend.engine.plan.dependencies.domain.dataQuality;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BasicDefect implements IDefect
{
    private String id = null;
    private String externalId = null;
    private String message = null;
    private EnforcementLevel enforcementLevel = null;
    private String ruleDefinerPath = null;
    private RuleType ruleType = null;
    private List<RelativePathNode> path = new ArrayList<>();

    private BasicDefect()
    {
        // For privacy
    }

    public String getId()
    {
        return this.id;
    }

    public String getExternalId()
    {
        return this.externalId;
    }

    public String getMessage()
    {
        return this.message;
    }

    public EnforcementLevel getEnforcementLevel()
    {
        return this.enforcementLevel;
    }

    public String getRuleDefinerPath()
    {
        return this.ruleDefinerPath;
    }

    public RuleType getRuleType()
    {
        return this.ruleType;
    }

    public List<RelativePathNode> getPath()
    {
        return this.path;
    }

    @Override
    public String toString()
    {
        return "BasicDefect{" +
                "id='" + id + '\'' +
                ", externalId='" + externalId + '\'' +
                ", message='" + message + '\'' +
                ", enforcementLevel=" + enforcementLevel +
                ", ruleDefinerPath='" + ruleDefinerPath + '\'' +
                ", ruleType=" + ruleType +
                ", path=" + path +
                '}';
    }

    public static IDefect newDefect(String id, String externalId, String message, EnforcementLevel enforcementLevel, String ruleDefinerPath, RuleType ruleType, List<RelativePathNode> path)
    {
        BasicDefect result = new BasicDefect();
        result.id = id;
        result.externalId = externalId;
        result.message = message;
        result.enforcementLevel = enforcementLevel;
        result.ruleDefinerPath = ruleDefinerPath;
        result.ruleType = ruleType;
        result.path = path;
        return result;
    }

    public static IDefect newConstraintDefect(String id, String externalId, String message, EnforcementLevel enforcementLevel, String ruleDefinerPath)
    {
        return BasicDefect.newDefect(id, externalId, message, enforcementLevel, ruleDefinerPath, RuleType.ClassConstraint, Collections.emptyList());
    }

    public static IDefect newStoreDefinitionDefect(String message, String ruleDefinerPath)
    {
        return BasicDefect.newDefect(null, null, message, EnforcementLevel.Critical, ruleDefinerPath, RuleType.StoreDefinition, Collections.emptyList());
    }

    public static IDefect newMappingDefinitionDefect(String message, String ruleDefinerPath)
    {
        return BasicDefect.newDefect(null, null, message, EnforcementLevel.Error, ruleDefinerPath, RuleType.MappingDefinition, Collections.emptyList());
    }

    public static IDefect newClassStructureDefect(String message, String ruleDefinerPath)
    {
        return BasicDefect.newDefect(null, null, message, EnforcementLevel.Critical, ruleDefinerPath, RuleType.ClassStructure, Collections.emptyList());
    }

    public static IDefect newInvalidInputWarningDefect(String message, String ruleDefinerPath)
    {
        return BasicDefect.newDefect(null, null, message, EnforcementLevel.Warn, ruleDefinerPath, RuleType.InvalidInput, Collections.emptyList());
    }

    public static IDefect newInvalidInputErrorDefect(String message, String ruleDefinerPath)
    {
        return BasicDefect.newDefect(null, null, message, EnforcementLevel.Error, ruleDefinerPath, RuleType.InvalidInput, Collections.emptyList());
    }

    public static IDefect newInvalidInputCriticalDefect(String message, String ruleDefinerPath)
    {
        return BasicDefect.newDefect(null, null, message, EnforcementLevel.Critical, ruleDefinerPath, RuleType.InvalidInput, Collections.emptyList());
    }

    public static IDefect newNoInputDefect(String ruleDefinerPath)
    {
        return BasicDefect.newDefect(null, null, "No Input Available", EnforcementLevel.Critical, ruleDefinerPath, RuleType.NoInput, Collections.emptyList());
    }

    public static IDefect newNonReturnableDefect(String ruleDefinerPath)
    {
        return BasicDefect.newDefect(null, null, "Input not directly returned to stream", EnforcementLevel.Warn, ruleDefinerPath, RuleType.UnreturnedInput, Collections.emptyList());
    }

    public static IDefect prefixPath(IDefect defect, RelativePathNode path)
    {
        return BasicDefect.prefixPath(defect, Collections.singletonList(path));
    }

    public static IDefect prefixPath(IDefect defect, List<RelativePathNode> path)
    {
        List<RelativePathNode> newPath = new ArrayList<>(path);
        newPath.addAll(defect.getPath());
        BasicDefect result = new BasicDefect();
        result.id = defect.getId();
        result.externalId = defect.getExternalId();
        result.message = defect.getMessage();
        result.enforcementLevel = defect.getEnforcementLevel();
        result.ruleDefinerPath = defect.getRuleDefinerPath();
        result.ruleType = defect.getRuleType();
        result.path = newPath;
        return result;
    }
}
