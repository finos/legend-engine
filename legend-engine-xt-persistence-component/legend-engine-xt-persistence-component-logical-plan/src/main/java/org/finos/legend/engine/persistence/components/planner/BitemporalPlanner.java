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

package org.finos.legend.engine.persistence.components.planner;

import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.ingestmode.BitemporalMilestoned;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.ValidDateTimeAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.ValidityMilestoned;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.ValidityMilestoningVisitor;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.derivation.SourceSpecifiesFromAndThruDateTimeAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.derivation.SourceSpecifiesFromDateTimeAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.derivation.ValidityDerivationVisitor;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.Value;
import org.finos.legend.engine.persistence.components.util.LogicalPlanUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

abstract class BitemporalPlanner extends UnitemporalPlanner
{
    BitemporalPlanner(Datasets datasets, BitemporalMilestoned bitemporalMilestoned, PlannerOptions plannerOptions)
    {
        super(datasets, bitemporalMilestoned, plannerOptions);

        // validate
        String targetValidDateTimeFrom = bitemporalMilestoned.validityMilestoning().accept(EXTRACT_TARGET_VALID_DATE_TIME_FROM);
        validatePrimaryKey(datasets.mainDataset().schema().fields(), targetValidDateTimeFrom);
    }

    @Override
    protected BitemporalMilestoned ingestMode()
    {
        return (BitemporalMilestoned) super.ingestMode();
    }

    protected List<Value> fieldsToSelect()
    {
        List<Value> fieldsToSelect = new ArrayList<>(stagingDataset().schemaReference().fieldValues());
        fieldsToSelect.addAll(transactionMilestoningFieldValues());

        String sourceValidDateTimeFrom = ingestMode().validityMilestoning().validityDerivation().accept(EXTRACT_SOURCE_VALID_DATE_TIME_FROM);
        Value validFromSourceValue = FieldValue.builder().datasetRef(stagingDataset().datasetReference()).fieldName(sourceValidDateTimeFrom).build();
        fieldsToSelect.add(validFromSourceValue);

        Value validThruSourceValue = ingestMode().validityMilestoning().validityDerivation().accept(new DetermineValidDateTimeThruValue(ingestMode(), stagingDataset()));
        fieldsToSelect.add(validThruSourceValue);
        return fieldsToSelect;
    }

    protected List<Value> fieldsToInsert()
    {
        List<Value> fieldsToInsert = new ArrayList<>(stagingDataset().schemaReference().fieldValues());
        fieldsToInsert.addAll(transactionMilestoningFields());

        String targetValidDateTimeFrom = ingestMode().validityMilestoning().accept(EXTRACT_TARGET_VALID_DATE_TIME_FROM);
        fieldsToInsert.add(FieldValue.builder().datasetRef(mainDataset().datasetReference()).fieldName(targetValidDateTimeFrom).build());

        String targetValidDateTimeThru = ingestMode().validityMilestoning().accept(EXTRACT_TARGET_VALID_DATE_TIME_THRU);
        fieldsToInsert.add(FieldValue.builder().datasetRef(mainDataset().datasetReference()).fieldName(targetValidDateTimeThru).build());

        return fieldsToInsert;
    }

    // validity milestoning visitors

    protected static final ExtractTargetValidDateTimeFrom EXTRACT_TARGET_VALID_DATE_TIME_FROM = new ExtractTargetValidDateTimeFrom();

    static class ExtractTargetValidDateTimeFrom implements ValidityMilestoningVisitor<String>
    {
        private ExtractTargetValidDateTimeFrom()
        {
        }

        @Override
        public String visitDateTime(ValidDateTimeAbstract validDateTime)
        {
            return validDateTime.dateTimeFromName();
        }
    }

    protected static final ExtractTargetValidDateTimeThru EXTRACT_TARGET_VALID_DATE_TIME_THRU = new ExtractTargetValidDateTimeThru();

    static class ExtractTargetValidDateTimeThru implements ValidityMilestoningVisitor<String>
    {
        private ExtractTargetValidDateTimeThru()
        {
        }

        @Override
        public String visitDateTime(ValidDateTimeAbstract validDateTime)
        {
            return validDateTime.dateTimeThruName();
        }
    }

    // validity derivation visitors

    protected static final ExtractSourceValidDateTimeFrom EXTRACT_SOURCE_VALID_DATE_TIME_FROM = new ExtractSourceValidDateTimeFrom();

    static class ExtractSourceValidDateTimeFrom implements ValidityDerivationVisitor<String>
    {
        private ExtractSourceValidDateTimeFrom()
        {
        }

        @Override
        public String visitSourceSpecifiesFromDateTime(SourceSpecifiesFromDateTimeAbstract sourceSpecifiesFromDateTime)
        {
            return sourceSpecifiesFromDateTime.sourceDateTimeFromField();
        }

        @Override
        public String visitSourceSpecifiesFromAndThruDateTime(SourceSpecifiesFromAndThruDateTimeAbstract sourceSpecifiesFromAndThruDateTime)
        {
            return sourceSpecifiesFromAndThruDateTime.sourceDateTimeFromField();
        }
    }

    protected static final ExtractSourceValidDateTimeThru EXTRACT_SOURCE_VALID_DATE_TIME_THRU = new ExtractSourceValidDateTimeThru();

    static class ExtractSourceValidDateTimeThru implements ValidityDerivationVisitor<Optional<String>>
    {
        private ExtractSourceValidDateTimeThru()
        {
        }

        @Override
        public Optional<String> visitSourceSpecifiesFromDateTime(SourceSpecifiesFromDateTimeAbstract sourceSpecifiesFromDateTime)
        {
            return Optional.empty();
        }

        @Override
        public Optional<String> visitSourceSpecifiesFromAndThruDateTime(SourceSpecifiesFromAndThruDateTimeAbstract sourceSpecifiesFromAndThruDateTime)
        {
            return Optional.of(sourceSpecifiesFromAndThruDateTime.sourceDateTimeThruField());
        }
    }

    // TODO: should we really make a decision like this? This is different from the rest because the structure of SQLs change entirely as opposed to only requiring a different value
    static class DetermineValidDateTimeThruValue implements ValidityDerivationVisitor<Value>
    {
        private final ValidityMilestoned validityMilestoned;
        private final Dataset stagingDataset;

        private DetermineValidDateTimeThruValue(ValidityMilestoned validityMilestoned, Dataset stagingDataset)
        {
            this.validityMilestoned = validityMilestoned;
            this.stagingDataset = stagingDataset;
        }

        @Override
        public Value visitSourceSpecifiesFromDateTime(SourceSpecifiesFromDateTimeAbstract sourceSpecifiesFromDateTime)
        {
            return LogicalPlanUtils.INFINITE_BATCH_TIME();
        }

        @Override
        public Value visitSourceSpecifiesFromAndThruDateTime(SourceSpecifiesFromAndThruDateTimeAbstract sourceSpecifiesFromAndThruDateTime)
        {
            Optional<String> sourceValidDateTimeThruOptional = validityMilestoned.validityMilestoning().validityDerivation().accept(EXTRACT_SOURCE_VALID_DATE_TIME_THRU);
            String sourceValidDateTimeThru = sourceValidDateTimeThruOptional.orElseThrow(IllegalStateException::new);
            return FieldValue.builder().datasetRef(stagingDataset.datasetReference()).fieldName(sourceValidDateTimeThru).build();
        }
    }
}
