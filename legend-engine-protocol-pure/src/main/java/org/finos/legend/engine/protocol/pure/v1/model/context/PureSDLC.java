// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.protocol.pure.v1.model.context;

import java.util.Objects;

public class PureSDLC extends SDLC
{
    public String overrideUrl;

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if ((o == null) || (o.getClass() != this.getClass()))
        {
            return false;
        }
        PureSDLC that = (PureSDLC) o;
        return Objects.equals(this.overrideUrl, that.overrideUrl) && Objects.equals(this.version, that.version) && Objects.equals(this.baseVersion, that.baseVersion) && Objects.equals(this.packageableElementPointers, that.packageableElementPointers);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.overrideUrl, this.version, this.baseVersion, this.packageableElementPointers);
    }
}
