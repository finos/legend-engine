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

public class MergeStrategyVisitors
{
    private MergeStrategyVisitors()
    {
    }

    public static final MergeStrategyVisitor<Optional<String>> EXTRACT_DELETE_FIELD = new MergeStrategyVisitor<Optional<String>>()
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
    };

    public static final MergeStrategyVisitor<List<Object>> EXTRACT_DELETE_VALUES = new MergeStrategyVisitor<List<Object>>()
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
    };
}
