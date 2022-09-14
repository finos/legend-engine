// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.util;

import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class DigestUtils
{
    private static final String EMPTY = "";

    public static class FieldIndexContext implements Serializable
    {
        private final String fieldName;
        private final int actualIndex;

        public FieldIndexContext(String fieldName, int actualIndex)
        {
            this.fieldName = fieldName;
            this.actualIndex = actualIndex;
        }

        public String getFieldName()
        {
            return fieldName;
        }

        public int getActualIndex()
        {
            return actualIndex;
        }
    }

    public static DigestContext getDigestContext(SchemaDefinition definition, Set<String> metaFields)
    {
        SortedSet<FieldIndexContext> sortedFieldDefinitions = new TreeSet<FieldIndexContext>(Comparator.comparing(FieldIndexContext::getFieldName));
        int index = 0;
        for (Field field : definition.fields())
        {
            if (metaFields == null || !metaFields.contains(field.name()))
            {
                sortedFieldDefinitions.add(new FieldIndexContext(field.name(), index++));
            }
        }
        return new DigestContext(sortedFieldDefinitions);
    }

    // The white knight character is being used as we need unique byte
    // representation for EMPTY_STRING values to ensure unique digest calculation.
    private static final char WHITE_KNIGHT_CHAR = '\u2658';
    private static final byte[] EMPTY_STRING = {(byte) WHITE_KNIGHT_CHAR};

    private DigestUtils()
    {
    }

    public static String getDigest(Object[] objects, DigestContext digestContext, boolean convertFieldNamesToUpperCase)
    {
        SortedSet<FieldIndexContext> sortedFieldDefinitionsByName = digestContext.getSortedFieldDefinitionsByName();
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(); DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream))
        {
            for (FieldIndexContext sortedFieldDefinition : sortedFieldDefinitionsByName)
            {
                Object value = objects[sortedFieldDefinition.getActualIndex()];
                if (value != null)
                {
                    writeValueWithFieldName(sortedFieldDefinition.getFieldName(), value, dataOutputStream, convertFieldNamesToUpperCase);
                }
            }

            dataOutputStream.flush();
            return org.apache.commons.codec.digest.DigestUtils.md5Hex(byteArrayOutputStream.toByteArray());
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static void writeValueWithFieldName(String fieldName, Object value, DataOutputStream dataOutputStream, boolean convertFieldNamesToUpperCase)
        throws IOException
    {
        if (convertFieldNamesToUpperCase)
        {
            fieldName = fieldName.toUpperCase();
        }
        dataOutputStream.writeInt(fieldName.hashCode());
        String stringValue = value == null ? EMPTY : value.toString();
        if (empty(stringValue))
        {
            dataOutputStream.write(EMPTY_STRING);
        }
        else
        {
            dataOutputStream.writeBytes(stringValue);
        }
    }

    private static boolean empty(CharSequence cs)
    {
        return cs == null || cs.length() == 0;
    }
}
