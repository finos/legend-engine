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

package org.finos.legend.engine.test;

import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.w3c.dom.CharacterData;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
    This class generates a summary report of SQL tests.
    - Input - Path to a directory containing JUnit XML test reports.
    - Output - Markdown printed to stdout

    The JUnit XML test reports can be downloaded from Github using download_github_workflow_artifacts.sh (see src/main/script).
 */

public class LegendDatabaseTestingReportGenerator
{
    public static void main(String[] args) throws Exception
    {
        LegendDatabaseTestingReportGenerator generator = new LegendDatabaseTestingReportGenerator();
        generator.generate(args[0], args[1]);
    }
    
    private void generate(String reportsDirPath, String outputFilePath) throws Exception
    {
        System.out.println("Reading from input dir " + reportsDirPath);
        System.out.println("Writing to output file " + outputFilePath);

        File reportsWorkDir = Paths.get(reportsDirPath).toFile();
        if (!reportsWorkDir.exists())
        {
            throw new RuntimeException("Directory does not exist : " + reportsWorkDir.getAbsolutePath());
        }

        ReportVisitor reportVisitor = new ReportVisitor();
        this.processTestReports(reportVisitor, reportsWorkDir);

        LegendDatabaseSQLTestingReport databaseSQLTestingReport = new LegendDatabaseSQLTestingReport(reportVisitor.sqlTestSummaries);
        String markdown = databaseSQLTestingReport.renderAsMarkdown();
        Files.write(Paths.get(outputFilePath), markdown.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);
    }

    private void processTestReports(ReportVisitor reportVisitor, File reportsWorkDir) throws IOException
    {
        EnumSet<FileVisitOption> opts = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
        Files.walkFileTree(reportsWorkDir.toPath(), opts, 2, reportVisitor);
    }

    public static class ReportVisitor implements FileVisitor<Path>
    {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;

        List<SQLTestSummary> sqlTestSummaries = new ArrayList<>();

        public ReportVisitor() throws Exception
        {
            builder = builderFactory.newDocumentBuilder();
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
        {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException
        {
            if (!path.toAbsolutePath().toString().endsWith(".xml"))
            {
                System.out.println("Skipping file .. " +  path.toAbsolutePath().toString());
                return FileVisitResult.CONTINUE;
            }

            System.out.println("Processing file .. " +  path.toAbsolutePath().toString());
            Pattern pattern = Pattern.compile(".*DbSpecific_.*");
            String fileName = path.getFileName().toString();
            Matcher matcher = pattern.matcher(fileName);
            if (!matcher.matches())
            {
                System.out.println("Skipping file .. " +  path.toAbsolutePath().toString());
                return FileVisitResult.CONTINUE;
            }
            SQLTestSummary sqlTestSummary = processJunitXmlReport(this.builder, path.toFile().getAbsolutePath());
            sqlTestSummaries.add(sqlTestSummary);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException
        {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
        {
            return FileVisitResult.CONTINUE;
        }
    }

    private static Pattern UNSUPPORTED_API_PATTERN = Pattern.compile(".*\\[unsupported-api\\].*");
    private static Pattern DEVIATING_FROM_STANDARD_PATTERN = Pattern.compile(".*\\[deviating-from-standard\\].*");

    private static TestStatus computeStatus(Node skipped, Node error)
    {
        if (skipped != null)
        {
            String message = skipped.getAttributes().getNamedItem("message").getTextContent();
            Matcher matcher = UNSUPPORTED_API_PATTERN.matcher(message);
            if (matcher.matches())
            {
                return TestStatus.UNSUPPORTED_IN_LEGEND;
            }
            return TestStatus.REPORT_GENERATION_ERROR;
        }
        else if (error != null)
        {
            String message = error.getAttributes().getNamedItem("message").getTextContent();
            Matcher matcher = DEVIATING_FROM_STANDARD_PATTERN.matcher(message);
            if (matcher.matches())
            {
                return TestStatus.DEVIATION;
            }
            return TestStatus.ERROR;
        }
        return TestStatus.SUCCESS;
    }

    private static SQLTestSummary processJunitXmlReport(DocumentBuilder builder, String file) throws IOException
    {
        try
        {
            FileInputStream fis = new FileInputStream(file);
            org.w3c.dom.Document xmlDocument = builder.parse(fis);
            XPath xPath = XPathFactory.newInstance().newXPath();
            NodeList testCasesList = (NodeList) xPath.compile("/testsuite/testcase").evaluate(xmlDocument, XPathConstants.NODESET);

            SQLTestSummary sqlTestSummary = new SQLTestSummary();
            for (int i = 0; i < testCasesList.getLength(); i++)
            {
                Node testCase = testCasesList.item(i);
                NamedNodeMap attributes = testCase.getAttributes();
                String testCaseNameAttribute = attributes.getNamedItem("name").getNodeValue();
                String testCaseName = testCaseNameAttribute.substring(0, testCaseNameAttribute.indexOf("["));
                String testCaseDatabaseName = testCaseNameAttribute.substring(testCaseNameAttribute.indexOf("[") + 1, testCaseNameAttribute.indexOf("]"));

                if (sqlTestSummary.database == null)
                {
                    sqlTestSummary.database = testCaseDatabaseName;
                }
                else
                {
                    if (!sqlTestSummary.database.equals(testCaseDatabaseName))
                    {
                        String message = "Report processing error. Report's database name has already been set to '%s' while the current test's database name is '%s'";
                        System.out.println(String.format(message, sqlTestSummary.database, testCaseDatabaseName));
                    }
                }

                Node skipped = (Node) xPath.compile("skipped").evaluate(testCase, XPathConstants.NODE);
                Node error = (Node) xPath.compile("error").evaluate(testCase, XPathConstants.NODE);
                Node sysout = (Node) xPath.compile("system-out").evaluate(testCase, XPathConstants.NODE);

                //testCaseName = extractPureFunctionName(testCaseName, sysout);

                TestStatus status = computeStatus(skipped, error);
                sqlTestSummary.add(testCaseName, status);
            }
            return sqlTestSummary;
        }
        catch (Exception e)
        {
            throw new IOException(e);
        }
    }

    private static String extractPureFunctionName(String testCaseName, Node sysout)
    {
        if (sysout == null)
        {
            return testCaseName;
        }
        String bigSysout = ((CharacterData) sysout.getFirstChild()).getData();
        String prefix = "Running db test ";
        if (!bigSysout.startsWith(prefix))
        {
            return testCaseName;
        }
        String substring = bigSysout.substring(prefix.length(), bigSysout.indexOf("\n", 0));
        if (substring.trim().isEmpty())
        {
            return testCaseName;
        }
        return substring;
    }

    public static class SQLTestSummary
    {
        MutableMap<String, TestStatus> resultsByTestName = Maps.mutable.empty();

        public String database;

        public SQLTestSummary()
        {
        }

        public void add(String test, TestStatus status)
        {
            resultsByTestName.put(test, status);
        }

        public Set<String> testNames()
        {
            return resultsByTestName.keySet();
        }

        public TestStatus getTestResult(String testName)
        {
            return resultsByTestName.getOrDefault(testName, TestStatus.MISSING);
        }
    }
}
