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

package org.finos.legend.engine.persistence.components.ingestmode.merge;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.finos.legend.engine.persistence.components.ingestmode.merge.MergeStrategyDeleteMode.DELETE_MISTAKE;
import static org.finos.legend.engine.persistence.components.ingestmode.merge.MergeStrategyDeleteMode.TERMINATE_LATEST_ACTIVE;

public class MergeStrategyVisitors
{
    private MergeStrategyVisitors()
    {
    }

    public static final MergeStrategyVisitor<Optional<String>> EXTRACT_INDICATOR_FIELD = new MergeStrategyVisitor<Optional<String>>()
    {
        @Override
        public Optional<String> visitNoDeletesMergeStrategy(NoDeletesMergeStrategyAbstract noDeletesMergeStrategy)
        {
            return Optional.empty();
        }

        @Override
        public Optional<String> visitDeleteIndicatorMergeStrategy(DeleteIndicatorMergeStrategyAbstract deleteIndicatorMergeStrategy)
        {
            return Optional.of(deleteIndicatorMergeStrategy.deleteField());
        }

        @Override
        public Optional<String> visitTerminateLatestActiveMergeStrategy(TerminateLatestActiveMergeStrategyAbstract terminateLatestActiveMergeStrategy)
        {
            return Optional.of(terminateLatestActiveMergeStrategy.terminateField());
        }
    };

    public static final MergeStrategyVisitor<List<Object>> EXTRACT_INDICATOR_VALUES = new MergeStrategyVisitor<List<Object>>()
    {
        @Override
        public List<Object> visitNoDeletesMergeStrategy(NoDeletesMergeStrategyAbstract noDeletesMergeStrategy)
        {
            return Collections.emptyList();
        }

        @Override
        public List<Object> visitDeleteIndicatorMergeStrategy(DeleteIndicatorMergeStrategyAbstract deleteIndicatorMergeStrategy)
        {
            return deleteIndicatorMergeStrategy.deleteValues();
        }

        @Override
        public List<Object> visitTerminateLatestActiveMergeStrategy(TerminateLatestActiveMergeStrategyAbstract terminateLatestActiveMergeStrategy)
        {
            return terminateLatestActiveMergeStrategy.terminateValues();
        }
    };

    public static final MergeStrategyVisitor<Optional<MergeStrategyDeleteMode>> DETERMINE_DELETE_MODE = new MergeStrategyVisitor<Optional<MergeStrategyDeleteMode>>()
    {
        @Override
        public Optional<MergeStrategyDeleteMode> visitNoDeletesMergeStrategy(NoDeletesMergeStrategyAbstract noDeletesMergeStrategy)
        {
            return Optional.empty();
        }

        @Override
        public Optional<MergeStrategyDeleteMode> visitDeleteIndicatorMergeStrategy(DeleteIndicatorMergeStrategyAbstract deleteIndicatorMergeStrategy)
        {
            return Optional.of(DELETE_MISTAKE);
        }

        @Override
        public Optional<MergeStrategyDeleteMode> visitTerminateLatestActiveMergeStrategy(TerminateLatestActiveMergeStrategyAbstract terminateLatestActiveMergeStrategy)
        {
            return Optional.of(TERMINATE_LATEST_ACTIVE);
        }
    };
}
