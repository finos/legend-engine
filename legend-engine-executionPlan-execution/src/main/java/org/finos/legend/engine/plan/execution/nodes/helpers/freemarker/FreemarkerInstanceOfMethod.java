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

package org.finos.legend.engine.plan.execution.nodes.helpers.freemarker;

import freemarker.ext.beans.StringModel;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

import java.util.List;
import java.util.stream.Stream;

public class FreemarkerInstanceOfMethod implements TemplateMethodModelEx
{
    public TemplateModel exec(List args) throws TemplateModelException
    {
        if (args.size() != 2)
        {
            throw new TemplateModelException("Instance Of function expects 2 arguments. Found : " + args.size());
        }
        return (TemplateBooleanModel) () ->
        {

            if (args.get(1).toString().equalsIgnoreCase("stream") && args.get(0) instanceof StringModel)
            {
                return (((StringModel) args.get(0)).getWrappedObject() instanceof Stream);
            }
            else
            {
                return false;
            }
        };
    }
}
