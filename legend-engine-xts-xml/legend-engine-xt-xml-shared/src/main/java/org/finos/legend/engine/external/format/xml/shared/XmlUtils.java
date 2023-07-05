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

package org.finos.legend.engine.external.format.xml.shared;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

public class XmlUtils
{
    public static final QName XSI_NIL = new QName(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "nil");

    public static String toShortString(QName name)
    {
        return (name.getPrefix().isEmpty() ? "" : name.getPrefix() + ":") + name.getLocalPart();
    }

    public static boolean lenientMatch(String name1, String name2)
    {
        int i1 = 0;
        int i2 = 0;
        while (i1 < name1.length() && i2 < name2.length())
        {
            if (name1.charAt(i1) == name2.charAt(i2))
            {
                i1++;
                i2++;
            }
            else if (!Character.isLetterOrDigit(name1.charAt(i1)))
            {
                i1++;
            }
            else if (!Character.isLetterOrDigit(name2.charAt(i2)))
            {
                i2++;
            }
            else if (Character.toLowerCase(name1.charAt(i1)) == Character.toLowerCase(name2.charAt(i2)))
            {
                i1++;
                i2++;
            }
            else
            {
                return false;
            }
        }
        return i1 == name1.length() && i2 == name2.length();
    }

}
