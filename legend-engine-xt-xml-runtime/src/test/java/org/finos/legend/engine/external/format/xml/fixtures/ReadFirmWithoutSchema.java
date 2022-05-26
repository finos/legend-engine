// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.external.format.xml.fixtures;

import org.finos.legend.engine.external.format.xml.read.handlers.Attribute;
import org.finos.legend.engine.external.format.xml.read.handlers.Choice;
import org.finos.legend.engine.external.format.xml.read.handlers.FlexCollectionElement;
import org.finos.legend.engine.external.format.xml.read.valueProcessors.AddBooleanToObject;
import org.finos.legend.engine.external.format.xml.read.valueProcessors.AddDoubleToObject;
import org.finos.legend.engine.external.format.xml.read.valueProcessors.AddLongToObject;
import org.finos.legend.engine.external.format.xml.shared.datatypes.BooleanSimpleTypeHandler;
import org.finos.legend.engine.external.format.xml.shared.datatypes.BuiltInDataTypes;
import org.finos.legend.engine.external.format.xml.shared.datatypes.DoubleSimpleTypeHandler;
import org.finos.legend.engine.external.format.xml.shared.datatypes.LongSimpleTypeHandler;
import org.finos.legend.engine.external.format.xml.shared.datatypes.SimpleTypeHandler;
import org.finos.legend.engine.external.format.xml.read.DeserializeContext;
import org.finos.legend.engine.external.format.xml.read.handlers.AnySurroundingElements;
import org.finos.legend.engine.external.format.xml.read.handlers.Document;
import org.finos.legend.engine.external.format.xml.read.handlers.Element;
import org.finos.legend.engine.external.format.xml.read.handlers.Sequence;
import org.finos.legend.engine.external.format.xml.read.handlers.TextContent;
import org.finos.legend.engine.external.format.xml.read.valueProcessors.AddObjectToObject;
import org.finos.legend.engine.external.shared.runtime.fixtures.firmModel.Firm;
import org.finos.legend.engine.external.shared.runtime.fixtures.firmModel.Person;
import org.finos.legend.engine.external.shared.runtime.dependencies.ExternalDataObjectAdder;

import java.time.temporal.Temporal;

public class ReadFirmWithoutSchema
{
    public void read(DeserializeContext<?> context)
    {
        SimpleTypeHandler<String> xsToken = context.simpleTypesContext.handler(BuiltInDataTypes.XS_TOKEN);
        LongSimpleTypeHandler xsLong = (LongSimpleTypeHandler) context.simpleTypesContext.<Long>handler(BuiltInDataTypes.XS_LONG);
        SimpleTypeHandler<Temporal> xsDate = context.simpleTypesContext.handler(BuiltInDataTypes.XS_DATE);
        BooleanSimpleTypeHandler xsBoolean = (BooleanSimpleTypeHandler) context.simpleTypesContext.<Long>handler(BuiltInDataTypes.XS_BOOLEAN);
        DoubleSimpleTypeHandler xsDouble = (DoubleSimpleTypeHandler) context.simpleTypesContext.<Long>handler(BuiltInDataTypes.XS_DOUBLE);

        Element firstName = Element.ofLenient(1, 1, "firstName")
                .add(new TextContent<Person>(new AddObjectToObject<Person, String>(Person._getAdderForProperty("firstName"), xsToken)));

        Element lastName = Element.ofLenient(1, 1, "lastName")
                .add(new TextContent<Person>(new AddObjectToObject<Person, String>(Person._getAdderForProperty("lastName"), xsToken)));

        Element dateOfBirth = Element.ofLenient(1, 1, "dateOfBirth")
                .add(new TextContent<Person>(new AddObjectToObject<Person, Temporal>(Person._getAdderForProperty("dateOfBirth"), xsDate)));

        Element isAlive = Element.ofLenient(1, 1, "isAlive")
                .add(new TextContent<Person>(new AddBooleanToObject<Person>(Person._getAdderForProperty("isAlive"), xsBoolean)));

        Element heightInMeters = Element.ofLenient(1, 1, "heightInMeters")
                .add(new TextContent<Person>(new AddDoubleToObject<Person>(Person._getAdderForProperty("heightInMeters"), xsDouble)));

        Choice personChoice = Choice.of(0, Long.MAX_VALUE)
                                    .add(firstName)
                                    .add(lastName)
                                    .add(dateOfBirth)
                                    .add(isAlive)
                                    .add(heightInMeters);

        Element wildcardPersonElement = Element.ofWildcard(0, Long.MAX_VALUE, Person.FACTORY, (ExternalDataObjectAdder) Firm._getAdderForProperty("employees"))
                .add(Attribute.ofLenient(0, 1, "firstName", new AddObjectToObject<Person, String>(Person._getAdderForProperty("firstName"), xsToken)))
                .add(Attribute.ofLenient(0, 1, "lastName", new AddObjectToObject<Person, String>(Person._getAdderForProperty("lastName"), xsToken)))
                .add(Attribute.ofLenient(0, 1, "dateOfBirth", new AddObjectToObject<Person, Temporal>(Person._getAdderForProperty("dateOfBirth"), xsDate)))
                .add(Attribute.ofLenient(0, 1, "isAlive", new AddBooleanToObject<Person>(Person._getAdderForProperty("isAlive"), xsBoolean)))
                .add(Attribute.ofLenient(0, 1, "heightInMeters", new AddDoubleToObject<Person>(Person._getAdderForProperty("heightInMeters"), xsDouble)))
                .add(personChoice);

        Element oneEmployeesElement = Element.ofLenient(1, 1, "employees")
                .add(new Sequence(1, 1).add(wildcardPersonElement));

        Element manyEmployeesElement = Element.ofLenient(0, Long.MAX_VALUE, "employees", Person.FACTORY, (ExternalDataObjectAdder) Firm._getAdderForProperty("employees"))
                .add(Attribute.ofLenient(0, 1, "firstName", new AddObjectToObject<Person, String>(Person._getAdderForProperty("firstName"), xsToken)))
                .add(Attribute.ofLenient(0, 1, "lastName", new AddObjectToObject<Person, String>(Person._getAdderForProperty("lastName"), xsToken)))
                .add(Attribute.ofLenient(0, 1, "dateOfBirth", new AddObjectToObject<Person, Temporal>(Person._getAdderForProperty("dateOfBirth"), xsDate)))
                .add(Attribute.ofLenient(0, 1, "isAlive", new AddBooleanToObject<Person>(Person._getAdderForProperty("isAlive"), xsBoolean)))
                .add(Attribute.ofLenient(0, 1, "heightInMeters", new AddDoubleToObject<Person>(Person._getAdderForProperty("heightInMeters"), xsDouble)))
                .add(personChoice);

        Element flexManyEmployeesElement = FlexCollectionElement.ofLenient(0, Long.MAX_VALUE, "employees", Person.FACTORY, (ExternalDataObjectAdder) Firm._getAdderForProperty("employees"))
                                                                .add(Attribute.ofLenient(0, 1, "firstName", new AddObjectToObject<Person, String>(Person._getAdderForProperty("firstName"), xsToken)))
                                                                .add(Attribute.ofLenient(0, 1, "lastName", new AddObjectToObject<Person, String>(Person._getAdderForProperty("lastName"), xsToken)))
                                                                .add(Attribute.ofLenient(0, 1, "dateOfBirth", new AddObjectToObject<Person, Temporal>(Person._getAdderForProperty("dateOfBirth"), xsDate)))
                                                                .add(Attribute.ofLenient(0, 1, "isAlive", new AddBooleanToObject<Person>(Person._getAdderForProperty("isAlive"), xsBoolean)))
                                                                .add(Attribute.ofLenient(0, 1, "heightInMeters", new AddDoubleToObject<Person>(Person._getAdderForProperty("heightInMeters"), xsDouble)))
                                                                .add(personChoice);

        Element nameElement = Element.ofLenient(1, 1, "name")
                .add(new TextContent<Firm>(new AddObjectToObject<Firm, String>(Firm._getAdderForProperty("name"), xsToken)));

        Element rankingElement = Element.ofLenient(1, 1, "ranking")
                .add(new TextContent<Firm>(new AddLongToObject<Firm>(Firm._getAdderForProperty("ranking"), xsLong)));

        Choice firmChoice = Choice.of(0, Long.MAX_VALUE)
                .add(nameElement)
                .add(rankingElement)
                .add(flexManyEmployeesElement);

        Element firmElement = Element.ofLenient(1, 1, "Firm", Firm.FACTORY, null)
                .add(Attribute.ofLenient(0, 1, "name", new AddObjectToObject<Firm, String>(Firm._getAdderForProperty("name"), xsToken)))
                .add(Attribute.ofLenient(0, 1, "ranking", new AddLongToObject<Firm>(Firm._getAdderForProperty("ranking"), xsLong)))
                .add(firmChoice);

        new Document(new AnySurroundingElements(firmElement)).process(context);
    }
}
