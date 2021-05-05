package org.finos.legend.engine.language.pure.grammar.test.from;

import org.finos.legend.engine.language.pure.grammar.test.TestGrammarRoundtrip;
import org.junit.Test;

public class TestMappingGrammarFrom extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite {

    @Test
    public void testSimpleModelToModelMapping() {
        testFrom("###Mapping\n" +
                "Mapping mapping::simpleModelMapping\n" +
                "(\n" +
                "  *model::TargetClass[my_mapping_id]: Pure\n" +
                "  {\n" +
                "    ~src model::SourceClass\n" +
                "    name: $src.name\n" +
                "  }\n" +
                ")\n", "SimpleM2MMapping.json");
    }


}
