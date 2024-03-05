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

package org.finos.legend.engine.plan.dependencies.domain.dataQuality;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BasicChecked<T> implements IChecked<T>
{
    private List<IDefect> defects;
    private Object source;
    private T value;

    private BasicChecked()
    {
        // For privacy
    }

    @Override
    public List<IDefect> getDefects()
    {
        return defects;
    }

    @Override
    public Object getSource()
    {
        return source;
    }

    @Override
    public T getValue()
    {
        return value;
    }

    public static <T> IChecked<T> newChecked(T value, Object source, List<IDefect> defects)
    {
        BasicChecked<T> result = new BasicChecked<>();
        result.defects = Collections.unmodifiableList(new ArrayList<>(defects));
        result.source = source;
        result.value = value;
        return result;
    }

    public static <T> IChecked<T> newChecked(T value, Object source, IDefect defect)
    {
        return newChecked(value, source, Collections.singletonList(defect));
    }

    public static <T> IChecked<T> newChecked(T value, Object source)
    {
        return newChecked(value, source, Collections.emptyList());
    }

    @Override
    public String toString()
    {
        return "BasicChecked{" +
                "defects=" + defects +
                ", source=" + source +
                ", value=" + value +
                '}';
    }
}
