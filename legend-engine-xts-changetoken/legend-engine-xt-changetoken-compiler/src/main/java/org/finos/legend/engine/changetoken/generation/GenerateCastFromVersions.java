//  Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.changetoken.generation;

import org.finos.legend.engine.external.language.java.generation.GenerateJavaProject;
import org.finos.legend.pure.generated.Root_meta_external_language_java_metamodel_project_Project;
import org.finos.legend.pure.generated.Root_meta_pure_changetoken_Versions;
import org.finos.legend.pure.generated.core_pure_changetoken_cast_generation;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;

public class GenerateCastFromVersions extends GenerateJavaProject
{
    private final Root_meta_pure_changetoken_Versions versions;
    private final String outputClassName;
    private boolean alwaysStampAtRootVersion = true;
    private boolean optionalStampAllVersions = false;
    private boolean obsoleteJsonAsString = true;
    private String typeKeyName = "@type";
    private String versionKeyName = "version";

    public GenerateCastFromVersions(String outputDirectory, Root_meta_pure_changetoken_Versions versions, String outputClassName)
    {
        super(outputDirectory);
        this.versions = versions;
        this.outputClassName = outputClassName;
    }

    public boolean isAlwaysStampAtRootVersion()
    {
        return alwaysStampAtRootVersion;
    }

    public void setAlwaysStampAtRootVersion(boolean alwaysStampAtRootVersion)
    {
        this.alwaysStampAtRootVersion = alwaysStampAtRootVersion;
    }

    public boolean isOptionalStampAllVersions()
    {
        return optionalStampAllVersions;
    }

    public void setOptionalStampAllVersions(boolean optionalStampAllVersions)
    {
        this.optionalStampAllVersions = optionalStampAllVersions;
    }

    public boolean isObsoleteJsonAsString()
    {
        return obsoleteJsonAsString;
    }

    public void setObsoleteJsonAsString(boolean obsoleteJsonAsString)
    {
        this.obsoleteJsonAsString = obsoleteJsonAsString;
    }

    public String getTypeKeyName()
    {
        return typeKeyName;
    }

    public void setTypeKeyName(String typeKeyName)
    {
        this.typeKeyName = typeKeyName;
    }

    public String getVersionKeyName()
    {
        return versionKeyName;
    }

    public void setVersionKeyName(String versionKeyName)
    {
        this.versionKeyName = versionKeyName;
    }

    @Override
    protected Root_meta_external_language_java_metamodel_project_Project doExecute(CompiledExecutionSupport executionSupport)
    {
        return doExecute(versions, executionSupport);
    }

    protected Root_meta_external_language_java_metamodel_project_Project doExecute(Root_meta_pure_changetoken_Versions versions, CompiledExecutionSupport executionSupport)
    {
        return core_pure_changetoken_cast_generation.Root_meta_pure_changetoken_cast_generation_generateCastFromVersions_Boolean_1__Boolean_1__Boolean_1__String_1__String_$0_1$__Versions_1__String_1__Project_1_(
                alwaysStampAtRootVersion, optionalStampAllVersions,
                obsoleteJsonAsString,
                typeKeyName, versionKeyName,
                versions, outputClassName, executionSupport);
    }
}
