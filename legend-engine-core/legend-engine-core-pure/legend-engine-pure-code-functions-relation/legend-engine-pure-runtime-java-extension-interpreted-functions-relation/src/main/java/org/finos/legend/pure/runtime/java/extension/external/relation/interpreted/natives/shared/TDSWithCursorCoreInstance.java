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

package org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.shared;

import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.simple.SimpleCoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.TestTDS;

public class TDSWithCursorCoreInstance extends SimpleCoreInstance
{
    private final TestTDSInterpreted tds;
    private final int currentRow;

    public TDSWithCursorCoreInstance(TestTDS tds, int currentRow, String name, SourceInformation sourceInformation, CoreInstance classifier, int internalSyntheticId, ModelRepository repository, boolean persistent)
    {
        super(name, sourceInformation, classifier, internalSyntheticId, repository, persistent);
        this.tds = (TestTDSInterpreted)tds;
        this.currentRow = currentRow;
    }

    public CoreInstance getValue(String name)
    {
        return tds.getValueAsCoreInstance(name, currentRow);
    }

    public int getCurrentRow()
    {
        return currentRow;
    }
}