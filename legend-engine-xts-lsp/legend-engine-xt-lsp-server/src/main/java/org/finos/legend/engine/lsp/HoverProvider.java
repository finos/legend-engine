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

package org.finos.legend.engine.lsp;

import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

/**
 * Provides hover information by navigating to the element under the cursor
 * and returning its type, qualified path, and source location.
 */
public class HoverProvider
{
    /**
     * Get hover information for the element at the given position.
     * Line and column are 1-based (PureRuntime convention).
     * Returns null if no hoverable element is found.
     */
    public static Hover hover(PureRuntime runtime, String sourceId, int line, int column)
    {
        Source source = runtime.getSourceById(sourceId);
        if (source == null)
        {
            return null;
        }

        CoreInstance raw = source.navigate(line, column, runtime.getProcessorSupport());
        if (raw == null)
        {
            return null;
        }

        // Resolve ImportStubs to their actual targets
        CoreInstance found = ImportStub.withImportStubByPass(raw, runtime.getProcessorSupport());
        if (found == null)
        {
            return null;
        }

        String classifierName = found.getClassifier().getName();
        String content = formatHover(found, classifierName, runtime);
        if (content == null)
        {
            return null;
        }

        MarkupContent markup = new MarkupContent();
        markup.setKind(MarkupKind.MARKDOWN);
        markup.setValue(content);
        return new Hover(markup);
    }

    static String formatHover(CoreInstance element, String classifierName, PureRuntime runtime)
    {
        StringBuilder sb = new StringBuilder();

        // Header: element type
        String qualifiedPath = getQualifiedPath(element);

        if (qualifiedPath != null)
        {
            sb.append("**").append(classifierName).append("** `").append(qualifiedPath).append("`");
        }
        else
        {
            sb.append("**").append(classifierName).append("** `").append(getElementName(element, runtime)).append("`");
        }

        // For classes: list properties
        if ("Class".equals(classifierName))
        {
            appendClassProperties(element, sb, runtime);
        }
        // For enumerations: list values
        else if ("Enumeration".equals(classifierName))
        {
            appendEnumValues(element, sb);
        }

        // Source location
        SourceInformation si = element.getSourceInformation();
        if (si != null)
        {
            sb.append("\n\n---\n*Defined in ").append(si.getSourceId())
                    .append(":").append(si.getStartLine()).append("*");
        }

        return sb.toString();
    }

    private static String getQualifiedPath(CoreInstance element)
    {
        try
        {
            if (element instanceof Package)
            {
                return PackageableElement.getUserPathForPackageableElement(element);
            }
            // Check if element has a _package property (i.e., it's a PackageableElement)
            CoreInstance pkg = element.getValueForMetaPropertyToOne(M3Properties._package);
            if (pkg != null)
            {
                return PackageableElement.getUserPathForPackageableElement(element);
            }
        }
        catch (Exception ignored)
        {
            // Not a packageable element
        }
        return null;
    }

    private static void appendClassProperties(CoreInstance classElement, StringBuilder sb, PureRuntime runtime)
    {
        try
        {
            ListIterable<? extends CoreInstance> properties = classElement.getValueForMetaPropertyToMany(M3Properties.properties);
            if (properties != null && properties.notEmpty())
            {
                sb.append("\n\n```\n");
                for (CoreInstance prop : properties)
                {
                    String propName = prop.getValueForMetaPropertyToOne(M3Properties.name).getName();
                    sb.append("  ").append(propName);
                    try
                    {
                        CoreInstance genericType = prop.getValueForMetaPropertyToOne(M3Properties.genericType);
                        if (genericType != null)
                        {
                            // Use GenericType.print() which handles type arguments,
                            // multiplicity arguments, and function signatures
                            String typeStr = GenericType.print(genericType, runtime.getProcessorSupport());
                            sb.append(": ").append(typeStr);
                        }
                        CoreInstance multiplicity = prop.getValueForMetaPropertyToOne(M3Properties.multiplicity);
                        if (multiplicity != null)
                        {
                            sb.append(formatMultiplicity(multiplicity));
                        }
                    }
                    catch (Exception ignored)
                    {
                        // Type info unavailable
                    }
                    sb.append("\n");
                }
                sb.append("```");
            }
        }
        catch (Exception ignored)
        {
            // Properties not accessible
        }
    }

    private static void appendEnumValues(CoreInstance enumeration, StringBuilder sb)
    {
        try
        {
            ListIterable<? extends CoreInstance> values = enumeration.getValueForMetaPropertyToMany(M3Properties.values);
            if (values != null && values.notEmpty())
            {
                sb.append("\n\n```\n");
                for (CoreInstance val : values)
                {
                    sb.append("  ").append(val.getName()).append("\n");
                }
                sb.append("```");
            }
        }
        catch (Exception ignored)
        {
            // Values not accessible
        }
    }

    /**
     * Get a human-readable name for an element. Resolves ImportStubs first,
     * then returns the short name for packageable elements (e.g., "String", "Person").
     */
    private static String getElementName(CoreInstance element, PureRuntime runtime)
    {
        // Resolve ImportStub if needed
        CoreInstance resolved = element;
        try
        {
            resolved = ImportStub.withImportStubByPass(element, runtime.getProcessorSupport());
            if (resolved == null)
            {
                resolved = element;
            }
        }
        catch (Exception ignored)
        {
            // Keep original
        }

        try
        {
            CoreInstance pkg = resolved.getValueForMetaPropertyToOne(M3Properties._package);
            if (pkg != null)
            {
                String fullPath = PackageableElement.getUserPathForPackageableElement(resolved);
                int lastSep = fullPath.lastIndexOf("::");
                return (lastSep >= 0) ? fullPath.substring(lastSep + 2) : fullPath;
            }
        }
        catch (Exception ignored)
        {
            // Fall through
        }

        String name = resolved.getName();
        if (name != null && !name.isEmpty() && !name.startsWith("@"))
        {
            return name;
        }

        return resolved.getClassifier().getName();
    }

    private static String formatMultiplicity(CoreInstance multiplicity)
    {
        try
        {
            CoreInstance lowerBound = multiplicity.getValueForMetaPropertyToOne(M3Properties.lowerBound);
            CoreInstance upperBound = multiplicity.getValueForMetaPropertyToOne(M3Properties.upperBound);

            long lower = 0;
            long upper = -1; // -1 means unbounded (*)

            if (lowerBound != null)
            {
                CoreInstance lowerValue = lowerBound.getValueForMetaPropertyToOne(M3Properties.value);
                if (lowerValue != null)
                {
                    lower = PrimitiveUtilities.getIntegerValue(lowerValue).longValue();
                }
            }
            if (upperBound != null)
            {
                CoreInstance upperValue = upperBound.getValueForMetaPropertyToOne(M3Properties.value);
                if (upperValue != null)
                {
                    upper = PrimitiveUtilities.getIntegerValue(upperValue).longValue();
                }
            }

            if (upper < 0)
            {
                if (lower == 0)
                {
                    return "[*]";
                }
                return "[" + lower + "..*]";
            }
            if (lower == upper)
            {
                return "[" + lower + "]";
            }
            return "[" + lower + ".." + upper + "]";
        }
        catch (Exception e)
        {
            return "";
        }
    }
}
