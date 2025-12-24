// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.reversePCT.http;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.pure.generated.core_reverse_pct_generateDocumentation;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

@Api(tags = "PCT")
@Path("pct")
@Produces(MediaType.TEXT_HTML)
public class Documentation
{
    @GET
    @ApiOperation(value = "")
    @Path("reversePCTdocumentation")
    public Response reversePCTdocumentation(
            @QueryParam("file")
            @ApiParam("The file path to generate a Doc for") String file,
            @QueryParam("reverseFunction")
            @ApiParam("The function that provides the reverse information for reverse PCT tests") String reverseFunction
    )
    {
        PureModel pureModel = PureModel.getCorePureModel();
        String result = core_reverse_pct_generateDocumentation.Root_meta_pure_test_pct_reversePCT_framework_documentation_generateDoc_String_1__String_1__String_1_(file, reverseFunction, pureModel.getExecutionSupport());
        return Response.status(Response.Status.OK).entity(
                "<!DOCTYPE html>\n" +
                        "<html lang=\"en\">\n" +
                        "<head>\n" +
                        "    <meta charset=\"UTF-8\">\n" +
                        "    <title>Reverse PCT Documentation</title>\n" +
                        "</head>\n" +
                        "<body><pre><code>\n" +
                        replace(
                                replace(
                                        replace(
                                                escapeHtml4(result)
                                                        .replaceAll("#SQL\\{(\\s*[\\s\\S]*?\\s*\\s*)}#", "<span style=\"color: #999999;\">#SQL{</span><span style=\"color: red;\">$1</span><span style=\"color: #999999;\">}#</span>"),
                                                "noReverse", "text-decoration: line-through;"
                                        ), "pythonToPure", "color: red;"
                                ), "expectedError", "color: red;"
                        ) +
                        "</code></pre></body>\n" +
                        "</html>"
        ).build();
    }

    public static String replace(String text, String funcName, String style)
    {
        int cursor = 0;
        int func = text.indexOf(funcName, cursor);
        while (func != -1)
        {
            int balance = 0;
            for (int i = func + funcName.length(); i < text.length(); i++)
            {
                if (text.charAt(i) != ' ')
                {
                    if (text.charAt(i) == '(')
                    {
                        balance++;
                    }
                    else if (text.charAt(i) == ')')
                    {
                        balance--;
                    }
                    if (balance == 0)
                    {
                        String function = text.substring(0, func) + "<span style=\"color:#999999;\">" + funcName + "(</span>";
                        cursor = function.length();
                        text = function + "<span style=\"" + style + "\">" + text.substring(func + funcName.length() + 1, i) + "</span><span style=\"color:#999999;\">)</span>" + text.substring(i + 1);
                        break;
                    }
                }
            }
            func = text.indexOf(funcName, cursor);
        }
        return text;
    }

}