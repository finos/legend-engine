package org.finos.legend.engine.language.pure.compiler.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.Warning;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.pure.generated.Root_meta_external_persistence_aws_metamodel_connection_AwsS3Connection;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RootRelationalInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Column;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.RelationalOperationElement;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAliasColumn;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.datatype.SemiStructured;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Table;
import org.junit.Assert;
import org.junit.Test;

public class TestS3CompilationFromGrammar extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite {

    @Test
    public void testS3() {
        test(
                "###Connection\n" +
                        "AwsS3Connection meta::mySimpleConnection\n" +
                        "{\n" +
                        "  store: store::Store;\n" +
                        "  partition: AWS;\n" +
                        "  region: 'US';\n" +
                        "  bucket: 'abc';\n" +
                        "  key: 'xyz';\n" +
                        "}\n");
    }

    @Override
    public String getDuplicatedElementTestCode() {
        return "Class anything::class {}\n" +
                "###Mapping\n" +
                "Mapping anything::somethingelse ()\n" +
                "###Connection\n" +
                "AwsS3Connection anything::somethingelse\n" +
                "{\n" +
                "  store: store::Store;\n" +
                "  partition: AWS;\n" +
                "  region: 'US';\n" +
                "  bucket: 'abc';\n" +
                "  key: 'xyz';\n" +
                "}";
    }

    @Override
    public String getDuplicatedElementTestExpectedErrorMessage() {
        return "COMPILATION error at [5:1-12:1]: Duplicated element 'anything::somethingelse'";
    }

    @Test
    public void testS3ConnectionProcessing()
    {
        PureModel pureModel = test(
                "###Connection\n" +
                        "AwsS3Connection meta::mySimpleConnection\n" +
                        "{\n" +
                        "  store: store::Store;\n" +
                        "  partition: AWS;\n" +
                        "  region: 'US';\n" +
                        "  bucket: 'abc';\n" +
                        "  key: 'xyz';\n" +
                        "}").getTwo();
        Root_meta_external_persistence_aws_metamodel_connection_AwsS3Connection connection = (Root_meta_external_persistence_aws_metamodel_connection_AwsS3Connection) pureModel.getConnection("meta::mySimpleConnection", SourceInformation.getUnknownSourceInformation());

        String partition = connection._partition()._name();

        Assert.assertEquals(partition, "AWS");
    }
}
