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

import org.finos.legend.engine.ide.api.concept.AbstractRenameConceptEntry;

public class RenamePackageEntry extends AbstractRenameConceptEntry
{
    private final String sourcePackage;
    private final String destinationPackage;

    public RenamePackageEntry(int line, int column, String name, String type, String sourcePackage, String destinationPackage)
    {
        super(line, column, name, type);
        this.sourcePackage = sourcePackage;
        this.destinationPackage = destinationPackage;
    }

    @Override
    public int getReplaceLineIndex()
    {
        return this.getConceptLineIndex();
    }

    @Override
    public int getReplaceColumnIndex()
    {
        return this.getConceptColumnIndex() - this.getOriginalReplaceString().length() - 2;
    }

    @Override
    public String getOriginalReplaceString()
    {
        return this.sourcePackage;
    }

    @Override
    public String getNewReplaceString()
    {
        return this.destinationPackage;
    }
}
