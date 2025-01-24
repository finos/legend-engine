// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.changetoken.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ElementPathTransformer
{
    private static final Pattern PACKAGE_PATH_PATTERN = Pattern.compile("^(\\w[\\w$]*+::)++\\w[\\w$]*+$");
    private static final String PACKAGE_KEY = "package";
    private static final String NAME_KEY = "name";

    private final Function<? super String, ? extends String> pathTransformer;
    private final List<PackageableElement> elements = new ArrayList<>();
    private final ObjectMapper objectMapper;

    private ElementPathTransformer(Function<? super String, ? extends String> pathTransformer)
    {
        this.pathTransformer = pathTransformer;
        this.objectMapper = PureProtocolObjectMapperFactory.getNewObjectMapper();
    }

    public static ElementPathTransformer newTransformer(Function<? super String, ? extends String> pathTransformer)
    {
        if (pathTransformer == null)
        {
            throw new IllegalArgumentException("path transformer function may not be null");
        }
        return new ElementPathTransformer(pathTransformer);
    }

    private static boolean isValidPath(String path)
    {
        return (path != null) && !path.startsWith("meta::") && PACKAGE_PATH_PATTERN.matcher(path).matches();
    }

    public ElementPathTransformer addElement(PackageableElement element)
    {
        this.elements.add(element);
        return this;
    }

    public ElementPathTransformer addElements(Iterable<? extends PackageableElement> elements)
    {
        elements.forEach(this::addElement);
        return this;
    }

    public ElementPathTransformer addElements(Stream<? extends PackageableElement> elements)
    {
        elements.forEach(this::addElement);
        return this;
    }

    public ElementPathTransformer addElements(PackageableElement... elements)
    {
        if (elements != null)
        {
            for (int i = 0; i < elements.length; i++)
            {
                addElement(elements[i]);
            }
        }
        return this;
    }

    public List<PackageableElement> getElements()
    {
        return Collections.unmodifiableList(this.elements);
    }

    public int getElementCount()
    {
        return this.elements.size();
    }

    public List<PackageableElement> transformElements()
    {
        List<PackageableElement> newElements = new ArrayList<>(this.elements.size());
        this.elements.stream().map(this::transformElement).forEach(newElements::add);
        return newElements;
    }

    private PackageableElement transformElement(PackageableElement element)
    {
        String newPath = applyPathTransformer(element.getPath());
        if (newPath == null)
        {
            throw new IllegalStateException("Could not find transformation for element path: " + element.getPath());
        }
        return objectMapper.convertValue(transformPackageableElement(objectMapper.convertValue(element, Map.class)), PackageableElement.class);
    }

    private Object transformValue(Object value)
    {
        if (value == null)
        {
            return null;
        }
        if (value instanceof String)
        {
            String string = (String) value;
            return isValidPath(string) ? applyPathTransformer(string) : string;
        }
        if (value instanceof List)
        {
            List<?> list = (List<?>) value;
            List<Object> newList = new ArrayList<>(list.size());
            list.stream().map(this::transformValue).forEach(newList::add);
            return newList;
        }
        if (value instanceof Map)
        {
            Map<?, ?> map = (Map<?, ?>) value;
            if (isPackageableElement(map))
            {
                return transformPackageableElement(map);
            }
            Map<Object, Object> newMap = new HashMap<>(map.size());
            map.forEach((k, v) -> newMap.put(k, transformValue(v)));
            return newMap;
        }
        return value;
    }

    private boolean isPackageableElement(Map<?, ?> map)
    {
        return map.containsKey(PACKAGE_KEY) && map.containsKey(NAME_KEY);
    }

    private Map<?, ?> transformPackageableElement(Map<?, ?> packageableElement)
    {
        String oldPath = packageableElement.get(PACKAGE_KEY) + "::" + packageableElement.get(NAME_KEY);
        String newPath = applyPathTransformer(oldPath);
        if (newPath == null)
        {
            throw new IllegalStateException("Could not find transformation for element path: " + oldPath);
        }
        return transformPackageableElement(newPath, packageableElement);
    }

    private <K> Map<K, ?> transformPackageableElement(String newPath, Map<K, ?> packageableElement)
    {
        int lastColon = newPath.lastIndexOf(':');
        String newPackage = (lastColon == -1) ? "::" : newPath.substring(0, lastColon - 1);
        String newName = (lastColon == -1) ? newPath : newPath.substring(lastColon + 1);

        Map<K, Object> transformed = new HashMap<>(packageableElement.size());
        packageableElement.forEach((k, v) ->
        {
            Object newValue = PACKAGE_KEY.equals(k) ? newPackage : (NAME_KEY.equals(k) ? newName : transformValue(v));
            transformed.put(k, newValue);
        });
        return transformed;
    }

    private String applyPathTransformer(String path)
    {
        String transformedPath = this.pathTransformer.apply(path);
        if (!Objects.equals(path, transformedPath) && !isValidPath(transformedPath))
        {
            StringBuilder builder = new StringBuilder("Invalid transformation for \"").append(path).append("\": ");
            if (transformedPath == null)
            {
                builder.append((String) null);
            }
            else
            {
                builder.append('"').append(transformedPath).append('"');
            }
            throw new RuntimeException(builder.toString());
        }
        return transformedPath;
    }
}
