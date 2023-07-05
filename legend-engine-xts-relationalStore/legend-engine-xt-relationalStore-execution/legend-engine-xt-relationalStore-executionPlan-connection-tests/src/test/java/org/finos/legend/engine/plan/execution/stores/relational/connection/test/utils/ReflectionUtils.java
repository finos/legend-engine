// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.connection.test.utils;

import java.lang.reflect.Field;

public class ReflectionUtils
{
    public static Object getFieldUsingReflection(Class clazz, Object object, String fieldName) throws Exception
    {
        Field field = null;
        try
        {
            field = clazz.getDeclaredField(fieldName);
        }
        catch (NoSuchFieldException e)
        {
            field = clazz.getSuperclass().getDeclaredField(fieldName);
        }
        return getValueFromObject(object, field);
    }

    public static Object getValueFromObject(Object object, Field field) throws IllegalAccessException
    {
        field.setAccessible(true);
        Object value = field.get(object);
        return value;
    }

    public static void resetStaticField(Class clazz, String fieldName) throws Exception
    {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, null);
    }
}
