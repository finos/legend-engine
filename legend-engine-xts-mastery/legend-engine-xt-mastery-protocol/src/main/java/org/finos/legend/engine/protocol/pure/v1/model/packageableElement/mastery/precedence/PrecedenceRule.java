/*******************************************************************************
 * // Copyright 2022 Goldman Sachs
 * //
 * // Licensed under the Apache License, Version 2.0 (the "License");
 * // you may not use this file except in compliance with the License.
 * // You may obtain a copy of the License at
 * //
 * //      http://www.apache.org/licenses/LICENSE-2.0
 * //
 * // Unless required by applicable law or agreed to in writing, software
 * // distributed under the License is distributed on an "AS IS" BASIS,
 * // WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * // See the License for the specific language governing permissions and
 * // limitations under the License.
 ******************************************************************************/

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.precedence;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;

import java.util.Collections;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SourcePrecedenceRule.class, name = "sourcePrecedenceRule"),
        @JsonSubTypes.Type(value = DeleteRule.class, name = "deleteRule"),
        @JsonSubTypes.Type(value = CreateRule.class, name = "createRule"),
        @JsonSubTypes.Type(value = ConditionalRule.class, name = "conditionalRule")
})
public abstract class PrecedenceRule
{

    public List<PropertyPath> paths = Collections.emptyList();
    public List<RuleScope> scopes = Collections.emptyList();
    public Lambda masterRecordFilter;
    public SourceInformation sourceInformation;

    public <T> T accept(PrecedenceRuleVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

    @JsonIgnore
    public abstract String getType();

}
