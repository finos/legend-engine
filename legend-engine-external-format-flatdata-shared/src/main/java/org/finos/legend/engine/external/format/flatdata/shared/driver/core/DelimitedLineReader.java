package org.finos.legend.engine.external.format.flatdata.shared.driver.core;

import org.finos.legend.engine.external.format.flatdata.shared.driver.core.connection.CharCursor;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.DelimitedLine;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.LineReader;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.BasicDefect;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IDefect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.LongSupplier;

public class DelimitedLineReader implements LineReader
{
    private final CharCursor cursor;
    private final String eol;
    private final String storePath;
    private final LongSupplier lineNumberSupplier;
    private final char delimiter;
    private final String quoteChar;
    private final String escapeChar;
    private final LineParser lineParser;
    private boolean lastLineEndedInEol = false;
    private boolean lastBlankRowReturned = false;

    DelimitedLineReader(CharCursor cursor, String eol, String storePath, LongSupplier lineNumberSupplier, char delimiter, String quoteChar, String escapeChar)
    {
        this.cursor = cursor;
        this.eol = eol;
        this.storePath = storePath;
        this.lineNumberSupplier = lineNumberSupplier;
        this.delimiter = delimiter;
        this.quoteChar = quoteChar;
        this.escapeChar = escapeChar;
        lineParser = new LineParser();
    }

    @Override
    public DelimitedLine readLine()
    {
        if (cursor.isEndOfData())
        {
            if (lastLineEndedInEol && !lastBlankRowReturned)
            {
                lastBlankRowReturned = true;
                return new DelimitedLine(lineNumberSupplier.getAsLong(), "", Collections.emptyList(), Collections.emptyList());
            }
            throw new IllegalStateException("Unexpected EOF Reached");
        }

        return lineParser.parseLine(lineNumberSupplier.getAsLong());
    }

    private class LineParser
    {
        private FullLineData fullLine = new FullLineData();
        private int capacity = 101;

        private int aheadOfCursor;
        private int ch;
        private int valueChars;
        private int skipChars;
        private String value;
        private List<String> values;
        private List<IDefect> defects;
        private long lineNumber;

        private DelimitedLine parseLine(long lineNumber)
        {
            this.lineNumber = lineNumber;

            EolCheck isEol;
            Runnable consumeEol;
            if (DelimitedLineReader.this.eol == null)
            {
                isEol = () -> ch == '\n' || ch == '\r';
                consumeEol = () -> {
                    int ch1 = cursor.advance();
                    if (ch1 == '\r' && cursor.peek(1) == '\n')
                    {
                        cursor.advance();
                    }
                };
            }
            else if (DelimitedLineReader.this.eol.length() == 1)
            {
                char eolChar = DelimitedLineReader.this.eol.charAt(0);
                isEol = () -> ch == eolChar;
                consumeEol = cursor::advance;
            }
            else if (DelimitedLineReader.this.eol.length() == 2)
            {
                char eolChar0 = DelimitedLineReader.this.eol.charAt(0);
                char eolChar1 = DelimitedLineReader.this.eol.charAt(1);
                isEol = () -> ch == eolChar0 && cursor.peek(aheadOfCursor + 1) == eolChar1;
                consumeEol = () -> cursor.advance(2);
            }
            else
            {
                throw new IllegalStateException("Specified EOL must be 1 or 2 chars only");
            }

            fullLine.init();
            aheadOfCursor = 0;
            valueChars = 0;
            skipChars = 0;
            value = null;
            values = new ArrayList<>(capacity);
            defects = Collections.emptyList();
            try
            {
                State state = startOfValue;
                nextChar();
                while (!isEndOfData() && !(state.shouldStopIfEol() && isEol.check()))
                {
                    state = state.evaluate();
                    nextChar();
                }
                state.finish();
                if (isEol.check())
                {
                    lastLineEndedInEol = true;
                    consumeEol.run();
                }

                String text = fullLine.finish();
                capacity = Math.max(capacity, values.size());
                return new DelimitedLine(lineNumber, text, values, defects);
            }
            finally
            {
                fullLine.destroy();
            }
        }

        private void nextChar()
        {
            aheadOfCursor++;
            ch = cursor.peek(aheadOfCursor);
        }

        private boolean isEndOfData()
        {
            return ch == CharCursor.END_OF_DATA;
        }

        private boolean isQuote()
        {
            return quoteChar != null && ch == quoteChar.charAt(0);
        }

        private boolean isDelimiter()
        {
            return ch == delimiter;
        }

        private boolean isWhitespace()
        {
            return Character.isWhitespace(ch);
        }

        private boolean isEscape()
        {
            return escapeChar != null && ch == escapeChar.charAt(0);
        }

        private void addDefect(String message)
        {
            IDefect defect = BasicDefect.newInvalidInputCriticalDefect(message + " at line " + lineNumber, storePath);
            if (defects.isEmpty())
            {
                defects = new ArrayList<>();
            }
            defects.add(defect);
        }

        private void skipChar()
        {
            if (this.valueChars > 0)
            {
                bufferValueChars();
            }
            skipChars++;
            fullLine.increment();
        }

        private void addCharToValue()
        {
            consumeSkippedChars();
            valueChars++;
            fullLine.increment();
        }

        private char[] advanceCursor(int nChars)
        {
            aheadOfCursor -= nChars;
            return cursor.advance(nChars);
        }

        private void bufferValueChars()
        {
            String text = new String(advanceCursor(valueChars));
            this.value = this.value == null ? text : this.value + text;
            valueChars = 0;
        }

        private void consumeSkippedChars()
        {
            if (skipChars > 0)
            {
                advanceCursor(skipChars);
                skipChars = 0;
            }
        }

        private void discardValue()
        {
            cursor.advance(valueChars);
            aheadOfCursor -= valueChars;
            value = null;
            valueChars = 0;
        }

        private void addNullValue()
        {
            discardValue();
            values.add(null);
        }

        private void finishValue()
        {
            if (this.valueChars > 0 && this.value == null)
            {
                values.add(new String(advanceCursor(valueChars)));
            }
            else
            {
                bufferValueChars();
                values.add(this.value);
            }
            value = null;
            valueChars = 0;
            consumeSkippedChars();
        }

        private abstract class State
        {
            abstract State evaluate();

            abstract void finish();

            boolean shouldStopIfEol()
            {
                return true;
            }
        }

        private abstract class EscapeState extends State
        {
            private final State returnToState;
            private final State skipState;

            EscapeState(State returnToState, State skipState) {
                this.returnToState = returnToState;
                this.skipState = skipState;
            }

            State evaluate()
            {
                if (isEscape() || isDelimiter() || isQuote())
                {
                    addCharToValue();
                    return returnToState;
                }
                else
                {
                    skipChar();
                    addNullValue();
                    addDefect("Unexpected characer following escape '" + ((char) ch) + "'");
                    return skipState;
                }
            }

            @Override
            void finish()
            {
                addNullValue();
                consumeSkippedChars();
                addDefect("Escape cannot be the last character of line");
            }
        }

        private State startOfValue = new State()
        {
            State evaluate()
            {
                if (isDelimiter())
                {
                    finishValue();
                    skipChar();
                    return startOfValue;
                }
                else if (isEscape())
                {
                    skipChar();
                    return escapeInUnquotedValue;
                }
                else if (isWhitespace())
                {
                    addCharToValue();
                    return whitespaceAtStartOfValue;
                }
                else if (isQuote())
                {
                    skipChar();
                    return inQuotedValue;
                }
                else
                {
                    addCharToValue();
                    return inUnquotedValue;
                }
            }

            @Override
            void finish()
            {
                if (!values.isEmpty())
                {
                    finishValue();
                }
            }
        };

        private State whitespaceAtStartOfValue = new State()
        {
            State evaluate()
            {
                if (isEscape())
                {
                    skipChar();
                    return escapeInUnquotedValue;
                }
                else if (isWhitespace())
                {
                    addCharToValue();
                    return this;
                }
                else if (isQuote())
                {
                    discardValue();
                    skipChar();
                    return inQuotedValue;
                }
                else
                {
                    addCharToValue();
                    return inUnquotedValue;
                }
            }

            @Override
            void finish()
            {
                finishValue();
            }
        };

        private State inUnquotedValue = new State()
        {
            State evaluate()
            {
                if (isEscape())
                {
                    skipChar();
                    return escapeInUnquotedValue;
                }
                else if (isDelimiter())
                {
                    finishValue();
                    skipChar();
                    return startOfValue;
                }
                else
                {
                    addCharToValue();
                    return this;
                }
            }

            @Override
            void finish()
            {
                finishValue();
            }
        };

        private State inQuotedValue = new State()
        {
            State evaluate()
            {
                if (isEscape())
                {
                    skipChar();
                    return escapeInQuotedValue;
                }
                else if (isQuote())
                {
                    skipChar();
                    return possiblyClosingQuotedValue;
                }
                else
                {
                    addCharToValue();
                    return this;
                }
            }

            @Override
            boolean shouldStopIfEol()
            {
                return false;
            }

            @Override
            void finish()
            {
                discardValue();
                addDefect("Unclosed quotes in value " + (values.size() + 1));
            }
        };

        private State possiblyClosingQuotedValue = new State()
        {
            State evaluate()
            {
                if (isQuote())
                {
                    // Two quotes is an escaped quote
                    addCharToValue();
                    return inQuotedValue;
                }
                else
                {
                    if (isWhitespace())
                    {
                        skipChar();
                        return whiteSpaceAfterQuotedValue;
                    }
                    else if (isDelimiter())
                    {
                        finishValue();
                        skipChar();
                        return startOfValue;
                    }
                    else
                    {
                        skipChar();
                        addNullValue();
                        addDefect("Unexpected text after closing quote in value " + values.size());
                        return skipToNextDelimiter;
                    }
                }
            }

            @Override
            void finish()
            {
                finishValue();
            }
        };

        private State whiteSpaceAfterQuotedValue = new State()
        {
            State evaluate()
            {
                if (isWhitespace())
                {
                    skipChar();
                    return this;
                }
                else if (isDelimiter())
                {
                    finishValue();
                    skipChar();
                    return startOfValue;
                }
                else
                {
                    skipChar();
                    addNullValue();
                    addDefect("Unexpected text after closing quote in value " + values.size());
                    return skipToNextDelimiter;
                }
            }

            @Override
            void finish()
            {
                finishValue();
            }
        };

        private State skipToNextDelimiter = new State()
        {
            State evaluate()
            {
                if (isEscape())
                {
                    skipChar();
                    return escapeWhileSkippingToDelimiter;
                }
                else if (isDelimiter())
                {
                    skipChar();
                    return startOfValue;
                }
                else
                {
                    skipChar();
                    return this;
                }
            }

            @Override
            void finish()
            {
                consumeSkippedChars();
            }
        };

        private State skipToClosingQuote = new State()
        {
            State evaluate()
            {
                if (isQuote())
                {
                    skipChar();
                    return possiblyClosingQuotedValueWhileSkipping;
                }
                else
                {
                    skipChar();
                    return this;
                }
            }

            @Override
            void finish()
            {
                consumeSkippedChars();
            }
        };

        private State possiblyClosingQuotedValueWhileSkipping = new State()
        {
            State evaluate()
            {
                if (isQuote())
                {
                    // Two quotes is an escaped quote
                    skipChar();
                    return skipToClosingQuote;
                }
                else
                {
                    if (isWhitespace())
                    {
                        skipChar();
                        return whiteSpaceAfterQuotedValue;
                    }
                    else if (isDelimiter())
                    {
                        skipChar();
                        return startOfValue;
                    }
                    else
                    {
                        skipChar();
                        return skipToNextDelimiter;
                    }
                }
            }

            @Override
            void finish()
            {
                consumeSkippedChars();
            }
        };

        private State escapeInUnquotedValue = new EscapeState(inUnquotedValue, skipToNextDelimiter)
        {
        };

        private State escapeInQuotedValue = new EscapeState(inQuotedValue, skipToClosingQuote)
        {
        };

        private State escapeWhileSkippingToDelimiter = new EscapeState(skipToNextDelimiter, skipToNextDelimiter)
        {
        };

        private class FullLineData
        {
            private int lineLength;
            private CharCursor lineCursor;

            private void init()
            {
                lineLength = 0;
                lineCursor = cursor.copy();
            }

            private void increment()
            {
                lineLength++;
            }

            private String finish()
            {
                return new String(lineCursor.advance(lineLength));
            }

            private void destroy()
            {
                lineCursor.destroy();
            }
        }
    }

    private interface EolCheck
    {
        boolean check();
    }
}
