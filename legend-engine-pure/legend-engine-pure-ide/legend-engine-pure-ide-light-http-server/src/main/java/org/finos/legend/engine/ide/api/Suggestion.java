// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.ide.api;

import io.swagger.annotations.Api;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.engine.ide.helpers.response.ExceptionTranslation;
import org.finos.legend.engine.ide.session.PureSession;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.PackageableFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValueInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpressionInstance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m3.navigation.function.Function;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.json.simple.JSONValue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Api(tags = "Suggestion")
@Path("/")
public class Suggestion
{
    // NOTE: this is the list of auto-import packages defined in m3.pure
    private static final List<String> AUTO_IMPORTS = Arrays.asList(
            "meta::pure::metamodel",
            "meta::pure::metamodel::type",
            "meta::pure::metamodel::type::generics",
            "meta::pure::metamodel::relationship",
            "meta::pure::metamodel::valuespecification",
            "meta::pure::metamodel::multiplicity",
            "meta::pure::metamodel::function",
            "meta::pure::metamodel::function::property",
            "meta::pure::metamodel::extension",
            "meta::pure::metamodel::import",
            "meta::pure::functions::date",
            "meta::pure::functions::string",
            "meta::pure::functions::collection",
            "meta::pure::functions::meta",
            "meta::pure::functions::constraints",
            "meta::pure::functions::lang",
            "meta::pure::functions::boolean",
            "meta::pure::functions::tools",
            "meta::pure::functions::io",
            "meta::pure::functions::math",
            "meta::pure::functions::asserts",
            "meta::pure::functions::test",
            "meta::pure::functions::multiplicity",
            "meta::pure::router",
            "meta::pure::service",
            "meta::pure::tds",
            "meta::pure::tools",
            "meta::pure::profiles"
    );
    private final PureSession session;

    public Suggestion(PureSession session)
    {
        this.session = session;
    }

    @POST
    @Path("suggestion/incompletePath")
    public Response getSuggestionsForIncompletePath(
            @Context HttpServletRequest request,
            IncompletePathSuggestionInput input,
            @Context HttpServletResponse response)
    {
        PureRuntime runtime = this.session.getPureRuntime();
        ProcessorSupport processorSupport = runtime.getProcessorSupport();
        try
        {
            CoreInstance coreInstance = runtime.getCoreInstance(input.path);
            if (coreInstance instanceof Package)
            {
                ListIterable<? extends CoreInstance> children = coreInstance.getValueForMetaPropertyToMany(M3Properties.children)
                        .select(child -> input.types == null || input.types.isEmpty() || input.types.contains(child.getClassifier().getName()));

                return Response.ok((StreamingOutput) outputStream ->
                {
                    outputStream.write("[".getBytes());
                    for (int i = 0; i < children.size(); i++)
                    {
                        CoreInstance child = children.get(i);
                        String pureName = child instanceof PackageableFunction ? child.getValueForMetaPropertyToOne(M3Properties.functionName).getName() : child.getValueForMetaPropertyToOne(M3Properties.name).getName();
                        String text = child instanceof PackageableFunction ? Function.prettyPrint(child, processorSupport) : child.getValueForMetaPropertyToOne(M3Properties.name).getName();
                        MutableList<Property> requiredClassProperties = child instanceof Class ? _Class.getSimpleProperties(child, processorSupport)
                                // NOTE: make sure to only consider required (non-qualified) properties: i.e. multiplicity lower bound != 0
                                .selectInstancesOf(Property.class).select(prop ->
                                        {
                                            CoreInstance lowerBound = prop.getValueForMetaPropertyToOne(M3Properties.multiplicity).getValueForMetaPropertyToOne(M3Properties.lowerBound);
                                            // NOTE: here the lower bound can be nullish when there's multiplicity parameter being used
                                            // but we skip that case for now
                                            return lowerBound != null && !lowerBound.getValueForMetaPropertyToOne(M3Properties.value).getName().equals("0");
                                        }
                                ).toList() : Lists.mutable.empty();

                        outputStream.write("{\"pureType\":\"".getBytes());
                        outputStream.write(JSONValue.escape(child.getClassifier().getName()).getBytes());
                        outputStream.write("\",\"pureName\":\"".getBytes());
                        outputStream.write(JSONValue.escape(pureName).getBytes());
                        outputStream.write("\",\"pureId\":\"".getBytes());
                        outputStream.write(JSONValue.escape(PackageableElement.getUserPathForPackageableElement(child)).getBytes());
                        outputStream.write("\",\"text\":\"".getBytes());
                        outputStream.write(JSONValue.escape(text).getBytes());
                        outputStream.write("\"".getBytes());

                        if (requiredClassProperties.notEmpty())
                        {
                            outputStream.write(",\"requiredClassProperties\":[".getBytes());

                            for (int j = 0; j < requiredClassProperties.size(); j++)
                            {
                                outputStream.write("\"".getBytes());
                                outputStream.write(JSONValue.escape(requiredClassProperties.get(j).getValueForMetaPropertyToOne(M3Properties.name).getName()).getBytes());
                                outputStream.write("\"".getBytes());

                                if (j != requiredClassProperties.size() - 1)
                                {
                                    outputStream.write(",".getBytes());
                                }
                            }

                            outputStream.write("]".getBytes());
                        }

                        outputStream.write("}".getBytes());

                        if (i != children.size() - 1)
                        {
                            outputStream.write(",".getBytes());
                        }
                    }
                    outputStream.write("]".getBytes());
                    outputStream.close();
                }).build();
            }
            return Response.ok((StreamingOutput) outputStream ->
            {
                outputStream.write("[]".getBytes());
                outputStream.close();
            }).build();
        }
        catch (Exception e)
        {
            return Response.status(Response.Status.BAD_REQUEST).entity((StreamingOutput) outputStream ->
            {
                outputStream.write(("\"" + JSONValue.escape(ExceptionTranslation.buildExceptionMessage(session, e, new ByteArrayOutputStream()).getText()) + "\"").getBytes());
                outputStream.close();
            }).build();
        }
    }

    public static class IncompletePathSuggestionInput
    {
        public String path;
        public List<String> types;
    }

    @POST
    @Path("suggestion/identifier")
    public Response getSuggestionsForIdentifier(@Context HttpServletRequest request,
                                                IdentifierSuggestionInput input,
                                                @Context HttpServletResponse response)
    {
        PureRuntime runtime = this.session.getPureRuntime();
        ProcessorSupport processorSupport = runtime.getProcessorSupport();
        // NOTE: here we take into account: first, the imported packages in scope, then the root package (::) and lastly
        // the auto imported packages in the global scope
        MutableList<String> allPackagePaths = Lists.mutable.withAll(input.importPaths).with("::").withAll(AUTO_IMPORTS).distinct();

        try
        {
            MutableList<Package> packages = allPackagePaths.collect(runtime::getCoreInstance).selectInstancesOf(Package.class);
            return Response.ok((StreamingOutput) outputStream ->
            {
                List<? extends CoreInstance> children = packages.flatCollect(pack -> pack.getValueForMetaPropertyToMany(M3Properties.children))
                        // we do not need to get the packages here
                        .select(child -> !(child instanceof Package))
                        .select(child -> input.types == null || input.types.isEmpty() || input.types.contains(child.getClassifier().getName()));

                outputStream.write("[".getBytes());
                for (int i = 0; i < children.size(); i++)
                {
                    CoreInstance child = children.get(i);
                    String pureName = child instanceof PackageableFunction ? child.getValueForMetaPropertyToOne(M3Properties.functionName).getName() : child.getValueForMetaPropertyToOne(M3Properties.name).getName();
                    String text = child instanceof PackageableFunction ? Function.prettyPrint(child, processorSupport) : child.getValueForMetaPropertyToOne(M3Properties.name).getName();
                    MutableList<Property> requiredClassProperties = child instanceof Class ? _Class.getSimpleProperties(child, processorSupport)
                            // NOTE: make sure to only consider required (non-qualified) properties: i.e. multiplicity lower bound != 0
                            .selectInstancesOf(Property.class).select(prop ->
                                    {
                                        CoreInstance lowerBound = prop.getValueForMetaPropertyToOne(M3Properties.multiplicity).getValueForMetaPropertyToOne(M3Properties.lowerBound);
                                        // NOTE: here the lower bound can be nullish when there's multiplicity parameter being used
                                        // but we skip that case for now
                                        return lowerBound != null && !lowerBound.getValueForMetaPropertyToOne(M3Properties.value).getName().equals("0");
                                    }
                            ).toList() : Lists.mutable.empty();

                    outputStream.write("{\"pureType\":\"".getBytes());
                    outputStream.write(JSONValue.escape(child.getClassifier().getName()).getBytes());
                    outputStream.write("\",\"pureName\":\"".getBytes());
                    outputStream.write(JSONValue.escape(pureName).getBytes());
                    outputStream.write("\",\"pureId\":\"".getBytes());
                    outputStream.write(JSONValue.escape(PackageableElement.getUserPathForPackageableElement(child)).getBytes());
                    outputStream.write("\",\"text\":\"".getBytes());
                    outputStream.write(JSONValue.escape(text).getBytes());
                    outputStream.write("\"".getBytes());

                    if (requiredClassProperties.notEmpty())
                    {
                        outputStream.write(",\"requiredClassProperties\":[".getBytes());

                        for (int j = 0; j < requiredClassProperties.size(); j++)
                        {
                            outputStream.write("\"".getBytes());
                            outputStream.write(JSONValue.escape(requiredClassProperties.get(j).getValueForMetaPropertyToOne(M3Properties.name).getName()).getBytes());
                            outputStream.write("\"".getBytes());

                            if (j != requiredClassProperties.size() - 1)
                            {
                                outputStream.write(",".getBytes());
                            }
                        }

                        outputStream.write("]".getBytes());
                    }

                    outputStream.write("}".getBytes());

                    if (i != children.size() - 1)
                    {
                        outputStream.write(",".getBytes());
                    }
                }
                outputStream.write("]".getBytes());
                outputStream.close();
            }).build();
        }
        catch (Exception e)
        {
            return Response.status(Response.Status.BAD_REQUEST).entity((StreamingOutput) outputStream ->
            {
                outputStream.write(("\"" + JSONValue.escape(ExceptionTranslation.buildExceptionMessage(session, e, new ByteArrayOutputStream()).getText()) + "\"").getBytes());
                outputStream.close();
            }).build();
        }
    }

    public static class IdentifierSuggestionInput
    {
        public List<String> importPaths;
        public List<String> types;
    }

    @POST
    @Path("suggestion/attribute")
    public Response getSuggestionsForAttribute(@Context HttpServletRequest request,
                                               AttributeSuggestionInput input,
                                               @Context HttpServletResponse response)
    {
        PureRuntime runtime = this.session.getPureRuntime();
        ProcessorSupport processorSupport = runtime.getProcessorSupport();
        // NOTE: here we take into account: first, the imported packages in scope, then the root package (::) and lastly
        // the auto imported packages in the global scope
        MutableList<String> allPackagePaths = Lists.mutable.withAll(input.importPaths).withAll(AUTO_IMPORTS).distinct();
        MutableList<String> paths = input.path.contains("::") ? Lists.mutable.of(input.path) : allPackagePaths.collect(pkg -> pkg.concat("::" + input.path));

        try
        {
            MutableList<AttributeSuggestion> suggestions = paths.collect(runtime::getCoreInstance)
                    // These are the sensible elements to get attributes from at the moment
                    .select(el -> el instanceof Class || el instanceof Enumeration || el instanceof Profile)
                    .flatCollect(el ->
                    {
                        if (el instanceof Class)
                        {
                            return _Class.computePropertiesByName(el, Lists.mutable.withAll(_Class.SIMPLE_PROPERTIES_PROPERTIES).withAll(_Class.QUALIFIED_PROPERTIES_PROPERTIES), processorSupport).collect(property -> new AttributeSuggestion(
                                    property.getClassifier().getName(),
                                    property.getValueForMetaPropertyToOne(M3Properties.name).getName(),
                                    el.getClassifier().getName(),
                                    PackageableElement.getUserPathForPackageableElement(el)
                            ));
                        }
                        if (el instanceof Profile)
                        {
                            return Lists.mutable.withAll(el.getValueForMetaPropertyToMany(M3Properties.p_tags).collect(tag -> new AttributeSuggestion(
                                    tag.getClassifier().getName(),
                                    tag.getValueForMetaPropertyToOne(M3Properties.value).getName(),
                                    el.getClassifier().getName(),
                                    PackageableElement.getUserPathForPackageableElement(el)
                            ))).withAll(el.getValueForMetaPropertyToMany(M3Properties.p_stereotypes).collect(stereotype -> new AttributeSuggestion(
                                    stereotype.getClassifier().getName(),
                                    stereotype.getValueForMetaPropertyToOne(M3Properties.value).getName(),
                                    el.getClassifier().getName(),
                                    PackageableElement.getUserPathForPackageableElement(el)))
                            );
                        }
                        if (el instanceof Enumeration)
                        {
                            return el.getValueForMetaPropertyToMany(M3Properties.values).collect(enumValue -> new AttributeSuggestion(
                                    enumValue.getClassifier().getName(),
                                    enumValue.getValueForMetaPropertyToOne(M3Properties.name).getName(),
                                    el.getClassifier().getName(),
                                    PackageableElement.getUserPathForPackageableElement(el)
                            ));
                        }
                        return Collections.emptyList();
                    });
            return Response.ok((StreamingOutput) outputStream ->
            {
                outputStream.write("[".getBytes());
                for (int i = 0; i < suggestions.size(); i++)
                {
                    AttributeSuggestion suggestion = suggestions.get(i);

                    outputStream.write("{\"pureType\":\"".getBytes());
                    outputStream.write(JSONValue.escape(suggestion.pureType).getBytes());
                    outputStream.write("\",\"pureName\":\"".getBytes());
                    outputStream.write(JSONValue.escape(suggestion.pureName).getBytes());
                    outputStream.write("\",\"owner\":\"".getBytes());
                    outputStream.write(JSONValue.escape(suggestion.owner).getBytes());
                    outputStream.write("\",\"ownerPureType\":\"".getBytes());
                    outputStream.write(JSONValue.escape(suggestion.ownerPureType).getBytes());
                    outputStream.write("\"}".getBytes());

                    if (i != suggestions.size() - 1)
                    {
                        outputStream.write(",".getBytes());
                    }
                }
                outputStream.write("]".getBytes());
                outputStream.close();
            }).build();
        }
        catch (Exception e)
        {
            return Response.status(Response.Status.BAD_REQUEST).entity((StreamingOutput) outputStream ->
            {
                outputStream.write(("\"" + JSONValue.escape(ExceptionTranslation.buildExceptionMessage(session, e, new ByteArrayOutputStream()).getText()) + "\"").getBytes());
                outputStream.close();
            }).build();
        }
    }

    private static class AttributeSuggestion
    {
        public final String pureType;
        public final String pureName;
        public final String ownerPureType;
        public final String owner;

        public AttributeSuggestion(String pureType, String pureName, String ownerPureType, String owner)
        {
            this.pureType = pureType;
            this.pureName = pureName;
            this.ownerPureType = ownerPureType;
            this.owner = owner;
        }
    }

    public static class AttributeSuggestionInput
    {
        public List<String> importPaths;
        public String path;
    }

    @POST
    @Path("suggestion/class")
    public Response getSuggestionsForClass(@Context HttpServletRequest request,
                                           ClassSuggestionInput input,
                                           @Context HttpServletResponse response)
    {
        PureRuntime runtime = this.session.getPureRuntime();
        ProcessorSupport processorSupport = runtime.getProcessorSupport();
        MutableList<String> packagePaths = Lists.mutable.withAll(input.importPaths).withAll(AUTO_IMPORTS).distinct();

        try
        {
            MutableList<Class> classes = packagePaths.collect(runtime::getCoreInstance)
                    .flatCollect(pkg -> pkg.getValueForMetaPropertyToMany(M3Properties.children))
                    .selectInstancesOf(Class.class);
            return Response.ok((StreamingOutput) outputStream ->
            {
                outputStream.write("[".getBytes());
                for (int i = 0; i < classes.size(); i++)
                {
                    Class<?> cls = classes.get(i);
                    MutableList<Property> requiredClassProperties = _Class.getSimpleProperties(cls, processorSupport)
                            // NOTE: make sure to only consider required (non-qualified) properties: i.e. multiplicity lower bound != 0
                            .selectInstancesOf(Property.class).select(prop ->
                                    {
                                        CoreInstance lowerBound = prop.getValueForMetaPropertyToOne(M3Properties.multiplicity).getValueForMetaPropertyToOne(M3Properties.lowerBound);
                                        // NOTE: here the lower bound can be nullish when there's multiplicity parameter being used
                                        // but we skip that case for now
                                        return lowerBound != null && !lowerBound.getValueForMetaPropertyToOne(M3Properties.value).getName().equals("0");
                                    }
                            ).toList();

                    outputStream.write("{\"pureName\":\"".getBytes());
                    outputStream.write(JSONValue.escape(cls.getValueForMetaPropertyToOne(M3Properties.name).getName()).getBytes());
                    outputStream.write("\",\"pureId\":\"".getBytes());
                    outputStream.write(JSONValue.escape(PackageableElement.getUserPathForPackageableElement(cls)).getBytes());
                    outputStream.write("\",\"requiredClassProperties\":[".getBytes());

                    for (int j = 0; j < requiredClassProperties.size(); j++)
                    {
                        outputStream.write("\"".getBytes());
                        outputStream.write(JSONValue.escape(requiredClassProperties.get(j).getValueForMetaPropertyToOne(M3Properties.name).getName()).getBytes());
                        outputStream.write("\"".getBytes());

                        if (j != requiredClassProperties.size() - 1)
                        {
                            outputStream.write(",".getBytes());
                        }
                    }

                    outputStream.write("]}".getBytes());

                    if (i != classes.size() - 1)
                    {
                        outputStream.write(",".getBytes());
                    }
                }

                outputStream.write("]".getBytes());
                outputStream.close();
            }).build();
        }
        catch (Exception e)
        {
            return Response.status(Response.Status.BAD_REQUEST).entity((StreamingOutput) outputStream ->
            {
                outputStream.write(("\"" + JSONValue.escape(ExceptionTranslation.buildExceptionMessage(session, e, new ByteArrayOutputStream()).getText()) + "\"").getBytes());
                outputStream.close();
            }).build();
        }
    }

    public static class ClassSuggestionInput
    {
        public List<String> importPaths;
    }

    @POST
    @Path("suggestion/variable")
    public Response getSuggestionsForVariable(@Context HttpServletRequest request,
                                              VariableSuggestionInput input,
                                              @Context HttpServletResponse response)
    {
        PureRuntime runtime = this.session.getPureRuntime();
        try
        {
            Source source = runtime.getSourceById(input.sourceId);
            ListIterable<CoreInstance> functionsOrLambdas = source.findFunctionsOrLambasAt(input.line, input.column);
            MutableSet<String> varNames = Sets.mutable.empty();

            for (CoreInstance fn : functionsOrLambdas)
            {
                // scan for the let expressions then follows by the parameters
                RichIterable<InstanceValueInstance> letVars = fn.getValueForMetaPropertyToMany(M3Properties.expressionSequence)
                        .select(expression -> expression instanceof SimpleFunctionExpression && "letFunction".equals(((SimpleFunctionExpression) expression)._functionName()))
                        .collect(expression -> ((SimpleFunctionExpression) expression)._parametersValues().toList().getFirst())
                        // NOTE: make sure to only consider let statements prior to the call
                        .select(letVar -> letVar.getSourceInformation().getEndLine() < input.line || (letVar.getSourceInformation().getEndLine() == input.line && letVar.getSourceInformation().getEndColumn() < input.column))
                        .selectInstancesOf(InstanceValueInstance.class);
                for (InstanceValueInstance var : letVars)
                {
                    varNames.add(var.getValueForMetaPropertyToOne(M3Properties.values).getName());
                }
                RichIterable<VariableExpressionInstance> params = fn.getValueForMetaPropertyToOne(M3Properties.classifierGenericType)
                        .getValueForMetaPropertyToOne(M3Properties.typeArguments)
                        .getValueForMetaPropertyToOne(M3Properties.rawType)
                        .getValueForMetaPropertyToMany(M3Properties.parameters)
                        .selectInstancesOf(VariableExpressionInstance.class);
                for (VariableExpressionInstance var : params)
                {
                    varNames.add(var._name());
                }
            }
            MutableList<String> suggestions = Lists.mutable.withAll(varNames);

            return Response.ok((StreamingOutput) outputStream ->
            {
                outputStream.write("[".getBytes());
                for (int i = 0; i < suggestions.size(); i++)
                {
                    String varName = suggestions.get(i);
                    outputStream.write("{\"name\":\"".getBytes());
                    outputStream.write(JSONValue.escape(varName).getBytes());
                    outputStream.write("\"}".getBytes());

                    if (i != suggestions.size() - 1)
                    {
                        outputStream.write(",".getBytes());
                    }
                }
                outputStream.write("]".getBytes());
                outputStream.close();
            }).build();
        }
        catch (Exception e)
        {
            return Response.status(Response.Status.BAD_REQUEST).entity((StreamingOutput) outputStream ->
            {
                outputStream.write(("\"" + JSONValue.escape(ExceptionTranslation.buildExceptionMessage(session, e, new ByteArrayOutputStream()).getText()) + "\"").getBytes());
                outputStream.close();
            }).build();
        }
    }

    public static class VariableSuggestionInput
    {
        public String sourceId;
        public int line;
        public int column;
    }
}
