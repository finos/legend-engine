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

package org.finos.legend.engine.shared.core.url;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class NamedInputStreamProvider implements StreamProvider
{
    private List<NamedInputStream> namedInputStreams;

    public NamedInputStreamProvider(List<NamedInputStream> namedInputStreams)
    {
        this.namedInputStreams = namedInputStreams;
    }

    private boolean isDefaultCase(String name)
    {
        return name.equals("default") && namedInputStreams.size() == 1;
    }

    @Override
    public InputStream getInputStream(String name)
    {
        if (isDefaultCase(name))
        {
            return namedInputStreams.get(0).getInputStream();
        }
        else
        {
            Optional<NamedInputStream> onis = this.namedInputStreams.stream().filter(nis -> nis.getName().equals(name)).findFirst();

            if (!onis.isPresent())
            {
                throw new IllegalArgumentException("Named InputStream: " + name + " not found, available Named InputStreams: " + this.namedInputStreams.stream().map(s -> s.getName()).collect(Collectors.joining(", ")));
            }
            return onis.get().getInputStream();
        }
    }
}
