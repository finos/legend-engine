package org.finos.legend.engine.language.pure.grammar.integration.util;

import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.PropertyInstance;

public class AssociationPropertyToClassName
{
    private String propertyName;
    private String classFullPath;
    private org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.PropertyInstance propertyInstance;

    public AssociationPropertyToClassName(String propertyName, String classFullPath, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.PropertyInstance propertyInstance)
    {
        this.propertyName = propertyName;
        this.classFullPath = classFullPath;
        this.propertyInstance = propertyInstance;
    }

    public String getClassFullPath()
    {
        return this.classFullPath;
    }

    public String getPropertyName()
    {
        return this.propertyName;
    }

    public PropertyInstance getPropertyInstance()
    {
        return this.propertyInstance;
    }
}
