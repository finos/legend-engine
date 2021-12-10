package org.finos.legend.engine.external.format.flatdata.shared.driver.core;

import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.FlatDataUtils;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataProperty;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;

import java.util.List;
import java.util.TimeZone;

public class DelimitedDriverHelper extends StreamingDriverHelper
{
    static final String DELIMITER = "delimiter";
    static final String QUOTE_CHAR = "quoteChar";
    static final String ESCAPING_CHAR = "escapingChar";
    static final String NULL_STRING = "nullString";

    private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

    final String delimiter;
    final String quoteChar;
    final String escapeChar;
    final List<String> nullStrings;

    DelimitedDriverHelper(FlatDataSection section, FlatDataProcessingContext context)
    {
        super(section, context);

        List<FlatDataProperty> properties = section.getSectionProperties();
        this.delimiter = FlatDataUtils.getString(properties, DELIMITER).orElse(null);
        this.quoteChar = FlatDataUtils.getString(properties, QUOTE_CHAR).orElse(null);
        this.escapeChar = FlatDataUtils.getString(properties, ESCAPING_CHAR).orElse(null);
        this.nullStrings = FlatDataUtils.getStrings(properties, NULL_STRING);
    }
}
