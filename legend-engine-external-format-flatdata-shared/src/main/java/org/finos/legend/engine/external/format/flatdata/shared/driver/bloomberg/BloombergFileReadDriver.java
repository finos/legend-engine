package org.finos.legend.engine.external.format.flatdata.shared.driver.bloomberg;

import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RawFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.LineReader;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.data.BasicRawFlatData;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.BasicDefect;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IDefect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class BloombergFileReadDriver<T> extends AbstractBloombergReadDriver<T>
{
    public static final String ID = "BloombergFile";
    public static final String COMMENT_START = "#";

    private final FlatDataSection section;
    private final FlatDataProcessingContext context;

    //private CheckedParsedDataFactory dataFactory;
    private long recordNumber;
    private String[] headings;

    BloombergFileReadDriver(FlatDataSection section, FlatDataProcessingContext context)
    {
        super(section, context);
        this.section = section;
        this.context = context;
    }

    @Override
    public String getId()
    {
        return BloombergFileReadDriver.ID;
    }

    @Override
    public void start()
    {
        super.start();
        findStartOfFile();
        parseMetadataUntil(START_OF_FIELDS);

        try
        {
            List<String> heads = new ArrayList<>(Arrays.asList("SECURITY", "ERROR_COUNT", "FIELD_COUNT"));
            untilLine(l -> END_OF_FIELDS.equals(l.getText()), l -> {
                if (l.getText().trim().length() > 0 && !l.getText().startsWith(COMMENT_START))
                {
                    heads.add(l.getText().trim());
                }
            });
            headings = heads.toArray(new String[0]);
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Unable to find " + END_OF_FIELDS);
        }

        parseMetadataUntil(START_OF_DATA);
        setupParsing();
        //dataFactory = factory();
    }

    //@Override
    public Collection<IChecked<T>> readCheckedObjects()
    {
        LineReader.Line line = nextLine();
        while (line.getText().startsWith(COMMENT_PREFIX))
        {
            line = nextLine();
        }

        if (line.getText().equals(END_OF_DATA))
        {
            return finish();
        }

        String[] values = line.getText().split(FIELD_SEPARATOR_REGEXP);
        List<IDefect> defects = new ArrayList<>();
        if (values.length != headings.length)
        {
            defects.add(BasicDefect.newInvalidInputErrorDefect("Expected " + headings.length + " fields but got " + values.length, context.getDefiningPath()));
        }
        RawFlatData unparsed = BasicRawFlatData.newRecord(++recordNumber, line.getLineNumber(), line.getText(), Arrays.asList(headings), Arrays.asList(values));
        Optional<IChecked<T>> parsed = Optional.empty(); //dataFactory.make(unparsed, defects);
        return parsed.map(Collections::singletonList).orElseGet(Collections::emptyList);
    }
}
