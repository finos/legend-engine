//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.api.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.eclipse.collections.api.RichIterable;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.pure.generated.Root_meta_analytics_quality_model_CheckResult;
import org.finos.legend.pure.generated.Root_meta_pure_functions_collection_List_Impl;
import org.finos.legend.pure.generated.core_analytics_quality_associationChecks;
import org.finos.legend.pure.generated.core_analytics_quality_classChecks;
import org.finos.legend.pure.generated.core_analytics_quality_enumerationChecks;
import org.finos.legend.pure.generated.core_analytics_quality_functionChecks;
import org.finos.legend.pure.generated.core_analytics_quality_propertyChecks;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Api(tags = "Analytics - Quality")
@Path("pure/v1/analytics/quality")
public class DataspaceQualityAnalytics
{
    private final ModelManager modelManager;
    private List<String> messages;

    public DataspaceQualityAnalytics(ModelManager modelManager)
    {
        this.modelManager = modelManager;
    }

    @POST
    @Path("check")
    @ApiOperation("Checks the quality of provided Data space")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response checkDataSpaceConstraints(PureModelContextData pureModelContextData)
    {
        messages = new ArrayList<>();
        PureModel pureModel = this.modelManager.loadModel(pureModelContextData, null, null, null);

        List<PackageableElement> classes = pureModelContextData.getElements().stream().filter(e -> e instanceof org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Class).collect(Collectors.toList());
        List<PackageableElement> associations = pureModelContextData.getElements().stream().filter(e -> e instanceof org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Association).collect(Collectors.toList());
        List<PackageableElement> enumerations = pureModelContextData.getElements().stream().filter(e -> e instanceof org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Enumeration).collect(Collectors.toList());
        List<PackageableElement> functions = pureModelContextData.getElements().stream().filter(e -> e instanceof org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function).collect(Collectors.toList());

        CompiledExecutionSupport es = pureModel.getExecutionSupport();

        if (classChecks(classes, pureModel, es) && associationChecks(associations, pureModel, es) && enumerationChecks(enumerations, pureModel, es) && functionChecks(functions, pureModel, es))
        {
            return Response.ok().build();
        }
        else
        {
            return Response.status(400).entity("Dataspace does not match the constraints - " + messages).build();
        }
    }

    private boolean classChecks(List<PackageableElement> classes, PureModel pureModel, CompiledExecutionSupport es)
    {
        List<Root_meta_analytics_quality_model_CheckResult> classCheckResults = new ArrayList<>();
        boolean allPropertiesInClassAreValid = false;
        for (PackageableElement pe : classes)
        {
            Class<?> _class = pureModel.getContext().resolveClass(pe.getPath(), pe.sourceInformation);
            allPropertiesInClassAreValid = propertyChecks(_class, es);
            //create rules
            core_analytics_quality_classChecks.Root_meta_analytics_quality_model_domain_classRules__Rule_MANY_(es);

            //run checks
            RichIterable<? extends Root_meta_analytics_quality_model_CheckResult> upperCase_class_1__checkResult_many_ = core_analytics_quality_classChecks.Root_meta_analytics_quality_model_domain_classNameShouldStartWithUpperCase_Class_1__CheckResult_MANY_(_class, es);
            RichIterable<? extends Root_meta_analytics_quality_model_CheckResult> entityNameShouldNotStartWithPackageName_class_1__checkResult_many_ = core_analytics_quality_classChecks.Root_meta_analytics_quality_model_domain_entityNameShouldNotStartWithPackageName_Class_1__CheckResult_MANY_(_class, es);
            RichIterable<? extends Root_meta_analytics_quality_model_CheckResult> classHasAtLeastOneMandatoryProperty_class_1__checkResult_many_ = core_analytics_quality_classChecks.Root_meta_analytics_quality_model_domain_classHasAtLeastOneMandatoryProperty_Class_1__CheckResult_MANY_(_class, es);
            RichIterable<? extends Root_meta_analytics_quality_model_CheckResult> allEntitiesAndPropertiesShouldHaveALongDescription_class_1__checkResult_many_ = core_analytics_quality_classChecks.Root_meta_analytics_quality_model_domain_allEntitiesAndPropertiesShouldHaveALongDescription_Class_1__CheckResult_MANY_(_class, es);

            upperCase_class_1__checkResult_many_.forEach(classCheckResults::add);
            entityNameShouldNotStartWithPackageName_class_1__checkResult_many_.forEach(classCheckResults::add);
            classHasAtLeastOneMandatoryProperty_class_1__checkResult_many_.forEach(classCheckResults::add);
            allEntitiesAndPropertiesShouldHaveALongDescription_class_1__checkResult_many_.forEach(classCheckResults::add);
        }

        return checkOverallResults(classCheckResults) && allPropertiesInClassAreValid;
    }

    private boolean associationChecks(List<PackageableElement> associations, PureModel pureModel, CompiledExecutionSupport es)
    {
        List<Root_meta_analytics_quality_model_CheckResult> associationCheckResults = new ArrayList<>();
        for (PackageableElement pe : associations)
        {
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association _association = pureModel.getContext().resolveAssociation(pe.getPath(), pe.sourceInformation);

            //create rules
            core_analytics_quality_associationChecks.Root_meta_analytics_quality_model_domain_associationRules__Rule_MANY_(es);

            //run checks
            RichIterable<? extends Root_meta_analytics_quality_model_CheckResult> associationNameShouldStartWithUpperCase_association_1__checkResult_many_ = core_analytics_quality_associationChecks.Root_meta_analytics_quality_model_domain_associationNameShouldStartWithUpperCase_Association_1__CheckResult_MANY_(_association, es);
            associationNameShouldStartWithUpperCase_association_1__checkResult_many_.forEach(associationCheckResults::add);
        }
        return checkOverallResults(associationCheckResults);
    }

    private boolean enumerationChecks(List<PackageableElement> enumerations, PureModel pureModel, CompiledExecutionSupport es)
    {
        List<Root_meta_analytics_quality_model_CheckResult> enumerationCheckResults = new ArrayList<>();
        for (PackageableElement pe : enumerations)
        {
            Enumeration<Enum> _enumeration = pureModel.getContext().resolveEnumeration(pe.getPath(), pe.sourceInformation);

            //create rules
            core_analytics_quality_enumerationChecks.Root_meta_analytics_quality_model_domain_enumerationRules__Rule_MANY_(es);

            //run checks
            RichIterable<? extends Root_meta_analytics_quality_model_CheckResult> enumerationName_enumeration_1__checkResult_many_ = core_analytics_quality_enumerationChecks.Root_meta_analytics_quality_model_domain_enumerationName_Enumeration_1__CheckResult_MANY_(_enumeration, es);
            RichIterable<? extends Root_meta_analytics_quality_model_CheckResult> enumerationValue_enumeration_1__checkResult_many_ = core_analytics_quality_enumerationChecks.Root_meta_analytics_quality_model_domain_enumerationValue_Enumeration_1__CheckResult_MANY_(_enumeration, es);

            enumerationName_enumeration_1__checkResult_many_.forEach(enumerationCheckResults::add);
            enumerationValue_enumeration_1__checkResult_many_.forEach(enumerationCheckResults::add);
        }
        return checkOverallResults(enumerationCheckResults);
    }

    private boolean functionChecks(List<PackageableElement> functions, PureModel pureModel, CompiledExecutionSupport es)
    {
        List<Root_meta_analytics_quality_model_CheckResult> functionCheckResults = new ArrayList<>();

        //create rules
        core_analytics_quality_functionChecks.Root_meta_analytics_quality_model_domain_functionRules__Rule_MANY_(es);

        org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List<ConcreteFunctionDefinition<?>> functionDefinitions = resolveConcreteFunctionDefinitions(functions, pureModel);

        //run checks
        RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression, ? extends org.finos.legend.pure.generated.Root_meta_analytics_quality_model_CheckResult>> validEqualityComparisons_list_1__pair_many_ = core_analytics_quality_functionChecks.Root_meta_analytics_quality_model_domain_validEqualityComparisons_List_1__Pair_MANY_(functionDefinitions, es);
        RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression, ? extends org.finos.legend.pure.generated.Root_meta_analytics_quality_model_CheckResult>> invalidContainsComparisons_list_1__pair_many_ = core_analytics_quality_functionChecks.Root_meta_analytics_quality_model_domain_invalidContainsComparisons_List_1__Pair_MANY_(functionDefinitions, es);
        RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression, ? extends org.finos.legend.pure.generated.Root_meta_analytics_quality_model_CheckResult>> badInstanceOfChecks_list_1__pair_many_ = core_analytics_quality_functionChecks.Root_meta_analytics_quality_model_domain_badInstanceOfChecks_List_1__Pair_MANY_(functionDefinitions, es);
        RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression, ? extends org.finos.legend.pure.generated.Root_meta_analytics_quality_model_CheckResult>> invalidMatchUsages_list_1__pair_many_ = core_analytics_quality_functionChecks.Root_meta_analytics_quality_model_domain_invalidMatchUsages_List_1__Pair_MANY_(functionDefinitions, es);
        RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression, ? extends org.finos.legend.pure.generated.Root_meta_analytics_quality_model_CheckResult>> findUnnecessaryComparisonsToTrue_list_1__pair_many_ = core_analytics_quality_functionChecks.Root_meta_analytics_quality_model_domain_findUnnecessaryComparisonsToTrue_List_1__Pair_MANY_(functionDefinitions, es);
        RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression, ? extends org.finos.legend.pure.generated.Root_meta_analytics_quality_model_CheckResult>> findInvalidCastBugs_list_1__pair_many_ = core_analytics_quality_functionChecks.Root_meta_analytics_quality_model_domain_findInvalidCastBugs_List_1__Pair_MANY_(functionDefinitions, es);
        RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition<? extends java.lang.Object>, ? extends org.finos.legend.pure.generated.Root_meta_analytics_quality_model_CheckResult>> findUnusedPrivateProtectedFunctionBugs_list_1__pair_many_ = core_analytics_quality_functionChecks.Root_meta_analytics_quality_model_domain_findUnusedPrivateProtectedFunctionBugs_List_1__Pair_MANY_(functionDefinitions, es);
        RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression, ? extends org.finos.legend.pure.generated.Root_meta_analytics_quality_model_CheckResult>> findUnnecessaryIfBugs_list_1__pair_many_ = core_analytics_quality_functionChecks.Root_meta_analytics_quality_model_domain_findUnnecessaryIfBugs_List_1__Pair_MANY_(functionDefinitions, es);
        RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression, ? extends org.finos.legend.pure.generated.Root_meta_analytics_quality_model_CheckResult>> findUnnecessaryIfBugs2_list_1__pair_many_ = core_analytics_quality_functionChecks.Root_meta_analytics_quality_model_domain_findUnnecessaryIfBugs2_List_1__Pair_MANY_(functionDefinitions, es);
        RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression, ? extends org.finos.legend.pure.generated.Root_meta_analytics_quality_model_CheckResult>> findUnnecessaryCasts_list_1__pair_many_ = core_analytics_quality_functionChecks.Root_meta_analytics_quality_model_domain_findUnnecessaryCasts_List_1__Pair_MANY_(functionDefinitions, es);
        RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition<? extends java.lang.Object>, ? extends org.finos.legend.pure.generated.Root_meta_analytics_quality_model_CheckResult>> findUnnecessaryLetFunctionsCheck_list_1__pair_many_ = core_analytics_quality_functionChecks.Root_meta_analytics_quality_model_domain_findUnnecessaryLetFunctionsCheck_List_1__Pair_MANY_(functionDefinitions, es);
        RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression, ? extends org.finos.legend.pure.generated.Root_meta_analytics_quality_model_CheckResult>> findPotentiallyExpensiveAssertions_list_1__pair_many_ = core_analytics_quality_functionChecks.Root_meta_analytics_quality_model_domain_findPotentiallyExpensiveAssertions_List_1__Pair_MANY_(functionDefinitions, es);

        validEqualityComparisons_list_1__pair_many_.forEach(each -> functionCheckResults.add(each._second()));
        invalidContainsComparisons_list_1__pair_many_.forEach(each -> functionCheckResults.add(each._second()));
        badInstanceOfChecks_list_1__pair_many_.forEach(each -> functionCheckResults.add(each._second()));
        invalidMatchUsages_list_1__pair_many_.forEach(each -> functionCheckResults.add(each._second()));
        findUnnecessaryComparisonsToTrue_list_1__pair_many_.forEach(each -> functionCheckResults.add(each._second()));
        findInvalidCastBugs_list_1__pair_many_.forEach(each -> functionCheckResults.add(each._second()));
        findUnusedPrivateProtectedFunctionBugs_list_1__pair_many_.forEach(each -> functionCheckResults.add(each._second()));
        findUnnecessaryIfBugs_list_1__pair_many_.forEach(each -> functionCheckResults.add(each._second()));
        findUnnecessaryIfBugs2_list_1__pair_many_.forEach(each -> functionCheckResults.add(each._second()));
        findUnnecessaryCasts_list_1__pair_many_.forEach(each -> functionCheckResults.add(each._second()));
        findUnnecessaryLetFunctionsCheck_list_1__pair_many_.forEach(each -> functionCheckResults.add(each._second()));
        findPotentiallyExpensiveAssertions_list_1__pair_many_.forEach(each -> functionCheckResults.add(each._second()));

        return checkOverallResults(functionCheckResults);
    }

    private boolean propertyChecks(Class<?> _class, CompiledExecutionSupport es)
    {
        ArrayList<Root_meta_analytics_quality_model_CheckResult> propertyCheckResults = new ArrayList<>();
        RichIterable<? extends Property<?, ?>> properties = _class._properties();
        for (Property<?, ?> property : properties)
        {
            //create rules
            core_analytics_quality_propertyChecks.Root_meta_analytics_quality_model_domain_propertyRules__Rule_MANY_(es);

            //run checks
            RichIterable<? extends Root_meta_analytics_quality_model_CheckResult> classPropertyShouldStartWithLowerLetter_abstractProperty_1__checkResult_many_ = core_analytics_quality_propertyChecks.Root_meta_analytics_quality_model_domain_classPropertyShouldStartWithLowerLetter_AbstractProperty_1__CheckResult_MANY_(property, es);
            RichIterable<? extends Root_meta_analytics_quality_model_CheckResult> classPropertyShouldNotStartWithClassName_abstractProperty_1__checkResult_many_ = core_analytics_quality_propertyChecks.Root_meta_analytics_quality_model_domain_classPropertyShouldNotStartWithClassName_AbstractProperty_1__CheckResult_MANY_(property, es);
            RichIterable<? extends Root_meta_analytics_quality_model_CheckResult> classBooleanPropertyShouldStartWithIsOrHasOrEndsWithFlag_abstractProperty_1__checkResult_many_ = core_analytics_quality_propertyChecks.Root_meta_analytics_quality_model_domain_classBooleanPropertyShouldStartWithIsOrHasOrEndsWithFlag_AbstractProperty_1__CheckResult_MANY_(property, es);
            RichIterable<? extends Root_meta_analytics_quality_model_CheckResult> classPropertyStartingWithIsOrHasShouldBeBoolean_abstractProperty_1__checkResult_many_ = core_analytics_quality_propertyChecks.Root_meta_analytics_quality_model_domain_classPropertyStartingWithIsOrHasShouldBeBoolean_AbstractProperty_1__CheckResult_MANY_(property, es);
            RichIterable<? extends Root_meta_analytics_quality_model_CheckResult> classPropertyEndingWithFlagShouldBeBoolean_abstractProperty_1__checkResult_many_ = core_analytics_quality_propertyChecks.Root_meta_analytics_quality_model_domain_classPropertyEndingWithFlagShouldBeBoolean_AbstractProperty_1__CheckResult_MANY_(property, es);
            RichIterable<? extends Root_meta_analytics_quality_model_CheckResult> classPropertyWithToManyMultiplicityAreNamedCorrectly_abstractProperty_1__checkResult_many_ = core_analytics_quality_propertyChecks.Root_meta_analytics_quality_model_domain_classPropertyWithToManyMultiplicityAreNamedCorrectly_AbstractProperty_1__CheckResult_MANY_(property, es);
            RichIterable<? extends Root_meta_analytics_quality_model_CheckResult> classPropertyWithToOneMultiplicityAreNamedCorrectly_abstractProperty_1__checkResult_many_ = core_analytics_quality_propertyChecks.Root_meta_analytics_quality_model_domain_classPropertyWithToOneMultiplicityAreNamedCorrectly_AbstractProperty_1__CheckResult_MANY_(property, es);
            RichIterable<? extends Root_meta_analytics_quality_model_CheckResult> classPropertyIntegersWithToOneMultiplicityAreNamedCorrectly_abstractProperty_1__checkResult_many_ = core_analytics_quality_propertyChecks.Root_meta_analytics_quality_model_domain_classPropertyIntegersWithToOneMultiplicityAreNamedCorrectly_AbstractProperty_1__CheckResult_MANY_(property, es);

            classPropertyShouldStartWithLowerLetter_abstractProperty_1__checkResult_many_.forEach(propertyCheckResults::add);
            classPropertyShouldNotStartWithClassName_abstractProperty_1__checkResult_many_.forEach(propertyCheckResults::add);
            classBooleanPropertyShouldStartWithIsOrHasOrEndsWithFlag_abstractProperty_1__checkResult_many_.forEach(propertyCheckResults::add);
            classPropertyStartingWithIsOrHasShouldBeBoolean_abstractProperty_1__checkResult_many_.forEach(propertyCheckResults::add);
            classPropertyEndingWithFlagShouldBeBoolean_abstractProperty_1__checkResult_many_.forEach(propertyCheckResults::add);
            classPropertyWithToManyMultiplicityAreNamedCorrectly_abstractProperty_1__checkResult_many_.forEach(propertyCheckResults::add);
            classPropertyWithToOneMultiplicityAreNamedCorrectly_abstractProperty_1__checkResult_many_.forEach(propertyCheckResults::add);
            classPropertyIntegersWithToOneMultiplicityAreNamedCorrectly_abstractProperty_1__checkResult_many_.forEach(propertyCheckResults::add);
        }
        return checkOverallResults(propertyCheckResults);
    }

    private boolean checkOverallResults(List<Root_meta_analytics_quality_model_CheckResult> results)
    {
        for (Root_meta_analytics_quality_model_CheckResult rs : results)
        {
            if (!rs._isValid())
            {
                messages.add(rs._message());
            }
        }
        return messages.isEmpty();
    }

    private org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List<ConcreteFunctionDefinition<?>> resolveConcreteFunctionDefinitions(List<PackageableElement> functions, PureModel pureModel)
    {

        org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List<ConcreteFunctionDefinition<?>> cfd = new Root_meta_pure_functions_collection_List_Impl<>(pureModel.getRoot()._name());
        for (PackageableElement pe : functions)
        {
            cfd._valuesAdd(pureModel.getConcreteFunctionDefinition(pe.getPath(), pe.sourceInformation));
        }
        return cfd;
    }
}
