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

package org.finos.legend.engine.postgres.e2e.coverage;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

/**
 * Converts generated Markdown reports to standalone HTML files.
 */
public class HtmlReportGenerator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlReportGenerator.class);

    private static final List<Extension> EXTENSIONS = Collections.singletonList(TablesExtension.create());
    private static final Parser PARSER = Parser.builder().extensions(EXTENSIONS).build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder().extensions(EXTENSIONS).build();

    private static final String HTML_TEMPLATE =
            "<!DOCTYPE html>\n"
                    + "<html>\n"
                    + "<head>\n"
                    + "<meta charset=\"UTF-8\">\n"
                    + "<title>%s</title>\n"
                    + "<style>\n"
                    + "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; max-width: 1200px; margin: 0 auto; padding: 20px; }\n"
                    + "table { border-collapse: collapse; width: 100%%; margin: 1em 0; }\n"
                    + "th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n"
                    + "th { background-color: #f4f4f4; }\n"
                    + "code { background-color: #f4f4f4; padding: 2px 4px; border-radius: 3px; }\n"
                    + "pre { background-color: #f4f4f4; padding: 12px; border-radius: 5px; overflow-x: auto; }\n"
                    + "</style>\n"
                    + "</head>\n"
                    + "<body>\n%s\n</body>\n"
                    + "</html>\n";

    public void generateAll(String outputDir)
    {
        generate(outputDir, "summary.md", "summary.html", "Legend SQL — Coverage Summary");
        generate(outputDir, "function-coverage.md", "function-coverage.html", "SQL Function Coverage");
        generate(outputDir, "structural-parity.md", "structural-parity.html", "SQL Structural Parity");
        generate(outputDir, "failure-details.md", "failure-details.html", "SQL Failure Details");
    }

    private void generate(String outputDir, String mdFileName, String htmlFileName, String title)
    {
        File mdFile = new File(outputDir, mdFileName);
        if (!mdFile.exists())
        {
            LOGGER.warn("Markdown file not found, skipping HTML generation: {}", mdFile);
            return;
        }
        try
        {
            String markdown = new String(Files.readAllBytes(mdFile.toPath()), StandardCharsets.UTF_8);
            Node document = PARSER.parse(markdown);
            String htmlBody = RENDERER.render(document);
            // Rewrite internal .md links to point to the corresponding .html files
            htmlBody = htmlBody.replaceAll("href=\"([^\"]*)\\.md(#[^\"]*)?\"", "href=\"$1.html$2\"");
            String html = String.format(HTML_TEMPLATE, title, htmlBody);
            File htmlFile = new File(outputDir, htmlFileName);
            Files.write(htmlFile.toPath(), html.getBytes(StandardCharsets.UTF_8));
            LOGGER.info("Generated HTML report: {}", htmlFile.getName());
        }
        catch (IOException e)
        {
            LOGGER.error("Failed to generate HTML report: {}", htmlFileName, e);
        }
    }
}

