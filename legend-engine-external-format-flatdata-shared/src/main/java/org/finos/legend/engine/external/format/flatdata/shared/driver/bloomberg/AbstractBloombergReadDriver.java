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

package org.finos.legend.engine.external.format.flatdata.shared.driver.bloomberg;

import org.finos.legend.engine.external.format.flatdata.shared.driver.core.StreamingDriverHelper;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.StreamingReadDriver;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.connection.CharCursor;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.CommonDataHandler;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.FlatDataUtils;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.LineReader;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.SimpleLineReader;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.variables.ObjectVariable;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.variables.StringListVariable;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.variables.StringVariable;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataProperty;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;

import java.util.*;
import java.util.function.LongSupplier;

abstract class AbstractBloombergReadDriver<T> extends StreamingReadDriver<T>
{
    static String MANDATORY_SECTION_PROPERTY = "mustBePresent";

    private static final String START_OF_FILE = "START-OF-FILE";
    private static final String END_OF_FILE = "END-OF-FILE";
    static final String START_OF_FIELDS = "START-OF-FIELDS";
    static final String END_OF_FIELDS = "END-OF-FIELDS";
    static final String START_OF_DATA = "START-OF-DATA";
    static final String END_OF_DATA = "END-OF-DATA";
    static final String COMMENT_START = "#";

    static final String NULL_STRING = "N.A.";
    static final String FIELD_SEPARATOR_REGEXP = "\\|";

    private static final String BLOOMBERG_DATE_FORMAT_HEADER_KEY = "DATEFORMAT";

    protected final FlatDataProcessingContext context;
    protected final FlatDataSection section;
    private final Map<String, String> filters = new HashMap<>();
    private final StringListVariable defaultStrictDateFormat;
    private final StringVariable defaultFalseString;
    private final StringVariable defaultTrueString;
    private final ObjectVariable<BloombergKeyValues> metadata;
    private final boolean mandatory;
    private boolean finished = false;

    AbstractBloombergReadDriver(FlatDataSection section, FlatDataProcessingContext context, String programName)
    {
        super(new StreamingDriverHelper(addAutomaticProperties(section), context));
        this.section = section;
        this.context = context;
        this.defaultStrictDateFormat = StringListVariable.reference(context, CommonDataHandler.VARIABLE_DEFAULT_DATE_FORMAT);
        this.defaultTrueString = StringVariable.reference(context, CommonDataHandler.VARIABLE_DEFAULT_TRUE_STRING);
        this.defaultFalseString = StringVariable.reference(context, CommonDataHandler.VARIABLE_DEFAULT_FALSE_STRING);
        this.metadata = ObjectVariable.reference(context, BloombergKeyValues.VARIABLE_LAST_METADATA);
        this.mandatory = FlatDataUtils.getBoolean(section.getSectionProperties(), MANDATORY_SECTION_PROPERTY);

        List<FlatDataProperty> properties = section.getSectionProperties();
        FlatDataUtils.getStrings(properties, BloombergDataDriverDescription.FILTER_PROPERTY)
                .ifPresent(clauses -> {
                    for (String clause : clauses)
                    {
                        String[] kv = clause.split("=");
                        filters.put(kv[0].trim(), kv[1].trim());
                    }
                });
        filters.put("PROGRAMNAME", programName);
    }

    @Override
    public boolean canStartAt(CharCursor cursor)
    {
        if (cursor.isEndOfData())
        {
            return false;
        }

        CharCursor csr = cursor.copy();
        BloombergKeyValues keyValues = new BloombergKeyValues(context);

        try
        {
            LineReader reader = createLineReader(csr, () -> 0);
            LineReader.Line line = reader.readLine();

            while (!line.getText().equals(START_OF_FILE))
            {
                if (csr.isEndOfData())
                {
                    return false;
                }
                line = reader.readLine();
            }

            while (!csr.isEndOfData())
            {
                line = reader.readLine();
                if (line.isEmpty() || line.getText().startsWith(COMMENT_START))
                {
                    // Ignore
                }
                else if (line.getText().equals(START_OF_FIELDS) || line.getText().equals(START_OF_DATA))
                {
                    break;
                }
                else
                {
                    keyValues.parse(line);
                }
            }
            return filters.entrySet().stream().allMatch(e -> e.getValue().equals(keyValues.get(e.getKey())));
        }
        finally
        {
            csr.destroy();
        }
    }

    boolean isMandatory()
    {
        return mandatory;
    }

    private static FlatDataSection addAutomaticProperties(FlatDataSection section)
    {
        ArrayList<FlatDataProperty> newProperties = new ArrayList<>(section.getSectionProperties());
        if (!FlatDataUtils.getString(newProperties, StreamingDriverHelper.SCOPE, StreamingDriverHelper.UNTIL_LINE_EQUALS).isPresent())
        {
            FlatDataUtils.setString(END_OF_FILE, newProperties, StreamingDriverHelper.SCOPE, StreamingDriverHelper.UNTIL_LINE_EQUALS);
        }
        return section.setSectionProperties(newProperties);
    }

    @Override
    protected LineReader createLineReader(CharCursor cursor, LongSupplier lineNumberSupplier)
    {
        return new SimpleLineReader(cursor, helper.eol, lineNumberSupplier);
    }

    @Override
    public void start()
    {
        super.start();
        metadata.set(new BloombergKeyValues(context));
    }

    @Override
    public boolean isFinished()
    {
        return finished;
    }

    protected LineReader.Line nextLine()
    {
        LineReader.Line line = super.nextLine();
        while (line.getText().startsWith(COMMENT_START))
        {
            line = super.nextLine();
        }
        return line;
    }

    private void parseMetadata(LineReader.Line line)
    {
        metadata.get().parse(line);
    }

    void findStartOfFile()
    {
        try
        {
            untilLine(l -> START_OF_FILE.equals(l.getText()), NO_OP);
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Unable to find " + START_OF_FILE, e);
        }
    }

    void parseMetadataUntil(String stopAt)
    {

        try
        {
            untilLine(l -> stopAt.equals(l.getText()), this::parseMetadata);
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Unable to find " + stopAt, e);
        }
    }

    Collection<IChecked<T>> finish()
    {
        try
        {
            untilLine(l -> END_OF_FILE.equals(l.getText()), this::parseMetadata);
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Unable to find " + END_OF_FILE, e);
        }

        finished = true;
        return Collections.emptyList();
    }

    void setupParsing()
    {
        if (metadata.get().containsKey(BLOOMBERG_DATE_FORMAT_HEADER_KEY))
        {
            defaultStrictDateFormat.set(metadata.get().get(BLOOMBERG_DATE_FORMAT_HEADER_KEY).replace('m', 'M'));
        }
        defaultTrueString.set("Y");
        defaultFalseString.set("N");
    }

    static void skipSection(CharCursor cursor)
    {
        if (cursor.isEndOfData())
        {
            return;
        }

        LineReader reader = new SimpleLineReader(cursor, null, () -> 0);
        LineReader.Line line = reader.readLine();

        while (!line.getText().equals(START_OF_FILE))
        {
            if (cursor.isEndOfData())
            {
                return;
            }
            line = reader.readLine();
        }
        while (!line.getText().equals(END_OF_FILE))
        {
            if (cursor.isEndOfData())
            {
                return;
            }
            line = reader.readLine();
        }
    }
}
