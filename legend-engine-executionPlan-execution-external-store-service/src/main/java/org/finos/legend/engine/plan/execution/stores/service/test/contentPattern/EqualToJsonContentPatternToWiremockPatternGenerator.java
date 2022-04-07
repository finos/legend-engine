package org.finos.legend.engine.plan.execution.stores.service.test.contentPattern;

import org.finos.legend.engine.protocol.pure.v1.model.data.contentPattern.ContentPattern;
import org.finos.legend.engine.protocol.pure.v1.model.data.contentPattern.EqualToJsonPattern;

public class EqualToJsonContentPatternToWiremockPatternGenerator implements ContentPatternToWiremockPatternGenerator
{
    @Override
    public boolean supports(ContentPattern contentPattern)
    {
        if(contentPattern instanceof EqualToJsonPattern)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    public com.github.tomakehurst.wiremock.matching.StringValuePattern generate(ContentPattern contentPattern)
    {
        EqualToJsonPattern equalToJsonPattern = (EqualToJsonPattern) contentPattern;

        return new com.github.tomakehurst.wiremock.matching.EqualToJsonPattern(equalToJsonPattern.expectedValue, true, false);
    }
}
