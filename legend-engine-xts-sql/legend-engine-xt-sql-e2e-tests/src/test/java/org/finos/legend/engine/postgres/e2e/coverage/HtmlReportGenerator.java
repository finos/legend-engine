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
                    + "*, *::before, *::after { box-sizing: border-box; }\n"
                    + "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;"
                    + " max-width: 1300px; margin: 0 auto; padding: 32px 40px; line-height: 1.6;"
                    + " color: #1a1a2e; background-color: #fafbfc; }\n"
                    // Title
                    + "h1 { font-size: 1.8em; color: #1a1a2e; border-bottom: 3px solid #3498db;"
                    + " padding-bottom: 12px; margin-bottom: 24px; }\n"
                    // Section headers (Summary, Category Summary, per-category, Error Categories, etc.)
                    + "h2 { font-size: 1.4em; color: #2c3e50; margin-top: 2.5em; margin-bottom: 0.6em;"
                    + " padding: 10px 16px; background: linear-gradient(135deg, #667eea11, #764ba211);"
                    + " border-left: 4px solid #3498db; border-radius: 4px; }\n"
                    // Sub-section headers (error category names in error details)
                    + "h3 { font-size: 1.15em; color: #34495e; margin-top: 2em; padding: 8px 14px;"
                    + " background-color: #f0f4f8; border-radius: 6px; border-left: 4px solid #3498db; }\n"
                    // Test ID headings in error details
                    + "h4 { font-size: 1.15em; margin-top: 2em; padding-bottom: 0.3em;"
                    + " border-bottom: 1px solid #e0e0e0; color: #2c3e50; }\n"
                    + "h4 code { font-size: 1.05em; background-color: #eef2f7; padding: 3px 8px; border-radius: 4px; }\n"
                    // Tables
                    + "table { border-collapse: separate; border-spacing: 0; width: 100%%;"
                    + " margin: 1em 0; border-radius: 8px; overflow: hidden;"
                    + " box-shadow: 0 1px 3px rgba(0,0,0,0.08); border: 1px solid #e0e4e8; }\n"
                    + "th { background: linear-gradient(180deg, #f8f9fb, #eef0f4); color: #2c3e50;"
                    + " font-weight: 600; padding: 10px 12px; text-align: left;"
                    + " border-bottom: 2px solid #d0d5dd; font-size: 0.88em; }\n"
                    + "td { padding: 8px 12px; text-align: left; border-bottom: 1px solid #eef0f4; }\n"
                    + "tr:last-child td { border-bottom: none; }\n"
                    + "tr:hover td { background-color: #f5f8fc; }\n"
                    + "tbody tr:nth-child(even) td { background-color: #fafbfd; }\n"
                    + "tbody tr:nth-child(even):hover td { background-color: #f0f4f8; }\n"
                    // Inline code
                    + "code { background-color: #eef2f7; padding: 2px 6px; border-radius: 4px;"
                    + " font-size: 0.9em; color: #c0392b; }\n"
                    // Code blocks (SQL)
                    + "pre { background-color: #1e1e2e; color: #cdd6f4; padding: 16px 20px;"
                    + " border-radius: 8px; overflow-x: auto; font-size: 1em;"
                    + " box-shadow: 0 2px 6px rgba(0,0,0,0.15); margin: 0.6em 0 1em 0; }\n"
                    + "pre code { background: none; color: inherit; padding: 0; font-size: inherit; }\n"
                    // Blockquotes (error messages)
                    + "blockquote { border-left: 4px solid #e74c3c; background-color: #fdf0ef;"
                    + " margin: 0.5em 0 1em 0; padding: 12px 18px; border-radius: 0 6px 6px 0; }\n"
                    + "blockquote p { margin: 0; line-height: 1.5; }\n"
                    // Horizontal rules
                    + "hr { border: none; border-top: 1px solid #e0e4e8; margin: 2em 0; }\n"
                    // Bold labels (Input SQL:, Legend SQL:, Error:)
                    + "strong { color: #2c3e50; }\n"
                    // Links
                    + "a { color: #2980b9; text-decoration: none; }\n"
                    + "a:hover { text-decoration: underline; color: #1a5276; }\n"
                    // Paragraphs
                    + "p { margin: 0.4em 0; }\n"
                    // Lists
                    + "ul, ol { padding-left: 1.5em; }\n"
                    + "li { margin: 0.3em 0; }\n"
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
            htmlBody = htmlBody.replaceAll("href=\"([^\"]*)\\.md(#[^\"]*)?\"", "href=\"$1$2\"");
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

