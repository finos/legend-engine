// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = BatchIdTransactionMilestoning.class, name = "batchIdTransactionMilestoning"),
        @JsonSubTypes.Type(value = BatchIdAndDateTimeTransactionMilestoning.class, name = "batchIdAndDateTimeTransactionMilestoning"),
        @JsonSubTypes.Type(value = DateTimeTransactionMilestoning.class, name = "dateTimeTransactionMilestoning"),
})
public abstract class TransactionMilestoning
{
    public SourceInformation sourceInformation;

    public abstract <T> T accept(TransactionMilestoningVisitor<T> visitor);
}