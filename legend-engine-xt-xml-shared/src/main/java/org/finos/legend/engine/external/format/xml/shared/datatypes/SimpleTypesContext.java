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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.utility.ListIterate;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SimpleTypesContext
{
    public static final BigDecimal BIG_DECIMAL_DOUBLE_MAX = BigDecimal.valueOf(Double.MAX_VALUE);
    public static final BigDecimal BIG_DECIMAL_DOUBLE_MIN = BigDecimal.valueOf(Double.MIN_VALUE);
    private final Supplier<NamespaceContext> namespacesSupplier;
    private MutableMap<QName, SimpleTypeHandler<?>> definedTypes = Maps.mutable.empty();
    // TODO Other timezones
    private ZoneOffset defaultTimezone = ZoneOffset.UTC;

    public SimpleTypesContext(Supplier<NamespaceContext> namespacesSupplier)
    {
        this.namespacesSupplier = namespacesSupplier;
        registerBuiltInTypes();
    }

    public <T> SimpleTypeHandler<T> handler(QName name)
    {
        if (definedTypes.containsKey(name))
        {
            return (SimpleTypeHandler<T>) definedTypes.get(name);
        }
        else
        {
            throw new IllegalArgumentException("Unknown datatype: " + name);
        }
    }

    public <T> SimpleTypeHandler<T> defineType(QName base, Facet... facets)
    {
        return defineType(new QName("anon", UUID.randomUUID().toString()), base, Arrays.asList(facets));
    }

    public <T> SimpleTypeHandler<T> defineType(QName name, QName base, Facet... facets)
    {
        return defineType(name, base, Arrays.asList(facets));
    }

    public <T> SimpleTypeHandler<T> defineType(QName base, List<Facet> facets)
    {
        return defineType(new QName("anon", UUID.randomUUID().toString()), base, facets);
    }

    public <T> SimpleTypeHandler<T> defineType(QName name, QName base, List<Facet> facets)
    {
        if (definedTypes.containsKey(name))
        {
            throw new IllegalArgumentException("Type already defined: " + name);
        }

        return register(((Handler<T>) handler(base)).derive(name, facets));
    }

    public <T> SimpleTypeHandler<List<T>> defineListType(QName itemType, Facet... facets)
    {
        return defineListType(new QName("anon", UUID.randomUUID().toString()), itemType, Arrays.asList(facets));
    }

    public <T> SimpleTypeHandler<List<T>> defineListType(QName name, QName itemType, Facet... facets)
    {
        return defineListType(name, itemType, Arrays.asList(facets));
    }

    public <T> SimpleTypeHandler<List<T>> defineListType(QName name, QName itemType, List<Facet> facets)
    {
        if (definedTypes.containsKey(name))
        {
            throw new IllegalArgumentException("Type already defined: " + name);
        }

        Handler<T> itemHandler = ((Handler<T>) handler(itemType)).derive(name, facets);
        return register(new ListHandler<T>(name, itemHandler));
    }

    public <T> SimpleTypeHandler<T> defineUnionType(List<QName> memberTypes, Facet... facets)
    {
        return defineUnionType(new QName("anon", UUID.randomUUID().toString()), memberTypes, Arrays.asList(facets));
    }

    public <T> SimpleTypeHandler<T> defineUnionType(QName name, List<QName> memberTypes, Facet... facets)
    {
        return defineUnionType(name, memberTypes, Arrays.asList(facets));
    }

    public <T> SimpleTypeHandler<T> defineUnionType(QName name, List<QName> memberTypes, List<Facet> facets)
    {
        if (definedTypes.containsKey(name))
        {
            throw new IllegalArgumentException("Type already defined: " + name);
        }

        List<Handler<T>> memberHandlers = ListIterate.collect(memberTypes, t -> ((Handler<T>) handler(t)).derive(name, facets));
        return register(new UnionHandler<T>(name, memberHandlers));
    }

    private void registerBuiltInTypes()
    {
        Handler<String> xsString = (Handler<String>) register(new StringHandler(BuiltInDataTypes.XS_STRING));
        register(xsString.derive(BuiltInDataTypes.XS_NORMALIZED_STRING, FacetType.WHITESPACE.of("replace")));
        Handler<String> xsToken = (Handler<String>) register(xsString.derive(BuiltInDataTypes.XS_TOKEN, FacetType.WHITESPACE.of("collapse")));

        register(xsToken.derive(BuiltInDataTypes.XS_NAME, FacetType.PATTERN.of("[\\p{L}_][\\p{L}\\d_\\-\\.:]*")));
        register(xsToken.derive(BuiltInDataTypes.XS_NCNAME, FacetType.PATTERN.of("[\\p{L}_][\\p{L}\\d_\\-\\.]*")));
        register(new QNameHandler(BuiltInDataTypes.XS_QNAME));

        Handler<BigDecimal> xsInteger = (Handler<BigDecimal>) register(new IntegerHandler(BuiltInDataTypes.XS_INTEGER));
        register(xsInteger.derive(BuiltInDataTypes.XS_NON_NEGATIVE_INTEGER, FacetType.MIN_INCLUSIVE.of("0")));
        register(xsInteger.derive(BuiltInDataTypes.XS_NON_POSITIVE_INTEGER, FacetType.MAX_INCLUSIVE.of("0")));
        register(xsInteger.derive(BuiltInDataTypes.XS_POSITIVE_INTEGER, FacetType.MIN_EXCLUSIVE.of("0")));
        register(xsInteger.derive(BuiltInDataTypes.XS_NEGATIVE_INTEGER, FacetType.MAX_EXCLUSIVE.of("0")));
        register(new IntHandler(BuiltInDataTypes.XS_INT));
        register(new LongHandler(BuiltInDataTypes.XS_LONG));
        register(new ShortHandler(BuiltInDataTypes.XS_SHORT));
        register(new ByteHandler(BuiltInDataTypes.XS_BYTE));
        register(new DoubleHandler(BuiltInDataTypes.XS_DOUBLE));
        register(new FloatHandler(BuiltInDataTypes.XS_FLOAT));
        register(new DecimalHandler(BuiltInDataTypes.XS_DECIMAL));

        register(new BooleanHandler(BuiltInDataTypes.XS_BOOLEAN));

        register(new DateHandler(BuiltInDataTypes.XS_DATE));
        register(new DateTimeHandler(BuiltInDataTypes.XS_DATE_TIME));
    }

    private <T> SimpleTypeHandler<T> register(SimpleTypeHandler<T> handler)
    {
        definedTypes.put(handler.getName(), handler);
        return handler;
    }

    private abstract class Handler<T> implements SimpleTypeHandler<T>
    {
        private final QName name;
        XsdWhiteSpaceType whiteSpaceType;
        final List<Facet> facets = Lists.mutable.empty();
        Set<T> enumerations;
        private List<Pattern> patterns;
        private Long length;
        private Long minLength;
        private Long maxLength;
        T minInclusive;
        T minExclusive;
        T maxInclusive;
        T maxExclusive;
        private Long totalDigits;
        private Long fractionDigits;

        private Handler(QName name)
        {
            this(name, XsdWhiteSpaceType.PRESERVE);
        }

        private Handler(QName name, XsdWhiteSpaceType whiteSpaceType)
        {
            this.name = name;
            this.whiteSpaceType = whiteSpaceType;
        }

        private Handler(QName name, Handler<T> base, List<Facet> facets)
        {
            whiteSpaceType = base.whiteSpaceType;
            this.name = name;
            base.facets.forEach(this::addFacet);
            facets.forEach(this::addFacet);
        }

        public Handler<T> derive(QName deriviativeName, Facet... facets)
        {
            return derive(deriviativeName, Arrays.asList(facets));
        }

        public abstract Handler<T> derive(QName deriviativeName, List<Facet> facets);

        @Override
        public QName getName()
        {
            return name;
        }

        @Override
        public T parse(String text)
        {
            if (text == null)
            {
                return null;
            }
            else
            {
                String whiteSpaceProcessed = doPreParse(text);
                T parsed = doParse(whiteSpaceProcessed);
                checkEnumerations(parsed);
                checkMinAndMaxValues(parsed);
                return parsed;
            }
        }

        protected String doPreParse(String text)
        {
            String whiteSpaceProcessed = text;
            if (whiteSpaceType != XsdWhiteSpaceType.PRESERVE)
            {
                whiteSpaceProcessed = whiteSpaceProcessed.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
            }
            if (whiteSpaceType == XsdWhiteSpaceType.COLLAPSE)
            {
                whiteSpaceProcessed = whiteSpaceProcessed.trim().replaceAll(" +", " ");
            }
            checkLength(whiteSpaceProcessed);
            checkPattern(whiteSpaceProcessed);
            checkDigits(whiteSpaceProcessed);
            return whiteSpaceProcessed;
        }

        abstract T doParse(String text);

        void addFacet(Facet facet)
        {
            this.facets.add(facet);
            if (facet.getType() == FacetType.WHITESPACE)
            {
                whiteSpaceType = XsdWhiteSpaceType.valueOf(facet.getValue().toUpperCase());
                if (whiteSpaceType.ordinal() < this.whiteSpaceType.ordinal())
                {
                    throw new IllegalArgumentException("Whitespace behaviour cannot be relaxed");
                }
            }
            else if (facet.getType() == FacetType.ENUMERATION)
            {
                if (enumerations == null)
                {
                    enumerations = new LinkedHashSet<>();
                }
                try
                {
                    enumerations.add(doParse(facet.getValue()));
                }
                catch (Exception e)
                {
                    throw new IllegalArgumentException("Invalid value specified for enumeration: '" + facet.getValue() + "'");
                }
            }
            else if (facet.getType() == FacetType.PATTERN)
            {
                if (patterns == null)
                {
                    patterns = new ArrayList<>();
                }
                // TODO Full compatibility with XSD RegExps
                patterns.add(Pattern.compile(facet.getValue()));
            }
            else if (facet.getType() == FacetType.LENGTH)
            {
                length = Long.parseLong(facet.getValue());
            }
            else if (facet.getType() == FacetType.MIN_LENGTH)
            {
                minLength = Long.parseLong(facet.getValue());
            }
            else if (facet.getType() == FacetType.MAX_LENGTH)
            {
                maxLength = Long.parseLong(facet.getValue());
            }
            else if (facet.getType() == FacetType.MIN_INCLUSIVE)
            {
                T min = doParse(facet.getValue());
                if (minInclusive != null || minExclusive != null)
                {
                    throw new IllegalArgumentException("Minimum value cannot be changed");
                }
                minInclusive = min;
            }
            else if (facet.getType() == FacetType.MIN_EXCLUSIVE)
            {
                T min = doParse(facet.getValue());
                if (minInclusive != null || minExclusive != null)
                {
                    throw new IllegalArgumentException("Minimum value cannot be changed");
                }
                minExclusive = min;
            }
            else if (facet.getType() == FacetType.MAX_INCLUSIVE)
            {
                T max = doParse(facet.getValue());
                if (maxInclusive != null || maxExclusive != null)
                {
                    throw new IllegalArgumentException("Maximum value cannot be changed");
                }
                maxInclusive = max;
            }
            else if (facet.getType() == FacetType.MAX_EXCLUSIVE)
            {
                T max = doParse(facet.getValue());
                if (maxInclusive != null || maxExclusive != null)
                {
                    throw new IllegalArgumentException("Maximum value cannot be changed");
                }
                maxExclusive = max;
            }
            else if (facet.getType() == FacetType.TOTAL_DIGITS)
            {
                totalDigits = Long.parseLong(facet.getValue());
            }
            else if (facet.getType() == FacetType.FRACTION_DIGITS)
            {
                fractionDigits = Long.parseLong(facet.getValue());
            }
        }

        private void checkPattern(String text)
        {
            checkTextValue(t -> patterns == null || patterns.stream()
                    .allMatch(p -> p.matcher(t)
                            .matches()), text, () -> "does not match expected pattern");
        }

        void checkEnumerations(T value)
        {
            if (enumerations != null)
            {
                checkParsedValue(v -> enumerations.contains(v), value,
                        () -> "expected one of: " + enumerations.stream()
                                .map(Object::toString)
                                .collect(Collectors.joining(",")));
            }
        }

        private void checkLength(String text)
        {
            checkTextValue(t -> length == null || t.length() == length, text, () -> "expected exactly " + length + " characters");
            checkTextValue(t -> minLength == null || maxLength == null || (t.length() >= minLength && t.length() <= maxLength), text, () -> "expected minimum of " + minLength + " characters and maximum of " + maxLength);
            checkTextValue(t -> minLength == null || t.length() >= minLength, text, () -> "expected minimum of " + minLength + " characters");
            checkTextValue(t -> maxLength == null || t.length() <= maxLength, text, () -> "expected maximum of " + maxLength + " characters");
        }

        private void checkDigits(String text)
        {
            if (totalDigits != null || fractionDigits != null)
            {
                int digits = 0;
                int fractional = 0;
                boolean decimalPointSeen = false;
                for (char ch : text.toCharArray())
                {
                    if (ch == '.')
                    {
                        decimalPointSeen = true;
                    }
                    else if (Character.isDigit(ch))
                    {
                        digits++;
                        if (decimalPointSeen)
                        {
                            fractional++;
                        }
                    }
                }
                if (totalDigits != null && digits > totalDigits)
                {
                    throw error(text, () -> "more than " + totalDigits + " total digits");
                }
                if (fractionDigits != null && fractional > fractionDigits)
                {
                    throw error(text, () -> "more than " + fractionDigits + " fraction digits");
                }
            }
        }

        abstract void checkMinAndMaxValues(T value);

        private void checkTextValue(Predicate<String> predicate, String value, Supplier<String> extraMessage)
        {
            if (!predicate.test(value))
            {
                throw error(value, extraMessage);
            }
        }

        void checkParsedValue(Predicate<T> predicate, T value, Supplier<String> extraMessage)
        {
            if (!predicate.test(value))
            {
                throw error(toText(value), extraMessage);
            }
        }

        protected IllegalArgumentException error(String value)
        {
            return new IllegalArgumentException("Invalid " + typeName() + " value: '" + value + "'");
        }

        protected IllegalArgumentException error(String value, Supplier<String> extraMessage)
        {
            String extra = extraMessage.get();
            return new IllegalArgumentException(
                    "Invalid " + typeName() + " value: '" + value + "'"
                            + (extra.isEmpty() ? "" : ", " + extra));
        }

        private String typeName()
        {
            return name.getLocalPart();
        }
    }

    private class StringHandler extends Handler<String>
    {
        private StringHandler(QName name)
        {
            super(name, XsdWhiteSpaceType.PRESERVE);
        }

        @Override
        protected String doParse(String text)
        {
            return text;
        }

        @Override
        public String toText(String value)
        {
            return value;
        }

        @Override
        void checkMinAndMaxValues(String value)
        {
            // Not applicable for Strings
        }

        @Override
        public Handler<String> derive(QName deriviativeName, List<Facet> facets)
        {
            StringHandler result = new StringHandler(deriviativeName);
            this.facets.forEach(result::addFacet);
            facets.forEach(result::addFacet);
            return result;
        }
    }

    private class QNameHandler extends Handler<QName>
    {
        private SimpleTypeHandler<String> ncNameHandler;

        private QNameHandler(QName name)
        {
            super(name, XsdWhiteSpaceType.REPLACE);
            this.ncNameHandler = SimpleTypesContext.this.<String>handler(BuiltInDataTypes.XS_NCNAME).derive(name);
        }

        @Override
        protected QName doParse(String text)
        {
            if (text.contains(":"))
            {
                String[] parts = text.split(":");
                if (parts.length != 2)
                {
                    throw error(text, () -> "should only contain one ':");
                }
                String prefix = ncNameHandler.parse(parts[0]);
                String localPart = ncNameHandler.parse(parts[1]);
                String uri = namespacesSupplier.get().getNamespaceURI(prefix);
                if (uri == null || uri.equals(XMLConstants.NULL_NS_URI))
                {
                    throw error(text, () -> "Prefix " + prefix + " unknown");
                }
                return new QName(uri, localPart, prefix);
            }
            else
            {
                String localPart = ncNameHandler.parse(text);
                String uri = namespacesSupplier.get().getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX);
                return new QName(uri, localPart);
            }
        }

        @Override
        public String toText(QName value)
        {
            String prefix = namespacesSupplier.get().getPrefix(value.getNamespaceURI());
            return (prefix.isEmpty()
                    ? ""
                    : prefix + ":") + value.getLocalPart();
        }

        @Override
        void checkMinAndMaxValues(QName value)
        {
            // Not applicable for QNames
        }

        @Override
        public Handler<QName> derive(QName deriviativeName, List<Facet> facets)
        {
            QNameHandler result = new QNameHandler(deriviativeName);
            this.facets.forEach(result::addFacet);
            facets.forEach(result::addFacet);
            return result;
        }
    }

    private class BooleanHandler extends Handler<Boolean> implements BooleanSimpleTypeHandler
    {
        private BooleanHandler(QName name)
        {
            super(name, XsdWhiteSpaceType.REPLACE);
        }

        @Override
        protected Boolean doParse(String text)
        {
            return doParseBoolean(text);
        }

        @Override
        public boolean parseBoolean(String text)
        {
            String whiteSpaceProcessed = doPreParse(text);
            boolean parsed = doParseBoolean(whiteSpaceProcessed);
            checkEnumerations(parsed);
            return parsed;
        }

        private boolean doParseBoolean(String text)
        {
            if (text.equals("true") || text.equals("1"))
            {
                return true;
            }
            else if (text.equals("false") || text.equals("0"))
            {
                return false;
            }
            else
            {
                throw error(text);
            }
        }

        @Override
        public String toText(Boolean value)
        {
            return String.valueOf(value);
        }

        @Override
        public String toText(boolean value)
        {
            return String.valueOf(value);
        }

        @Override
        void checkMinAndMaxValues(Boolean value)
        {
            // Not Applicable for booleans
        }

        @Override
        public Handler<Boolean> derive(QName deriviativeName, List<Facet> facets)
        {
            BooleanHandler result = new BooleanHandler(deriviativeName);
            this.facets.forEach(result::addFacet);
            facets.forEach(result::addFacet);
            return result;
        }
    }

    private class LongHandler extends Handler<Long> implements LongSimpleTypeHandler
    {
        private LongHandler(QName name)
        {
            super(name, XsdWhiteSpaceType.REPLACE);
        }

        @Override
        protected Long doParse(String text)
        {
            return doParseLong(text);
        }

        @Override
        public long parseLong(String text)
        {
            String whiteSpaceProcessed = doPreParse(text);
            long parsed = doParseLong(whiteSpaceProcessed);
            checkEnumerations(parsed);
            if (minInclusive != null || minExclusive != null || maxInclusive != null || maxExclusive != null)
            {
                checkMinAndMaxValues(parsed);
            }
            return parsed;
        }

        private long doParseLong(String text)
        {
            try
            {
                return Long.parseLong(text);
            }
            catch (Exception e)
            {
                throw error(text);
            }
        }

        @Override
        public String toText(Long value)
        {
            return String.valueOf(value);
        }

        @Override
        public String toText(long value)
        {
            return String.valueOf(value);
        }

        @Override
        void checkMinAndMaxValues(Long value)
        {
            checkParsedValue(v -> minInclusive == null || value >= minInclusive, value, () -> "below minimum");
            checkParsedValue(v -> minExclusive == null || value > minExclusive, value, () -> "below minimum");
            checkParsedValue(v -> maxInclusive == null || value <= maxInclusive, value, () -> "above maximum");
            checkParsedValue(v -> maxExclusive == null || value < maxExclusive, value, () -> "above maximum");
        }

        @Override
        public Handler<Long> derive(QName deriviativeName, List<Facet> facets)
        {
            LongHandler result = new LongHandler(deriviativeName);
            this.facets.forEach(result::addFacet);
            facets.forEach(result::addFacet);
            return result;
        }
    }

    private class IntHandler extends Handler<Integer> implements LongSimpleTypeHandler
    {
        private IntHandler(QName name)
        {
            super(name, XsdWhiteSpaceType.REPLACE);
        }

        @Override
        protected Integer doParse(String text)
        {
            return (int) doParseLong(text);
        }

        @Override
        public long parseLong(String text)
        {
            String whiteSpaceProcessed = doPreParse(text);
            long parsed = doParseLong(whiteSpaceProcessed);
            if (enumerations != null)
            {
                checkEnumerations((int) parsed);
            }
            if (minInclusive != null || minExclusive != null || maxInclusive != null || maxExclusive != null)
            {
                checkMinAndMaxValues((int) parsed);
            }
            return parsed;
        }

        private long doParseLong(String text)
        {
            try
            {
                return Integer.parseInt(text);
            }
            catch (Exception e)
            {
                throw error(text);
            }
        }

        @Override
        public String toText(Integer value)
        {
            return String.valueOf(value);
        }

        @Override
        public String toText(long value)
        {
            return String.valueOf(value);
        }

        @Override
        void checkMinAndMaxValues(Integer value)
        {
            checkParsedValue(v -> minInclusive == null || value >= minInclusive, value, () -> "below minimum");
            checkParsedValue(v -> minExclusive == null || value > minExclusive, value, () -> "below minimum");
            checkParsedValue(v -> maxInclusive == null || value <= maxInclusive, value, () -> "above maximum");
            checkParsedValue(v -> maxExclusive == null || value < maxExclusive, value, () -> "above maximum");
        }

        @Override
        public Handler<Integer> derive(QName deriviativeName, List<Facet> facets)
        {
            IntHandler result = new IntHandler(deriviativeName);
            this.facets.forEach(result::addFacet);
            facets.forEach(result::addFacet);
            return result;
        }
    }

    private class ShortHandler extends Handler<Short> implements LongSimpleTypeHandler
    {
        private ShortHandler(QName name)
        {
            super(name, XsdWhiteSpaceType.REPLACE);
        }

        @Override
        protected Short doParse(String text)
        {
            return (short) doParseLong(text);
        }

        @Override
        public long parseLong(String text)
        {
            String whiteSpaceProcessed = doPreParse(text);
            long parsed = doParseLong(whiteSpaceProcessed);
            if (enumerations != null)
            {
                checkEnumerations((short) parsed);
            }
            if (minInclusive != null || minExclusive != null || maxInclusive != null || maxExclusive != null)
            {
                checkMinAndMaxValues((short) parsed);
            }
            return parsed;
        }

        private long doParseLong(String text)
        {
            try
            {
                return Short.parseShort(text);
            }
            catch (Exception e)
            {
                throw error(text);
            }
        }

        @Override
        public String toText(Short value)
        {
            return String.valueOf(value);
        }

        @Override
        public String toText(long value)
        {
            return String.valueOf(value);
        }

        @Override
        void checkMinAndMaxValues(Short value)
        {
            checkParsedValue(v -> minInclusive == null || value >= minInclusive, value, () -> "below minimum");
            checkParsedValue(v -> minExclusive == null || value > minExclusive, value, () -> "below minimum");
            checkParsedValue(v -> maxInclusive == null || value <= maxInclusive, value, () -> "above maximum");
            checkParsedValue(v -> maxExclusive == null || value < maxExclusive, value, () -> "above maximum");
        }

        @Override
        public Handler<Short> derive(QName deriviativeName, List<Facet> facets)
        {
            ShortHandler result = new ShortHandler(deriviativeName);
            this.facets.forEach(result::addFacet);
            facets.forEach(result::addFacet);
            return result;
        }
    }

    private class ByteHandler extends Handler<Byte> implements LongSimpleTypeHandler
    {
        private ByteHandler(QName name)
        {
            super(name, XsdWhiteSpaceType.REPLACE);
        }

        @Override
        protected Byte doParse(String text)
        {
            return (byte) doParseLong(text);
        }

        @Override
        public long parseLong(String text)
        {
            String whiteSpaceProcessed = doPreParse(text);
            long parsed = doParseLong(whiteSpaceProcessed);
            if (enumerations != null)
            {
                checkEnumerations((byte) parsed);
            }
            if (minInclusive != null || minExclusive != null || maxInclusive != null || maxExclusive != null)
            {
                checkMinAndMaxValues((byte) parsed);
            }
            return parsed;
        }

        private long doParseLong(String text)
        {
            try
            {
                return Byte.parseByte(text);
            }
            catch (Exception e)
            {
                throw error(text);
            }
        }

        @Override
        public String toText(Byte value)
        {
            return String.valueOf(value);
        }

        @Override
        public String toText(long value)
        {
            return String.valueOf(value);
        }

        @Override
        void checkMinAndMaxValues(Byte value)
        {
            checkParsedValue(v -> minInclusive == null || value >= minInclusive, value, () -> "below minimum");
            checkParsedValue(v -> minExclusive == null || value > minExclusive, value, () -> "below minimum");
            checkParsedValue(v -> maxInclusive == null || value <= maxInclusive, value, () -> "above maximum");
            checkParsedValue(v -> maxExclusive == null || value < maxExclusive, value, () -> "above maximum");
        }

        @Override
        public Handler<Byte> derive(QName deriviativeName, List<Facet> facets)
        {
            ByteHandler result = new ByteHandler(deriviativeName);
            this.facets.forEach(result::addFacet);
            facets.forEach(result::addFacet);
            return result;
        }
    }

    private class DoubleHandler extends Handler<Double> implements DoubleSimpleTypeHandler
    {
        private DoubleHandler(QName name)
        {
            super(name, XsdWhiteSpaceType.REPLACE);
        }

        @Override
        protected Double doParse(String text)
        {
            return doParseDouble(text);
        }

        @Override
        public double parseDouble(String text)
        {
            String whiteSpaceProcessed = doPreParse(text);
            double parsed = doParseDouble(whiteSpaceProcessed);
            checkEnumerations(parsed);
            if (minInclusive != null || minExclusive != null || maxInclusive != null || maxExclusive != null)
            {
                checkMinAndMaxValues(parsed);
            }
            return parsed;
        }

        private double doParseDouble(String text)
        {
            try
            {
                return Double.parseDouble(text);
            }
            catch (Exception e)
            {
                throw error(text);
            }
        }

        @Override
        public String toText(Double value)
        {
            return String.valueOf(value);
        }

        @Override
        public String toText(double value)
        {
            return String.valueOf(value);
        }

        @Override
        void checkMinAndMaxValues(Double value)
        {
            checkParsedValue(v -> minInclusive == null || value >= minInclusive.longValue(), value, () -> "below minimum");
            checkParsedValue(v -> minExclusive == null || value > minExclusive.longValue(), value, () -> "below minimum");
            checkParsedValue(v -> maxInclusive == null || value <= maxInclusive.longValue(), value, () -> "above maximum");
            checkParsedValue(v -> maxExclusive == null || value < maxExclusive.longValue(), value, () -> "above maximum");
        }

        @Override
        public Handler<Double> derive(QName deriviativeName, List<Facet> facets)
        {
            DoubleHandler result = new DoubleHandler(deriviativeName);
            this.facets.forEach(result::addFacet);
            facets.forEach(result::addFacet);
            return result;
        }
    }

    private class FloatHandler extends Handler<Float> implements DoubleSimpleTypeHandler
    {
        private FloatHandler(QName name)
        {
            super(name, XsdWhiteSpaceType.REPLACE);
        }

        @Override
        protected Float doParse(String text)
        {
            return (float) doParseDouble(text);
        }

        @Override
        public double parseDouble(String text)
        {
            String whiteSpaceProcessed = doPreParse(text);
            double parsed = doParseDouble(whiteSpaceProcessed);
            if (enumerations != null)
            {
                checkEnumerations((float) parsed);
            }
            if (minInclusive != null || minExclusive != null || maxInclusive != null || maxExclusive != null)
            {
                checkMinAndMaxValues((float) parsed);
            }
            return parsed;
        }

        private double doParseDouble(String text)
        {
            try
            {
                return Float.parseFloat(text);
            }
            catch (Exception e)
            {
                throw error(text);
            }
        }

        @Override
        public String toText(Float value)
        {
            return String.valueOf(value);
        }

        @Override
        public String toText(double value)
        {
            return String.valueOf(value);
        }

        @Override
        void checkMinAndMaxValues(Float value)
        {
            checkParsedValue(v -> minInclusive == null || value >= minInclusive, value, () -> "below minimum");
            checkParsedValue(v -> minExclusive == null || value > minExclusive, value, () -> "below minimum");
            checkParsedValue(v -> maxInclusive == null || value <= maxInclusive, value, () -> "above maximum");
            checkParsedValue(v -> maxExclusive == null || value < maxExclusive, value, () -> "above maximum");
        }

        @Override
        public Handler<Float> derive(QName deriviativeName, List<Facet> facets)
        {
            FloatHandler result = new FloatHandler(deriviativeName);
            this.facets.forEach(result::addFacet);
            facets.forEach(result::addFacet);
            return result;
        }
    }

    private class IntegerHandler extends Handler<BigDecimal> implements LongSimpleTypeHandler
    {
        private final Pattern INTEGER_PATTERN = Pattern.compile("[+-]?\\d*");

        private Long minInclusiveLong;
        private Long minExclusiveLong;
        private Long maxInclusiveLong;
        private Long maxExclusiveLong;

        private IntegerHandler(QName name)
        {
            super(name, XsdWhiteSpaceType.REPLACE);
        }

        @Override
        protected BigDecimal doParse(String text)
        {
            if (!INTEGER_PATTERN.matcher(text).matches())
            {
                throw error(text);
            }
            return new BigDecimal(text);
        }

        @Override
        public long parseLong(String text)
        {
            String whiteSpaceProcessed = doPreParse(text);
            if (!INTEGER_PATTERN.matcher(whiteSpaceProcessed).matches())
            {
                throw error(whiteSpaceProcessed);
            }
            long parsed;
            try
            {
                parsed = Long.parseLong(whiteSpaceProcessed);
            }
            catch (Exception e)
            {
                throw error(whiteSpaceProcessed, () -> "(possibly range restricted due to modelling as PURE Integer)");
            }
            if (enumerations != null)
            {
                checkEnumerations(new BigDecimal(parsed));
            }
            if (minInclusiveLong != null && parsed < minInclusiveLong)
            {
                throw error(whiteSpaceProcessed, () -> "below minimum");
            }
            if (minExclusiveLong != null && parsed <= minExclusiveLong)
            {
                throw error(whiteSpaceProcessed, () -> "below minimum");
            }
            if (maxInclusiveLong != null && parsed > maxInclusiveLong)
            {
                throw error(whiteSpaceProcessed, () -> "above maximum");
            }
            if (maxExclusiveLong != null && parsed >= maxExclusiveLong)
            {
                throw error(whiteSpaceProcessed, () -> "above maximum");
            }
            return parsed;
        }

        @Override
        void addFacet(Facet facet)
        {
            super.addFacet(facet);
            if (facet.getType() == FacetType.MIN_INCLUSIVE)
            {
                if (minInclusive.compareTo(new BigDecimal(Long.MIN_VALUE)) < 0)
                {
                    minInclusiveLong = null;
                    minExclusiveLong = null;
                }
                else if (minInclusive.compareTo(new BigDecimal(Long.MAX_VALUE)) > 0)
                {
                    minInclusiveLong = null;
                    minExclusiveLong = Long.MAX_VALUE;
                }
                else
                {
                    minInclusiveLong = minInclusive.longValue();
                    minExclusiveLong = null;
                }
            }
            else if (facet.getType() == FacetType.MIN_EXCLUSIVE)
            {
                if (minExclusive.compareTo(new BigDecimal(Long.MIN_VALUE)) < 0)
                {
                    minInclusiveLong = null;
                    minExclusiveLong = null;
                }
                else if (minExclusive.compareTo(new BigDecimal(Long.MAX_VALUE)) > 0)
                {
                    minInclusiveLong = null;
                    minExclusiveLong = Long.MAX_VALUE;
                }
                else
                {
                    minInclusiveLong = null;
                    minExclusiveLong = minExclusive.longValue();
                }
            }
            else if (facet.getType() == FacetType.MAX_INCLUSIVE)
            {
                if (maxInclusive.compareTo(new BigDecimal(Long.MIN_VALUE)) < 0)
                {
                    maxInclusiveLong = null;
                    maxExclusiveLong = Long.MIN_VALUE;
                }
                else if (maxInclusive.compareTo(new BigDecimal(Long.MAX_VALUE)) > 0)
                {
                    maxInclusiveLong = null;
                    maxExclusiveLong = null;
                }
                else
                {
                    maxInclusiveLong = maxInclusive.longValue();
                    maxExclusiveLong = null;
                }
            }
            else if (facet.getType() == FacetType.MAX_EXCLUSIVE)
            {
                if (maxExclusive.compareTo(new BigDecimal(Long.MIN_VALUE)) < 0)
                {
                    maxInclusiveLong = null;
                    maxExclusiveLong = Long.MIN_VALUE;
                }
                else if (maxExclusive.compareTo(new BigDecimal(Long.MAX_VALUE)) > 0)
                {
                    maxInclusiveLong = null;
                    maxExclusiveLong = null;
                }
                else
                {
                    maxInclusiveLong = null;
                    maxExclusiveLong = maxExclusive.longValue();
                }
            }
        }

        @Override
        public String toText(BigDecimal value)
        {
            return String.valueOf(value);
        }

        @Override
        public String toText(long value)
        {
            return String.valueOf(value);
        }

        @Override
        void checkMinAndMaxValues(BigDecimal value)
        {
            checkParsedValue(v -> minInclusive == null || value.compareTo(minInclusive) >= 0, value, () -> "below minimum");
            checkParsedValue(v -> minExclusive == null || value.compareTo(minExclusive) > 0, value, () -> "below minimum");
            checkParsedValue(v -> maxInclusive == null || value.compareTo(maxInclusive) <= 0, value, () -> "above maximum");
            checkParsedValue(v -> maxExclusive == null || value.compareTo(maxExclusive) < 0, value, () -> "above maximum");
        }

        @Override
        public Handler<BigDecimal> derive(QName deriviativeName, List<Facet> facets)
        {
            IntegerHandler result = new IntegerHandler(deriviativeName);
            this.facets.forEach(result::addFacet);
            facets.forEach(result::addFacet);
            return result;
        }
    }

    private class DecimalHandler extends Handler<BigDecimal> implements DoubleSimpleTypeHandler
    {
        private final Pattern DECIMAL_PATTERN = Pattern.compile("[+-]?\\d*(\\.\\d*)?");

        private Double minInclusiveDouble;
        private Double minExclusiveDouble;
        private Double maxInclusiveDouble;
        private Double maxExclusiveDouble;

        private DecimalHandler(QName name)
        {
            super(name, XsdWhiteSpaceType.REPLACE);
        }

        @Override
        protected BigDecimal doParse(String text)
        {
            if (!DECIMAL_PATTERN.matcher(text).matches())
            {
                throw error(text);
            }
            return new BigDecimal(text).stripTrailingZeros();
        }

        @Override
        public double parseDouble(String text)
        {
            String whiteSpaceProcessed = doPreParse(text);
            if (!DECIMAL_PATTERN.matcher(whiteSpaceProcessed).matches())
            {
                throw error(whiteSpaceProcessed);
            }
            double parsed;
            try
            {
                parsed = Double.parseDouble(whiteSpaceProcessed);
            }
            catch (Exception e)
            {
                throw error(whiteSpaceProcessed, () -> "(possibly range restricted due to modelling as PURE Float)");
            }
            if (enumerations != null)
            {
                checkEnumerations(BigDecimal.valueOf(parsed));
            }
            if (minInclusiveDouble != null && parsed < minInclusiveDouble)
            {
                throw error(whiteSpaceProcessed, () -> "below minimum");
            }
            if (minExclusiveDouble != null && parsed <= minExclusiveDouble)
            {
                throw error(whiteSpaceProcessed, () -> "below minimum");
            }
            if (maxInclusiveDouble != null && parsed > maxInclusiveDouble)
            {
                throw error(whiteSpaceProcessed, () -> "above maximum");
            }
            if (maxExclusiveDouble != null && parsed >= maxExclusiveDouble)
            {
                throw error(whiteSpaceProcessed, () -> "above maximum");
            }
            return parsed;
        }

        @Override
        void addFacet(Facet facet)
        {
            super.addFacet(facet);
            if (facet.getType() == FacetType.MIN_INCLUSIVE)
            {
                if (minInclusive.compareTo(BIG_DECIMAL_DOUBLE_MIN) < 0)
                {
                    minInclusiveDouble = null;
                    minExclusiveDouble = null;
                }
                else if (minInclusive.compareTo(BIG_DECIMAL_DOUBLE_MAX) > 0)
                {
                    minInclusiveDouble = null;
                    minExclusiveDouble = Double.MAX_VALUE;
                }
                else
                {
                    minInclusiveDouble = minInclusive.doubleValue();
                    minExclusiveDouble = null;
                }
            }
            else if (facet.getType() == FacetType.MIN_EXCLUSIVE)
            {
                if (minExclusive.compareTo(BIG_DECIMAL_DOUBLE_MIN) < 0)
                {
                    minInclusiveDouble = null;
                    minExclusiveDouble = null;
                }
                else if (minExclusive.compareTo(BIG_DECIMAL_DOUBLE_MAX) > 0)
                {
                    minInclusiveDouble = null;
                    minExclusiveDouble = Double.MAX_VALUE;
                }
                else
                {
                    minInclusiveDouble = null;
                    minExclusiveDouble = minExclusive.doubleValue();
                }
            }
            else if (facet.getType() == FacetType.MAX_INCLUSIVE)
            {
                if (maxInclusive.compareTo(BIG_DECIMAL_DOUBLE_MIN) < 0)
                {
                    maxInclusiveDouble = null;
                    maxExclusiveDouble = Double.MIN_VALUE;
                }
                else if (maxInclusive.compareTo(BIG_DECIMAL_DOUBLE_MAX) > 0)
                {
                    maxInclusiveDouble = null;
                    maxExclusiveDouble = null;
                }
                else
                {
                    maxInclusiveDouble = maxInclusive.doubleValue();
                    maxExclusiveDouble = null;
                }
            }
            else if (facet.getType() == FacetType.MAX_EXCLUSIVE)
            {
                if (maxExclusive.compareTo(BIG_DECIMAL_DOUBLE_MIN) < 0)
                {
                    maxInclusiveDouble = null;
                    maxExclusiveDouble = Double.MIN_VALUE;
                }
                else if (maxExclusive.compareTo(BIG_DECIMAL_DOUBLE_MAX) > 0)
                {
                    maxInclusiveDouble = null;
                    maxExclusiveDouble = null;
                }
                else
                {
                    maxInclusiveDouble = null;
                    maxExclusiveDouble = maxExclusive.doubleValue();
                }
            }
        }

        @Override
        public String toText(BigDecimal value)
        {
            return value.toString();
        }

        @Override
        public String toText(double value)
        {
            return String.valueOf(value);
        }

        @Override
        void checkMinAndMaxValues(BigDecimal value)
        {
            checkParsedValue(v -> minInclusive == null || value.compareTo(minInclusive) >= 0, value, () -> "below minimum");
            checkParsedValue(v -> minExclusive == null || value.compareTo(minExclusive) > 0, value, () -> "below minimum");
            checkParsedValue(v -> maxInclusive == null || value.compareTo(maxInclusive) <= 0, value, () -> "above maximum");
            checkParsedValue(v -> maxExclusive == null || value.compareTo(maxExclusive) < 0, value, () -> "above maximum");
        }

        @Override
        public Handler<BigDecimal> derive(QName deriviativeName, List<Facet> facets)
        {
            DecimalHandler result = new DecimalHandler(deriviativeName);
            this.facets.forEach(result::addFacet);
            facets.forEach(result::addFacet);
            return result;
        }
    }

    private class DateHandler extends Handler<Temporal>
    {
        private final Pattern WITH_TIMEZONE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}(Z|\\+\\d{2}:?\\d{2})");
        private final Pattern WITHOUT_TIMEZONE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

        private final DateTimeFormatter ZONED_DATE_FORMAT = new DateTimeFormatterBuilder()
                .append(DateTimeFormatter.ISO_OFFSET_DATE)
                .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
                .toFormatter();

        private DateHandler(QName name)
        {
            super(name, XsdWhiteSpaceType.REPLACE);
        }

        @Override
        protected Temporal doParse(String text)
        {
            try
            {
                if (WITH_TIMEZONE.matcher(text).matches())
                {
                    return ZonedDateTime.parse(text, ZONED_DATE_FORMAT);
                }
                else if (WITHOUT_TIMEZONE.matcher(text).matches())
                {
                    return LocalDate.parse(text);
                }
            }
            catch (Exception e)
            {
                // Throws below
            }
            throw error(text);
        }

        @Override
        public String toText(Temporal value)
        {
            return value instanceof LocalDate
                    ? DateTimeFormatter.ISO_DATE.format(value)
                    : DateTimeFormatter.ISO_OFFSET_DATE.format(value);
        }

        @Override
        void checkMinAndMaxValues(Temporal value)
        {
            checkParsedValue(v -> minInclusive == null || compare(value, minInclusive) >= 0, value, () -> "below minimum");
            checkParsedValue(v -> minExclusive == null || compare(value, minExclusive) > 0, value, () -> "below minimum");
            checkParsedValue(v -> maxInclusive == null || compare(value, maxInclusive) <= 0, value, () -> "above maximum");
            checkParsedValue(v -> maxExclusive == null || compare(value, maxExclusive) < 0, value, () -> "above maximum");
        }

        private int compare(Temporal left, Temporal right)
        {
            Instant l = left instanceof LocalDate
                    ? ((LocalDate) left).atStartOfDay(defaultTimezone).toInstant()
                    : ((ZonedDateTime) left).toInstant();
            Instant r = right instanceof LocalDate
                    ? ((LocalDate) right).atStartOfDay(defaultTimezone).toInstant()
                    : ((ZonedDateTime) right).toInstant();
            return l.compareTo(r);
        }

        @Override
        public Handler<Temporal> derive(QName deriviativeName, List<Facet> facets)
        {
            DateHandler result = new DateHandler(deriviativeName);
            this.facets.forEach(result::addFacet);
            facets.forEach(result::addFacet);
            return result;
        }
    }

    private class DateTimeHandler extends Handler<Temporal>
    {
        private final Pattern WITH_TIMEZONE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?(Z|[+-]\\d{2}:?\\d{2})");
        private final Pattern WITHOUT_TIMEZONE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?");

        private DateTimeHandler(QName name)
        {
            super(name, XsdWhiteSpaceType.REPLACE);
        }

        @Override
        protected Temporal doParse(String text)
        {
            try
            {
                if (WITH_TIMEZONE.matcher(text).matches())
                {
                    return Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(text));
                }
                else if (WITHOUT_TIMEZONE.matcher(text).matches())
                {
                    return LocalDateTime.parse(text).atZone(defaultTimezone).toInstant();
                }
            }
            catch (Exception e)
            {
                // Throws below
            }
            throw error(text);
        }

        @Override
        public String toText(Temporal value)
        {
            return DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(defaultTimezone).format(value);
        }

        @Override
        void checkMinAndMaxValues(Temporal value)
        {
            checkParsedValue(v -> minInclusive == null || compare(value, minInclusive) >= 0, value, () -> "below minimum");
            checkParsedValue(v -> minExclusive == null || compare(value, minExclusive) > 0, value, () -> "below minimum");
            checkParsedValue(v -> maxInclusive == null || compare(value, maxInclusive) <= 0, value, () -> "above maximum");
            checkParsedValue(v -> maxExclusive == null || compare(value, maxExclusive) < 0, value, () -> "above maximum");
        }

        private int compare(Temporal left, Temporal right)
        {
            Instant l = (Instant) left;
            Instant r = (Instant) right;
            return l.compareTo(r);
        }

        @Override
        public Handler<Temporal> derive(QName deriviativeName, List<Facet> facets)
        {
            DateHandler result = new DateHandler(deriviativeName);
            this.facets.forEach(result::addFacet);
            facets.forEach(result::addFacet);
            return result;
        }
    }

    private class ListHandler<T> implements SimpleTypeHandler<List<T>>
    {
        private final QName name;
        private final Handler<T> itemHandler;
        private final SimpleTypeHandler<String> xsToken = handler(BuiltInDataTypes.XS_TOKEN);

        private ListHandler(QName name, Handler<T> itemHandler)
        {
            this.name = name;
            this.itemHandler = itemHandler;
        }

        @Override
        public QName getName()
        {
            return name;
        }

        @Override
        public List<T> parse(String text)
        {
            if (text == null)
            {
                return Collections.emptyList();
            }
            else
            {
                String cleaned = xsToken.parse(text);
                return Arrays.stream(cleaned.split(" ")).map(itemHandler::parse).collect(Collectors.toList());
            }
        }

        @Override
        public String toText(List<T> value)
        {
            return value.stream().map(itemHandler::toText).collect(Collectors.joining(" "));
        }

        @Override
        public SimpleTypeHandler<List<T>> derive(QName derivativeName, Facet... facets)
        {
            throw new UnsupportedOperationException("Derivation from list types is not supported");
        }

        @Override
        public SimpleTypeHandler<List<T>> derive(QName derivativeName, List<Facet> facets)
        {
            throw new UnsupportedOperationException("Derivation from list types is not supported");
        }
    }

    private class UnionHandler<T> implements SimpleTypeHandler<T>
    {
        private final QName name;
        private final List<Handler<T>> memberHandlers;

        private UnionHandler(QName name, List<Handler<T>> memberHandlers)
        {
            this.name = name;
            this.memberHandlers = memberHandlers;
        }

        @Override
        public QName getName()
        {
            return name;
        }

        @Override
        public T parse(String text)
        {
            for (SimpleTypeHandler<? extends T> member : memberHandlers)
            {
                try
                {
                    return member.parse(text);
                }
                catch (IllegalArgumentException e)
                {
                    // Ignore
                }
            }
            throw new IllegalArgumentException("");
        }

        @Override
        public String toText(T value)
        {
            for (SimpleTypeHandler<T> member : memberHandlers)
            {
                try
                {
                    return member.toText(value);
                }
                catch (Exception e)
                {
                    // Ignore
                }
            }
            return value.toString();
        }

        @Override
        public SimpleTypeHandler<T> derive(QName derivativeName, Facet... facets)
        {
            throw new UnsupportedOperationException("Derivation from union types is not supported");
        }

        @Override
        public SimpleTypeHandler<T> derive(QName derivativeName, List<Facet> facets)
        {
            throw new UnsupportedOperationException("Derivation from union types is not supported");
        }
    }
}

