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

package org.finos.legend.engine.application.query.model;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.StereotypePtr;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.TaggedValue;

import java.util.List;

public class QuerySearchSpecification
{
    public String searchTerm;
    public List<QueryProjectCoordinates> projectCoordinates;
    public List<TaggedValue> taggedValues;
    public List<StereotypePtr> stereotypes;
    public Integer limit;
    public Boolean showCurrentUserQueriesOnly;
    // This boolean flag helps to perform And condition on filter we apply for tagged values
    // so that we can search if a query contains all the taggedValues specified in the
    // search specification
    public Boolean combineTaggedValuesCondition;
}
