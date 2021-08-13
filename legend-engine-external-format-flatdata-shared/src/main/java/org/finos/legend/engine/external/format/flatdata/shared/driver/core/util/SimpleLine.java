package org.finos.legend.engine.external.format.flatdata.shared.driver.core.util;

import java.util.function.Predicate;
import java.util.regex.Pattern;

public class SimpleLine implements LineReader.Line
{
    private static final Predicate<String> BLANK_LINE = Pattern.compile("^\\s*$").asPredicate();

    private final long lineNumber;
    private final String text;

    SimpleLine(long lineNumber, String text)
    {
        this.lineNumber = lineNumber;
        this.text = text;
    }

    @Override
    public long getLineNumber()
    {
        return lineNumber;
    }

    @Override
    public boolean isEmpty()
    {
        return BLANK_LINE.test(text);
    }

    @Override
    public String getText()
    {
        return text;
    }
}
