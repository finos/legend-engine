package org.finos.legend.engine.protocol.graphQL;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.graphQL.grammar.from.GraphQLGrammarParser;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.graphQL.metamodel.Document;
import org.finos.legend.engine.protocol.graphQL.metamodel.Translator;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.pure.generated.core_pure_serialization_toPureGrammar;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;

public class GenerateIntrospectionPureModel
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
        RichIterable<? extends Type> types =  org.finos.legend.pure.generated.core_external_query_graphql_transformation.Root_meta_external_query_graphQL_binding_toPure_typeSystem_graphQLTypeSystemtoPure_Document_1__String_1__Type_MANY_(new Translator().translate(document, pureModel), "meta::external::query::graphQL::introspection::model", pureModel.getExecutionSupport());
        String res = types.collect(t -> core_pure_serialization_toPureGrammar.Root_meta_pure_metamodel_serialization_grammar_printType_Type_1__String_1_(t, pureModel.getExecutionSupport())).makeString("\n");
        System.out.println(res);
    }
}
