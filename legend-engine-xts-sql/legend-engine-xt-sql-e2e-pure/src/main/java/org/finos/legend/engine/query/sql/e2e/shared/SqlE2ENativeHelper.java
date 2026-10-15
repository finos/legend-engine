// Copyright 2026 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.query.sql.e2e.shared;

import java.util.List;

import org.finos.legend.engine.postgres.e2e.ResultComparator;
import org.finos.legend.engine.postgres.e2e.ResultMatrix;
import org.finos.legend.engine.postgres.e2e.SqlE2ERunner;
import org.finos.legend.engine.postgres.e2e.TdsJsonResultMatrix;
import org.finos.legend.pure.m3.pct.shared.PCTTools;

/**
 * Single implementation behind both the compiled and interpreted natives, so the two runtimes
 * cannot diverge. Building CoreInstances stays in each runtime's own extension class; corpus access
 * and outcome classification live here once.
 */
public class SqlE2ENativeHelper
{
    private SqlE2ENativeHelper()
    {
    }

    /** Mangled path of meta::external::query::sql::e2e::runOneCase. */
    public static final String RUN_ONE_CASE =
            "meta::external::query::sql::e2e::runOneCase_SqlE2ECaseRef_1__SqlE2EConnection_1__String_1_";

    /** Fully-qualified path of the meta::external::query::sql::e2e::SqlE2EPath enum. */
    public static final String SQL_E2E_PATH_ENUM = "meta::external::query::sql::e2e::SqlE2EPath";

    /** Fully-qualified path of the meta::external::query::sql::e2e::SqlE2ECaseRef class. */
    public static final String SQL_E2E_CASE_REF_CLASS = "meta::external::query::sql::e2e::SqlE2ECaseRef";

    /** Fully-qualified path of the meta::external::query::sql::e2e::SqlE2EConnection class. */
    public static final String SQL_E2E_CONNECTION_CLASS = "meta::external::query::sql::e2e::SqlE2EConnection";

    /** Fully-qualified path of the meta::external::query::sql::e2e::SqlE2ETestResult class. */
    public static final String SQL_E2E_TEST_RESULT_CLASS = "meta::external::query::sql::e2e::SqlE2ETestResult";

    /** Fully-qualified path of the meta::external::query::sql::e2e::SqlE2ETestStatus enum. */
    public static final String SQL_E2E_TEST_STATUS_ENUM = "meta::external::query::sql::e2e::SqlE2ETestStatus";

    /** Fully-qualified path of the meta::external::query::sql::e2e::SqlE2EAdhocResult class. */
    public static final String SQL_E2E_ADHOC_RESULT_CLASS = "meta::external::query::sql::e2e::SqlE2EAdhocResult";

    /** Mangled path of meta::external::query::sql::e2e::runOneAdhocLegend. */
    public static final String RUN_ONE_ADHOC_LEGEND = "meta::external::query::sql::e2e::"
            + "runOneAdhocLegend_String_1__String_1__SqlE2EConnection_1__String_1_";

    public static List<SqlE2ERunner.CaseRef> resolveCaseRefs(String filter)
    {
        return SqlE2ERunner.get().resolveCaseRefs(filter);
    }

    public static SqlE2ERunner.CaseRef resolveCaseRef(String corpusId, String path)
    {
        return SqlE2ERunner.get().resolveCaseRef(corpusId, path);
    }

    public static SqlE2ERunner.ConnectionInfo connectionInfo()
    {
        return SqlE2ERunner.get().connectionInfo();
    }

    public static SqlE2ERunner.AdhocPrep prepareAdhoc(String sql, String path)
    {
        return SqlE2ERunner.get().prepareAdhoc(sql, path);
    }

    /** Best-effort yes/no comparison for ad hoc SQL. Returns null ("undetermined") rather than throwing. */
    public static Boolean matchAgainstReference(String legendResultJson, ResultMatrix reference, boolean hasOrderBy)
    {
        if (legendResultJson == null || reference == null)
        {
            return null;
        }
        try
        {
            ResultMatrix actual = TdsJsonResultMatrix.parse(legendResultJson);
            ResultMatrix expectedCmp = hasOrderBy ? reference : reference.sorted();
            ResultMatrix actualCmp = hasOrderBy ? actual : actual.sorted();
            return ResultComparator.compare(expectedCmp, actualCmp).isMatch();
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public static String unwrapThrowMessage(Throwable t)
    {
        Throwable unwrapped = PCTTools.unwrapExecutionError(t);
        String msg = PCTTools.getMessageFromError(unwrapped);
        return msg == null ? String.valueOf(unwrapped) : msg;
    }

    /**
     * The result of classifying a runOneCase call, before either runtime turns it into a
     * TestResult CoreInstance.
     */
    public static final class Classification
    {
        public final String status;
        public final String message;

        private Classification(String status, String message)
        {
            this.status = status;
            this.message = message;
        }
    }

    /**
     * Classifies a ref without ever invoking runOneCase. Returns null when the ref has real SQL to
     * run, meaning the caller must invoke runOneCase and classify its result via {@link #classifySuccess}.
     */
    public static Classification classifyShortCircuit(SqlE2ERunner.CaseRef ref)
    {
        if (ref.skip != null)
        {
            return new Classification("SKIP", ref.skip);
        }
        if (ref.bugReason != null)
        {
            return new Classification("BUG", "postgres rejected reference SQL: " + ref.bugReason);
        }
        if (ref.rewriteError != null)
        {
            return new Classification("ERROR", "rewrite error: " + ref.rewriteError);
        }
        return null;
    }

    /** Classifies a successful runOneCase return by comparing it against the cached reference result. */
    public static Classification classifySuccess(String corpusId, boolean hasOrderBy, String raw)
    {
        String diff = SqlE2ERunner.get().compareToReference(corpusId, hasOrderBy, raw);
        return diff == null ? new Classification("PASS", null) : new Classification("FAIL", diff);
    }

    public static Classification classifyThrow(Throwable t)
    {
        Throwable unwrapped = PCTTools.unwrapExecutionError(t);
        String msg = PCTTools.getMessageFromError(unwrapped);
        return new Classification("ERROR", msg == null ? String.valueOf(unwrapped) : msg);
    }
}
