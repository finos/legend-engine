// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.plan.dependencies.util;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.dependencies.domain.date.DayOfWeek;
import org.finos.legend.engine.plan.dependencies.domain.date.DurationUnit;
import org.finos.legend.engine.plan.dependencies.domain.date.PureDate;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

@SuppressWarnings({"unused", "WeakerAccess"})
public class Library
{
    private static final List<Class> PRIMITIVE_CLASS_COMPARISON_ORDER = Arrays.asList(Long.class, Double.class, PureDate.class, Boolean.class, String.class);
    private static final Comparator<Object> DEFAULT_COMPARATOR = Library::compareInt;
    private static final TimeZone GMT_TIME_ZONE = TimeZone.getTimeZone("GMT");

    public static PureDate adjustDate(PureDate date, long number, DurationUnit unit)
    {
        switch (unit)
        {
            case YEARS:
            {
                return date.addYears(number);
            }
            case MONTHS:
            {
                return date.addMonths(number);
            }
            case WEEKS:
            {
                return date.addWeeks(number);
            }
            case DAYS:
            {
                return date.addDays(number);
            }
            case HOURS:
            {
                return date.addHours(number);
            }
            case MINUTES:
            {
                return date.addMinutes(number);
            }
            case SECONDS:
            {
                return date.addSeconds(number);
            }
            case MILLISECONDS:
            {
                return date.addMilliseconds(number);
            }
            case MICROSECONDS:
            {
                return date.addMicroseconds(number);
            }
            case NANOSECONDS:
            {
                return date.addNanoseconds(number);
            }
            default:
            {
                throw new RuntimeException("Unsupported duration unit: " + unit);
            }
        }
    }

    public static long dateDiff(PureDate date1, PureDate date2, DurationUnit unit)
    {
        return date1.dateDifference(date2, unit.name());
    }

    public static PureDate datePart(PureDate date)
    {
        if (!date.hasHour())
        {
            return date;
        }
        else
        {
            return PureDate.newPureDate(date.getYear(), date.getMonth(), date.getDay());
        }
    }

    public static long dayOfMonth(PureDate date)
    {
        if (!date.hasDay())
        {
            throw new IllegalArgumentException("Cannot get day of month for " + date.toString());
        }

        return date.getDay();
    }

    public static long dayOfYear(PureDate date)
    {
        if (!date.hasDay())
        {
            throw new IllegalArgumentException("Cannot get day of year for " + date.toString());
        }

        return date.getCalendar().get(Calendar.DAY_OF_YEAR);
    }


    public static long dayOfWeekNumber(PureDate date)
    {
        if (!date.hasDay())
        {
            throw new IllegalArgumentException("Cannot get day of week for " + date);
        }
        switch (date.getCalendar().get(Calendar.DAY_OF_WEEK))
        {
            case Calendar.MONDAY:
            {
                return 1;
            }
            case Calendar.TUESDAY:
            {
                return 2;
            }
            case Calendar.WEDNESDAY:
            {
                return 3;
            }
            case Calendar.THURSDAY:
            {
                return 4;
            }
            case Calendar.FRIDAY:
            {
                return 5;
            }
            case Calendar.SATURDAY:
            {
                return 6;
            }
            case Calendar.SUNDAY:
            {
                return 7;
            }
            default:
            {
                throw new IllegalArgumentException("Error getting day of week for " + date);
            }
        }
    }

    public static PureDate firstDayOfWeek(PureDate date)
    {
        return mostRecentDayOfWeek(date, DayOfWeek.Monday);
    }

    public static PureDate firstDayOfMonth(PureDate date)
    {
        return PureDate.newPureDate(date.getYear(), date.getMonth(), 1);
    }

    public static PureDate firstDayOfQuarter(PureDate date)
    {
        return PureDate.newPureDate(date.getYear(), (date.getQuarter() * 3) - 2, 1);
    }

    public static PureDate firstDayOfYear(PureDate date)
    {
        return PureDate.newPureDate(date.getYear(), 1, 1);
    }

    public static PureDate today()
    {
        GregorianCalendar calendar = new GregorianCalendar(GMT_TIME_ZONE);
        return PureDate.newPureDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
    }

    public static PureDate firstDayOfThisMonth()
    {
        return firstDayOfMonth(today());
    }

    public static PureDate firstDayOfThisQuarter()
    {
        return firstDayOfQuarter(today());
    }

    public static PureDate firstDayOfThisYear()
    {
        return firstDayOfYear(today());
    }

    public static PureDate firstHourOfDay(PureDate date)
    {
        if (date.hasMonth() && date.hasDay())
        {
            return PureDate.newPureDate(date.getYear(), date.getMonth(), date.getDay(), 0);
        }
        throw new IllegalArgumentException("Date must have year, month and day");
    }


    public static PureDate firstMinuteOfHour(PureDate date)
    {
        if (date.hasMonth() && date.hasDay() && date.hasHour()) 
        {
            return PureDate.newPureDate(date.getYear(), date.getMonth(), date.getDay(), date.getHour(), 0);
        }

        throw new IllegalArgumentException("Date must have year, month, day and hour");
    }

    public static PureDate firstSecondOfMinute(PureDate date)
    {
        if (date.hasMonth() && date.hasDay() && date.hasHour() && date.hasMinute()) 
        {
            return PureDate.newPureDate(date.getYear(), date.getMonth(), date.getDay(), date.getHour(), date.getMinute(), 0);
        }

        throw new IllegalArgumentException("Date must have year, month, day, hour and minute");
    }

    public static PureDate firstMillisecondOfSecond(PureDate date)
    {
        if (date.hasMonth() && date.hasDay() && date.hasHour() && date.hasMinute() && date.hasSecond()) 
        {
            return PureDate.newPureDate(date.getYear(), date.getMonth(), date.getDay(), date.getHour(), date.getMinute(), date.getSecond(), "000");
        }

        throw new IllegalArgumentException("Date must have year, month, day, hour, minute and second");
    }

    public static long weekOfYear(PureDate date)
    {
        if (!date.hasDay())
        {
            throw new IllegalArgumentException("Cannot get week of year for " + date);
        }
        return date.getCalendar().get(Calendar.WEEK_OF_YEAR);
    }

    public static PureDate mostRecentDayOfWeek(PureDate date, DayOfWeek dayOfWeek)
    {
        PureDate datePart = datePart(date);
        int dayOfWeekNumber = dayOfWeek.ordinal() + 1;
        int dayOfWeekNumberDate = (int) dayOfWeekNumber(date);

        int toAdjustBy = dayOfWeekNumber - dayOfWeekNumberDate > 0 ? dayOfWeekNumber - dayOfWeekNumberDate - 7 : dayOfWeekNumber - dayOfWeekNumberDate;

        return adjustDate(datePart, toAdjustBy, DurationUnit.DAYS);
    }

    public static PureDate mostRecentDayOfWeek(DayOfWeek dayOfWeek)
    {
        return mostRecentDayOfWeek(today(), dayOfWeek);
    }

    public static PureDate previousDayOfWeek(PureDate date, DayOfWeek dayOfWeek)
    {
        PureDate datePart = datePart(date);
        int dayOfWeekNumber = dayOfWeek.ordinal() + 1;
        int dayOfWeekNumberDate = (int) dayOfWeekNumber(date);

        int toAdjustBy = dayOfWeekNumber - dayOfWeekNumberDate >= 0 ? dayOfWeekNumber - dayOfWeekNumberDate - 7 : dayOfWeekNumber - dayOfWeekNumberDate;

        return adjustDate(datePart, toAdjustBy, DurationUnit.DAYS);
    }

    public static PureDate previousDayOfWeek(DayOfWeek dayOfWeek)
    {
        return previousDayOfWeek(today(), dayOfWeek);
    }

    public static boolean equals(Object left, Object right)
    {
        if (left == right)
        {
            return true;
        }
        if (left == null)
        {
            return (right instanceof List) && ((List<?>) right).isEmpty();
        }
        if (right == null)
        {
            return (left instanceof List) && ((List<?>) left).isEmpty();
        }
        if (left instanceof List)
        {
            List<?> leftList = (List<?>) left;
            int size = leftList.size();
            if (right instanceof List)
            {
                List<?> rightList = (List<?>) right;
                return (size == rightList.size()) && iteratorsEqual(leftList.iterator(), rightList.iterator());
            }
            return (size == 1) && equals(leftList.get(0), right);
        }
        if (right instanceof List)
        {
            List<?> rightList = (List<?>) right;
            return (rightList.size() == 1) && equals(left, rightList.get(0));
        }
        if (left instanceof Number)
        {
            return (right instanceof Number) && eq((Number) left, (Number) right);
        }

        return left.equals(right);
    }

    private static boolean eq(Number left, Number right)
    {
        if (left instanceof BigDecimal && right instanceof Double ||
                left instanceof Double && right instanceof BigDecimal)
        {
            return false;
        }

        if ((left instanceof Byte) || (right instanceof Byte))
        {
            return (left.getClass() == right.getClass()) && (left.byteValue() == right.byteValue());
        }

        left = left.equals(-0.0d) ? 0.0d : left;
        right = right.equals(-0.0d) ? 0.0d : right;
        return left.equals(right) || left.toString().equals(right.toString());
    }

    private static boolean iteratorsEqual(Iterator<?> leftIterator, Iterator<?> rightIterator)
    {
        while (leftIterator.hasNext() && rightIterator.hasNext())
        {
            if (!equals(leftIterator.next(), rightIterator.next()))
            {
                return false;
            }
        }
        return !leftIterator.hasNext() && !rightIterator.hasNext();
    }

    public static boolean lessThan(PureDate date1, PureDate date2)
    {
        if (date1 == null || date2 == null)
        {
            return false;
        }
        else
        {
            return date1.compareTo(date2) < 0;
        }
    }

    public static boolean lessThanEqual(PureDate date1, PureDate date2)
    {
        if (date1 == null || date2 == null)
        {
            return false;
        }
        else
        {
            return date1.compareTo(date2) <= 0;
        }
    }

    public static boolean greaterThan(PureDate date1, PureDate date2)
    {
        if (date1 == null || date2 == null)
        {
            return false;
        }
        else
        {
            return date1.compareTo(date2) > 0;
        }
    }

    public static boolean greaterThanEqual(PureDate date1, PureDate date2)
    {
        if (date1 == null || date2 == null)
        {
            return false;
        }
        else
        {
            return date1.compareTo(date2) >= 0;
        }
    }

    public static boolean greaterThan(Boolean left, Boolean right)
    {
        return left != null && right != null && Boolean.compare(left, right) > 0;
    }

    public static boolean greaterThanEqual(Boolean left, Boolean right)
    {
        return left != null && right != null && Boolean.compare(left, right) >= 0;
    }

    public static int safeCompareNumbers(Number left, Number right)
    {
        if (left == null)
        {
            return right == null ? 0 : -1;
        }
        else
        {
            return right == null ? 1 : Library.compareInt(left, right);
        }
    }

    public static Number numberPlus(Number left, Number right)
    {
        if (left instanceof Long && right instanceof Long)
        {
            return left.longValue() + right.longValue();
        }
        else if (left instanceof BigDecimal && right instanceof BigDecimal)
        {
            return ((BigDecimal) left).add((BigDecimal) right);
        }
        else if (left instanceof BigDecimal && right instanceof Long)
        {
            return ((BigDecimal) left).add(BigDecimal.valueOf(right.longValue()));
        }
        else if (left instanceof BigDecimal && right instanceof Double)
        {
            return ((BigDecimal) left).add(BigDecimal.valueOf(right.doubleValue()));
        }
        else if (left instanceof Long && right instanceof BigDecimal)
        {
            return BigDecimal.valueOf(left.longValue()).add((BigDecimal) right);
        }
        else if (left instanceof Double && right instanceof BigDecimal)
        {
            return BigDecimal.valueOf(left.doubleValue()).add((BigDecimal) right);
        }
        else
        {
            return left.doubleValue() + right.doubleValue();
        }
    }

    public static long integerPlus(long left, long right)
    {
        return left + right;
    }

    public static double floatPlus(double left, double right)
    {
        return left + right;
    }

    public static BigDecimal decimalPlus(BigDecimal left, BigDecimal right)
    {
        return left.add(right);
    }

    public static Number numberMultiply(Number left, Number right)
    {
        if (left instanceof Long && right instanceof Long)
        {
            return left.longValue() * right.longValue();
        }
        else if (left instanceof BigDecimal && right instanceof BigDecimal)
        {
            return ((BigDecimal) left).multiply((BigDecimal) right);
        }
        else if (left instanceof BigDecimal && right instanceof Long)
        {
            return ((BigDecimal) left).multiply(BigDecimal.valueOf(right.longValue()));
        }
        else if (left instanceof BigDecimal && right instanceof Double)
        {
            return ((BigDecimal) left).multiply(BigDecimal.valueOf(right.doubleValue()));
        }
        else if (left instanceof Long && right instanceof BigDecimal)
        {
            return BigDecimal.valueOf(left.longValue()).multiply((BigDecimal) right);
        }
        else if (left instanceof Double && right instanceof BigDecimal)
        {
            return BigDecimal.valueOf(left.doubleValue()).multiply((BigDecimal) right);
        }
        else
        {
            return left.doubleValue() * right.doubleValue();
        }
    }

    public static long integerMultiply(long left, long right)
    {
        return left * right;
    }

    public static double floatMultiply(double left, double right)
    {
        return left * right;
    }

    public static BigDecimal decimalMultiply(BigDecimal left, BigDecimal right)
    {
        return left.multiply(right);
    }

    public static Number minus(Number left, Number right)
    {
        if (left instanceof Long && right instanceof Long)
        {
            return left.longValue() - right.longValue();
        }
        else if (left instanceof BigDecimal && right instanceof BigDecimal)
        {
            return ((BigDecimal) left).subtract((BigDecimal) right);
        }
        else if (left instanceof BigDecimal && right instanceof Long)
        {
            return ((BigDecimal) left).subtract(BigDecimal.valueOf(right.longValue()));
        }
        else if (left instanceof BigDecimal && right instanceof Double)
        {
            return ((BigDecimal) left).subtract(BigDecimal.valueOf(right.doubleValue()));
        }
        else if (left instanceof Long && right instanceof BigDecimal)
        {
            return BigDecimal.valueOf(left.longValue()).subtract((BigDecimal) right);
        }
        else if (left instanceof Double && right instanceof BigDecimal)
        {
            return BigDecimal.valueOf(left.doubleValue()).subtract((BigDecimal) right);
        }
        else
        {
            return left.doubleValue() - right.doubleValue();
        }
    }

    public static Number minus(List<Number> col)
    {
        int size = col.size();
        if (size == 0)
        {
            return 0;
        }
        else
        {
            Number result = size == 1 ? 0 : col.get(0);
            for (int i = size == 1 ? 0 : 1; i < col.size(); i++)
            {
                result = Library.minus(result, col.get(i));
            }
            return result;
        }
    }

    public static Number minus(Number number)
    {
        if (number == null)
        {
            return 0;
        }
        else if (number instanceof BigDecimal)
        {
            return ((BigDecimal) number).negate();
        }
        else if (number instanceof Long)
        {
            return -number.longValue();
        }
        else
        {
            return -number.doubleValue();
        }
    }

    public static double divide(Number left, Number right)
    {
        if (right.doubleValue() == 0)
        {
            throw new RuntimeException("Cannot divide by zero");
        }
        if (left instanceof BigDecimal && right instanceof BigDecimal)
        {
            return ((BigDecimal) left).divide((BigDecimal) right, RoundingMode.HALF_UP).doubleValue();
        }
        else if (left instanceof BigDecimal && right instanceof Long)
        {
            return ((BigDecimal) left).divide(BigDecimal.valueOf(right.longValue()), RoundingMode.HALF_UP).doubleValue();
        }
        else if (left instanceof BigDecimal && right instanceof Double)
        {
            return ((BigDecimal) left).divide(BigDecimal.valueOf(right.doubleValue()), RoundingMode.HALF_UP).doubleValue();
        }
        else if (left instanceof Long && right instanceof BigDecimal)
        {
            return BigDecimal.valueOf(left.longValue()).divide((BigDecimal) right, RoundingMode.HALF_UP).doubleValue();
        }
        else if (left instanceof Double && right instanceof BigDecimal)
        {
            return BigDecimal.valueOf(left.doubleValue()).divide((BigDecimal) right, RoundingMode.HALF_UP).doubleValue();
        }
        else
        {
            return (left instanceof Long ? left.longValue() : left.doubleValue()) / (right instanceof Long ? right.longValue() : right.doubleValue());
        }
    }

    public static Number rem(Number left, Number right)
    {
        if (right.doubleValue() == 0)
        {
            throw new RuntimeException("Cannot divide by zero");
        }
        if (left instanceof Long && right instanceof Long)
        {
            return left.longValue() % right.longValue();
        }
        else if (left instanceof BigDecimal && right instanceof BigDecimal)
        {
            return ((BigDecimal) left).remainder((BigDecimal) right).doubleValue();
        }
        else if (left instanceof BigDecimal && right instanceof Long)
        {
            return ((BigDecimal) left).remainder(BigDecimal.valueOf(right.longValue())).doubleValue();
        }
        else if (left instanceof BigDecimal && right instanceof Double)
        {
            return ((BigDecimal) left).remainder(BigDecimal.valueOf(right.doubleValue())).doubleValue();
        }
        else if (left instanceof Long && right instanceof BigDecimal)
        {
            return BigDecimal.valueOf(left.longValue()).remainder((BigDecimal) right).doubleValue();
        }
        else if (left instanceof Double && right instanceof BigDecimal)
        {
            return BigDecimal.valueOf(left.doubleValue()).remainder((BigDecimal) right).doubleValue();
        }
        else
        {
            return (left instanceof Long ? left.longValue() : left.doubleValue()) % (right instanceof Long ? right.longValue() : right.doubleValue());
        }
    }

    public static double average(List<Number> col)
    {
        int size = col.size();
        return (size == 0)
                ? 0
                : Library.divide(col.stream().reduce(0L, Library::numberPlus), size);
    }

    public static long round(Number number)
    {
        if (number instanceof Long)
        {
            return (Long) number;
        }
        else
        {
            double toRound = number.doubleValue();
            if (toRound == 0x1.fffffffffffffp-2)
            {
                return 0L;
            }
            else
            {
                toRound = toRound + 0.5;
                double floor = Math.floor(toRound);
                return floor == toRound && floor % 2 != 0 ? (long) (floor - 1) : (long) floor;
            }
        }
    }

    public static Number round(Number number, long scale)
    {
        if (number instanceof Double)
        {
            return Library.round((Double) number, scale);
        }
        else if (number instanceof BigDecimal)
        {
            return Library.round((BigDecimal) number, scale);
        }
        else
        {
            throw new IllegalArgumentException("Unknown number type");
        }
    }

    public static Double round(Double number, long scale)
    {
        return Library.round(BigDecimal.valueOf(number), scale).doubleValue();
    }

    public static BigDecimal round(BigDecimal number, long scale)
    {
        return number.setScale((int) scale, RoundingMode.HALF_UP);
    }

    public static long ceiling(Number number)
    {
        if (number instanceof Long)
        {
            return (Long) number;
        }
        else
        {
            return (long) Math.ceil(number.doubleValue());
        }
    }

    public static long floor(Number number)
    {
        if (number instanceof Long)
        {
            return (Long) number;
        }
        else
        {
            return (long) Math.floor(number.doubleValue());
        }
    }

    public static Number abs(Number number)
    {
        if (number instanceof Long)
        {
            return Math.abs((Long) number);
        }
        else if (number instanceof Double)
        {
            return Math.abs((Double) number);
        }
        else if (number instanceof BigDecimal)
        {
            return ((BigDecimal) number).abs();
        }
        else
        {
            throw new IllegalArgumentException("Unknown number type");
        }
    }

    public static Number max(Number left, Number right)
    {
        return Library.safeCompareNumbers(left, right) > 0 ? left : right;
    }

    public static Number min(Number left, Number right)
    {
        return Library.safeCompareNumbers(left, right) < 0 ? left : right;
    }

    @SuppressWarnings("unchecked")
    public static <T> int compareInt(T left, T right)
    {
        if (Objects.equals(left, right))
        {
            return 0;
        }
        Class leftClass = left.getClass();
        Class rightClass = right.getClass();
        if (!Objects.equals(leftClass, rightClass))
        {
            if (left instanceof Number && right instanceof Number)
            {
                return Library.compareUnmatchedNumbers((Number) left, (Number) right);
            }
            if (left instanceof PureDate && right instanceof PureDate)
            {
                return ((Comparable) left).compareTo(right);
            }
            int leftIndex = Library.PRIMITIVE_CLASS_COMPARISON_ORDER.indexOf(leftClass);
            int rightIndex = Library.PRIMITIVE_CLASS_COMPARISON_ORDER.indexOf(rightClass);
            if (leftIndex == -1)
            {
                return rightIndex == -1 ? leftClass.getCanonicalName().compareTo(rightClass.getCanonicalName()) : 1;
            }
            return rightIndex == -1 ? -1 : Integer.compare(leftIndex, rightIndex);
        }
        if (left instanceof Comparable)
        {
            return ((Comparable) left).compareTo(right);
        }
        return Integer.compare(Objects.hashCode(left), Objects.hashCode(right));
    }

    private static int compareUnmatchedNumbers(Number left, Number right)
    {
        if (Library.isSpecialNumber(left) || Library.isSpecialNumber(right))
        {
            return Double.compare(left.doubleValue(), right.doubleValue());
        }
        return Library.toBigDecimal(left).compareTo(Library
                .toBigDecimal(right));
    }

    private static boolean isSpecialNumber(Number x)
    {
        boolean specialDouble = x instanceof Double && (Double.isNaN((Double) x) || Double.isInfinite((Double) x));
        boolean specialFloat = x instanceof Float && (Float.isNaN((Float) x) || Float.isInfinite((Float) x));
        return specialDouble || specialFloat;
    }

    private static BigDecimal toBigDecimal(Number x)
    {
        if (x instanceof BigDecimal)
        {
            return (BigDecimal) x;
        }
        if (x instanceof BigInteger)
        {
            return new BigDecimal((BigInteger) x);
        }
        if (x instanceof Byte || x instanceof Short || x instanceof Integer || x instanceof Long)
        {
            return new BigDecimal(x.longValue());
        }
        if (x instanceof Double || x instanceof Float)
        {
            return BigDecimal.valueOf(x.doubleValue());
        }
        try
        {
            return new BigDecimal(x.toString());
        }
        catch (NumberFormatException e)
        {
            throw new RuntimeException("The given number (\"" + x + "\" of class " + x.getClass().getName() + ") does not have a parsable string representation", e);
        }
    }

    public static <T> Comparator<T> adaptPureComparator(BiFunction<T, T, Long> pureComparator)
    {
        return (T t1, T t2) ->
        {
            long pureComparatorResult = pureComparator.apply(t1, t2);
            return pureComparatorResult == 0 ? 0 : (int) (pureComparatorResult / Math.abs(pureComparatorResult));
        };
    }

    public static Stream<Long> range(long start, long stop, long step)
    {
        if (step == 0)
        {
            throw new RuntimeException("Step in range can't be 0");
        }
        long limit = Math.signum((float) stop - start) != Math.signum(step) ? 0L : (stop - start) % step == 0 ? (stop - start) / step : (stop - start) / step + 1;
        return LongStream.iterate(start, (long x) -> x + step).limit(limit).boxed();
    }

    public static <T> List<T> init(List<T> col)
    {
        if (col == null)
        {
            return null;
        }
        if (col.size() <= 1)
        {
            return Collections.emptyList();
        }
        return col.subList(0, col.size() - 1);
    }

    public static <T> List<T> sort(List<T> col)
    {
        ArrayList<T> sorted = new ArrayList<T>(col);
        Collections.sort(sorted, Library.DEFAULT_COMPARATOR);
        return sorted;
    }

    public static <T> List<T> reverse(List<T> col)
    {
        ArrayList<T> reversed = new ArrayList<T>(col);
        Collections.reverse(reversed);
        return reversed;
    }

    public static <T, U> U fold(List<T> col, BiFunction<? super T, U, U> accumulator, U identity)
    {
        java.util.function.BinaryOperator<U> dummyCombiner = (U combine1, U combine2) -> combine1;
        return col.stream().reduce(identity, Library.adaptPureAccumulator(accumulator), dummyCombiner);
    }

    public static <T, U> BiFunction<U, ? super T, U> adaptPureAccumulator(BiFunction<? super T, U, U> pureAccumulator)
    {
        return (U u, T t) -> pureAccumulator.apply(t, u);
    }

    public static <T> List<T> sort(List<T> col, Comparator<? super T> comp)
    {
        ArrayList<T> sorted = new ArrayList<T>(col);
        Collections.sort(sorted, comp);
        return sorted;
    }

    public static <T, V> List<T> removeDuplicates(List<T> col, Function<T, V> key, BiPredicate<V, V> eql)
    {
        if (key == null && eql == null)
        {
            return col.stream().distinct().collect(Collectors.toList());
        }
        else if (eql == null)
        {
            return col.stream().filter(Library.distinctByKey(key)).collect(Collectors.toList());
        }
        else if (key == null)
        {
            return col.stream().map((T x) -> (V) x).filter(Library.distinctByEql(eql)).map((V v) -> (T) v).filter((T x) -> x != null).collect(Collectors.toList());
        }
        else
        {
            Predicate<V> eqlP = Library.distinctByEql(eql);
            return col.stream().filter((T x) -> eqlP.test(key.apply(x))).collect(Collectors.toList());
        }
    }

    public static <T> List<T> removeAll(List<T> col1, List<T> col2, BiPredicate<T, T> eql)
    {
        if (eql == null)
        {
            return col1.stream().filter((T c1) -> !col2.contains(c1)).collect(Collectors.toList());
        }
        else
        {
            return col1.stream().filter((T c1) -> col2.stream().noneMatch((T c2) -> eql.test(c1, c2))).collect(Collectors.toList());
        }
    }

    public static <T> boolean contains(List<T> col, T toFind, BiFunction<T, T, Boolean> eql)
    {
        return col.stream().anyMatch((T c) -> eql.apply(c, toFind));
    }

    public static <T> boolean contains(T col, T toFind, BiFunction<T, T, Boolean> eql)
    {
        return eql.apply(col, toFind);
    }

    public static <T> List<T> repeat(long count, T val)
    {
        if (count <= 0)
        {
            return Collections.emptyList();
        }
        return Collections.nCopies((int) count, val);
    }

    public static <T> T uniqueValueOnly(List<T> col, T defaultValue)
    {
        List<T> unique = col.stream().distinct().collect(Collectors.toList());
        return unique.size() == 1 ? unique.get(0) : defaultValue;
    }

    public static <T> T uniqueValueOnly(T col, T defaultValue)
    {
        return col != null ? col : defaultValue;
    }

    public static <T> List<T> dropAt(List<T> col, long index, long count)
    {
        if (index < 0 || index >= col.size())
        {
            throw new IllegalArgumentException("Invalid index");
        }
        List<T> newList = new ArrayList<>(col.subList(0, (int) index));
        if (index + count <= col.size())
        {
            newList.addAll(col.subList((int) (index + count), col.size()));
        }
        return newList;
    }

    public static String hash(String text, Enum hashType)
    {
        String type = hashType.name();
        if (type.equals("MD5"))
        {
            return DigestUtils.md5Hex(text);
        }
        else if (type.equals("SHA1"))
        {
            return DigestUtils.sha1Hex(text);
        }
        else if (type.equals("SHA256"))
        {
            return DigestUtils.sha256Hex(text);
        }
        else
        {
            throw new IllegalArgumentException("Invalid hash type " + type);
        }
    }

    public static String decodeBase64(String text)
    {
        return new String(Base64.decodeBase64(text.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    }

    public static String encodeBase64(String text)
    {
        return Base64.encodeBase64URLSafeString(text.getBytes(StandardCharsets.UTF_8));
    }

    public static String decodeUrl(String text, String charset)
    {
        try
        {
            return URLDecoder.decode(text, charset);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static String encodeUrl(String text, String charset)
    {
        try
        {
            return URLEncoder.encode(text, charset);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static <T> Predicate<T> distinctByKey(Function<T, ?> key)
    {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return (T t) -> seen.add(key.apply(t));
    }

    public static <V> Predicate<V> distinctByEql(BiPredicate<V, V> eql)
    {
        List<V> seen = new ArrayList<V>();
        return (V v) -> seen.stream().noneMatch((V vv) -> eql.test(vv, v)) && seen.add(v);
    }

    public static <T> T first(T in)
    {
        return in;
    }

    public static <T> T first(List<T> in)
    {
        if (in == null || in.size() == 0)
        {
            return null;
        }
        return in.get(0);
    }

    public static <T> T at(T in, long index)
    {
        if (in == null)
        {
            throw new IllegalStateException("The system is trying to get an element at offset " + index + " where the collection is of size 0");
        }
        if (index != 0)
        {
            throw new IllegalStateException("The system is trying to get an element at offset " + index + " where the collection is of size 1");
        }
        return in;
    }

    public static <T> T at(List<T> in, long index)
    {
        if (in == null)
        {
            throw new IllegalStateException("The system is trying to get an element at offset " + index + " where the collection is of size 0");
        }
        if (index < 0 || index >= in.size())
        {
            throw new IllegalStateException("The system is trying to get an element at offset " + index + " where the collection is of size " + in.size());
        }
        return in.get((int) index);
    }

    public static <T> Integer indexOf(T in, T object)
    {
        if (in == null)
        {
            return -1;
        }
        return in.equals(object) ? 0 : -1;
    }

    public static <T> Integer indexOf(List<T> in, T object)
    {
        if (in == null || in.size() == 0)
        {
            return -1;
        }
        return in.indexOf(object);
    }

    public static <R> R match(Object object, List<Predicate<Object>> predicates, List<Function<Object, R>> actions)
    {
        for (int i = 0; i < predicates.size(); i++)
        {
            if (predicates.get(i).test(object))
            {
                return actions.get(i).apply(object);
            }
        }
        throw new RuntimeException("Match failure for object:" + object);
    }

    public static <T> T toOne(T in)
    {
        if (in == null)
        {
            throw new IllegalStateException("Cannot cast a collection of size 0 to multiplicity [1]");
        }
        return in;
    }

    public static <T> T toOne(List<T> in)
    {
        if (in == null || in.size() != 1)
        {
            throw new IllegalStateException("Cannot cast a collection of size " + (in == null ? 0 : in.size()) + " to multiplicity [1]");
        }
        return in.get(0);
    }

    public static <T> List<T> toOneMany(T in)
    {
        if (in == null)
        {
            throw new IllegalStateException("Cannot cast a collection of size 0 to multiplicity [1..*]");
        }
        return Collections.singletonList(in);
    }

    public static <T> List<T> toOneMany(List<T> in)
    {
        if (in == null || in.size() < 1)
        {
            throw new IllegalStateException("Cannot cast a collection of size " + (in == null ? 0 : in.size()) + " to multiplicity [1..*]");
        }
        return in;
    }

    public static String pureToString(Object any)
    {
        if (any instanceof PureDate)
        {
            return any.toString() + (((PureDate) any).hasMinute() ? "+0000" : "");
        }
        else if (any instanceof Double || any instanceof Float)
        {
            if ((double) any == 0.0)
            {
                return "0.0";
            }
            else
            {
                java.text.DecimalFormat format = new java.text.DecimalFormat("0.0", java.text.DecimalFormatSymbols.getInstance(Locale.ENGLISH));
                format.setMaximumFractionDigits(340);
                return format.format((double) any);
            }
        }
        else
        {
            return any.toString();
        }
    }

    public static String toRepresentation(Object any)
    {
        if (any instanceof String)
        {
            return "'" + any.toString().replace("'", "\\'") + "'";
        }
        else if (any instanceof PureDate)
        {
            return "%" + Library.pureToString(any);
        }
        else if (any instanceof BigDecimal)
        {
            return any.toString() + "D";
        }
        else
        {
            return Library.pureToString(any);
        }
    }

    private static int findEndOfDateFormatString(String formatString, int start)
    {
        int length = formatString.length();
        if (start >= length || formatString.charAt(start) != '{')
        {
            return -1;
        }
        boolean inQuotes = false;
        boolean escaped = false;
        for (int i = start + 1; i < length; i++)
        {
            char next = formatString.charAt(i);
            if (inQuotes)
            {
                if (next == '\"')
                {
                    if (!escaped)
                    {
                        inQuotes = false;
                    }
                }
                else if (next == '\\')
                {
                    escaped = !escaped;
                }
            }
            else if (next == '\"')
            {
                inQuotes = true;
            }
            else if (next == '}')
            {
                return i;
            }
        }
        throw new IllegalArgumentException("Could not find end of date format starting at index " + start + " of: " + formatString);
    }

    private static void appendZeros(StringBuilder builder, int zeros)
    {
        for (; zeros > 0; zeros--)
        {
            builder.append('0');
        }
    }

    private static boolean isSign(char character)
    {
        return character == '-' || character == '+';
    }

    private static boolean isSigned(String s)
    {
        return Library.isSign(s.charAt(0));
    }

    private static int getSignCount(String s)
    {
        int i = 0;
        while (Library.isSign(s.charAt(i)))
        {
            i++;
        }
        return i;
    }

    private static int getZeroCountFrom(String s, int index)
    {
        int i = index;
        while (s.charAt(i) == '0')
        {
            i++;
        }
        return i - index;
    }

    private static void appendIntegerString(StringBuilder builder, String s, int zeros)
    {
        if (zeros < 0)
        {
            builder.append(s);
        }
        else if (Library.isSigned(s))
        {
            int signCount = Library.getSignCount(s);
            int length = s.length();
            int digitCount = length - signCount;
            builder.append(s, 0, signCount);
            Library.appendZeros(builder, zeros - digitCount);
            builder.append(s, signCount, length);
        }
        else
        {
            Library.appendZeros(builder, zeros - s.length());
            builder.append(s);
        }
    }

    private static void appendFloatString(StringBuilder builder, String s)
    {
        Library.appendFloatString(builder, s, -1);
    }

    private static void appendFloatString(StringBuilder builder, String s, int precision)
    {
        if (precision == -1)
        {
            builder.append(s);
        }
        else if (precision == 0)
        {
            int decimalIndex = s.indexOf('.');
            if (decimalIndex == -1)
            {
                builder.append(s);
            }
            else if (decimalIndex == s.length() - 1)
            {
                builder.append(s, 0, decimalIndex);
            }
            else
            {
                char charAfterDecimal = s.charAt(decimalIndex + 1);
                if (charAfterDecimal < '5')
                {
                    builder.append(s, 0, decimalIndex);
                }
                else
                {
                    int roundingPrecision = decimalIndex - Library.getSignCount(s);
                    String roundedFloatString = new BigDecimal(s).round(new java.math.MathContext(roundingPrecision, RoundingMode.HALF_EVEN)).toString();
                    int roundedDecimalIndex = roundedFloatString.indexOf('.');
                    if (roundedDecimalIndex == -1)
                    {
                        builder.append(roundedFloatString);
                    }
                    else
                    {
                        builder.append(roundedFloatString, 0, roundedDecimalIndex);
                    }
                }
            }
        }
        else
        {
            int decimalIndex = s.indexOf('.');
            if (decimalIndex == -1)
            {
                builder.append(s);
                builder.append('.');
                Library.appendZeros(builder, precision);
            }
            else
            {
                int decimalCount = s.length() - decimalIndex - 1;
                if (decimalCount <= precision)
                {
                    builder.append(s);
                    Library.appendZeros(builder, precision - decimalCount);
                }
                else
                {
                    int signCount = Library.getSignCount(s);
                    int leadingZeroesBeforeDecimal = Library.getZeroCountFrom(s, signCount);
                    int insignificantCharactersBeforeDecimal = signCount + leadingZeroesBeforeDecimal;
                    int significantDigitsBeforeDecimal = decimalIndex - insignificantCharactersBeforeDecimal;
                    int roundingPrecision = precision;
                    if (significantDigitsBeforeDecimal > 0)
                    {
                        roundingPrecision = roundingPrecision + significantDigitsBeforeDecimal;
                    }
                    else
                    {
                        roundingPrecision = roundingPrecision - Library.getZeroCountFrom(s, decimalIndex + 1);
                    }
                    if (roundingPrecision > 0)
                    {
                        String roundedFloatString = new BigDecimal(s).round(new java.math.MathContext(roundingPrecision, RoundingMode.HALF_EVEN)).toString();
                        if (roundedFloatString.equals(s))
                        {
                            throw new RuntimeException("Error appending float string '" + s + "' at precision " + precision + ": rounding to precision " + roundingPrecision + " failed");
                        }
                        Library.appendFloatString(builder, roundedFloatString, precision);
                    }
                    else if (roundingPrecision < 0)
                    {
                        builder.append(s, 0, decimalIndex + precision + 1);
                    }
                    else
                    {
                        int endIndex = decimalIndex + precision + 1;
                        char endChar = s.charAt(endIndex);
                        boolean roundUp;
                        if (endChar < '5')
                        {
                            roundUp = false;
                        }
                        else if (endChar > '5')
                        {
                            roundUp = true;
                        }
                        else
                        {
                            String roundedFloatString = new BigDecimal(s).round(new java.math.MathContext(1, RoundingMode.UP)).toString();
                            roundUp = roundedFloatString.charAt(endIndex) >= '6';
                        }
                        if (roundUp)
                        {
                            builder.append(s, 0, endIndex - 1);
                            builder.append('1');
                        }
                        else
                        {
                            builder.append(s, 0, endIndex);
                        }
                    }
                }
            }
        }
    }

    public static String format(String formatString, List<Object> formatArgs)
    {
        int index = 0;
        int length = formatString.length();
        Iterator<Object> argIterator = formatArgs.iterator();
        StringBuilder builder = new StringBuilder(length * 2);
        try
        {
            while (index < length)
            {
                char character = formatString.charAt(index++);
                if (character == '%')
                {
                    char formatCh = formatString.charAt(index++);
                    if (formatCh == '%')
                    {
                        builder.append('%');
                    }
                    else if (formatCh == 's')
                    {
                        builder.append(Library.pureToString(argIterator.next()));
                    }
                    else if (formatCh == 'r')
                    {
                        builder.append(Library.toRepresentation(argIterator.next()));
                    }
                    else if (formatCh == 't')
                    {
                        Object arg = argIterator.next();
                        if (arg instanceof Long)
                        {
                            throw new IllegalArgumentException("Expected Date, got: " + arg);
                        }
                        int dateFormatEnd = Library.findEndOfDateFormatString(formatString, index);
                        if (dateFormatEnd == -1)
                        {
                            builder.append(Library.pureToString(arg));
                        }
                        else
                        {
                            builder.append(((PureDate) arg).format(formatString.substring(index + 1, dateFormatEnd)));
                            index = dateFormatEnd + 1;
                        }
                    }
                    else if (formatCh == 'd')
                    {
                        Object arg = argIterator.next();
                        if (arg instanceof Long)
                        {
                            builder.append(((Long) arg).longValue());
                        }
                        else if (arg instanceof Integer)
                        {
                            builder.append(((Integer) arg).intValue());
                        }
                        else if (arg instanceof BigInteger)
                        {
                            builder.append(arg.toString());
                        }
                        else
                        {
                            throw new IllegalArgumentException("Expected Integer, got: " + arg);
                        }
                    }
                    else if (formatCh == '0')
                    {
                        int j = index;
                        while (Character.isDigit(formatString.charAt(j)))
                        {
                            j++;
                        }
                        if (formatString.charAt(j) != 'd')
                        {
                            throw new IllegalArgumentException("Invalid format specifier: %" + formatString.substring(index, j + 1) + "\n" + formatString + "\n" + index + "\n" + j);
                        }
                        int zeroPad = Integer.valueOf(formatString.substring(index, j));
                        Object arg = argIterator.next();
                        if (!(arg instanceof Long || arg instanceof Integer || arg instanceof BigInteger))
                        {
                            throw new IllegalArgumentException("Expected Integer, got: " + arg);
                        }
                        Library.appendIntegerString(builder, arg.toString(), zeroPad);
                        index = j + 1;
                    }
                    else if (formatCh == 'f')
                    {
                        Object arg = argIterator.next();
                        if (!(arg instanceof Double || arg instanceof Float || arg instanceof BigDecimal))
                        {
                            throw new IllegalArgumentException("Expected Float, got: " + arg);
                        }
                        Library.appendFloatString(builder, Library
                                .pureToString(arg));
                    }
                    else if (formatCh == '.')
                    {
                        int j = index;
                        while (Character.isDigit(formatString.charAt(j)))
                        {
                            j++;
                        }
                        if (formatString.charAt(j) != 'f')
                        {
                            throw new IllegalArgumentException("Invalid format specifier: %" + formatString.substring(index, j + 1));
                        }
                        int precision = Integer.valueOf(formatString.substring(index, j));
                        Object arg = argIterator.next();
                        if (!(arg instanceof Double || arg instanceof Float || arg instanceof BigDecimal))
                        {
                            throw new IllegalArgumentException("Expected Float, got: " + arg);
                        }
                        Library.appendFloatString(builder, Library
                                .pureToString(arg), precision);
                        index = j + 1;
                    }
                    else
                    {
                        throw new IllegalArgumentException("Invalid format specifier: %" + formatCh);
                    }
                }
                else
                {
                    builder.append(character);
                }
            }
        }
        catch (NoSuchElementException e)
        {
            throw new IllegalStateException("Too few arguments passed to format function. Format expression \"" + formatString + "\", number of arguments [" + formatArgs.size() + "]");
        }
        if (argIterator.hasNext())
        {
            throw new IllegalStateException("Unused format args. [" + formatArgs.size() + "] arguments provided to expression \"" + formatString + "\"");
        }
        return builder.toString();
    }

    public static List<String> split(String s, String token)
    {
        List<String> result = new ArrayList<>();
        if (token.length() == 0)
        {
            result.add(s);
        }
        else
        {
            int pos = 0;
            int nextPos;
            do
            {
                nextPos = s.indexOf(token, pos);
                result.add(s.substring(pos, nextPos == -1 ? s.length() : nextPos));
                pos = nextPos + token.length();
            }
            while (nextPos != -1 && pos < s.length());
        }
        return result;
    }

    public static List<String> chunk(String s, int val)
    {
        List<String> result = new ArrayList<>((s.length() + val - 1) / val);
        int startIdx = 0;

        while (startIdx < s.length())
        {
            result.add(s.substring(startIdx, Math.min(s.length(), startIdx + val)));
            startIdx += val;
        }
        return result;
    }

    public static String repeatString(String s, int times)
    {
        return String.join("", Collections.nCopies(times, s));
    }

    public static String toUpperFirstCharacter(String s)
    {
        if (s == null)
        {
            return null;
        }
        else if (s.equals(""))
        {
            return s;
        }
        else
        {
            return s.substring(0, 1).toUpperCase() + s.substring(1);
        }
    }

    public static int safeCompare(String left, String right)
    {
        if (left == null)
        {
            return right == null ? 0 : -1;
        }
        else
        {
            return right == null ? 1 : left.compareTo(right);
        }
    }

    public static boolean pureAssert(boolean test, Supplier<String> message)
    {
        if (!test)
        {
            throw new IllegalStateException(message.get());
        }
        return true;
    }

    public static double sqrt(Number input)
    {
        double res = Math.sqrt(input.doubleValue());
        if (Double.isNaN(res))
        {
            throw new RuntimeException("can't compute sqrt for input: " + input);
        }
        return res;
    }

    public static double cbrt(Number input)
    {
        double res = Math.cbrt(input.doubleValue());
        if (Double.isNaN(res))
        {
            throw new RuntimeException("can't compute cbrt for input: " + input);
        }
        return res;
    }

    public static Number stdDev(List<Number> list, boolean isBiasCorrected)
    {
        if (list == null || list.isEmpty())
        {
            throw new RuntimeException("Unable to process empty list");
        }
        MutableList<Number> javaNumbers = Lists.mutable.withAll(list);
        double[] values = new double[javaNumbers.size()];
        for (int i = 0; i < javaNumbers.size(); i++)
        {
            values[i] = javaNumbers.get(i).doubleValue();
        }
        return standardDeviation(values, isBiasCorrected);
    }

    public static double standardDeviation(double[] values, boolean isBiasCorrected)
    {
        return Math.sqrt(variance(values, isBiasCorrected));
    }

    public static Number variance(List<Number> list, boolean isBiasCorrected)
    {
        if (list == null || list.isEmpty())
        {
            throw new RuntimeException("Unable to process empty list");
        }
        MutableList<Number> javaNumbers = Lists.mutable.withAll(list);
        double[] values = new double[javaNumbers.size()];
        for (int i = 0; i < javaNumbers.size(); i++)
        {
            values[i] = javaNumbers.get(i).doubleValue();
        }
        return variance(values, isBiasCorrected);
    }

    public static double variance(double[] values, boolean isBiasCorrected)
    {
        int length = values.length;
        if (length == 1)
        {
            if (isBiasCorrected)
            {
                //calculating sample variance for only 1 number is not allowed
                throw new IllegalArgumentException("calculating sample variance for only 1 number is not allowed");
            }
            else
            {
                //population variance for only 1 number
                return 0.0;
            }
        }

        double mean = mean(values);
        double val = 0.0;
        for (int i = 0; i < length; i++)
        {
            double value = values[i];
            double diff = value - mean;
            val += diff * diff;
        }
        if (isBiasCorrected)
        {
            return val / (length - 1);
        }
        else
        {
            return val / length;
        }
    }

    private static double mean(double[] values)
    {
        int length = values.length;
        if (length == 0)
        {
            throw new IllegalArgumentException("Cannot compute mean with no values");
        }

        double sum = values[0];
        for (int i = 1; i < length; i++)
        {
            sum += values[i];
        }
        return sum / length;
    }

    public static double coTangent(double input)
    {
        return 1.0 / Math.tan(input);
    }

    public static double jaroWinklerSimilarity(String str1, String str2)
    {
        return new JaroWinklerSimilarity().apply(str1, str2);
    }

    public static int levenshteinDistance(String str1, String str2)
    {
        return new LevenshteinDistance().apply(str1, str2);
    }
}
