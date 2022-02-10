// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.external.format.flatdata.read;

import org.finos.legend.engine.external.format.flatdata.FlatDataContext;
import org.finos.legend.engine.external.shared.runtime.read.ExternalFormatReader;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;

import java.io.InputStream;
import java.util.function.Consumer;

public class FlatDataReader<T> extends ExternalFormatReader<T>
{
    private final FlatDataContext<T> context;
    private final InputStream inputStream;

    public FlatDataReader(FlatDataContext<T> context, InputStream inputStream)
    {
        this.context = context;
        this.inputStream = inputStream;
    }

    @Override
    public void readData(Consumer<IChecked<T>> consumer)
    {
        context.createProcessor().readData(inputStream, consumer);
    }
}
