//  Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.m3.function.property.Property;
import org.finos.legend.engine.protocol.pure.m3.type.Class;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.operational.errorManagement.ChangeInstruction;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestChangeInstructions
{

    @Test
    public void testExceptionMessageDueToChangeInstructions()
    {
        String pureCode = "Class demo::test\n" +
                "{\n" +
                "  first_name: String[1];\n" +
                "  last_name: String[1];\n" +
                "}";

        PureModelContextData pmcd = PureGrammarParser.newInstance().parseModel(pureCode);
        PureModel pureModel = new PureModel(pmcd, Identity.getAnonymousIdentity().getName(), DeploymentMode.TEST);
        Class srcClass = pmcd.getElementsOfType(Class.class).get(0);
        Property firstNameProp = srcClass.properties.get(0);
        Property lastNameProp = srcClass.properties.get(1);

        firstNameProp.sourceInformation.sourceId = "test2/demo.pure";
        lastNameProp.sourceInformation.sourceId = "test/demo.pure";

        ChangeInstruction<Property> changeInstruction = new ChangeInstruction<>(firstNameProp, srcClass, "name",
                "firstName", ChangeInstruction.ChangeType.REPLACE, "You must change the name of your property to firstName",
                firstNameProp.sourceInformation, TestChangeInstructions::getPropertiesFromClass);

        ChangeInstruction<Property> changeInstruction2 = new ChangeInstruction<>(lastNameProp, srcClass, "name",
                "lastName", ChangeInstruction.ChangeType.REPLACE, "You must change the name of your property to lastName",
                lastNameProp.sourceInformation, TestChangeInstructions::getPropertiesFromClass);

        pureModel.addChangeInstructions(Lists.mutable.with(changeInstruction, changeInstruction2));

        assertEquals("The following changes must be applied before your model can compile:\n" +
                "   test/demo.pure[4:3-23]: You must change the name of your property to lastName,\n" +
                "   test2/demo.pure[3:3-24]: You must change the name of your property to firstName", pureModel.getChangeInstructionsExceptionMessage());
    }

    @Test
    public void testChangeInstructionToStringMethod()
    {
        Class srcClass = new Class();
        srcClass.name = "srcClass";
        Property property = new Property();
        property.name = "TEST1";
        property.sourceInformation = new SourceInformation("clas1", 0, 0, 5, 10);
        srcClass.properties = Lists.mutable.with(property);

        ChangeInstruction<Property> changeInstruction = new ChangeInstruction<Property>(property, srcClass, "name",
                "TEST2", ChangeInstruction.ChangeType.REPLACE, "You must change the name of your property to TEST2",
                property.sourceInformation, TestChangeInstructions::getPropertiesFromClass);

        assertEquals("ChangeInstruction{\n" +
                "coreObject: " + property + ",\n" +
                "topLevelObject: srcClass,\n" +
                "propertyToCorrect: name,\n" +
                "correctValue: TEST2,\n" +
                "changeType: REPLACE\n" +
                "messageToUser: You must change the name of your property to TEST2\n" +
                "sourceInformation: [0:0-5:10]\n" +
                "}", changeInstruction.toString());
    }

    private static Property getPropertiesFromClass(PackageableElement packageableElement)
    {
        return ((Class)packageableElement).properties.get(0);
    }
}
