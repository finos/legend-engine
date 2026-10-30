// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.plan.generation.stores.graphQLStore;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.authentication.credentialprovider.CredentialProviderProvider;
import org.finos.legend.engine.external.format.json.toModel.JsonSchemaToModelConfiguration;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtensionLoader;
import org.finos.legend.engine.external.shared.format.model.transformation.toModel.SchemaToModelGenerator;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.plan.generation.stores.graphQLStore.introspection.GraphQLIntrospectionClient;
import org.finos.legend.engine.plan.generation.stores.graphQLStore.synthesize.GraphQLJsonSchemaSynthesizer;
import org.finos.legend.engine.plan.generation.stores.graphQLStore.walker.GraphQLSchemaWalker;
import org.finos.legend.engine.plan.generation.stores.graphQLStore.walker.QueryWrapperFieldCandidate;
import org.finos.legend.engine.protocol.graphQL.metamodel.Document;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.OperationType;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.EnumTypeDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.EnumValueDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.FieldDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.InputObjectTypeDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.InputValueDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.InterfaceTypeDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.ListTypeReference;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.NamedTypeReference;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.ObjectTypeDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.RootOperationTypeDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.ScalarTypeDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.SchemaDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.TypeReference;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.UnionTypeDefinition;
import org.finos.legend.engine.protocol.graphQLStore.introspection.GraphQLIntrospectionField;
import org.finos.legend.engine.protocol.graphQLStore.introspection.GraphQLIntrospectionInputValue;
import org.finos.legend.engine.protocol.graphQLStore.introspection.GraphQLIntrospectionSchema;
import org.finos.legend.engine.protocol.graphQLStore.introspection.GraphQLIntrospectionType;
import org.finos.legend.engine.protocol.graphQLStore.introspection.UnwrappedType;
import org.finos.legend.engine.protocol.graphQLStore.metamodel.pure.GraphQLClassMapping;
import org.finos.legend.engine.protocol.graphQLStore.metamodel.pure.GraphQLStore;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.m3.extension.TagPtr;
import org.finos.legend.engine.protocol.pure.m3.extension.TaggedValue;
import org.finos.legend.engine.protocol.pure.m3.function.property.Property;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CString;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.ExternalFormatSchema;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.ExternalFormatSchemaSet;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GraphQLStoreModelGenerator
{
    private final GraphQLIntrospectionClient introspectionClient = new GraphQLIntrospectionClient();
    private final GraphQLSchemaWalker walker = new GraphQLSchemaWalker();
    private final GraphQLJsonSchemaSynthesizer synthesizer = new GraphQLJsonSchemaSynthesizer(walker);
    private final CredentialProviderProvider credentialProviderProvider;

    public GraphQLStoreModelGenerator()
    {
        this(CredentialProviderProvider.builder().build());
    }

    public GraphQLStoreModelGenerator(CredentialProviderProvider credentialProviderProvider)
    {
        this.credentialProviderProvider = credentialProviderProvider;
    }

    public GraphQLStoreModelGenerationResult generate(GraphQLStoreModelGenerationInput input, Identity identity) throws Exception
    {
        validateIntrospectionUrl(input.url);
        GraphQLIntrospectionSchema schema = introspectionClient.introspect(input.url, input.authenticationSpecification, credentialProviderProvider, identity);

        Map<String, GraphQLIntrospectionType> typesByName = GraphQLSchemaWalker.typesByName(schema);
        MutableList<String> skipped = Lists.mutable.empty();
        List<QueryWrapperFieldCandidate> candidates = walker.selectQueryWrapperFields(schema, skipped);

        if (candidates.isEmpty())
        {
            return new GraphQLStoreModelGenerationResult("", skipped);
        }

        String schemaSetPath = input.targetPackage + "::GeneratedSchema";
        String sharedBindingPath = input.targetBindingPath + "::GeneratedBinding";
        String wrapperClassName = schema.queryType.name != null ? schema.queryType.name : "Query";
        String fullyQualifiedWrapperClassName = input.targetPackage + "::" + wrapperClassName;

        Map<String, String> proxyNamesByField = resolveProxyPropertyNames(candidates);
        Map<String, String> qpNameByProxyName = new LinkedHashMap<>();
        proxyNamesByField.forEach((qpName, proxyName) -> qpNameByProxyName.put(proxyName, qpName));

        GraphQLIntrospectionType syntheticQueryType = new GraphQLIntrospectionType();
        syntheticQueryType.kind = "OBJECT";
        syntheticQueryType.name = wrapperClassName;
        syntheticQueryType.fields = Lists.mutable.empty();
        for (QueryWrapperFieldCandidate candidate : candidates)
        {
            if (candidate.isPlainProperty)
            {
                syntheticQueryType.fields.add(candidate.field);
            }
            else
            {
                // A QP has no JSON-Schema representation; its proxy (a plain stored property) stands in.
                GraphQLIntrospectionField proxyField = new GraphQLIntrospectionField();
                proxyField.name = proxyNamesByField.get(candidate.field.name);
                proxyField.type = candidate.field.type;
                syntheticQueryType.fields.add(proxyField);
            }
        }

        ExternalFormatSchema wrapperSchema = new ExternalFormatSchema();
        wrapperSchema.id = wrapperClassName;
        wrapperSchema.location = wrapperClassName + ".json";
        wrapperSchema.content = synthesizer.toJsonSchemaText(syntheticQueryType, fullyQualifiedWrapperClassName, typesByName, skipped);

        MutableList<ExternalFormatSchema> schemaDefinitions = Lists.mutable.with(wrapperSchema);

        GraphQLClassMapping classMapping = new GraphQLClassMapping();
        classMapping.root = true;
        classMapping._class = fullyQualifiedWrapperClassName;
        classMapping.storePath = input.storePath;
        classMapping.typeName = schema.queryType.name;
        classMapping.bindingPath = sharedBindingPath;
        MutableList<GraphQLClassMapping> classMappings = Lists.mutable.with(classMapping);

        ExternalFormatSchemaSet schemaSet = new ExternalFormatSchemaSet();
        schemaSet._package = input.targetPackage;
        schemaSet.name = "GeneratedSchema";
        schemaSet.format = "JSON";
        schemaSet.schemas = schemaDefinitions;

        PureModelContextData scratchModel = singleElementModel(schemaSet);
        PureModel pureModel = Compiler.compile(scratchModel, null, identity.getName());

        JsonSchemaToModelConfiguration config = new JsonSchemaToModelConfiguration();
        config.format = "JSON";
        config.sourceSchemaId = schemaDefinitions.get(0).id;
        config.targetPackage = input.targetPackage;

        SchemaToModelGenerator generator = new SchemaToModelGenerator(pureModel, PureClientVersions.production, ExternalFormatExtensionLoader.extensions());
        PureModelContextData generated = generator.generate(config, schemaSetPath, true, sharedBindingPath);

        // findPolymorphicPropertyPossibleTypes scans every type in the whole introspected schema, not just
        // ones reachable from the generated Query wrapper - a possible-type belonging to some unrelated part
        // of the schema was never actually generated into `generated`, so mapping it would reference an
        // undefined class at compile time. Only map subtypes that were actually generated.
        MutableSet<String> generatedClassNames = Sets.mutable.empty();
        MutableSet<String> generatedEnumerationNames = Sets.mutable.empty();
        for (PackageableElement element : generated.getElements())
        {
            if (element instanceof org.finos.legend.engine.protocol.pure.m3.type.Class && input.targetPackage.equals(element._package))
            {
                generatedClassNames.add(element.name);
            }
            else if (element instanceof org.finos.legend.engine.protocol.pure.m3.type.Enumeration && input.targetPackage.equals(element._package))
            {
                generatedEnumerationNames.add(element.name);
            }
        }

        MutableList<GraphQLIntrospectionType> possibleSubtypes = Lists.mutable.empty();
        MutableSet<String> seenSubtypeNames = Sets.mutable.empty();
        for (GraphQLIntrospectionType possibleType : findPolymorphicPropertyPossibleTypes(schema, typesByName))
        {
            if (generatedClassNames.contains(possibleType.name) && seenSubtypeNames.add(possibleType.name))
            {
                possibleSubtypes.add(possibleType);
            }
        }
        for (GraphQLIntrospectionType possibleSubtype : possibleSubtypes)
        {
            GraphQLClassMapping subtypeClassMapping = new GraphQLClassMapping();
            subtypeClassMapping.root = true;
            subtypeClassMapping._class = input.targetPackage + "::" + possibleSubtype.name;
            subtypeClassMapping.storePath = input.storePath;
            subtypeClassMapping.typeName = possibleSubtype.name;
            subtypeClassMapping.bindingPath = sharedBindingPath;
            classMappings.add(subtypeClassMapping);
        }

        Mapping mapping = new Mapping();
        mapping._package = packageOf(input.targetMappingPath);
        mapping.name = nameOf(input.targetMappingPath);
        mapping.classMappings = Lists.mutable.<ClassMapping>withAll(classMappings);

        GraphQLStore graphQLStore = new GraphQLStore();
        graphQLStore._package = packageOf(input.storePath);
        graphQLStore.name = nameOf(input.storePath);
        graphQLStore.schema = buildSchemaDocument(schema);

        PureModelContextData.Builder finalModelBuilder = PureModelContextData.newBuilder();
        finalModelBuilder.addElement(schemaSet);
        for (PackageableElement element : generated.getElements())
        {
            // The shared JSON-Schema-to-Pure generator (fromJSONSchema.pure) attaches an auto-generated,
            // vacuously-true "<property>_inLine" discriminator check to a generated class for any oneOf/
            // polymorphic property - unavoidable at generation time (no suppression flag exists on
            // JsonSchemaToModelConfiguration/SchemaToModelConfiguration), but entirely unused here. Left in
            // place, it causes the shared graphFetch router to widen any fetch of the constrained class to
            // include the discriminated property even when unrequested.
            if (element instanceof org.finos.legend.engine.protocol.pure.m3.type.Class)
            {
                org.finos.legend.engine.protocol.pure.m3.type.Class clazz = (org.finos.legend.engine.protocol.pure.m3.type.Class) element;
                clazz.constraints = ListIterate.reject(clazz.constraints, c -> c.name != null && c.name.endsWith("_inLine"));
                if (fullyQualifiedWrapperClassName.equals(clazz._package + "::" + clazz.name))
                {
                    stampGeneratedProxyTags(clazz, qpNameByProxyName);
                }
            }
            finalModelBuilder.addElement(element);
        }
        finalModelBuilder.addElement(mapping);
        finalModelBuilder.addElement(graphQLStore);
        PureModelContextData finalModel = finalModelBuilder.build();

        String composed = PureGrammarComposer.newInstance(PureGrammarComposerContext.Builder.newInstance().build()).renderPureModelContextData(finalModel);
        composed = spliceQualifiedPropertiesIntoClass(composed, fullyQualifiedWrapperClassName, buildQualifiedPropertiesText(candidates, proxyNamesByField, typesByName, input.targetPackage));

        List<GraphQLIntrospectionType> inputObjectTypes = findInputObjectTypes(candidates, typesByName);
        List<GraphQLIntrospectionType> enumTypesOnlyReachableFromInputObjects = findEnumTypesReachableFromInputObjects(inputObjectTypes, typesByName, generatedEnumerationNames);
        composed = spliceInputObjectClassesIntoPureSection(composed, buildEnumClassesText(enumTypesOnlyReachableFromInputObjects, input.targetPackage) + buildInputObjectClassesText(inputObjectTypes, typesByName, input.targetPackage));

        return new GraphQLStoreModelGenerationResult(composed, skipped);
    }

    private static void validateIntrospectionUrl(String url)
    {
        URI uri;
        try
        {
            uri = new URI(url);
        }
        catch (URISyntaxException | NullPointerException e)
        {
            throw new EngineException("GraphQL introspection URL '" + url + "' is not a valid URL");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")))
        {
            throw new EngineException("GraphQL introspection URL '" + url + "' must use the http or https scheme");
        }
    }

    private List<GraphQLIntrospectionType> findPolymorphicPropertyPossibleTypes(GraphQLIntrospectionSchema schema, Map<String, GraphQLIntrospectionType> typesByName)
    {
        MutableList<GraphQLIntrospectionType> result = Lists.mutable.empty();
        if (schema.types == null)
        {
            return result;
        }
        for (GraphQLIntrospectionType type : schema.types)
        {
            if (!"OBJECT".equals(type.kind) || type.name == null || type.fields == null)
            {
                continue;
            }
            for (GraphQLIntrospectionField field : type.fields)
            {
                if (!GraphQLSchemaWalker.isValidGraphQLName(field.name))
                {
                    continue;
                }
                UnwrappedType unwrapped = walker.unwrap(field.type, typesByName);
                if (unwrapped.baseType == null)
                {
                    continue;
                }
                String kind = unwrapped.baseType.kind;
                if (("INTERFACE".equals(kind) || "UNION".equals(kind)) && GraphQLSchemaWalker.isValidGraphQLName(unwrapped.baseType.name)
                        && unwrapped.baseType.possibleTypes != null && !unwrapped.baseType.possibleTypes.isEmpty())
                {
                    result.addAll(unwrapped.baseType.possibleTypes);
                }
            }
        }
        return result;
    }

    private String spliceQualifiedPropertiesIntoClass(String composed, String fullyQualifiedClassName, String qualifiedPropertiesText)
    {
        if (qualifiedPropertiesText.isEmpty())
        {
            return composed;
        }
        String classDeclaration = "Class " + fullyQualifiedClassName + "\n";
        int declarationIndex = composed.indexOf(classDeclaration);
        if (declarationIndex < 0)
        {
            throw new IllegalStateException("Expected to find the generated class declaration '" + classDeclaration.trim() + "' in the composed grammar, but it was not present");
        }
        int bodyStartIndex = composed.indexOf("{\n", declarationIndex + classDeclaration.length());
        if (bodyStartIndex < 0)
        {
            throw new IllegalStateException("Expected to find the generated class '" + fullyQualifiedClassName + "'s opening brace after its declaration, but it was not present");
        }
        int bodyEndIndex = composed.indexOf("\n}", bodyStartIndex);
        if (bodyEndIndex < 0)
        {
            throw new IllegalStateException("Expected to find the generated class '" + fullyQualifiedClassName + "'s closing brace after its header, but it was not present");
        }
        return composed.substring(0, bodyEndIndex) + "\n" + qualifiedPropertiesText + composed.substring(bodyEndIndex);
    }

    private String buildQualifiedPropertiesText(List<QueryWrapperFieldCandidate> candidates, Map<String, String> proxyNamesByField, Map<String, GraphQLIntrospectionType> typesByName, String targetPackage)
    {
        StringBuilder sb = new StringBuilder();
        for (QueryWrapperFieldCandidate candidate : candidates)
        {
            if (candidate.isPlainProperty)
            {
                continue;
            }
            UnwrappedType unwrapped = walker.unwrap(candidate.field.type, typesByName);
            String type = purePropertyTypeName(unwrapped.baseType, targetPackage);
            String multiplicity = pureMultiplicity(unwrapped);
            String proxyName = proxyNamesByField.get(candidate.field.name);

            StringBuilder params = new StringBuilder();
            MutableList<String> requiredListArgumentNames = Lists.mutable.empty();
            for (int i = 0; i < candidate.field.args.size(); i++)
            {
                GraphQLIntrospectionInputValue argument = candidate.field.args.get(i);
                UnwrappedType argUnwrapped = walker.unwrap(argument.type, typesByName);
                String argType = "ENUM".equals(argUnwrapped.baseType.kind) || "INPUT_OBJECT".equals(argUnwrapped.baseType.kind)
                        ? targetPackage + "::" + argUnwrapped.baseType.name
                        : GraphQLSchemaWalker.pureTypeForGraphQLScalar(argUnwrapped.baseType.name);
                if (i > 0)
                {
                    params.append(", ");
                }
                params.append(argument.name).append(": ").append(argType).append(argumentMultiplicity(argUnwrapped, argument.defaultValue));
                if (argUnwrapped.isList && argUnwrapped.isNonNull)
                {
                    requiredListArgumentNames.add(argument.name);
                }
            }

            // A required ([Type!]!) list argument called with [] is ambiguous with "omit this argument" under
            // the full-call-arity convention (see valueSpecToPushedDownArgument) - stamp its name here so the
            // translator can tell the two apart and still send an explicit [] rather than dropping the argument.
            String requiredArgumentsTaggedValues = requiredListArgumentNames.collect(name -> "meta::external::store::graphQL::generation::RequiredArgumentsProfile.requiredArguments = '" + name + "'").makeString(", ");
            String requiredArgumentsPrefix = requiredArgumentsTaggedValues.isEmpty() ? "" : "{" + requiredArgumentsTaggedValues + "} ";

            sb.append("  ").append(requiredArgumentsPrefix).append(candidate.field.name).append("(").append(params).append(") { $this.").append(proxyName).append(" }: ").append(type).append(multiplicity).append(";\n");
        }
        return sb.toString();
    }

    // Recursively walks every QP candidate's own arguments (direct QP params only - a plain property's
    // arguments are already dropped elsewhere, see buildQualifiedPropertiesText), following any discovered
    // INPUT_OBJECT type's own inputFields since input objects can nest, and dedupes by name the same way
    // findPolymorphicProperties's subtype collection does.
    private List<GraphQLIntrospectionType> findInputObjectTypes(List<QueryWrapperFieldCandidate> candidates, Map<String, GraphQLIntrospectionType> typesByName)
    {
        MutableList<GraphQLIntrospectionType> result = Lists.mutable.empty();
        MutableSet<String> seenNames = Sets.mutable.empty();
        for (QueryWrapperFieldCandidate candidate : candidates)
        {
            if (candidate.isPlainProperty || candidate.field.args == null)
            {
                continue;
            }
            for (GraphQLIntrospectionInputValue argument : candidate.field.args)
            {
                collectInputObjectTypes(walker.unwrap(argument.type, typesByName).baseType, typesByName, seenNames, result);
            }
        }
        return result;
    }

    private void collectInputObjectTypes(GraphQLIntrospectionType type, Map<String, GraphQLIntrospectionType> typesByName, MutableSet<String> seenNames, MutableList<GraphQLIntrospectionType> result)
    {
        if (type == null || !"INPUT_OBJECT".equals(type.kind) || type.name == null || !seenNames.add(type.name))
        {
            return;
        }
        result.add(type);
        if (type.inputFields != null)
        {
            for (GraphQLIntrospectionInputValue field : type.inputFields)
            {
                collectInputObjectTypes(walker.unwrap(field.type, typesByName).baseType, typesByName, seenNames, result);
            }
        }
    }

    // An Enum field on an input object is only ever discovered via collectInputObjectTypes's own field walk
    // (INPUT_OBJECT kind only) - an ENUM-kind field is never recursed into there, so a Status-typed field on
    // an input object was previously left as a dangling, ungenerated type reference unless that same Enum
    // happened to also be reachable from the main JSON-Schema object graph (eg. as a regular field elsewhere
    // in the schema). alreadyGenerated excludes those already-covered cases to avoid a duplicate definition.
    private List<GraphQLIntrospectionType> findEnumTypesReachableFromInputObjects(List<GraphQLIntrospectionType> inputObjectTypes, Map<String, GraphQLIntrospectionType> typesByName, MutableSet<String> alreadyGenerated)
    {
        MutableList<GraphQLIntrospectionType> result = Lists.mutable.empty();
        MutableSet<String> seenNames = Sets.mutable.withAll(alreadyGenerated);
        for (GraphQLIntrospectionType inputObjectType : inputObjectTypes)
        {
            if (inputObjectType.inputFields == null)
            {
                continue;
            }
            for (GraphQLIntrospectionInputValue field : inputObjectType.inputFields)
            {
                GraphQLIntrospectionType fieldBaseType = walker.unwrap(field.type, typesByName).baseType;
                if (fieldBaseType != null && "ENUM".equals(fieldBaseType.kind) && fieldBaseType.name != null && seenNames.add(fieldBaseType.name))
                {
                    result.add(fieldBaseType);
                }
            }
        }
        return result;
    }

    private String buildEnumClassesText(List<GraphQLIntrospectionType> enumTypes, String targetPackage)
    {
        StringBuilder sb = new StringBuilder();
        for (GraphQLIntrospectionType enumType : enumTypes)
        {
            String values = enumType.enumValues == null ? "" : ListIterate.collect(enumType.enumValues, v -> v.name).makeString(", ");
            sb.append("Enum ").append(targetPackage).append("::").append(enumType.name).append("\n{\n  ").append(values).append("\n}\n\n");
        }
        return sb.toString();
    }

    // Input-object classes are argument-side value containers only - never deserialized from a JSON
    // response - so they're generated directly as text rather than routed through the
    // JSON-Schema-to-Pure pipeline used for the response-shaped Classes.
    private String buildInputObjectClassesText(List<GraphQLIntrospectionType> inputObjectTypes, Map<String, GraphQLIntrospectionType> typesByName, String targetPackage)
    {
        StringBuilder sb = new StringBuilder();
        for (GraphQLIntrospectionType inputObjectType : inputObjectTypes)
        {
            sb.append("Class ").append(targetPackage).append("::").append(inputObjectType.name).append("\n{\n");
            if (inputObjectType.inputFields != null)
            {
                for (GraphQLIntrospectionInputValue field : inputObjectType.inputFields)
                {
                    UnwrappedType fieldUnwrapped = walker.unwrap(field.type, typesByName);
                    String requiredListPrefix = (fieldUnwrapped.isList && fieldUnwrapped.isNonNull)
                            ? "{meta::external::store::graphQL::generation::RequiredArgumentsProfile.requiredArguments = '" + field.name + "'} "
                            : "";
                    sb.append("  ").append(requiredListPrefix).append(field.name).append(": ").append(purePropertyTypeName(fieldUnwrapped.baseType, targetPackage)).append(pureMultiplicity(fieldUnwrapped)).append(";\n");
                }
            }
            sb.append("}\n\n");
        }
        return sb.toString();
    }

    // Input-object Classes have no JSON-Schema counterpart to anchor off of, unlike spliceQualifiedPropertiesIntoClass's
    // per-class splice - inserted as new top-level elements at the end of the existing ###Pure section instead,
    // immediately before ###Mapping; Pure doesn't care about top-level element ordering.
    private String spliceInputObjectClassesIntoPureSection(String composed, String inputObjectClassesText)
    {
        if (inputObjectClassesText.isEmpty())
        {
            return composed;
        }
        String mappingHeader = "###Mapping\n";
        int mappingHeaderIndex = composed.indexOf(mappingHeader);
        if (mappingHeaderIndex < 0)
        {
            throw new IllegalStateException("Expected to find the generated grammar's '###Mapping' section header, but it was not present");
        }
        return composed.substring(0, mappingHeaderIndex) + inputObjectClassesText + "\n" + composed.substring(mappingHeaderIndex);
    }

    private static String purePropertyTypeName(GraphQLIntrospectionType baseType, String targetPackage)
    {
        if ("OBJECT".equals(baseType.kind) || "ENUM".equals(baseType.kind) || "INPUT_OBJECT".equals(baseType.kind)
                || "INTERFACE".equals(baseType.kind) || "UNION".equals(baseType.kind))
        {
            return targetPackage + "::" + baseType.name;
        }
        if ("Int".equals(baseType.name))
        {
            return "Integer";
        }
        if ("Float".equals(baseType.name))
        {
            return "Float";
        }
        if ("Boolean".equals(baseType.name))
        {
            return "Boolean";
        }
        return "String";
    }

    private static String pureMultiplicity(UnwrappedType unwrapped)
    {
        if (unwrapped.isList)
        {
            return "[*]";
        }
        return unwrapped.isNonNull ? "[1]" : "[0..1]";
    }

    private static String argumentMultiplicity(UnwrappedType unwrapped, String defaultValue)
    {
        if (unwrapped.isList)
        {
            return "[*]";
        }
        return unwrapped.isNonNull && defaultValue == null ? "[1]" : "[0..1]";
    }

    private static Map<String, String> resolveProxyPropertyNames(List<QueryWrapperFieldCandidate> candidates)
    {
        MutableSet<String> usedNames = Sets.mutable.empty();
        for (QueryWrapperFieldCandidate candidate : candidates)
        {
            usedNames.add(candidate.field.name);
        }
        Map<String, String> proxyNamesByField = new LinkedHashMap<>();
        for (QueryWrapperFieldCandidate candidate : candidates)
        {
            if (candidate.isPlainProperty)
            {
                continue;
            }
            String base = candidate.field.name + "Proxy";
            String proxyName = base;
            int suffix = 2;
            while (usedNames.contains(proxyName))
            {
                proxyName = base + suffix;
                suffix++;
            }
            usedNames.add(proxyName);
            proxyNamesByField.put(candidate.field.name, proxyName);
        }
        return proxyNamesByField;
    }

    private static void stampGeneratedProxyTags(org.finos.legend.engine.protocol.pure.m3.type.Class clazz, Map<String, String> qpNameByProxyName)
    {
        for (Property property : clazz.properties)
        {
            String qpName = qpNameByProxyName.get(property.name);
            if (qpName == null)
            {
                continue;
            }
            TagPtr tagPtr = new TagPtr();
            tagPtr.profile = "meta::external::store::graphQL::generation::GeneratedProxyProfile";
            tagPtr.value = "generatedProxyForQualifiedProperty";
            TaggedValue taggedValue = new TaggedValue();
            taggedValue.tag = tagPtr;
            taggedValue.value = new CString(qpName);
            property.taggedValues = Lists.mutable.withAll(property.taggedValues).with(taggedValue);
        }
    }

    private static PureModelContextData singleElementModel(PackageableElement element)
    {
        PureModelContextData.Builder builder = PureModelContextData.newBuilder();
        builder.addElement(element);
        return builder.build();
    }

    private static String packageOf(String fullPath)
    {
        int sepPos = fullPath.lastIndexOf("::");
        return sepPos < 0 ? "" : fullPath.substring(0, sepPos);
    }

    private static String nameOf(String fullPath)
    {
        int sepPos = fullPath.lastIndexOf("::");
        return sepPos < 0 ? fullPath : fullPath.substring(sepPos + 2);
    }

    private static Document buildSchemaDocument(GraphQLIntrospectionSchema schema)
    {
        Document document = new org.finos.legend.engine.protocol.graphQL.metamodel.ExecutableDocument();
        MutableList<org.finos.legend.engine.protocol.graphQL.metamodel.Definition> definitions = Lists.mutable.empty();

        if (schema.queryType != null && schema.queryType.name != null)
        {
            SchemaDefinition schemaDefinition = new SchemaDefinition();
            RootOperationTypeDefinition queryRoot = new RootOperationTypeDefinition();
            queryRoot.operationType = OperationType.query;
            queryRoot.type = schema.queryType.name;
            schemaDefinition.rootOperationTypeDefinitions = Lists.mutable.with(queryRoot);
            definitions.add(schemaDefinition);
        }

        if (schema.types != null)
        {
            for (GraphQLIntrospectionType type : schema.types)
            {
                if (type.name == null || type.name.startsWith("__") || !GraphQLSchemaWalker.isValidGraphQLName(type.name))
                {
                    continue;
                }
                switch (type.kind == null ? "" : type.kind)
                {
                    case "OBJECT":
                        definitions.add(toObjectTypeDefinition(type));
                        break;
                    case "INTERFACE":
                        definitions.add(toInterfaceTypeDefinition(type));
                        break;
                    case "UNION":
                        definitions.add(toUnionTypeDefinition(type));
                        break;
                    case "ENUM":
                        definitions.add(toEnumTypeDefinition(type));
                        break;
                    case "INPUT_OBJECT":
                        definitions.add(toInputObjectTypeDefinition(type));
                        break;
                    case "SCALAR":
                        // The five spec-builtin scalars are implicit in every GraphQL schema - declaring
                        // them explicitly (eg. `scalar String`) would conflict with the spec, not describe it.
                        if (GraphQLSchemaWalker.pureTypeForGraphQLScalar(type.name) == null)
                        {
                            definitions.add(toScalarTypeDefinition(type));
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        document.definitions = definitions;
        return document;
    }

    private static ObjectTypeDefinition toObjectTypeDefinition(GraphQLIntrospectionType type)
    {
        ObjectTypeDefinition objectTypeDefinition = new ObjectTypeDefinition();
        objectTypeDefinition.name = type.name;
        objectTypeDefinition.fields = buildFieldDefinitions(type);
        objectTypeDefinition._implements = buildImplementsList(type);
        return objectTypeDefinition;
    }

    private static InterfaceTypeDefinition toInterfaceTypeDefinition(GraphQLIntrospectionType type)
    {
        InterfaceTypeDefinition interfaceTypeDefinition = new InterfaceTypeDefinition();
        interfaceTypeDefinition.name = type.name;
        interfaceTypeDefinition.fields = buildFieldDefinitions(type);
        interfaceTypeDefinition._implements = buildImplementsList(type);
        return interfaceTypeDefinition;
    }

    private static List<FieldDefinition> buildFieldDefinitions(GraphQLIntrospectionType type)
    {
        return type.fields == null ? Lists.mutable.empty() : ListIterate.collect(ListIterate.reject(type.fields, f -> !isValidGraphQLNameAtBase(f.type)), GraphQLStoreModelGenerator::toFieldDefinition);
    }

    private static List<String> buildImplementsList(GraphQLIntrospectionType type)
    {
        return type.interfaces == null ? Lists.mutable.empty() : ListIterate.collect(ListIterate.reject(type.interfaces, i -> i.name == null), i -> i.name);
    }

    private static UnionTypeDefinition toUnionTypeDefinition(GraphQLIntrospectionType type)
    {
        UnionTypeDefinition unionTypeDefinition = new UnionTypeDefinition();
        unionTypeDefinition.name = type.name;
        unionTypeDefinition.members = type.possibleTypes == null ? Lists.mutable.empty() : ListIterate.collect(ListIterate.reject(type.possibleTypes, t -> t.name == null), t -> t.name);
        return unionTypeDefinition;
    }

    private static EnumTypeDefinition toEnumTypeDefinition(GraphQLIntrospectionType type)
    {
        EnumTypeDefinition enumTypeDefinition = new EnumTypeDefinition();
        enumTypeDefinition.name = type.name;
        enumTypeDefinition.values = type.enumValues == null ? Lists.mutable.empty() : ListIterate.collect(type.enumValues, v ->
        {
            EnumValueDefinition enumValueDefinition = new EnumValueDefinition();
            enumValueDefinition.value = v.name;
            return enumValueDefinition;
        });
        return enumTypeDefinition;
    }

    private static InputObjectTypeDefinition toInputObjectTypeDefinition(GraphQLIntrospectionType type)
    {
        InputObjectTypeDefinition inputObjectTypeDefinition = new InputObjectTypeDefinition();
        inputObjectTypeDefinition.name = type.name;
        inputObjectTypeDefinition.fields = type.inputFields == null ? Lists.mutable.empty() : ListIterate.collect(type.inputFields, GraphQLStoreModelGenerator::toInputValueDefinition);
        return inputObjectTypeDefinition;
    }

    private static ScalarTypeDefinition toScalarTypeDefinition(GraphQLIntrospectionType type)
    {
        ScalarTypeDefinition scalarTypeDefinition = new ScalarTypeDefinition();
        scalarTypeDefinition.name = type.name;
        return scalarTypeDefinition;
    }

    private static boolean isValidGraphQLNameAtBase(GraphQLIntrospectionType type)
    {
        GraphQLIntrospectionType base = type;
        while (base != null && base.name == null && base.ofType != null)
        {
            base = base.ofType;
        }
        return base != null && base.name != null && GraphQLSchemaWalker.isValidGraphQLName(base.name);
    }

    private static FieldDefinition toFieldDefinition(GraphQLIntrospectionField field)
    {
        FieldDefinition fieldDefinition = new FieldDefinition();
        fieldDefinition.name = field.name;
        fieldDefinition.type = toTypeReference(field.type);
        fieldDefinition.argumentDefinitions = field.args == null ? Lists.mutable.empty() : ListIterate.collect(field.args, GraphQLStoreModelGenerator::toInputValueDefinition);
        return fieldDefinition;
    }

    private static InputValueDefinition toInputValueDefinition(GraphQLIntrospectionInputValue argument)
    {
        InputValueDefinition inputValueDefinition = new InputValueDefinition();
        inputValueDefinition.name = argument.name;
        inputValueDefinition.type = toTypeReference(argument.type);
        return inputValueDefinition;
    }

    private static TypeReference toTypeReference(GraphQLIntrospectionType type)
    {
        if ("NON_NULL".equals(type.kind))
        {
            TypeReference inner = toTypeReference(type.ofType);
            inner.nullable = false;
            return inner;
        }
        if ("LIST".equals(type.kind))
        {
            ListTypeReference listType = new ListTypeReference();
            listType.itemType = toTypeReference(type.ofType);
            listType.nullable = true;
            return listType;
        }
        NamedTypeReference namedType = new NamedTypeReference();
        namedType.name = type.name;
        namedType.nullable = true;
        return namedType;
    }
}
