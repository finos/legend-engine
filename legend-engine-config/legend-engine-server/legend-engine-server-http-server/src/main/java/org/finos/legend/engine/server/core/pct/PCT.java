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

package org.finos.legend.engine.server.core.pct;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.net.URI;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.finos.legend.pure.m3.pct.aggregate.generation.DocumentationGeneration;
import org.finos.legend.pure.m3.pct.aggregate.model.Documentation;

@Api(tags = "PCT")
@Path("pct")
public class PCT
{
    @GET
    @Path("form")
    @ApiOperation(value = "PCT report form")
    @Produces(MediaType.TEXT_HTML)
    public Response formPCT(@Context UriInfo urlInfo)
    {
        String path = URI.create(urlInfo.getAbsolutePath().toString().replace("pct/form", "pct/html")).getPath();

        Documentation doc = DocumentationGeneration.buildDocumentation();
        String adapters = doc.adapters.stream().map(x -> x.adapter.name)
                .distinct()
                .sorted()
                .map(x -> String.format("      <option value='%s'>%s</option>\n", x, x))
                .collect(Collectors.joining());

        String qualifiers = doc.functionsDocumentation.stream()
                .flatMap(x -> x.functionTestResults.values().stream())
                .flatMap(x -> x.tests.stream())
                .flatMap(x -> x.qualifiers.stream())
                .distinct()
                .sorted()
                .map(x -> String.format("      <option value='%s'>%s</option>\n", x, x))
                .collect(Collectors.joining());

        String form = "<!DOCTYPE html>\n" +
                "<html lang='en'>\n" +
                "<head>\n" +
                "  <meta charset='UTF-8' />\n" +
                "  <title>PCT Report Form</title>\n" +
                "  <style>\n" +
                "    body {\n" +
                "      font-family: sans-serif;\n" +
                "      padding: 2em;\n" +
                "      background: #f9f9f9;\n" +
                "    }\n" +
                "    #formContainer {\n" +
                "      position: sticky;\n" +
                "      top: 0;\n" +
                "      background: #fff;\n" +
                "      padding: 1em 2em;\n" +
                "      box-shadow: 0 2px 4px rgba(0,0,0,0.1);\n" +
                "      z-index: 100;\n" +
                "    }\n" +
                "    form {\n" +
                "      display: flex;\n" +
                "      flex-direction: row;\n" +
                "      gap: 2em;\n" +
                "      align-items: flex-start;\n" +
                "    }\n" +
                "    label {\n" +
                "      display: block;\n" +
                "      margin-top: 1em;\n" +
                "      font-weight: bold;\n" +
                "    }\n" +
                "    select {\n" +
                "      width: 200px;\n" +
                "      height: 120px;\n" +
                "    }\n" +
                "    #iframeContainer {\n" +
                "      position: fixed;\n" +
//                "      top: calc(1em + 2em + 120px); /* padding + label + select height */\n" +
                "      left: 0;\n" +
                "      right: 0;\n" +
//                "      bottom: 0;\n" +
                "      width: 100%;\n" +
                "      height: 80%;\n" +
                "    }\n" +
                "    #resultFrame {\n" +
                "      width: 100%;\n" +
                "      height: 100%;\n" +
                "      border: none;\n" +
                "      display: block;\n" +
                "    }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                " <div id='formContainer'>\n" +
                "  <form id='adapterForm'>\n" +
                "    <label for='adapter'>Adapter:</label>\n" +
                "    <select id='adapter' name='adapter' multiple>\n" +
                adapters +
                "    </select>\n" +
                "\n" +
                "    <label for='qualifier'>Qualifier:</label>\n" +
                "    <select id='qualifier' name='qualifier' multiple>\n" +
                qualifiers +
                "    </select>\n" +
                "    <input type='checkbox' id='skipWithoutTests' name='skipWithoutTests' checked />\n" +
                "    <label for='skipWithoutTests' style='margin-left: 0.5em;'>Skip functions without tests</label>\n" +
                "  </form>\n" +
                " </div>\n" +
                "\n" +
                "  <div id='iframeContainer'>\n" +
                "    <iframe id='resultFrame' title='Query Results'></iframe>\n" +
                "  </div>\n" +
                "\n" +
                "  <script>\n" +
                "    const adapterSelect = document.getElementById('adapter');\n" +
                "    const qualifierSelect = document.getElementById('qualifier');\n" +
                "    const resultFrame = document.getElementById('resultFrame');\n" +
                "    const skipWithoutTestsCheckbox = document.getElementById('skipWithoutTests');" +
                "\n" +
                "    function getSelectedValues(selectElement) {\n" +
                "      return Array.from(selectElement.selectedOptions).map(opt => opt.value);\n" +
                "    }\n" +
                "\n" +
                "    function loadSelectionsFromURL() {\n" +
                "      const params = new URLSearchParams(window.location.search);\n" +
                "\n" +
                "      // Set group selection\n" +
                "      const groups = params.getAll('adapter');\n" +
                "      for (const option of adapterSelect.options) {\n" +
                "        option.selected = groups.includes(option.value);\n" +
                "      }\n" +
                "\n" +
                "      // Set qualifier selection\n" +
                "      const qualifiers = params.getAll('qualifier');\n" +
                "      for (const option of qualifierSelect.options) {\n" +
                "        option.selected = qualifiers.includes(option.value);\n" +
                "      }\n" +
                "\n" +
                "      // Set checkbox\n" +
                "      skipWithoutTestsCheckbox.checked = params.get('skipFunctionsWithoutTest') === 'true' || params.get('skipFunctionsWithoutTest') === null;\n" +
                "    }\n" +
                "\n" +
                "    async function fetchAndUpdate() {\n" +
                "      const adapters = getSelectedValues(adapterSelect);\n" +
                "      const qualifiers = getSelectedValues(qualifierSelect);\n" +
                "      const skipWithoutTests = skipWithoutTestsCheckbox.checked;\n" +
                "\n" +
                "      const params = new URLSearchParams();\n" +
                "      adapters.forEach(g => params.append('adapter', g));\n" +
                "      qualifiers.forEach(q => params.append('qualifier', q));\n" +
                "      if (skipWithoutTests) {\n" +
                "        params.append('skipFunctionsWithoutTest', 'true');\n" +
                "      }" +
                "\n" +
                "      const query = params.toString();\n" +
                String.format("      const url = query ? `%s?${query}` : `%s`;\n", path, path) +
                "\n" +
                "      resultFrame.src = url;\n" +
                "      const newURL = query ? `?${query}` : location.pathname;\n" +
                "      history.replaceState(null, '', newURL);\n" +
                "    }\n" +
                "\n" +
                "    // Auto-refresh on change\n" +
                "    adapterSelect.addEventListener('change', fetchAndUpdate);\n" +
                "    qualifierSelect.addEventListener('change', fetchAndUpdate);\n" +
                "    skipWithoutTestsCheckbox.addEventListener('change', fetchAndUpdate);\n" +
                "\n" +
                "    // Initial fetch on page load\n" +
                "    loadSelectionsFromURL();\n" +
                "    fetchAndUpdate();\n" +
                "  </script>\n" +
                "</body>\n" +
                "</html>\n";

        return Response.status(200).type(MediaType.TEXT_HTML).entity(form).build();
    }

    @GET
    @Path("html")
    @ApiOperation(value = "PCT report in HTML")
    @Produces(MediaType.TEXT_HTML)
    public Response htmlPCT(@QueryParam("adapter") Set<String> adapterKeys, @QueryParam("qualifier") Set<String> adapterQualifiers, @QueryParam("skipFunctionsWithoutTest") @DefaultValue("true") boolean skipFunctionsWithoutTest)
    {
        return Response.status(200).type(MediaType.TEXT_HTML).entity(PCT_to_SimpleHTML.buildHTML(adapterKeys, adapterQualifiers, skipFunctionsWithoutTest)).build();
    }

    @GET
    @Path("json")
    @ApiOperation(value = "PCT report in JSON")
    @Produces(MediaType.APPLICATION_JSON)
    public Response jsonPCT()
    {
        Documentation doc = DocumentationGeneration.buildDocumentation();
        return Response.status(200).type(MediaType.APPLICATION_JSON).entity(doc).build();
    }
}
