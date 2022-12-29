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

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.auditing.AuditingVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.auditing.DateTimeAuditing;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.auditing.NoAuditing;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.IngestModeVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.appendonly.AppendOnly;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.BitemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.NontemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.UnitemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.snapshot.BitemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.snapshot.NontemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.snapshot.UnitemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning.BatchIdAndDateTimeTransactionMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning.BatchIdTransactionMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning.DateTimeTransactionMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning.TransactionMilestoningVisitor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.finos.legend.engine.testable.persistence.mapper.IngestModeMapper.DIGEST_FIELD_DEFAULT;

public class IngestModeVisitors
{
    public static final IngestModeVisitor<Set<String>> EXTRACT_FIELDS_TO_EXCLUDE = new IngestModeVisitor<Set<String>>()
    {
        @Override
        public Set<String> visit(AppendOnly appendOnly)
        {
            Set<String> fieldsToIgnore = new HashSet<>();
            Optional<String> auditField = appendOnly.auditing.accept(EXTRACT_AUDIT_FIELD);
            if (auditField.isPresent())
            {
                fieldsToIgnore.add(auditField.get());
            }
            if (appendOnly.filterDuplicates)
            {
                fieldsToIgnore.add(DIGEST_FIELD_DEFAULT);
            }
            return fieldsToIgnore;
        }

        @Override
        public Set<String> visit(BitemporalDelta bitemporalDelta)
        {
            Set<String> fieldsToIgnore = bitemporalDelta.transactionMilestoning.accept(EXTRACT_TX_DATE_TIME_FIELDS);
            fieldsToIgnore.add(DIGEST_FIELD_DEFAULT);
            return fieldsToIgnore;
        }

        @Override
        public Set<String> visit(BitemporalSnapshot bitemporalSnapshot)
        {
            Set<String> fieldsToIgnore = bitemporalSnapshot.transactionMilestoning.accept(EXTRACT_TX_DATE_TIME_FIELDS);
            fieldsToIgnore.add(DIGEST_FIELD_DEFAULT);
            return fieldsToIgnore;
        }

        @Override
        public Set<String> visit(NontemporalDelta nontemporalDelta)
        {
            Set<String> fieldsToIgnore = new HashSet<>();
            Optional<String> auditField = nontemporalDelta.auditing.accept(EXTRACT_AUDIT_FIELD);
            if (auditField.isPresent())
            {
                fieldsToIgnore.add(auditField.get());
            }
            fieldsToIgnore.add(DIGEST_FIELD_DEFAULT);
            return fieldsToIgnore;
        }

        @Override
        public Set<String> visit(NontemporalSnapshot nontemporalSnapshot)
        {
            Set<String> fieldsToIgnore = new HashSet<>();
            Optional<String> auditField = nontemporalSnapshot.auditing.accept(EXTRACT_AUDIT_FIELD);
            if (auditField.isPresent())
            {
                fieldsToIgnore.add(auditField.get());
            }
            return fieldsToIgnore;
        }

        @Override
        public Set<String> visit(UnitemporalDelta unitemporalDelta)
        {
            Set<String> fieldsToIgnore = unitemporalDelta.transactionMilestoning.accept(EXTRACT_TX_DATE_TIME_FIELDS);
            fieldsToIgnore.add(DIGEST_FIELD_DEFAULT);
            return fieldsToIgnore;
        }

        @Override
        public Set<String> visit(UnitemporalSnapshot unitemporalSnapshot)
        {
            Set<String> fieldsToIgnore = unitemporalSnapshot.transactionMilestoning.accept(EXTRACT_TX_DATE_TIME_FIELDS);
            fieldsToIgnore.add(DIGEST_FIELD_DEFAULT);
            return fieldsToIgnore;
        }
    };

    public static final IngestModeVisitor<Map<String, Object>> EXTRACT_ACTIVE_ROWS_FILTER_CONDITIONS = new IngestModeVisitor<Map<String, Object>>()
    {
        @Override
        public Map<String, Object> visit(AppendOnly appendOnly)
        {
            return new HashMap<>();
        }

        @Override
        public Map<String, Object> visit(BitemporalDelta bitemporalDelta)
        {
            return bitemporalDelta.transactionMilestoning.accept(EXTRACT_TX_ACTIVE_ROWS_FILTER_CONDITIONS);
        }

        @Override
        public Map<String, Object> visit(BitemporalSnapshot bitemporalSnapshot)
        {
            return bitemporalSnapshot.transactionMilestoning.accept(EXTRACT_TX_ACTIVE_ROWS_FILTER_CONDITIONS);
        }

        @Override
        public Map<String, Object> visit(NontemporalDelta nontemporalDelta)
        {
            return new HashMap<>();
        }

        @Override
        public Map<String, Object> visit(NontemporalSnapshot nontemporalSnapshot)
        {
            return new HashMap<>();
        }

        @Override
        public Map<String, Object> visit(UnitemporalDelta unitemporalDelta)
        {
            return unitemporalDelta.transactionMilestoning.accept(EXTRACT_TX_ACTIVE_ROWS_FILTER_CONDITIONS);
        }

        @Override
        public Map<String, Object> visit(UnitemporalSnapshot unitemporalSnapshot)
        {
            return unitemporalSnapshot.transactionMilestoning.accept(EXTRACT_TX_ACTIVE_ROWS_FILTER_CONDITIONS);
        }
    };


    public static final IngestModeVisitor<Boolean> EXTRACT_TRANSACTION_MILESTONING_TIME_BASED = new IngestModeVisitor<Boolean>()
    {
        @Override
        public Boolean visit(AppendOnly val)
        {
            return false;
        }

        @Override
        public Boolean visit(BitemporalDelta val)
        {
            return val.transactionMilestoning.accept(TRANSACTION_MILESTONING_TIME_BASED);
        }

        @Override
        public Boolean visit(BitemporalSnapshot val)
        {
            return val.transactionMilestoning.accept(TRANSACTION_MILESTONING_TIME_BASED);
        }

        @Override
        public Boolean visit(NontemporalDelta val)
        {
            return false;
        }

        @Override
        public Boolean visit(NontemporalSnapshot val)
        {
            return false;
        }

        @Override
        public Boolean visit(UnitemporalDelta val)
        {
            return val.transactionMilestoning.accept(TRANSACTION_MILESTONING_TIME_BASED);
        }

        @Override
        public Boolean visit(UnitemporalSnapshot val)
        {
            return val.transactionMilestoning.accept(TRANSACTION_MILESTONING_TIME_BASED);
        }
    };

    public static final AuditingVisitor<Optional<String>> EXTRACT_AUDIT_FIELD = new AuditingVisitor<Optional<String>>()
    {
        @Override
        public Optional<String> visit(NoAuditing val)
        {
            return Optional.empty();
        }

        @Override
        public Optional<String> visit(DateTimeAuditing val)
        {
            return Optional.of(val.dateTimeName);
        }
    };

    public static final TransactionMilestoningVisitor<Set<String>> EXTRACT_TX_DATE_TIME_FIELDS = new TransactionMilestoningVisitor<Set<String>>()
    {
        @Override
        public Set<String> visit(BatchIdTransactionMilestoning val)
        {
            return new HashSet();
        }

        @Override
        public Set<String> visit(DateTimeTransactionMilestoning val)
        {
            return new HashSet(Arrays.asList(val.dateTimeInName, val.dateTimeOutName));
        }

        @Override
        public Set<String> visit(BatchIdAndDateTimeTransactionMilestoning val)
        {
            return new HashSet(Arrays.asList(val.dateTimeInName, val.dateTimeOutName));
        }
    };

    public static final TransactionMilestoningVisitor<Boolean> TRANSACTION_MILESTONING_TIME_BASED = new TransactionMilestoningVisitor<Boolean>()
    {

        @Override
        public Boolean visit(BatchIdTransactionMilestoning val)
        {
            return false;
        }

        @Override
        public Boolean visit(DateTimeTransactionMilestoning val)
        {
            return true;
        }

        @Override
        public Boolean visit(BatchIdAndDateTimeTransactionMilestoning val)
        {
            return false;
        }
    };

    public static final TransactionMilestoningVisitor<Map<String, Object>> EXTRACT_TX_ACTIVE_ROWS_FILTER_CONDITIONS = new TransactionMilestoningVisitor<Map<String, Object>>()
    {
        @Override
        public Map<String, Object> visit(BatchIdTransactionMilestoning val)
        {
            HashMap<String, Object> activeRowsFilterConditions = new HashMap<>();
            activeRowsFilterConditions.put(val.batchIdOutName, IngestModeMapper.INFINITE_BATCH_ID);
            return activeRowsFilterConditions;
        }

        @Override
        public Map<String, Object> visit(DateTimeTransactionMilestoning val)
        {
            HashMap<String, Object> activeRowsFilterConditions = new HashMap<>();
            activeRowsFilterConditions.put(val.dateTimeOutName, IngestModeMapper.INFINITE_BATCH_TIME);
            return activeRowsFilterConditions;
        }

        @Override
        public Map<String, Object> visit(BatchIdAndDateTimeTransactionMilestoning val)
        {
            HashMap<String, Object> activeRowsFilterConditions = new HashMap<>();
            activeRowsFilterConditions.put(val.batchIdOutName, IngestModeMapper.INFINITE_BATCH_ID);
            activeRowsFilterConditions.put(val.dateTimeOutName, IngestModeMapper.INFINITE_BATCH_TIME);
            return activeRowsFilterConditions;
        }
    };
}
