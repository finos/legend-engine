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

package org.finos.legend.engine.shared.core.operational.errorManagement;

import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;

import java.util.Objects;
import java.util.function.Function;

public class ChangeInstruction<T>
{
    public enum ChangeType
    {
        ADD,
        DELETE,
        REPLACE
    }

    private final T coreObject;
    private final PackageableElement topLevelObject;
    private final String propertyToCorrect;
    private final Object correctValue;
    private final ChangeType changeType;
    private final String messageToUser;
    private final SourceInformation sourceInformation;
    private final Function<PackageableElement, T> routeFromTopLevelToCore;

    public ChangeInstruction(T coreObject, PackageableElement topLevelObject, String propertyToCorrect,
                             Object correctValue, ChangeType changeType, String messageToUser,
                             SourceInformation sourceInformation, Function<PackageableElement, T> routeFromTopLevelToCore)
    {
        this.coreObject = Objects.requireNonNull(coreObject);
        this.topLevelObject = Objects.requireNonNull(topLevelObject);
        this.propertyToCorrect = Objects.requireNonNull(propertyToCorrect);
        this.correctValue = Objects.requireNonNull(correctValue);
        this.changeType = Objects.requireNonNull(changeType);
        this.sourceInformation = Objects.requireNonNull(sourceInformation);
        this.messageToUser = Objects.requireNonNull(messageToUser);
        this.routeFromTopLevelToCore = Objects.requireNonNull(routeFromTopLevelToCore);
    }

    public Object getCoreObject()
    {
        return coreObject;
    }

    public PackageableElement getTopLevelObject()
    {
        return topLevelObject;
    }

    public String getPropertyToCorrect()
    {
        return propertyToCorrect;
    }

    public Object getCorrectValue()
    {
        return correctValue;
    }

    public ChangeType getChangeType()
    {
        return changeType;
    }

    public SourceInformation getSourceInformation()
    {
        return sourceInformation;
    }

    public String getMessageToUser()
    {
        return messageToUser;
    }

    public Function<PackageableElement, T> getRouteFromTopLevelToCore()
    {
        return routeFromTopLevelToCore;
    }

    @Override
    public final boolean equals(Object other)
    {
        if (!(other instanceof ChangeInstruction))
        {
            return false;
        }
        ChangeInstruction otherInstruction = (ChangeInstruction) other;
        return coreObject.equals(otherInstruction.coreObject) && topLevelObject.equals(otherInstruction.topLevelObject)
                && propertyToCorrect.equals(otherInstruction.propertyToCorrect) && changeType.equals(otherInstruction.changeType)
                && messageToUser.equals(otherInstruction.messageToUser) && routeFromTopLevelToCore.equals(otherInstruction.routeFromTopLevelToCore);
    }

    @Override
    public final int hashCode()
    {
        return Objects.hash(coreObject, topLevelObject, propertyToCorrect, correctValue,
                changeType, sourceInformation, messageToUser, routeFromTopLevelToCore);
    }

    @Override
    public String toString()
    {
        return "ChangeInstruction{\n" +
                "coreObject: " + coreObject + ",\n" +
                "topLevelObject: " + topLevelObject.name + ",\n" +
                "propertyToCorrect: " + propertyToCorrect + ",\n" +
                "correctValue: " + correctValue + ",\n" +
                "changeType: " + changeType + "\n" +
                "messageToUser: " + messageToUser + "\n" +
                "sourceInformation: " + sourceInformation.getMessage() + "\n" +
                "}";
    }
}
