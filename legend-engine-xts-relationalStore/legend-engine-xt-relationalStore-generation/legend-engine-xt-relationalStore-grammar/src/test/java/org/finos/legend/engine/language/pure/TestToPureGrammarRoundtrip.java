package org.finos.legend.engine.language.pure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.RichIterable;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.core_relational_relational_extensions_grammarSerializerExtension;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_serialization_grammar_Configuration;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public abstract class TestToPureGrammarRoundtrip
{
    protected abstract org.finos.legend.pure.generated.Root_meta_pure_metamodel_serialization_grammar_Configuration getConfiguration(ExecutionSupport es);

    public void test(String code)
    {
        test(code, code);
    }

    public void test(String code, String expectedCode)
    {
        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(code);
        try
        {
            ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
            String json = objectMapper.writeValueAsString(modelData);
            modelData = objectMapper.readValue(json, PureModelContextData.class);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        PureModel pureModel = Compiler.compile(modelData, DeploymentMode.TEST, Identity.getAnonymousIdentity().getName());
        CompiledExecutionSupport es = pureModel.getExecutionSupport();
        org.finos.legend.pure.generated.Root_meta_pure_metamodel_serialization_grammar_Configuration configuration = this.getConfiguration(es);

        Assert.assertEquals(expectedCode, org.finos.legend.pure.generated.core_pure_serialization_toPureGrammar.Root_meta_pure_metamodel_serialization_grammar_printPackageableElements_PackageableElement_MANY__Configuration_$0_1$__String_1_(pureModel.getPackageableElements(), configuration, es));
    }
}
