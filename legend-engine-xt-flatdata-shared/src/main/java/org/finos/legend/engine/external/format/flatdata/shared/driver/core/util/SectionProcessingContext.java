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

package org.finos.legend.engine.external.format.flatdata.shared.driver.core.util;

import org.finos.legend.engine.external.format.flatdata.shared.driver.core.variables.ProcessingVariables;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.variables.VariablesProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.Connection;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataDriverDescription;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.ObjectToParsedFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.ParsedFlatDataToObject;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordType;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;

import java.util.Objects;
import java.util.function.Function;

public class SectionProcessingContext extends VariablesProcessingContext
{
    private final Connection connection;
    private final String definingPath;
    private final FlatDataSection section;
    private final Function<FlatDataRecordType, ParsedFlatDataToObject<?>> toObjectFactoryFactory;
    private final Function<FlatDataRecordType, ObjectToParsedFlatData<?>> fromObjectFactoryFactory;

    public SectionProcessingContext(Connection connection,
                                    String definingPath,
                                    FlatDataDriverDescription description,
                                    FlatDataSection section,
                                    Function<FlatDataRecordType, ParsedFlatDataToObject<?>> toObjectFactoryFactory,
                                    Function<FlatDataRecordType, ObjectToParsedFlatData<?>> fromObjectFactoryFactory,
                                    ProcessingVariables variables)
    {
        super(variables, description.getDeclares());
        this.connection = connection;
        this.definingPath = definingPath;
        this.section = section;
        this.toObjectFactoryFactory = toObjectFactoryFactory;
        this.fromObjectFactoryFactory = fromObjectFactoryFactory;
    }

    @Override
    public String getDefiningPath()
    {
        return definingPath;
    }

    @Override
    public Connection getConnection()
    {
        return connection;
    }

    @Override
    public <T> ParsedFlatDataToObject<? extends T> createToObjectFactory(FlatDataRecordType recordType)
    {
        return (ParsedFlatDataToObject<? extends T>) Objects.requireNonNull(toObjectFactoryFactory, "No factory for section '" + section.getName() + "'").apply(recordType);
    }

    @Override
    public <T> ObjectToParsedFlatData<? extends T> createFromObjectFactory(FlatDataRecordType recordType)
    {
        return (ObjectToParsedFlatData<? extends T>) Objects.requireNonNull(fromObjectFactoryFactory, "No factory for section '" + section.getName() + "'").apply(recordType);
    }
}
