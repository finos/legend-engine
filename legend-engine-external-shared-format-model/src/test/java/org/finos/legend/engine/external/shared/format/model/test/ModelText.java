//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.external.shared.format.model.test;

import java.util.Objects;

public class ModelText
{
    private final String path;
    private final String grammar;

    public ModelText(String path, String grammar)
    {
        this.path = Objects.requireNonNull(path);
        this.grammar = Objects.requireNonNull(grammar);
    }

    public String getPath()
    {
        return path;
    }

    @Override
    public boolean equals(Object o)
    {
        return o instanceof ModelText && o.toString().equals(toString());
    }

    @Override
    public int hashCode()
    {
        return path.hashCode();
    }

    @Override
    public String toString()
    {
        return ">>>" + path + "\n" + grammar;
    }
}
