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

package org.finos.legend.engine.testable.persistence.mapper;

import org.finos.legend.engine.persistence.components.ingestmode.merge.DeleteIndicatorMergeStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.merge.MergeStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchId;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchIdAndDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.TransactionDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.TransactionMilestoning;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.ValidDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.ValidityMilestoning;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.derivation.ValidityDerivation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.auditing.AuditingVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.auditing.DateTimeAuditing;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.auditing.NoAuditing;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.merge.MergeStrategyVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.merge.NoDeletesMergeStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning.BatchIdAndDateTimeTransactionMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning.BatchIdTransactionMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning.DateTimeTransactionMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning.TransactionMilestoningVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.DateTimeValidityMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.ValidityMilestoningVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.derivation.SourceSpecifiesFromAndThruDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.derivation.SourceSpecifiesFromDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.derivation.ValidityDerivationVisitor;

public class MappingVisitors
{
    public static final String BATCH_ID_IN_FIELD_DEFAULT = "BATCH_IN";
    public static final String BATCH_ID_OUT_FIELD_DEFAULT = "BATCH_OUT";
    public static final String BATCH_TIME_IN_FIELD_DEFAULT = "BATCH_TIME_IN";
    public static final String BATCH_TIME_OUT_FIELD_DEFAULT = "BATCH_TIME_OUT";

    public static final AuditingVisitor<org.finos.legend.engine.persistence.components.ingestmode.audit.Auditing> MAP_TO_COMPONENT_AUDITING = new AuditingVisitor<org.finos.legend.engine.persistence.components.ingestmode.audit.Auditing>()
    {
        @Override
        public org.finos.legend.engine.persistence.components.ingestmode.audit.Auditing visit(NoAuditing val)
        {
            return org.finos.legend.engine.persistence.components.ingestmode.audit.NoAuditing.builder().build();
        }

        @Override
        public org.finos.legend.engine.persistence.components.ingestmode.audit.Auditing visit(DateTimeAuditing val)
        {
            return org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditing.builder().dateTimeField(val.dateTimeName).build();
        }
    };

    public static final MergeStrategyVisitor<MergeStrategy> MAP_TO_COMPONENT_MERGE_STRATEGY = new MergeStrategyVisitor<MergeStrategy>()
    {

        @Override
        public MergeStrategy visit(NoDeletesMergeStrategy val)
        {
            return org.finos.legend.engine.persistence.components.ingestmode.merge.NoDeletesMergeStrategy.builder().build();
        }

        @Override
        public MergeStrategy visit(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.merge.DeleteIndicatorMergeStrategy val)
        {
            return DeleteIndicatorMergeStrategy.builder()
                    .deleteField(val.deleteField)
                    .addAllDeleteValues(val.deleteValues)
                    .build();
        }
    };

    public static final TransactionMilestoningVisitor<TransactionMilestoning> MAP_TO_COMPONENT_TRANSACTION_MILESTONING = new TransactionMilestoningVisitor<TransactionMilestoning>()
    {

        @Override
        public TransactionMilestoning visit(BatchIdTransactionMilestoning val)
        {
            return BatchId.of(
                    val.batchIdInName == null || val.batchIdInName.isEmpty() ? BATCH_ID_IN_FIELD_DEFAULT : val.batchIdInName,
                    val.batchIdOutName == null || val.batchIdOutName.isEmpty() ? BATCH_ID_OUT_FIELD_DEFAULT : val.batchIdOutName);
        }

        @Override
        public TransactionMilestoning visit(DateTimeTransactionMilestoning val)
        {
            return TransactionDateTime.of(
                    val.dateTimeInName == null || val.dateTimeInName.isEmpty() ? BATCH_TIME_IN_FIELD_DEFAULT : val.dateTimeInName,
                    val.dateTimeOutName == null || val.dateTimeOutName.isEmpty() ? BATCH_TIME_OUT_FIELD_DEFAULT : val.dateTimeOutName);
        }

        @Override
        public TransactionMilestoning visit(BatchIdAndDateTimeTransactionMilestoning val)
        {
            return BatchIdAndDateTime.of(
                    val.batchIdInName == null || val.batchIdInName.isEmpty() ? BATCH_ID_IN_FIELD_DEFAULT : val.batchIdInName,
                    val.batchIdOutName == null || val.batchIdOutName.isEmpty() ? BATCH_ID_OUT_FIELD_DEFAULT : val.batchIdOutName,
                    val.dateTimeInName == null || val.dateTimeInName.isEmpty() ? BATCH_TIME_IN_FIELD_DEFAULT : val.dateTimeInName,
                    val.dateTimeOutName == null || val.dateTimeOutName.isEmpty() ? BATCH_TIME_OUT_FIELD_DEFAULT : val.dateTimeOutName);
        }
    };

    public static final ValidityMilestoningVisitor<ValidityMilestoning> MAP_TO_COMPONENT_VALIDITY_MILESTONING = new ValidityMilestoningVisitor<ValidityMilestoning>()
    {
        @Override
        public ValidityMilestoning visit(DateTimeValidityMilestoning val)
        {
            return ValidDateTime.builder()
                    .dateTimeFromName(val.dateTimeFromName)
                    .dateTimeThruName(val.dateTimeThruName)
                    .validityDerivation(val.derivation.accept(MAP_TO_COMPONENT_VALIDITY_DERIVATION))
                    .build();
        }
    };

    public static final ValidityDerivationVisitor<ValidityDerivation> MAP_TO_COMPONENT_VALIDITY_DERIVATION = new ValidityDerivationVisitor<ValidityDerivation>()
    {
        @Override
        public ValidityDerivation visit(SourceSpecifiesFromDateTime val)
        {
            return org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.derivation.SourceSpecifiesFromDateTime.builder()
                    .sourceDateTimeFromField(val.sourceDateTimeFromField)
                    .build();
        }

        @Override
        public ValidityDerivation visit(SourceSpecifiesFromAndThruDateTime val)
        {
            return org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.derivation.SourceSpecifiesFromAndThruDateTime.builder()
                    .sourceDateTimeFromField(val.sourceDateTimeFromField)
                    .sourceDateTimeThruField(val.sourceDateTimeThruField)
                    .build();
        }
    };
}
