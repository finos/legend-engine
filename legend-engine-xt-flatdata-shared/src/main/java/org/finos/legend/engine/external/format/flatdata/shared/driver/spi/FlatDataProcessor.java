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

package org.finos.legend.engine.external.format.flatdata.shared.driver.spi;

import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordType;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public interface FlatDataProcessor<T>
{
    void readData(InputStream inputStream, Consumer<IChecked<T>> consumer);

    void writeData(Stream<T> inputStream, OutputStream outputStream);

    interface Builder<T>
    {
        Builder<T> withDefiningPath(String definingPath);

        Builder<T> withToObjectFactoryFactory(String sectionId, Function<FlatDataRecordType, ParsedFlatDataToObject<?>> toObjectFactoryFactory);

        Builder<T> withFromObjectFactoryFactory(String sectionId, Function<FlatDataRecordType, ObjectToParsedFlatData<?>> fromObjectFactoryFactory);

        FlatDataProcessor<T> build();
    }
}
