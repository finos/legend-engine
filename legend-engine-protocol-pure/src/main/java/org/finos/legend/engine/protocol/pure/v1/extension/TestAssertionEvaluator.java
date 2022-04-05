package org.finos.legend.engine.protocol.pure.v1.extension;

import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.TestAssertion;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.TestAssertionVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertionStatus;

public interface TestAssertionEvaluator extends TestAssertionVisitor<AssertionStatus>
{
    AssertionStatus visit(TestAssertion testAssertion);
}
