// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.ingestmode.deduplication;

import java.util.Optional;

import static org.finos.legend.engine.persistence.components.ingestmode.deduplication.DatasetDeduplicationHandler.COUNT;

public class DeduplicationVisitors
{

    public static final DeduplicationStrategyVisitor<Optional<String>> EXTRACT_DEDUP_FIELD = new DeduplicationStrategyVisitor<Optional<String>>()
    {

        @Override
        public Optional<String> visitAllowDuplicates(AllowDuplicatesAbstract allowDuplicates)
        {
            return Optional.empty();
        }

        @Override
        public Optional<String> visitFilterDuplicates(FilterDuplicatesAbstract filterDuplicates)
        {
            return Optional.of(COUNT);
        }

        @Override
        public Optional<String> visitFailOnDuplicates(FailOnDuplicatesAbstract failOnDuplicates)
        {
            return Optional.of(COUNT);
        }
    };

    public static final DeduplicationStrategyVisitor<Boolean> IS_TEMP_TABLE_NEEDED = new DeduplicationStrategyVisitor<Boolean>()
    {

        @Override
        public Boolean visitAllowDuplicates(AllowDuplicatesAbstract allowDuplicates)
        {
            return false;
        }

        @Override
        public Boolean visitFilterDuplicates(FilterDuplicatesAbstract filterDuplicates)
        {
            return true;
        }

        @Override
        public Boolean visitFailOnDuplicates(FailOnDuplicatesAbstract failOnDuplicates)
        {
            return true;
        }
    };
}
