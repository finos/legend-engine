package org.finos.legend.engine.plan.execution.stores.service.test.contentPattern;

import org.finos.legend.engine.protocol.pure.v1.model.data.contentPattern.ContentPattern;
import org.finos.legend.engine.protocol.pure.v1.model.data.contentPattern.EqualToPattern;

public class EqualToContentPatternToWiremockPatternGenerator implements ContentPatternToWiremockPatternGenerator
{
    @Override
    public boolean supports(ContentPattern contentPattern)
    {
        if(contentPattern instanceof EqualToPattern)
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
        EqualToPattern equalToPattern = (EqualToPattern) contentPattern;

        return new com.github.tomakehurst.wiremock.matching.EqualToPattern(equalToPattern.expectedValue);
    }
}
