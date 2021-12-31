package org.finos.legend.engine.query.graphQL.api;

import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.graphQL.grammar.from.GraphQLGrammarParser;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.graphQL.Document;
import org.finos.legend.engine.protocol.graphQL.Translator;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.query.graphQL.api.model.Query;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.pure.generated.core_external_query_graphQL_introspection_transformation;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;

@Api(tags = "GraphQL - Execution")
@Path("graphQL/v1/execution")
@Produces(MediaType.APPLICATION_JSON)
public class Execute
{
    private ModelManager modelManager;

    public Execute(ModelManager modelManager)
    {
        this.modelManager = modelManager;
    }

    @POST
    @ApiOperation(value = "Execute a GraphQL query in the context of a Mapping and a Runtime.")
    @Path("execute")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response execute(@Context HttpServletRequest request, Query query, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        try (Scope scope = GlobalTracer.get().buildSpan("GraphQL: Execute").startActive(true))
        {
            PureModelContextData pureModelContextData = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(Execute.class.getClassLoader().getResourceAsStream("exampleModel.json"), PureModelContextData.class);
            PureModel pureModel = this.modelManager.loadModel(pureModelContextData, "vX_X_X", profiles, "");

            // Query in Pure space --
            GraphQLGrammarParser parser = GraphQLGrammarParser.newInstance();
            Document document = parser.parseDocument(query.query);
            org.finos.legend.pure.generated.Root_meta_external_query_graphQL_metamodel_Document queryDoc = new Translator().translate(document, pureModel);
            //-----------------------

            // Root Class --
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> _class = pureModel.getClass("meta::external::query::graphQL::introspection::tests::Firm");
            //--------------

            // Perform Introspection Query on Root Class ----
            String result = core_external_query_graphQL_introspection_transformation.Root_meta_external_query_graphQL_introspection_graphQLIntrospectionQuery_Class_1__Document_1__String_1_(_class, queryDoc, pureModel.getExecutionSupport());
            // ----------------------------------------------

            return Response.ok("{\"data\":" + result + "}").type(MediaType.TEXT_HTML_TYPE).build();
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.EXECUTE_INTERACTIVE_ERROR, profiles);
        }
    }
}
