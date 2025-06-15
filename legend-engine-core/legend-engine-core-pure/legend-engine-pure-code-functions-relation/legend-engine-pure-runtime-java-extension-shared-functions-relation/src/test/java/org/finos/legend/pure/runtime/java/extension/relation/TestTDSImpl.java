// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.extension.relation;

import io.deephaven.csv.parsers.DataType;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.TestTDS;

public class TestTDSImpl extends TestTDS
{
    public TestTDSImpl()
    {

    }

    public TestTDSImpl(String csv)
    {
        super(csv);
    }

    public TestTDSImpl(MutableList<String> columnOrdered, MutableMap<String, DataType> columnType, int rows)
    {
        super(columnOrdered, columnType, rows);
    }

    @Override
    public Object getValueAsCoreInstance(String columnName, int rowNum)
    {
        return null;
    }

    @Override
    public TestTDS newTDS()
    {
        return new TestTDSImpl();
    }

    @Override
    public TestTDS newTDS(MutableList<String> columnOrdered, MutableMap<String, DataType> columnType, int rows)
    {
        return new TestTDSImpl(columnOrdered, columnType, rows);
    }
}
