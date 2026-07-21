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

package org.finos.legend.engine.testable.dataquality.extension;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.protocol.dataquality.metamodel.DataqualityRelationValidation;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.testable.extension.TestRunner;
import org.finos.legend.engine.testable.extension.TestableRunnerExtension;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_DataQualityRelationValidation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.testable.Testable;

public class DataQualityRelationValidationTestableRunnerExtension implements TestableRunnerExtension
{
    public static final String CLASSIFIER_PATH = "meta::external::dataquality::DataQualityRelationValidation";

    private String pureVersion = PureClientVersions.production;

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("PackageableElement", "DataQualityValidation");
    }

    @Override
    public String getSupportedClassifierPath()
    {
        return CLASSIFIER_PATH;
    }

    @Override
    public Boolean isTestable(PackageableElement element)
    {
        return element instanceof DataqualityRelationValidation;
    }

    @Override
    public Boolean isTestableEmpty(PackageableElement element)
    {
        DataqualityRelationValidation validation = (DataqualityRelationValidation) element;
        return validation.testSuites == null || validation.testSuites.isEmpty();
    }

    @Override
    public TestRunner getTestRunner(Testable testable)
    {
        if (testable instanceof Root_meta_external_dataquality_DataQualityRelationValidation)
        {
            return new DataQualityRelationValidationTestRunner((Root_meta_external_dataquality_DataQualityRelationValidation) testable, this.pureVersion);
        }
        return null;
    }

    public void setPureVersion(String pureVersion)
    {
        this.pureVersion = pureVersion;
    }
}
