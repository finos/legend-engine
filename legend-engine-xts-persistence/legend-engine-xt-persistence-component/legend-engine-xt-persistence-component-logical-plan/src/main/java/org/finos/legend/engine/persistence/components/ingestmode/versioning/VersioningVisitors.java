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

package org.finos.legend.engine.persistence.components.ingestmode.versioning;

import java.util.Optional;

public class VersioningVisitors
{

    public static final VersioningStrategyVisitor<Optional<String>> EXTRACT_DATA_SPLIT_FIELD = new VersioningStrategyVisitor<Optional<String>>()
    {
        @Override
        public Optional<String> visitNoVersioningStrategy(NoVersioningStrategyAbstract noVersioningStrategy)
        {
            return Optional.empty();
        }

        @Override
        public Optional<String> visitMaxVersionStrategy(MaxVersionStrategyAbstract maxVersionStrategy)
        {
            return Optional.empty();
        }

        @Override
        public Optional<String> visitAllVersionsStrategy(AllVersionsStrategyAbstract allVersionsStrategyAbstract)
        {
            return Optional.of(allVersionsStrategyAbstract.dataSplitFieldName());
        }
    };

    public static final VersioningStrategyVisitor<Boolean> IS_TEMP_TABLE_NEEDED = new VersioningStrategyVisitor<Boolean>()
    {

        @Override
        public Boolean visitNoVersioningStrategy(NoVersioningStrategyAbstract noVersioningStrategy)
        {
            return false;
        }

        @Override
        public Boolean visitMaxVersionStrategy(MaxVersionStrategyAbstract maxVersionStrategy)
        {
            return maxVersionStrategy.performStageVersioning();
        }

        @Override
        public Boolean visitAllVersionsStrategy(AllVersionsStrategyAbstract allVersionsStrategyAbstract)
        {
            return allVersionsStrategyAbstract.performStageVersioning();
        }
    };

    public static final VersioningStrategyVisitor<Optional<String>> EXTRACT_VERSIONING_FIELD = new VersioningStrategyVisitor<Optional<String>>()
    {
        @Override
        public Optional<String> visitNoVersioningStrategy(NoVersioningStrategyAbstract noVersioningStrategy)
        {
            return Optional.empty();
        }

        @Override
        public Optional<String> visitMaxVersionStrategy(MaxVersionStrategyAbstract maxVersionStrategy)
        {
            return Optional.of(maxVersionStrategy.versioningField());
        }

        @Override
        public Optional<String> visitAllVersionsStrategy(AllVersionsStrategyAbstract allVersionsStrategyAbstract)
        {
            return Optional.of(allVersionsStrategyAbstract.versioningField());
        }
    };



}
