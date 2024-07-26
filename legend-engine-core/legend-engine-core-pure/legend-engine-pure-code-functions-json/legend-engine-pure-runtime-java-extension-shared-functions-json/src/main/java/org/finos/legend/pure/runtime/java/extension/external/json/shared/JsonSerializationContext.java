// Copyright 2020 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.extension.external.json.shared;

import org.eclipse.collections.api.RichIterable;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.ConversionCache;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.SerializationContext;

import java.util.Stack;

public abstract class JsonSerializationContext<F, T> extends SerializationContext
{
    private final String typeKeyName;
    private final boolean includeType;
    private final boolean fullyQualifiedTypePath;
    private final boolean serializeQualifiedProperties;
    private final String dateTimeFormat;
    private final boolean serializePackageableElementName;
    private final boolean removePropertiesWithEmptyValues;
    private final boolean serializeMultiplicityAsNumber;
    private final boolean enableEncryption;
    private final String encryptionKey;
    private final RichIterable<? extends CoreInstance> encryptionStereotypes;
    private final boolean enableDecryption;
    private final String decryptionKey;
    private final RichIterable<? extends CoreInstance> decryptionStereotypes;

    public JsonSerializationContext(ConversionCache conversionCache, SourceInformation sourceInformation, ProcessorSupport processorSupport, Stack visitedInstances,
                                    String typeKeyName, boolean includeType, boolean fullyQualifiedTypePath, boolean serializeQualifiedProperties,
                                    String dateTimeFormat, boolean serializePackageableElementName, boolean removePropertiesWithEmptyValues,
                                    boolean serializeMultiplicityAsNumber, String encryptionKey, RichIterable<? extends CoreInstance> encryptionStereotypes, String decryptionKey, RichIterable<? extends CoreInstance> decryptionStereotypes)
    {
        super(conversionCache, sourceInformation, processorSupport, visitedInstances);
        this.typeKeyName = typeKeyName;
        this.includeType = includeType;
        this.fullyQualifiedTypePath = fullyQualifiedTypePath;
        this.serializeQualifiedProperties = serializeQualifiedProperties;
        this.dateTimeFormat = dateTimeFormat;
        this.serializePackageableElementName = serializePackageableElementName;
        this.removePropertiesWithEmptyValues = removePropertiesWithEmptyValues;
        this.serializeMultiplicityAsNumber = serializeMultiplicityAsNumber;
        this.enableEncryption = encryptionKey != null;
        this.encryptionKey = encryptionKey;
        this.encryptionStereotypes = encryptionStereotypes;
        this.enableDecryption = decryptionKey != null;
        this.decryptionKey = decryptionKey;
        this.decryptionStereotypes = decryptionStereotypes;
    }

    protected abstract Object extractPrimitiveValue(Object potentiallyWrappedPrimitive);

    protected abstract Object getValueForProperty(F pureObject, Property property, String className);

    protected abstract Object evaluateQualifiedProperty(F pureObject, QualifiedProperty qualifiedProperty, Type type, Multiplicity multiplicity, String propertyName);

    protected abstract CoreInstance getClassifier(F pureObject);

    protected abstract RichIterable<CoreInstance> getMapKeyValues(F pureObject);

    public String getTypeKeyName()
    {
        return this.typeKeyName;
    }

    public boolean isIncludeType()
    {
        return this.includeType;
    }

    public boolean isFullyQualifiedTypePath()
    {
        return this.fullyQualifiedTypePath;
    }

    public boolean isSerializeQualifiedProperties()
    {
        return this.serializeQualifiedProperties;
    }

    public String getDateTimeFormat()
    {
        return this.dateTimeFormat;
    }

    public boolean isSerializePackageableElementName()
    {
        return this.serializePackageableElementName;
    }

    public boolean isRemovePropertiesWithEmptyValues()
    {
        return this.removePropertiesWithEmptyValues;
    }

    public boolean isSerializeMultiplicityAsNumber()
    {
        return this.serializeMultiplicityAsNumber;
    }

    public boolean isEnableEncryption()
    {
        return this.enableEncryption;
    }

    public String getEncryptionKey()
    {
        return this.encryptionKey;
    }

    public RichIterable<? extends CoreInstance> getEncryptionStereotypes()
    {
        return this.encryptionStereotypes;
    }

    public boolean isEnableDecryption()
    {
        return this.enableDecryption;
    }

    public String getDecryptionKey()
    {
        return this.decryptionKey;
    }

    public RichIterable<? extends CoreInstance> getDecryptionStereotypes()
    {
        return this.decryptionStereotypes;
    }
}
