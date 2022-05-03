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

package org.finos.legend.engine.external.format.xml.shared.test;

import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.external.format.xml.shared.datatypes.BooleanSimpleTypeHandler;
import org.finos.legend.engine.external.format.xml.shared.datatypes.BuiltInDataTypes;
import org.finos.legend.engine.external.format.xml.shared.datatypes.DoubleSimpleTypeHandler;
import org.finos.legend.engine.external.format.xml.shared.datatypes.FacetType;
import org.finos.legend.engine.external.format.xml.shared.datatypes.LongSimpleTypeHandler;
import org.finos.legend.engine.external.format.xml.shared.datatypes.SimpleTypeHandler;
import org.finos.legend.engine.external.format.xml.shared.datatypes.SimpleTypesContext;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

public class TestSimpleTypeHandlers
{
    private NamespaceContext namespaces;
    private SimpleTypesContext context = new SimpleTypesContext(this::getNamespaces);

    @Test
    public void testXsdStringWhiteSpace()
    {
        SimpleTypeHandler handler = context.handler(BuiltInDataTypes.XS_STRING);
        Assert.assertEquals("ABC", handler.parse("ABC"));
        Assert.assertEquals("ABC", handler.toText("ABC"));
        Assert.assertEquals(" \n\tA\t\tB\tC\r\n ", handler.parse(" \n\tA\t\tB\tC\r\n "));

        QName replace = new QName("replace");
        context.defineType(replace, BuiltInDataTypes.XS_STRING, FacetType.WHITESPACE.of("replace"));
        SimpleTypeHandler withReplace = context.handler(replace);
        Assert.assertEquals("   A  B C   ", withReplace.parse(" \n\tA\t\tB\tC\r\n "));

        QName collapse = new QName("collapse");
        context.defineType(collapse, BuiltInDataTypes.XS_STRING, FacetType.WHITESPACE.of("collapse"));
        SimpleTypeHandler withCollapse = context.handler(collapse);
        Assert.assertEquals("A B C", withCollapse.parse(" \n\tA\t\tB\tC\r\n "));
    }

    @Test
    public void testXsdStringEnumeration()
    {
        QName yesNo = new QName("yes-no");
        context.defineType(yesNo, BuiltInDataTypes.XS_STRING, FacetType.ENUMERATION.of("yes"), FacetType.ENUMERATION.of("no"));
        SimpleTypeHandler handler = context.handler(yesNo);

        Assert.assertEquals("yes", handler.parse("yes"));
        Assert.assertEquals("no", handler.parse("no"));
        assertInvalidValue("Invalid yes-no value: 'maybe', expected one of: yes,no", handler, "maybe");
    }

    @Test
    public void testXsdStringLength()
    {
        QName fiveChars = new QName("five-chars");
        context.defineType(fiveChars, BuiltInDataTypes.XS_STRING, FacetType.LENGTH.of("5"));
        SimpleTypeHandler handler = context.handler(fiveChars);

        Assert.assertEquals("abcde", handler.parse("abcde"));
        assertInvalidValue("Invalid five-chars value: 'abcd', expected exactly 5 characters", handler, "abcd");
    }

    @Test
    public void testXsdStringMinLength()
    {
        QName minFiveChars = new QName("min-five-chars");
        context.defineType(minFiveChars, BuiltInDataTypes.XS_STRING, FacetType.MIN_LENGTH.of("5"));
        SimpleTypeHandler handler = context.handler(minFiveChars);

        Assert.assertEquals("abcde", handler.parse("abcde"));
        Assert.assertEquals("abcdef", handler.parse("abcdef"));
        Assert.assertEquals("abcdefghijklmn", handler.parse("abcdefghijklmn"));
        assertInvalidValue("Invalid min-five-chars value: 'abcd', expected minimum of 5 characters", handler, "abcd");
    }

    @Test
    public void testXsdStringMaxLength()
    {
        QName maxTenChars = new QName("max-ten-chars");
        context.defineType(maxTenChars, BuiltInDataTypes.XS_STRING, FacetType.MAX_LENGTH.of("10"));
        SimpleTypeHandler handler = context.handler(maxTenChars);

        Assert.assertEquals("abcdefghi", handler.parse("abcdefghi"));
        Assert.assertEquals("abcdefghij", handler.parse("abcdefghij"));
        assertInvalidValue("Invalid max-ten-chars value: 'abcdefghijklmn', expected maximum of 10 characters", handler, "abcdefghijklmn");
    }

    @Test
    public void testXsdStringMinMaxLength()
    {
        QName fiveToTenChars = new QName("five-to-ten-chars");
        context.defineType(fiveToTenChars, BuiltInDataTypes.XS_STRING, FacetType.MIN_LENGTH.of("5"), FacetType.MAX_LENGTH
                .of("10"));
        SimpleTypeHandler handler = context.handler(fiveToTenChars);

        Assert.assertEquals("abcde", handler.parse("abcde"));
        Assert.assertEquals("abcdef", handler.parse("abcdef"));
        Assert.assertEquals("abcdefghi", handler.parse("abcdefghi"));
        Assert.assertEquals("abcdefghij", handler.parse("abcdefghij"));
        assertInvalidValue("Invalid five-to-ten-chars value: 'abcd', expected minimum of 5 characters and maximum of 10", handler, "abcd");
        assertInvalidValue("Invalid five-to-ten-chars value: 'abcdefghijklmn', expected minimum of 5 characters and maximum of 10", handler, "abcdefghijklmn");
    }

    @Test
    public void testXsdStringPattern()
    {
        QName pattern = new QName("code");
        context.defineType(pattern, BuiltInDataTypes.XS_STRING, FacetType.PATTERN.of("(GB|US|EU)\\d{5}"));
        SimpleTypeHandler handler = context.handler(pattern);

        Assert.assertEquals("GB12345", handler.parse("GB12345"));
        Assert.assertEquals("US67890", handler.parse("US67890"));
        assertInvalidValue("Invalid code value: 'abcd', does not match expected pattern", handler, "abcd");
        assertInvalidValue("Invalid code value: 'xxUS67890', does not match expected pattern", handler, "xxUS67890");
    }

    @Test
    public void testXsdNormalizedString()
    {
        SimpleTypeHandler handler = context.handler(BuiltInDataTypes.XS_NORMALIZED_STRING);
        Assert.assertEquals(" A B C ", handler.parse(" A B C "));
        Assert.assertEquals("   A  B C   ", handler.parse(" \n\tA\t\tB\tC\r\n "));
        Assert.assertEquals("ABC", handler.toText("ABC"));
    }

    @Test
    public void testXsdToken()
    {
        SimpleTypeHandler handler = context.handler(BuiltInDataTypes.XS_TOKEN);
        Assert.assertEquals("A B C", handler.parse(" A B C "));
        Assert.assertEquals("A B C", handler.parse(" \n\tA\t\tB\tC\r\n "));

        Assert.assertEquals("ABC", handler.toText("ABC"));
    }

    @Test
    public void testXsdName()
    {
        SimpleTypeHandler handler = context.handler(BuiltInDataTypes.XS_NAME);
        Assert.assertEquals("Hello-World", handler.parse("Hello-World"));
        Assert.assertEquals("Hello:World", handler.parse("Hello:World"));
        Assert.assertEquals("_123", handler.parse("_123"));
        assertInvalidValue("Invalid Name value: '123', does not match expected pattern", handler, "123");
    }

    @Test
    public void testXsdNCName()
    {
        SimpleTypeHandler handler = context.handler(BuiltInDataTypes.XS_NCNAME);
        Assert.assertEquals("Hello-World", handler.parse("Hello-World"));
        assertInvalidValue("Invalid NCName value: 'Hello:World', does not match expected pattern", handler, "Hello:World");
    }

    @Test
    public void testXsdQNameDefaultMapped()
    {
        setNamespaces(Maps.mutable.of(XMLConstants.DEFAULT_NS_PREFIX, "http://test.com/test"));
        SimpleTypeHandler<QName> handler = context.handler(BuiltInDataTypes.XS_QNAME);
        QName qName = handler.parse("local");
        Assert.assertEquals("", qName.getPrefix());
        Assert.assertEquals("http://test.com/test", qName.getNamespaceURI());
        Assert.assertEquals("local", qName.getLocalPart());
    }

    @Test
    public void testXsdQNameDefaultNotMapped()
    {
        setNamespaces(Maps.mutable.empty());
        SimpleTypeHandler<QName> handler = context.handler(BuiltInDataTypes.XS_QNAME);
        QName qName = handler.parse("local");
        Assert.assertEquals("", qName.getPrefix());
        Assert.assertEquals("", qName.getNamespaceURI());
        Assert.assertEquals("local", qName.getLocalPart());
    }

    @Test
    public void testXsdQNamePrefixed()
    {
        setNamespaces(Maps.mutable.of("tst", "http://test.com/test"));
        SimpleTypeHandler<QName> handler = context.handler(BuiltInDataTypes.XS_QNAME);
        QName qName = handler.parse("tst:local");
        Assert.assertEquals("tst", qName.getPrefix());
        Assert.assertEquals("http://test.com/test", qName.getNamespaceURI());
        Assert.assertEquals("local", qName.getLocalPart());
    }

    @Test
    public void testXsdQNameInvalid()
    {
        setNamespaces(Maps.mutable.empty());
        SimpleTypeHandler handler = context.handler(BuiltInDataTypes.XS_QNAME);
        assertInvalidValue("Invalid QName value: 'tst:local', Prefix tst unknown", handler, "tst:local");
        assertInvalidValue("Invalid QName value: 'lo?cal', does not match expected pattern", handler, "lo?cal");
    }

    @Test
    public void testXsdDecimal()
    {
        SimpleTypeHandler<BigDecimal> handler = context.handler(BuiltInDataTypes.XS_DECIMAL);
        Assert.assertEquals(new BigDecimal("1234.56"), handler.parse("1234.56"));
        Assert.assertEquals(new BigDecimal("1234.56"), handler.parse("001234.5600"));
        Assert.assertEquals(new BigDecimal("1234.56"), handler.parse("+1234.56"));
        Assert.assertEquals(new BigDecimal("-1234.56"), handler.parse("-1234.56"));
        Assert.assertEquals(new BigDecimal("1234"), handler.parse("+1234"));
        Assert.assertEquals(new BigDecimal("-1234"), handler.parse("-1234"));
        Assert.assertEquals(new BigDecimal("0.12"), handler.parse(".12"));
        Assert.assertEquals(new BigDecimal("0.12"), handler.parse("+.12"));
        Assert.assertEquals(new BigDecimal("-0.12"), handler.parse("-.12"));
        assertInvalidValue("Invalid decimal value: '12E2'", handler, "12E2");

        Assert.assertEquals("-0.12", handler.toText(new BigDecimal("-0.12")));
    }

    @Test
    public void testXsdDecimalAsDouble()
    {
        DoubleSimpleTypeHandler handler = (DoubleSimpleTypeHandler) context.handler(BuiltInDataTypes.XS_DECIMAL);
        Assert.assertEquals(1234.56, handler.parseDouble("1234.56"), 0.000001);
        Assert.assertEquals(1234.56, handler.parseDouble("001234.5600"), 0.000001);
        Assert.assertEquals(1234.56, handler.parseDouble("+1234.56"), 0.000001);
        Assert.assertEquals(-1234.56, handler.parseDouble("-1234.56"), 0.000001);
        Assert.assertEquals(1234, handler.parseDouble("+1234"), 0.000001);
        Assert.assertEquals(-1234, handler.parseDouble("-1234"), 0.000001);
        Assert.assertEquals(0.12, handler.parseDouble(".12"), 0.000001);
        Assert.assertEquals(0.12, handler.parseDouble("+.12"), 0.000001);
        Assert.assertEquals(-0.12, handler.parseDouble("-.12"), 0.000001);
        assertInvalidValue("Invalid decimal value: '12E2'", handler, "12E2");

        Assert.assertEquals("-0.12", handler.toText(-0.12));
    }

    @Test
    public void testXsdInteger()
    {
        SimpleTypeHandler<BigDecimal> handler = context.handler(BuiltInDataTypes.XS_INTEGER);
        Assert.assertEquals(new BigDecimal("1234"), handler.parse("1234"));
        Assert.assertEquals(new BigDecimal("1234"), handler.parse("001234"));
        Assert.assertEquals(new BigDecimal("1234"), handler.parse("+1234"));
        Assert.assertEquals(new BigDecimal("-1234"), handler.parse("-1234"));
        assertInvalidValue("Invalid integer value: '-.56'", handler, "-.56");
        assertInvalidValue("Invalid integer value: '1234.56'", handler, "1234.56");
        assertInvalidValue("Invalid integer value: '12E2'", handler, "12E2");

        Assert.assertEquals("1234", handler.toText(new BigDecimal("1234")));
    }

    @Test
    public void testXsdIntegerAsLong()
    {
        LongSimpleTypeHandler handler = (LongSimpleTypeHandler) context.handler(BuiltInDataTypes.XS_INTEGER);
        Assert.assertEquals(1234L, handler.parseLong("1234"));
        Assert.assertEquals(1234L, handler.parseLong("001234"));
        Assert.assertEquals(1234L, handler.parseLong("+1234"));
        Assert.assertEquals(-1234L, handler.parseLong("-1234"));
        assertInvalidValue("Invalid integer value: '-.56'", handler, "-.56");
        assertInvalidValue("Invalid integer value: '1234.56'", handler, "1234.56");
        assertInvalidValue("Invalid integer value: '12E2'", handler, "12E2");
        assertInvalidValue("Invalid integer value: '9223372036854775808', (possibly range restricted due to modelling as PURE Integer)", handler, "9223372036854775808");
        assertInvalidValue("Invalid integer value: '-9223372036854775809', (possibly range restricted due to modelling as PURE Integer)", handler, "-9223372036854775809");

        Assert.assertEquals("1234", handler.toText(1234L));
    }

    @Test
    public void testXsdNonPositiveInteger()
    {
        SimpleTypeHandler<BigDecimal> handler = context.handler(BuiltInDataTypes.XS_NON_POSITIVE_INTEGER);
        Assert.assertEquals(BigDecimal.ZERO, handler.parse("0"));
        Assert.assertEquals(new BigDecimal("-5"), handler.parse("-5"));
        assertInvalidValue("Invalid nonPositiveInteger value: '5', above maximum", handler, "5");
    }

    @Test
    public void testXsdNonPositiveIntegerAsLong()
    {
        LongSimpleTypeHandler handler = (LongSimpleTypeHandler) context.handler(BuiltInDataTypes.XS_NON_POSITIVE_INTEGER);
        Assert.assertEquals(0L, handler.parseLong("0"));
        Assert.assertEquals(-5L, handler.parseLong("-5"));
        assertInvalidValue("Invalid nonPositiveInteger value: '5', above maximum", handler, "5");
    }

    @Test
    public void testXsdNegativeInteger()
    {
        SimpleTypeHandler<BigDecimal> handler = context.handler(BuiltInDataTypes.XS_NEGATIVE_INTEGER);
        assertInvalidValue("Invalid negativeInteger value: '0', above maximum", handler, "0");
        Assert.assertEquals(new BigDecimal("-5"), handler.parse("-5"));
        assertInvalidValue("Invalid negativeInteger value: '5', above maximum", handler, "5");
    }

    @Test
    public void testXsdNegativeIntegerAsLong()
    {
        LongSimpleTypeHandler handler = (LongSimpleTypeHandler) context.handler(BuiltInDataTypes.XS_NEGATIVE_INTEGER);
        assertInvalidValue("Invalid negativeInteger value: '0', above maximum", handler, "0");
        Assert.assertEquals(-5L, handler.parseLong("-5"));
        assertInvalidValue("Invalid negativeInteger value: '5', above maximum", handler, "5");
    }

    @Test
    public void testXsdNonNegativeInteger()
    {
        SimpleTypeHandler handler = context.handler(BuiltInDataTypes.XS_NON_NEGATIVE_INTEGER);
        Assert.assertEquals(BigDecimal.ZERO, handler.parse("0"));
        Assert.assertEquals(new BigDecimal("5"), handler.parse("5"));
        assertInvalidValue("Invalid nonNegativeInteger value: '-5', below minimum", handler, "-5");
    }

    @Test
    public void testXsdNonNegativeIntegerAsLong()
    {
        LongSimpleTypeHandler handler = (LongSimpleTypeHandler) context.handler(BuiltInDataTypes.XS_NON_NEGATIVE_INTEGER);
        Assert.assertEquals(0L, handler.parseLong("0"));
        Assert.assertEquals(5L, handler.parseLong("5"));
        assertInvalidValue("Invalid nonNegativeInteger value: '-5', below minimum", handler, "-5");
    }

    @Test
    public void testXsdPositiveInteger()
    {
        SimpleTypeHandler handler = context.handler(BuiltInDataTypes.XS_POSITIVE_INTEGER);
        assertInvalidValue("Invalid positiveInteger value: '0', below minimum", handler, "0");
        Assert.assertEquals(new BigDecimal("5"), handler.parse("5"));
        assertInvalidValue("Invalid positiveInteger value: '-5', below minimum", handler, "-5");
    }

    @Test
    public void testXsdPositiveIntegerAsLong()
    {
        LongSimpleTypeHandler handler = (LongSimpleTypeHandler) context.handler(BuiltInDataTypes.XS_POSITIVE_INTEGER);
        assertInvalidValue("Invalid positiveInteger value: '0', below minimum", handler, "0");
        Assert.assertEquals(5L, handler.parseLong("5"));
        assertInvalidValue("Invalid positiveInteger value: '-5', below minimum", handler, "-5");
    }

    @Test
    public void testXsdLong()
    {
        SimpleTypeHandler<Long> handler = context.handler(BuiltInDataTypes.XS_LONG);
        Assert.assertEquals(2147483648L, handler.parse("2147483648").longValue());
        Assert.assertEquals(1234L, handler.parse("001234").longValue());
        Assert.assertEquals(1234L, handler.parse("+1234").longValue());
        Assert.assertEquals(-1234L, handler.parse("-1234").longValue());
        assertInvalidValue("Invalid long value: '-.56'", handler, "-.56");
        assertInvalidValue("Invalid long value: '1234.56'", handler, "1234.56");
        assertInvalidValue("Invalid long value: '12E2'", handler, "12E2");
        assertInvalidValue("Invalid long value: '9223372036854775808'", handler, "9223372036854775808");
        Assert.assertEquals("1234", handler.toText(1234L));
    }

    @Test
    public void testXsdLongPrimitive()
    {
        LongSimpleTypeHandler handler = (LongSimpleTypeHandler) context.<Long>handler(BuiltInDataTypes.XS_LONG);
        Assert.assertEquals(2147483648L, handler.parseLong("2147483648"));
        Assert.assertEquals(1234L, handler.parseLong("001234"));
        Assert.assertEquals(1234L, handler.parseLong("+1234"));
        Assert.assertEquals(-1234L, handler.parseLong("-1234"));
        assertInvalidValue("Invalid long value: '-.56'", handler, "-.56");
        assertInvalidValue("Invalid long value: '1234.56'", handler, "1234.56");
        assertInvalidValue("Invalid long value: '12E2'", handler, "12E2");
        assertInvalidValue("Invalid long value: '9223372036854775808'", handler, "9223372036854775808");
        Assert.assertEquals("1234", handler.toText(1234L));
    }

    @Test
    public void testXsdInt()
    {
        SimpleTypeHandler<Integer> handler = context.handler(BuiltInDataTypes.XS_INT);
        Assert.assertEquals(1234, handler.parse("1234").intValue());
        Assert.assertEquals(1234, handler.parse("001234").intValue());
        Assert.assertEquals(1234, handler.parse("+1234").intValue());
        Assert.assertEquals(-1234, handler.parse("-1234").intValue());
        assertInvalidValue("Invalid int value: '-.56'", handler, "-.56");
        assertInvalidValue("Invalid int value: '1234.56'", handler, "1234.56");
        assertInvalidValue("Invalid int value: '12E2'", handler, "12E2");
        assertInvalidValue("Invalid int value: '2147483648'", handler, "2147483648");
        Assert.assertEquals("1234", handler.toText(1234));
    }

    @Test
    public void testXsdIntAsLong()
    {
        LongSimpleTypeHandler handler = (LongSimpleTypeHandler) context.<Long>handler(BuiltInDataTypes.XS_INT);
        Assert.assertEquals(1234, handler.parseLong("1234"));
        Assert.assertEquals(1234, handler.parseLong("001234"));
        Assert.assertEquals(1234, handler.parseLong("+1234"));
        Assert.assertEquals(-1234, handler.parseLong("-1234"));
        assertInvalidValue("Invalid int value: '-.56'", handler, "-.56");
        assertInvalidValue("Invalid int value: '1234.56'", handler, "1234.56");
        assertInvalidValue("Invalid int value: '12E2'", handler, "12E2");
        assertInvalidValue("Invalid int value: '2147483648'", handler, "2147483648");
        Assert.assertEquals("1234", handler.toText(1234));
    }

    @Test
    public void testXsdShort()
    {
        SimpleTypeHandler<Short> handler = context.handler(BuiltInDataTypes.XS_SHORT);
        Assert.assertEquals(1234, handler.parse("001234").shortValue());
        assertInvalidValue("Invalid short value: '32768'", handler, "32768");
        Assert.assertEquals("1234", handler.toText((short) 1234));
    }

    @Test
    public void testXsdShortAsLong()
    {
        LongSimpleTypeHandler handler = (LongSimpleTypeHandler) context.<Long>handler(BuiltInDataTypes.XS_SHORT);
        Assert.assertEquals(1234, handler.parseLong("001234"));
        assertInvalidValue("Invalid short value: '32768'", handler, "32768");
        Assert.assertEquals("1234", handler.toText((short) 1234));
    }

    @Test
    public void testXsdByte()
    {
        SimpleTypeHandler<Byte> handler = context.handler(BuiltInDataTypes.XS_BYTE);
        Assert.assertEquals(12, handler.parse("0012").byteValue());
        assertInvalidValue("Invalid byte value: '129'", handler, "129");
        Assert.assertEquals("12", handler.toText((byte) 12));
    }

    @Test
    public void testXsdByteAsLong()
    {
        LongSimpleTypeHandler handler = (LongSimpleTypeHandler) context.<Long>handler(BuiltInDataTypes.XS_BYTE);
        Assert.assertEquals(12, handler.parseLong("0012"));
        assertInvalidValue("Invalid byte value: '129'", handler, "129");
        Assert.assertEquals("12", handler.toText((byte) 12));
    }

    @Test
    public void testXsdDouble()
    {
        SimpleTypeHandler<Double> handler = context.handler(BuiltInDataTypes.XS_DOUBLE);
        Assert.assertEquals(1234.56, handler.parse("1234.56"), 0.000001);
        Assert.assertEquals(1234.56, handler.parse("001234.5600"), 0.000001);
        Assert.assertEquals(1234.56, handler.parse("+1234.56"), 0.000001);
        Assert.assertEquals(-1234.56, handler.parse("-1234.56"), 0.000001);
        Assert.assertEquals(1234, handler.parse("+1234"), 0.000001);
        Assert.assertEquals(-1234, handler.parse("-1234"), 0.000001);
        Assert.assertEquals(0.12, handler.parse(".12"), 0.000001);
        Assert.assertEquals(0.12, handler.parse("+.12"), 0.000001);
        Assert.assertEquals(-0.12, handler.parse("-.12"), 0.000001);
        Assert.assertEquals(12E2, handler.parse("12E2"), 0.000001);
        assertInvalidValue("Invalid double value: 'xyz'", handler, "xyz");
    }

    @Test
    public void testXsdDoubleAsPrimitive()
    {
        DoubleSimpleTypeHandler handler = (DoubleSimpleTypeHandler) context.<Double>handler(BuiltInDataTypes.XS_DOUBLE);
        Assert.assertEquals(1234.56, handler.parseDouble("1234.56"), 0.000001);
        Assert.assertEquals(1234.56, handler.parseDouble("001234.5600"), 0.000001);
        Assert.assertEquals(1234.56, handler.parseDouble("+1234.56"), 0.000001);
        Assert.assertEquals(-1234.56, handler.parseDouble("-1234.56"), 0.000001);
        Assert.assertEquals(1234, handler.parseDouble("+1234"), 0.000001);
        Assert.assertEquals(-1234, handler.parseDouble("-1234"), 0.000001);
        Assert.assertEquals(0.12, handler.parseDouble(".12"), 0.000001);
        Assert.assertEquals(0.12, handler.parseDouble("+.12"), 0.000001);
        Assert.assertEquals(-0.12, handler.parseDouble("-.12"), 0.000001);
        Assert.assertEquals(12E2, handler.parseDouble("12E2"), 0.000001);
        assertInvalidValue("Invalid double value: 'xyz'", handler, "xyz");
    }

    @Test
    public void testXsdFloat()
    {
        DoubleSimpleTypeHandler handler = (DoubleSimpleTypeHandler) context.<Double>handler(BuiltInDataTypes.XS_FLOAT);
        Assert.assertEquals(1234.56F, handler.parseDouble("1234.56"), 0.000001);
        Assert.assertEquals(1234.56F, handler.parseDouble("001234.5600"), 0.000001);
        Assert.assertEquals(1234.56F, handler.parseDouble("+1234.56"), 0.000001);
        Assert.assertEquals(-1234.56F, handler.parseDouble("-1234.56"), 0.000001);
        Assert.assertEquals(1234F, handler.parseDouble("+1234"), 0.000001);
        Assert.assertEquals(-1234F, handler.parseDouble("-1234"), 0.000001);
        Assert.assertEquals(0.12F, handler.parseDouble(".12"), 0.000001);
        Assert.assertEquals(0.12F, handler.parseDouble("+.12"), 0.000001);
        Assert.assertEquals(-0.12F, handler.parseDouble("-.12"), 0.000001);
        Assert.assertEquals(12E2F, handler.parseDouble("12E2"), 0.000001);
        assertInvalidValue("Invalid float value: 'xyz'", handler, "xyz");
    }

    @Test
    public void testXsdMinInclusive()
    {
        QName minIncTen = new QName("min-inc-ten");
        context.defineType(minIncTen, BuiltInDataTypes.XS_INTEGER, FacetType.MIN_INCLUSIVE.of("10"));
        SimpleTypeHandler<BigDecimal> handler = context.handler(minIncTen);

        Assert.assertEquals(new BigDecimal("11"), handler.parse("11"));
        Assert.assertEquals(new BigDecimal("10"), handler.parse("10"));
        assertInvalidValue("Invalid min-inc-ten value: '9', below minimum", handler, "9");
    }

    @Test
    public void testXsdMinExclusive()
    {
        QName minExcTen = new QName("min-exc-ten");
        context.defineType(minExcTen, BuiltInDataTypes.XS_INTEGER, FacetType.MIN_EXCLUSIVE.of("10"));
        SimpleTypeHandler<BigDecimal> handler = context.handler(minExcTen);

        Assert.assertEquals(new BigDecimal("11"), handler.parse("11"));
        assertInvalidValue("Invalid min-exc-ten value: '10', below minimum", handler, "10");
        assertInvalidValue("Invalid min-exc-ten value: '9', below minimum", handler, "9");
    }

    @Test
    public void testXsdMaxInclusive()
    {
        QName maxIncTen = new QName("max-inc-ten");
        context.defineType(maxIncTen, BuiltInDataTypes.XS_INTEGER, FacetType.MAX_INCLUSIVE.of("10"));
        SimpleTypeHandler<BigDecimal> handler = context.handler(maxIncTen);

        assertInvalidValue("Invalid max-inc-ten value: '11', above maximum", handler, "11");
        Assert.assertEquals(new BigDecimal("10"), handler.parse("10"));
        Assert.assertEquals(new BigDecimal("9"), handler.parse("9"));
    }

    @Test
    public void testXsdMaxExclusive()
    {
        QName maxExcTen = new QName("max-exc-ten");
        context.defineType(maxExcTen, BuiltInDataTypes.XS_INTEGER, FacetType.MAX_EXCLUSIVE.of("10"));
        SimpleTypeHandler<BigDecimal> handler = context.handler(maxExcTen);

        assertInvalidValue("Invalid max-exc-ten value: '11', above maximum", handler, "11");
        assertInvalidValue("Invalid max-exc-ten value: '10', above maximum", handler, "10");
        Assert.assertEquals(new BigDecimal("9"), handler.parse("9"));
    }

    @Test
    public void testXsdTotalDigits()
    {
        QName maxTenDigitsInt = new QName("max-ten-digits-int");
        context.defineType(maxTenDigitsInt, BuiltInDataTypes.XS_INTEGER, FacetType.TOTAL_DIGITS.of("10"));
        SimpleTypeHandler<BigDecimal> handlerInt = context.handler(maxTenDigitsInt);

        assertInvalidValue("Invalid max-ten-digits-int value: '12345678901', more than 10 total digits", handlerInt, "12345678901");
        Assert.assertEquals(new BigDecimal("1234567890"), handlerInt.parse("1234567890"));

        QName maxTenDigitsDec = new QName("max-ten-digits-dec");
        context.defineType(maxTenDigitsDec, BuiltInDataTypes.XS_DECIMAL, FacetType.TOTAL_DIGITS.of("10"));
        SimpleTypeHandler<BigDecimal> handlerDec = context.handler(maxTenDigitsDec);

        assertInvalidValue("Invalid max-ten-digits-dec value: '12345678901', more than 10 total digits", handlerDec, "12345678901");
        assertInvalidValue("Invalid max-ten-digits-dec value: '1234567890.1', more than 10 total digits", handlerDec, "1234567890.1");
        assertInvalidValue("Invalid max-ten-digits-dec value: '123456789.01', more than 10 total digits", handlerDec, "123456789.01");
        Assert.assertEquals(new BigDecimal("1234567891"), handlerDec.parse("1234567891"));
        Assert.assertEquals(new BigDecimal("12345678.91"), handlerDec.parse("12345678.91"));
        Assert.assertEquals(new BigDecimal("1234567.891"), handlerDec.parse("1234567.891"));
    }

    @Test
    public void testXsdTotalDigitsAsLong()
    {
        QName maxTenDigitsInt = new QName("max-ten-digits-int");
        context.defineType(maxTenDigitsInt, BuiltInDataTypes.XS_INTEGER, FacetType.TOTAL_DIGITS.of("10"));
        LongSimpleTypeHandler handlerInt = (LongSimpleTypeHandler) context.handler(maxTenDigitsInt);

        assertInvalidValue("Invalid max-ten-digits-int value: '12345678901', more than 10 total digits", handlerInt, "12345678901");
        Assert.assertEquals(1234567890L, handlerInt.parseLong("1234567890"));
    }

    @Test
    public void testXsdTotalDigitsAsDouble()
    {
        QName maxTenDigitsDec = new QName("max-ten-digits-dec");
        context.defineType(maxTenDigitsDec, BuiltInDataTypes.XS_DECIMAL, FacetType.TOTAL_DIGITS.of("10"));
        DoubleSimpleTypeHandler handlerDec = (DoubleSimpleTypeHandler) context.handler(maxTenDigitsDec);

        assertInvalidValue("Invalid max-ten-digits-dec value: '12345678901', more than 10 total digits", handlerDec, "12345678901");
        assertInvalidValue("Invalid max-ten-digits-dec value: '1234567890.1', more than 10 total digits", handlerDec, "1234567890.1");
        assertInvalidValue("Invalid max-ten-digits-dec value: '123456789.01', more than 10 total digits", handlerDec, "123456789.01");
        Assert.assertEquals(1234567891, handlerDec.parseDouble("1234567891"), 0.000001);
        Assert.assertEquals(12345678.91, handlerDec.parseDouble("12345678.91"), 0.000001);
        Assert.assertEquals(1234567.891, handlerDec.parseDouble("1234567.891"), 0.000001);
    }

    @Test
    public void testXsdFractionDigits()
    {
        QName maxTwoFractional = new QName("max-two-fractional");
        context.defineType(maxTwoFractional, BuiltInDataTypes.XS_DECIMAL, FacetType.FRACTION_DIGITS.of("2"));
        SimpleTypeHandler<BigDecimal> handler = context.handler(maxTwoFractional);

        assertInvalidValue("Invalid max-two-fractional value: '1.234', more than 2 fraction digits", handler, "1.234");
        Assert.assertEquals(new BigDecimal("1.23"), handler.parse("1.23"));
        Assert.assertEquals(new BigDecimal("-1.23"), handler.parse("-1.23"));
    }

    @Test
    public void testXsdFractionDigitsAsDouble()
    {
        QName maxTwoFractional = new QName("max-two-fractional");
        context.defineType(maxTwoFractional, BuiltInDataTypes.XS_DECIMAL, FacetType.FRACTION_DIGITS.of("2"));
        DoubleSimpleTypeHandler handler = (DoubleSimpleTypeHandler) context.handler(maxTwoFractional);

        assertInvalidValue("Invalid max-two-fractional value: '1.234', more than 2 fraction digits", handler, "1.234");
        Assert.assertEquals(1.23, handler.parseDouble("1.23"), 0.000001);
        Assert.assertEquals(-1.23, handler.parseDouble("-1.23"), 0.000001);
    }

    @Test
    public void testXsdBoolean()
    {
        SimpleTypeHandler<Boolean> handler = context.handler(BuiltInDataTypes.XS_BOOLEAN);
        Assert.assertEquals(true, handler.parse("true"));
        Assert.assertEquals(true, handler.parse("1"));
        Assert.assertEquals(false, handler.parse("false"));
        Assert.assertEquals(false, handler.parse("0"));
        assertInvalidValue("Invalid boolean value: 'xyz'", handler, "xyz");
        assertInvalidValue("Invalid boolean value: '5'", handler, "5");
        Assert.assertEquals("true", handler.toText(true));
        Assert.assertEquals("false", handler.toText(false));
    }

    @Test
    public void testXsdBooleanPrimitive()
    {
        BooleanSimpleTypeHandler handler = (BooleanSimpleTypeHandler) context.<Boolean>handler(BuiltInDataTypes.XS_BOOLEAN);
        Assert.assertEquals(true, handler.parseBoolean("true"));
        Assert.assertEquals(true, handler.parseBoolean("1"));
        Assert.assertEquals(false, handler.parseBoolean("false"));
        Assert.assertEquals(false, handler.parseBoolean("0"));
        assertInvalidValue("Invalid boolean value: 'xyz'", handler, "xyz");
        assertInvalidValue("Invalid boolean value: '5'", handler, "5");
        Assert.assertEquals("true", handler.toText(true));
        Assert.assertEquals("false", handler.toText(false));
    }

    @Test
    public void testXsdDateTime()
    {
        SimpleTypeHandler<Temporal> handler = context.handler(BuiltInDataTypes.XS_DATE_TIME);
        Assert.assertEquals(Instant.parse("2001-10-26T21:32:52Z"), handler.parse("2001-10-26T21:32:52"));
        Assert.assertEquals(Instant.parse("2001-10-26T19:32:52Z"), handler.parse("2001-10-26T21:32:52+02:00"));
        Assert.assertEquals(Instant.parse("2001-10-26T19:32:52.123Z"), handler.parse("2001-10-26T21:32:52.123+02:00"));
        assertInvalidValue("Invalid dateTime value: '2001-10-26T21:32'", handler, "2001-10-26T21:32");
        Assert.assertEquals("2001-10-26T21:32:52Z", handler.toText(Instant.parse("2001-10-26T21:32:52Z")));
    }

    @Test
    public void testXsdDate()
    {
        SimpleTypeHandler handler = context.handler(BuiltInDataTypes.XS_DATE);
        Assert.assertEquals(LocalDate.parse("2001-10-26"), handler.parse("2001-10-26"));
        Assert.assertEquals(ZonedDateTime.parse("2001-10-26T00:00+02:00"), handler.parse("2001-10-26+02:00"));
        Assert.assertEquals("2001-10-26", handler.toText(LocalDate.parse("2001-10-26")));
        Assert.assertEquals("2001-10-26+02:00", handler.toText(ZonedDateTime.parse("2001-10-26T00:00+02:00")));
        assertInvalidValue("Invalid date value: '2001-10'", handler, "2001-10");
    }

    @Test
    public void testList()
    {
        QName longList = new QName("long-list");
        context.defineListType(longList, BuiltInDataTypes.XS_LONG);
        SimpleTypeHandler handler = context.handler(longList);

        Assert.assertEquals(Arrays.asList(1L, 2L, 3L), handler.parse("1  2 3"));
        Assert.assertEquals("1 2 3", handler.toText(Arrays.asList(1L, 2L, 3L)));
    }

    @Test
    public void testUnion()
    {
        QName longOrDate = new QName("long-or-date");
        context.defineUnionType(longOrDate, Arrays.asList(BuiltInDataTypes.XS_LONG, BuiltInDataTypes.XS_DATE));
        SimpleTypeHandler handler = context.handler(longOrDate);

        Assert.assertEquals(LocalDate.parse("2001-10-26"), handler.parse("2001-10-26"));
        Assert.assertEquals(3L, handler.parse("3"));
        Assert.assertEquals("2001-10-26", handler.toText(LocalDate.parse("2001-10-26")));
        Assert.assertEquals("3", handler.toText(3L));
    }

    private NamespaceContext getNamespaces()
    {
        return namespaces;
    }

    private void setNamespaces(Map<String, String> namespaces)
    {
        this.namespaces = new NamespaceContext()
        {
            @Override
            public String getNamespaceURI(String prefix)
            {
                return namespaces.containsKey(prefix)
                       ? namespaces.get(prefix)
                       : XMLConstants.NULL_NS_URI;
            }

            @Override
            public String getPrefix(String namespaceURI)
            {
                Iterator<String> prefixes = getPrefixes(namespaceURI);
                return prefixes.hasNext()
                       ? prefixes.next()
                       : null;
            }

            @Override
            public Iterator<String> getPrefixes(String namespaceURI)
            {
                return namespaces.entrySet()
                                 .stream()
                                 .filter(e -> namespaceURI.equals(e.getValue()))
                                 .map(Map.Entry::getKey)
                                 .iterator();
            }
        };
    }

    private void assertInvalidValue(String expectedMessage, SimpleTypeHandler converter, String value)
    {
        try
        {
            converter.parse(value);
            Assert.fail("Should throw IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    private void assertInvalidValue(String expectedMessage, BooleanSimpleTypeHandler converter, String value)
    {
        try
        {
            converter.parseBoolean(value);
            Assert.fail("Should throw IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    private void assertInvalidValue(String expectedMessage, LongSimpleTypeHandler converter, String value)
    {
        try
        {
            converter.parseLong(value);
            Assert.fail("Should throw IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    private void assertInvalidValue(String expectedMessage, DoubleSimpleTypeHandler converter, String value)
    {
        try
        {
            converter.parseDouble(value);
            Assert.fail("Should throw IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }
}
