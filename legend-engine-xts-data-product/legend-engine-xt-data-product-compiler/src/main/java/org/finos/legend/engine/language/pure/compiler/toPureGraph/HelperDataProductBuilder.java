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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataProduct.DataProductElementPointer;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataProduct_DataProduct;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;

public class HelperDataProductBuilder
{
    public static Root_meta_pure_metamodel_dataProduct_DataProduct getDataProduct(String fullPath, SourceInformation sourceInformation, CompileContext context)
    {
        Root_meta_pure_metamodel_dataProduct_DataProduct dataProduct = (Root_meta_pure_metamodel_dataProduct_DataProduct) context.pureModel.getPackageableElement_safe(fullPath);
        Assert.assertTrue(dataProduct != null, () -> "Can't find data product '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION);
        return dataProduct;
    }

    public static Root_meta_pure_metamodel_dataProduct_DataProduct resolveDataProduct(String fullPath, SourceInformation sourceInformation, CompileContext context)
    {
        return context.resolve(fullPath, sourceInformation, path -> getDataProduct(path, sourceInformation, context));
    }

    private static void collectPackageElements(Package pack, MutableSet<PackageableElement> elements, MutableSet<String> excludePaths, CompileContext context)
    {
        pack._children().forEach(child ->
        {
            String path = HelperModelBuilder.getElementFullPath(child, context.pureModel.getExecutionSupport());
            if (excludePaths.contains(path))
            {
                return;
            }
            if (child instanceof Class || child instanceof Enumeration || child instanceof Association)
            {
                elements.add(child);
            }
            else if (child instanceof Package)
            {
                collectPackageElements((Package) child, elements, excludePaths, context);
            }
        });
    }

    public static void collectElements(DataProductElementPointer include, MutableSet<PackageableElement> elements, MutableSet<String> excludePaths, CompileContext context)
    {
        PackageableElement element = context.pureModel.getPackageableElement(include.path, include.sourceInformation);
        if (excludePaths.contains(include.path))
        {
            return;
        }
        if (element instanceof Class || element instanceof Enumeration || element instanceof Association)
        {
            elements.add(element);
        }
        else if (element instanceof Package)
        {
            collectPackageElements((Package) element, elements, excludePaths, context);
        }
        else
        {
            throw new EngineException("Included element is not of supported types (only packages, classes, enumerations, and associations are supported)", include.sourceInformation, EngineErrorType.COMPILATION);
        }
    }
}

