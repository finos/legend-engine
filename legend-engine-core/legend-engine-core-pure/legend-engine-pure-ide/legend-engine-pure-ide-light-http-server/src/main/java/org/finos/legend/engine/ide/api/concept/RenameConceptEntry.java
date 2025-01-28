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

package org.finos.legend.engine.ide.api.concept;


public class RenameConceptEntry extends AbstractRenameConceptEntry
{
    private final String newName;

    public RenameConceptEntry(int line, int column, String name, String type, String newName)
    {
        super(line, column, name, type);
        this.newName = newName;
    }

    @Override
    public int getReplaceLineIndex()
    {
        return this.getConceptLineIndex();
    }

    @Override
    public int getReplaceColumnIndex()
    {
        return this.getConceptColumnIndex();
    }

    @Override
    public String getOriginalReplaceString()
    {
        return this.getConceptName();
    }

    @Override
    public String getNewReplaceString()
    {
        return this.newName;
    }
}
