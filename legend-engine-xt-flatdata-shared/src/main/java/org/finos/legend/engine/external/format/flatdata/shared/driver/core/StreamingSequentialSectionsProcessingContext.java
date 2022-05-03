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

package org.finos.legend.engine.external.format.flatdata.shared.driver.core;

import org.finos.legend.engine.external.format.flatdata.shared.driver.core.connection.CharCursor;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.SectionProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.variables.ProcessingVariables;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.*;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordType;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;

import java.util.function.Function;

public class StreamingSequentialSectionsProcessingContext extends SectionProcessingContext
{
    private FlatDataDriver nextDriver;

    public StreamingSequentialSectionsProcessingContext(Connection connection,
                                                        String definingPath,
                                                        FlatDataDriverDescription description,
                                                        FlatDataSection section,
                                                        Function<FlatDataRecordType, ParsedFlatDataToObject<?>> toObjectFactoryFactory,
                                                        Function<FlatDataRecordType, ObjectToParsedFlatData<?>> fromObjectFactoryFactory,
                                                        ProcessingVariables variables,
                                                        FlatDataDriver nextDriver)
    {
        super(connection, definingPath, description, section, toObjectFactoryFactory, fromObjectFactoryFactory, variables);
        this.nextDriver = nextDriver;
    }

    boolean isNextSectionReadyToStartAt(CharCursor cursor)
    {
        return (nextDriver == null) ? cursor.isEndOfData() : ((StreamingReadDriver) nextDriver).canStartAt(cursor);
    }
}
