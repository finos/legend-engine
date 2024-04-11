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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ModelTexts
{
    private final Map<String, ModelText> pathToText;

    private ModelTexts(Map<String, ModelText> pathToText)
    {
        this.pathToText = pathToText;
    }

    public Collection<String> allPaths()
    {
        return pathToText.keySet();
    }

    public boolean containsPath(String path)
    {
        return pathToText.containsKey(path);
    }

    public ModelText getText(String path)
    {
        return Objects.requireNonNull(pathToText.get(path), "No text for path: " + path);
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof ModelTexts))
        {
            return false;
        }
        ModelTexts other = (ModelTexts) o;
        if (pathToText.size() != other.pathToText.size())
        {
            return false;
        }
        for (String path : allPaths())
        {
            if (!other.containsPath(path) || !other.getText(path).equals(getText(path)))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        return 0;
    }

    @Override
    public String toString()
    {
        return allPaths().stream()
                .sorted()
                .map(this::getText)
                .map(Object::toString)
                .collect(Collectors.joining("\n"));
    }

    public static Builder newBuilder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private Map<String, ModelText> pathToText = new LinkedHashMap<>();

        public Builder with(ModelText text)
        {
            pathToText.put(text.getPath(), text);
            return this;
        }

        public ModelTexts build()
        {
            ModelTexts result = new ModelTexts(pathToText);
            pathToText = null;
            return result;
        }
    }
}
