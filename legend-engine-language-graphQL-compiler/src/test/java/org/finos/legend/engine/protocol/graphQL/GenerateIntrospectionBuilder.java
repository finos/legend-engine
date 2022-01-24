package org.finos.legend.engine.protocol.graphQL;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.graphQL.grammar.from.GraphQLGrammarParser;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;

public class GenerateIntrospectionBuilder
{
    public static void main(String args[])
    {
        PureModel pureModel = new PureModel(PureModelContextData.newBuilder().build(), Lists.mutable.empty(), DeploymentMode.TEST);
        String introspection = "type __Schema {\n" +
                "  types: [__Type!]!\n" +
                "  queryType: __Type!\n" +
                "  mutationType: __Type\n" +
                "  subscriptionType: __Type\n" +
                "  directives: [__Directive!]!\n" +
                "}\n" +
                "\n" +
                "type __Type {\n" +
                "  kind: __TypeKind!\n" +
                "  name: String\n" +
                "  description: String\n" +
                "  fields(includeDeprecated: Boolean = false): [__Field!]\n" +
                "  interfaces: [__Type!]\n" +
                "  possibleTypes: [__Type!]\n" +
                "  enumValues(includeDeprecated: Boolean = false): [__EnumValue!]\n" +
                "  inputFields: [__InputValue!]\n" +
                "  ofType: __Type\n" +
                "}\n" +
                "\n" +
                "type __Field {\n" +
                "  name: String!\n" +
                "  description: String\n" +
                "  args: [__InputValue!]!\n" +
                "  type: __Type!\n" +
                "  isDeprecated: Boolean!\n" +
                "  deprecationReason: String\n" +
                "}\n" +
                "\n" +
                "type __InputValue {\n" +
                "  name: String!\n" +
                "  description: String\n" +
                "  type: __Type!\n" +
                "  defaultValue: String\n" +
                "}\n" +
                "\n" +
                "type __EnumValue {\n" +
                "  name: String!\n" +
                "  description: String\n" +
                "  isDeprecated: Boolean!\n" +
                "  deprecationReason: String\n" +
                "}\n" +
                "\n" +
                "enum __TypeKind {\n" +
                "  SCALAR\n" +
                "  OBJECT\n" +
                "  INTERFACE\n" +
                "  UNION\n" +
                "  ENUM\n" +
                "  INPUT_OBJECT\n" +
                "  LIST\n" +
                "  NON_NULL\n" +
                "}\n" +
                "\n" +
                "type __Directive {\n" +
                "  name: String!\n" +
                "  description: String\n" +
                "  locations: [__DirectiveLocation!]!\n" +
                "  args: [__InputValue!]!\n" +
                "}\n" +
                "\n" +
                "enum __DirectiveLocation {\n" +
                "  QUERY\n" +
                "  MUTATION\n" +
                "  SUBSCRIPTION\n" +
                "  FIELD\n" +
                "  FRAGMENT_DEFINITION\n" +
                "  FRAGMENT_SPREAD\n" +
                "  INLINE_FRAGMENT\n" +
                "  SCHEMA\n" +
                "  SCALAR\n" +
                "  OBJECT\n" +
                "  FIELD_DEFINITION\n" +
                "  ARGUMENT_DEFINITION\n" +
                "  INTERFACE\n" +
                "  UNION\n" +
                "  ENUM\n" +
                "  ENUM_VALUE\n" +
                "  INPUT_OBJECT\n" +
                "  INPUT_FIELD_DEFINITION\n" +
                "}";

        GraphQLGrammarParser parser = GraphQLGrammarParser.newInstance();
        Document document = parser.parseDocument(introspection);
        String res = org.finos.legend.pure.generated.core_pure_protocol_generation_builder_generation.Root_meta_protocols_generation_builder_builderGeneration_Any_1__String_1_(new Translator().translate(document, pureModel), pureModel.getExecutionSupport());
        System.out.println(res);
    }
}
