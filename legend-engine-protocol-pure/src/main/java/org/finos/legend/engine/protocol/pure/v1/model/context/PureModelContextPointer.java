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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.engine.protocol.Protocol;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PureModelContextPointer extends PureModelContext
{
    public Protocol serializer;
    public SDLC sdlcInfo = new PureSDLC();

    @JsonIgnore
    public PureModelContextPointer combine(PureModelContextPointer other)
    {
        if (other == null)
        {
            return this;
        }

        if (!(safeEqual(this.sdlcInfo.version, other.sdlcInfo.version) && safeEqual(this.sdlcInfo.baseVersion, other.sdlcInfo.baseVersion)))
        {
            throw new RuntimeException("Can't merge two context as they come from two different environment");
        }
        PureSDLC pureSdlc = new PureSDLC();
        pureSdlc.version = this.sdlcInfo.version;
        pureSdlc.baseVersion = this.sdlcInfo.baseVersion;
        MutableSet<PackageableElementPointer> set = Sets.mutable.withAll(((PureSDLC) this.sdlcInfo).packageableElementPointers);
        set.addAll(((PureSDLC) other.sdlcInfo).packageableElementPointers);
        pureSdlc.packageableElementPointers = set.toList();
        PureModelContextPointer pointer = new PureModelContextPointer();
        pointer.sdlcInfo = pureSdlc;
        pointer.serializer = this.serializer;
        return pointer;
    }

    @JsonIgnore
    public boolean safeEqual(Object o, Object o2)
    {
        return o == null ? o2 == null : o.equals(o2);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        PureModelContextPointer that = (PureModelContextPointer) o;
        return Objects.equals(serializer, that.serializer) &&
                Objects.equals(sdlcInfo, that.sdlcInfo);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(serializer, sdlcInfo);
    }
}
