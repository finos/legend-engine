//  Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.changetoken.generation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GenerateCastUtil
{
    public static Object resolveRelativeReference(Map<String, Object> rootObjectNode, List<Object> path, String relativeReference)
    {
        List<Object> newPath = resolvePath(path, relativeReference);

        /*
            extract the value by path
         */
        Object cur = rootObjectNode;
        int i = 0;
        for (Object key : newPath)
        {
            try
            {
                if (cur instanceof Map)
                {
                    cur = ((Map<String, Object>) cur).get((String) key);
                }
                else if (cur instanceof List)
                {
                    cur = ((List<Object>) cur).get(toInt(key));
                }
                else
                {
                    throw new RuntimeException("Unexpected key on a non-container object of type " + cur.getClass().getName());
                }
                if (cur == null)
                {
                    throw new RuntimeException("No such element");
                }
            }
            catch (RuntimeException ex)
            {
                throw new RuntimeException(
                        ex + ": at index " + i + " of " + newPath.stream().map(Object::toString).collect(Collectors.joining("/")), ex);
            }
            ++i;
        }
        return cur;
    }

    public static void setRelativeReference(Map<String,Object> rootObjectNode, List<Object> path, String relativeReference, Object value)
    {
        List<Object> newPath = resolvePath(path, relativeReference);

        /*
            set the value by path
         */
        Object cur = rootObjectNode;
        int i = 0;
        for (Object key : newPath.subList(0, newPath.size() - 1))
        {
            try
            {
                if (cur instanceof Map)
                {
                    cur = ((Map<String, Object>) cur).get((String) key);
                }
                else if (cur instanceof List)
                {
                    cur = ((List<Object>) cur).get(toInt(key));
                }
                else
                {
                    throw new RuntimeException("Unexpected key on a non-container object of type " + cur.getClass().getName());
                }
                if (cur == null)
                {
                    throw new RuntimeException("No such element");
                }
            }
            catch (RuntimeException ex)
            {
                throw new RuntimeException(
                        ex + ": at index " + i + " of " + newPath.stream().map(Object::toString).collect(Collectors.joining("/")), ex);
            }
            ++i;
        }
        try
        {
            if (cur instanceof Map)
            {
                ((Map<String, Object>) cur).put((String) newPath.get(newPath.size() - 1), value);
            }
            else if (cur instanceof List)
            {
                ((List<Object>) cur).set(toInt(newPath.get(newPath.size() - 1)), value);
            }
            else
            {
                throw new RuntimeException("Unexpected non-container object of type " + cur.getClass().getName());
            }
        }
        catch (RuntimeException ex)
        {
            throw new RuntimeException(
                    ex + ": at index " + i + " of " + newPath.stream().map(Object::toString).collect(Collectors.joining("/")), ex);
        }
    }

    protected static List<Object> resolvePath(List<Object> path, String relativeReference)
    {
        List<Object> newPath = new ArrayList<>(path);
        String[] parts = relativeReference.split("/");
        for (int i = 0; i < parts.length; i++)
        {
            String part = parts[i];
            if ("..".equals(part))
            {
                try
                {
                    newPath.remove(newPath.size() - 1);
                }
                catch (IndexOutOfBoundsException e)
                {
                    throw new RuntimeException("Relative reference escapes root ("
                            + path.stream().map(Object::toString).collect(Collectors.joining("/"))
                            + ") at index " + i + " of " + relativeReference, e);
                }
            }
            else
            {
                newPath.add(part);
            }
        }
        return newPath;
    }

    private static int toInt(Object key)
    {
        return (key instanceof Integer) ? (Integer) key : Integer.valueOf((String)key);
    }
}
