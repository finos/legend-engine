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

package org.finos.legend.engine.external.format.xml.shared.datatypes;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class BuiltInDataTypes
{
    private static final String NAMESPACE = XMLConstants.W3C_XML_SCHEMA_NS_URI;

    public static QName XS_ANY_URI = new QName(NAMESPACE, "anyURI");
    public static QName XS_BASE64_BINARY = new QName(NAMESPACE, "base64Binary");
    public static QName XS_BOOLEAN = new QName(NAMESPACE, "boolean");
    public static QName XS_BYTE = new QName(NAMESPACE, "byte");
    public static QName XS_DATE = new QName(NAMESPACE, "date");
    public static QName XS_DATE_TIME = new QName(NAMESPACE, "dateTime");
    public static QName XS_DATE_TIME_STAMP = new QName(NAMESPACE, "dateTimeStamp");
    public static QName XS_DAY_TIME_DURATION = new QName(NAMESPACE, "dayTimeDuration");
    public static QName XS_DECIMAL = new QName(NAMESPACE, "decimal");
    public static QName XS_DOUBLE = new QName(NAMESPACE, "double");
    public static QName XS_DURATION = new QName(NAMESPACE, "duration");
    public static QName XS_ENTITIES = new QName(NAMESPACE, "ENTITIES");
    public static QName XS_ENTITY = new QName(NAMESPACE, "ENTITY");
    public static QName XS_FLOAT = new QName(NAMESPACE, "float");
    public static QName XS_G_DAY = new QName(NAMESPACE, "gDay");
    public static QName XS_G_MONTH = new QName(NAMESPACE, "gMonth");
    public static QName XS_G_MONTH_DAY = new QName(NAMESPACE, "gMonthDay");
    public static QName XS_G_YEAR = new QName(NAMESPACE, "gYear");
    public static QName XS_G_YEAR_MONTH = new QName(NAMESPACE, "gYearMonth");
    public static QName XS_HEX_BINARY = new QName(NAMESPACE, "hexBinary");
    public static QName XS_ID = new QName(NAMESPACE, "ID");
    public static QName XS_IDREF = new QName(NAMESPACE, "IDREF");
    public static QName XS_IDREFS = new QName(NAMESPACE, "IDREFS");
    public static QName XS_INT = new QName(NAMESPACE, "int");
    public static QName XS_INTEGER = new QName(NAMESPACE, "integer");
    public static QName XS_LONG = new QName(NAMESPACE, "long");
    public static QName XS_LANGUAGE = new QName(NAMESPACE, "language");
    public static QName XS_NAME = new QName(NAMESPACE, "Name");
    public static QName XS_NCNAME = new QName(NAMESPACE, "NCName");
    public static QName XS_NEGATIVE_INTEGER = new QName(NAMESPACE, "negativeInteger");
    public static QName XS_NMTOKEN = new QName(NAMESPACE, "NMTOKEN");
    public static QName XS_NMTOKENS = new QName(NAMESPACE, "NMTOKENS");
    public static QName XS_NON_NEGATIVE_INTEGER = new QName(NAMESPACE, "nonNegativeInteger");
    public static QName XS_NON_POSITIVE_INTEGER = new QName(NAMESPACE, "nonPositiveInteger");
    public static QName XS_NORMALIZED_STRING = new QName(NAMESPACE, "normalizedString");
    public static QName XS_NOTATION = new QName(NAMESPACE, "NOTATION");
    public static QName XS_POSITIVE_INTEGER = new QName(NAMESPACE, "positiveInteger");
    public static QName XS_QNAME = new QName(NAMESPACE, "QName");
    public static QName XS_SHORT = new QName(NAMESPACE, "short");
    public static QName XS_STRING = new QName(NAMESPACE, "string");
    public static QName XS_TIME = new QName(NAMESPACE, "time");
    public static QName XS_TOKEN = new QName(NAMESPACE, "token");
    public static QName XS_UNSIGNED_BYTE = new QName(NAMESPACE, "unsignedByte");
    public static QName XS_UNSIGNED_INT = new QName(NAMESPACE, "unsignedInt");
    public static QName XS_UNSIGNED_LONG = new QName(NAMESPACE, "unsignedLong");
    public static QName XS_UNSIGNED_SHORT = new QName(NAMESPACE, "unsignedShort");
    public static QName XS_YEAR_MONTH_DURATION = new QName(NAMESPACE, "yearMonthDuration");
}
