package org.finos.legend.engine.plan.execution.stores.service.test.contentPattern;

import org.finos.legend.engine.protocol.pure.v1.model.data.contentPattern.ContentPattern;

public interface ContentPatternToWiremockPatternGenerator
{
    boolean supports(ContentPattern contentPattern);

    com.github.tomakehurst.wiremock.matching.StringValuePattern generate(ContentPattern contentPattern);
}
