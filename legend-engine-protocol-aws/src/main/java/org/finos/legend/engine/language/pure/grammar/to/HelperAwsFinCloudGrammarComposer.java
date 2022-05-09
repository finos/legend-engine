package org.finos.legend.engine.language.pure.grammar.to;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.FinCloudDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.FinCloudTargetSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.*;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.convertString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class HelperAwsFinCloudGrammarComposer {

    public static String visitAwsFinCloudConnectionDatasourceSpecification(FinCloudTargetSpecification _spec, PureGrammarComposerContext context)
    {
        if (_spec instanceof FinCloudDatasourceSpecification)
        {
            FinCloudDatasourceSpecification spec = (FinCloudDatasourceSpecification)_spec;
            int baseIndentation = 1;
            return "AwsFinCloudDatasourceSpecification\n" +
                    context.getIndentationString() + getTabString(baseIndentation) + "{\n" +
                    (spec.apiUrl != null ? context.getIndentationString() + getTabString(baseIndentation + 1) + "apiUrl: " + convertString(spec.apiUrl, true) + ";\n" : "") +
                    context.getIndentationString() + getTabString(baseIndentation) + "}";
        // check on indentation and tabs
        }

        return null;
    }

    public static String visitAwsFinCloudConnectionAuthenticationStrategy(AuthenticationStrategy _auth, RelationalGrammarComposerContext context)
    {
        if (_auth instanceof AwsOAuthAuthenticationStrategy)
        {
            AwsOAuthAuthenticationStrategy auth = (AwsOAuthAuthenticationStrategy)_auth;
            int baseIndentation = 1;
            return "awsOAuth" +
                    "\n" +
                    context.getIndentationString() + getTabString(baseIndentation) + "{\n" +
                    context.getIndentationString() + getTabString(baseIndentation + 1) + "secretArn: " + convertString(auth.secretArn, true) + ";\n" +
                    context.getIndentationString() + getTabString(baseIndentation + 1) + "discoveryUrl: " + convertString(auth.discoveryUrl, true) + ";\n" +
                    context.getIndentationString() + getTabString(baseIndentation) + "}";
        }

        return null;
    }

}
