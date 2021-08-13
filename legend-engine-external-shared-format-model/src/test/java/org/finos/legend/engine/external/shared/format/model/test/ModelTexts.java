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
