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

package org.finos.legend.engine.ide.api.execution.function.manager;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.finos.legend.pure.m3.exception.PureExecutionStreamingException;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.execution.OutputWriter;
import org.finos.legend.pure.m3.execution.XLSXOutputWriter;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.function.FunctionDescriptor;
import org.finos.legend.pure.m3.navigation.function.InvalidFunctionDescriptorException;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.shared.canstreamstate.CanStreamState;

import java.io.OutputStream;
import java.util.Map;

public class ExecutionManager
{
    public static final String OUTPUT_FORMAT_PARAMETER = "format";
    public static final String OUTPUT_PROCESSOR_PARAMETER = "outFunc";
    public static final String OUTPUT_PROCESSOR_PARAMETER_PARAMETER = "outParam";

    public static final String EXECUTION_MODE_PARAM = "mode";

    public static final String FUNCTION_PARAMETER = "func";
    public static final String PARAMETER_PARAMETER = "param";
    public static final String QUERY_PARAMETER = "query";
    public static final String BLOCK_PARAMETER = "block";
    public static final String PROCESSING_FUNC_PARAMETER = "processingFunc";
    public static final String PROCESSING_PARAM_PARAMETER = "processingParam";

    public static final String OUTPUT_FORMAT_RAW = "raw";
    public static final String OUTPUT_FORMAT_PRE = "pre";
    public static final String OUTPUT_FORMAT_JSON = "json";
    public static final String OUTPUT_FORMAT_CSV = "csv";
    public static final String OUTPUT_FORMAT_XLSX = "xlsx";

    private static final ImmutableMap<String, org.finos.legend.engine.ide.api.execution.function.manager.OutputFormat> OUTPUT_FORMATS = Maps.mutable.<String, org.finos.legend.engine.ide.api.execution.function.manager.OutputFormat>empty()
            .withKeyValue(OUTPUT_FORMAT_RAW, org.finos.legend.engine.ide.api.execution.function.manager.OutputFormat.RAW)
            .withKeyValue(OUTPUT_FORMAT_PRE, org.finos.legend.engine.ide.api.execution.function.manager.OutputFormat.PRE)
            .withKeyValue(OUTPUT_FORMAT_JSON, org.finos.legend.engine.ide.api.execution.function.manager.OutputFormat.JSON)
            .withKeyValue(OUTPUT_FORMAT_CSV, org.finos.legend.engine.ide.api.execution.function.manager.OutputFormat.CSV)
            .withKeyValue(OUTPUT_FORMAT_XLSX, org.finos.legend.engine.ide.api.execution.function.manager.OutputFormat.XLSX)
            .toImmutable();

    private static final org.finos.legend.engine.ide.api.execution.function.manager.OutputFormat DEFAULT_OUTPUT_FORMAT = org.finos.legend.engine.ide.api.execution.function.manager.OutputFormat.JSON;

    private static final String EXECUTE_FUNCTION = "meta::pure::ide::execute_String_$0_1$__List_MANY__String_$0_1$__String_$0_1$__String_$0_1$__String_$0_1$__List_MANY__String_$0_1$__List_MANY__String_MANY_";

    private final FunctionExecutionParser parser;
    private final FunctionExecution functionExecution;

    public ExecutionManager(FunctionExecution functionExecution)
    {
        this.parser = new FunctionExecutionParser(functionExecution.getProcessorSupport());
        this.functionExecution = functionExecution;

    }

    public void execute(ExecutionRequest executionRequest, HttpResponseWriter response, ContentType inputContentType)
    {
        org.finos.legend.engine.ide.api.execution.function.manager.OutputFormat format = this.getOutputFormat(executionRequest.getRequestParams());

        CoreInstance executeFunction = this.getFunction(EXECUTE_FUNCTION);

        String funcParam = executionRequest.getRequestParamToOne(FUNCTION_PARAMETER);
        String blockParam = executionRequest.getRequestParamToOne(BLOCK_PARAMETER);
        String queryParam = executionRequest.getRequestParamToOne(QUERY_PARAMETER);
        if (funcParam == null && blockParam == null && queryParam == null)
        {
            StringBuilder message = new StringBuilder("Invalid request, please provide one of these request parameters :");
            message.append(FUNCTION_PARAMETER).append(", ").append(BLOCK_PARAMETER).append(", ").append(QUERY_PARAMETER);
            message.append("\nRequest parameters received were:\n");
            executionRequest.getRequestParams().forEach((key, values) -> ArrayIterate.appendString(values, message.append(key), "=[", ", ", "]\n"));
            throw new IllegalArgumentException(message.toString());
        }

        HttpInformation httpInformation;
        if (format != null)
        {
            if (format == org.finos.legend.engine.ide.api.execution.function.manager.OutputFormat.RAW && funcParam != null)
            {
                //try to find a content type on the function itself
                String functionId = convertFunctionToFunctionId(funcParam);
                CoreInstance function = this.getFunction(functionId);
                httpInformation = HttpInformation.fromFunction(function, this.functionExecution.getProcessorSupport());

                //has no content type flagged so use defaults
                if (httpInformation.getContentType() == null)
                {
                    httpInformation = new HttpInformation(format.getDefaultContentType().getType(), format.getDefaultContentDisposition());
                }
            }
            else
            {
                httpInformation = new HttpInformation(format.getDefaultContentType().getType(), format.getDefaultContentDisposition());
            }
        }
        else
        {
            String outFuncParam = executionRequest.getRequestParamToOne(OUTPUT_PROCESSOR_PARAMETER);
            if (outFuncParam != null)
            {
                String functionId = convertFunctionToFunctionId(outFuncParam);
                CoreInstance function = this.getFunction(functionId);
                httpInformation = HttpInformation.fromFunction(function, this.functionExecution.getProcessorSupport());
            }
            else
            {
                httpInformation = new HttpInformation(org.finos.legend.engine.ide.api.execution.function.manager.OutputFormat.JSON.getDefaultContentType().getType(), null);
            }
        }
        boolean explicitDisableStreaming = false;
        if (funcParam != null)
        {
            CoreInstance functionToExecute = this.getFunction(funcParam);
            explicitDisableStreaming = executionRequest.isStreamingDisabled() || disableStreaming(functionToExecute, this.functionExecution.getProcessorSupport());
        }
        boolean canStreamOutput = !explicitDisableStreaming && isStreamingOutputFunction(executionRequest, format);

        this.execute(executeFunction, getParameters(executionRequest, this.parser, inputContentType), response, httpInformation, canStreamOutput, format);
    }

    private static boolean disableStreaming(CoreInstance functionToExecute, ProcessorSupport processorSupport)
    {
        ListIterable<? extends CoreInstance> functionStereotypes = ImportStub.withImportStubByPasses(functionToExecute.getValueForMetaPropertyToMany(M3Properties.stereotypes), processorSupport);
        CoreInstance serviceProfile = processorSupport.package_getByUserPath(M3Paths.service);
        return functionStereotypes.anySatisfy(each -> each.getValueForMetaPropertyToOne(M3Properties.profile) == serviceProfile && "disableStreaming".equals(each.getValueForMetaPropertyToOne(M3Properties.value).getName()));
    }

    private CoreInstance getFunction(String functionIdOrDescriptor)
    {
        CoreInstance function = this.functionExecution.getRuntime().getFunction(functionIdOrDescriptor);
        if (function == null)
        {
            throw new IllegalArgumentException("Invalid function specification: " + functionIdOrDescriptor);
        }
        return function;
    }

    private ListIterable<CoreInstance> getParameters(ExecutionRequest executionRequest, FunctionExecutionParser parser, ContentType inputContentType)
    {

        CoreInstance nil = parser.parseParameter("[]", false);
        String funcParam = executionRequest.getRequestParamToOne(FUNCTION_PARAMETER);
        //todo - just look up the func directly, instead of string?
        CoreInstance func = funcParam == null ? nil : parser.parseParameter("'" + convertFunctionToFunctionId(funcParam) + "'", false);

        String[] parametersStrings = executionRequest.getRequestParams().get(PARAMETER_PARAMETER);
        CoreInstance params = parametersStrings == null ? nil : ContentType.json.equals(inputContentType) ? parser.parseJsonParameter(parametersStrings[0]) : parser.parseParametersAsList(parametersStrings);

        String blockParam = executionRequest.getRequestParamToOne(BLOCK_PARAMETER);

        String queryParam = executionRequest.getRequestParamToOne(QUERY_PARAMETER);
        CoreInstance query = queryParam == null ? nil : parser.parseParameter("'" + queryParam + "'", false);
        CoreInstance block = blockParam == null ? nil : parser.wrapString(blockParam);

        String processingFuncParam = executionRequest.getRequestParamToOne(PROCESSING_FUNC_PARAMETER);
        CoreInstance processingFunc = processingFuncParam == null ? nil : parser.parseParameter("'" + convertFunctionToFunctionId(processingFuncParam) + "'", false);

        String[] processingParametersStrings = executionRequest.getRequestParams().get(PROCESSING_PARAM_PARAMETER);
        CoreInstance processingFuncParams = processingParametersStrings == null ? nil : parser.parseParametersAsList(processingParametersStrings);

        String formatParam = executionRequest.getRequestParamToOne(OUTPUT_FORMAT_PARAMETER);
        CoreInstance format = formatParam == null ? nil : parser.parseParameter("'" + formatParam + "'", false);

        String outFuncParam = executionRequest.getRequestParamToOne(OUTPUT_PROCESSOR_PARAMETER);
        CoreInstance outFunc = outFuncParam == null ? nil : parser.parseParameter("'" + convertFunctionToFunctionId(outFuncParam) + "'", false);

        String[] outputParametersStrings = executionRequest.getRequestParams().get(OUTPUT_PROCESSOR_PARAMETER_PARAMETER);
        CoreInstance outputFuncParams = outputParametersStrings == null ? nil : parser.parseParametersAsList(outputParametersStrings);

        return Lists.fixedSize.of(func, params, query, block, format, processingFunc, processingFuncParams, outFunc, outputFuncParams);
    }

    private String convertFunctionToFunctionId(String functionName)
    {
        String functionId;
        try
        {
            functionId = FunctionDescriptor.functionDescriptorToId(functionName);
        }
        catch (InvalidFunctionDescriptorException exp)
        {
            functionId = functionName;
        }
        return functionId;
    }

    private void execute(CoreInstance function, ListIterable<CoreInstance> parameters, HttpResponseWriter response, HttpInformation httpInformation, boolean canStreamOutput, org.finos.legend.engine.ide.api.execution.function.manager.OutputFormat format)
    {
        try
        {
            CanStreamState.setCanStream(canStreamOutput);
            response.setIsStreamingResponse(canStreamOutput);
            this.addContentTypeAndDispositionHeaders(response, httpInformation);
            response.setHeader("Trailer", "X-Streaming-Error");
            // Do not open outputStream in try-with-resources
            OutputStream outputStream = response.getOutputStream();
            try
            {
                OutputWriter writer = this.functionExecution.newOutputWriter();
                if (format == org.finos.legend.engine.ide.api.execution.function.manager.OutputFormat.XLSX)
                {
                    writer = new XLSXOutputWriter(writer);
                }

                this.functionExecution.start(function, parameters, outputStream, writer);
            }
            catch (PureExecutionStreamingException ignore)
            {
                // ignore streaming exception
            }
            // outputStream must NOT be closed in case of an uncaught exception
            // so the call to close must not be in a finally block or implicit in a try-with-resources
            outputStream.close();
        }
        catch (Throwable t)
        {
            if (t instanceof RuntimeException)
            {
                throw (RuntimeException) t;
            }
            if (t instanceof Error)
            {
                throw (Error) t;
            }
            throw new RuntimeException(t);
        }
        finally
        {
            //Reset
            CanStreamState.resetCanStream();
        }
    }

    private void addContentTypeAndDispositionHeaders(HttpResponseWriter response, HttpInformation httpInformation)
    {
        String resultContentType = httpInformation.getContentType();
        response.setContentType(resultContentType == null ? ContentType.text.getType() : resultContentType);
        String resultContentDisposition = httpInformation.getContentDisposition();
        if (resultContentDisposition != null)
        {
            response.setContentDisposition(resultContentDisposition);
        }
    }

    private boolean isStreamingOutputFunction(ExecutionRequest executionRequest, org.finos.legend.engine.ide.api.execution.function.manager.OutputFormat format)
    {
        return false;
//        String[] param = executionRequest.getRequestParams().get(ExecutionManager.OUTPUT_PROCESSOR_PARAMETER);
//        return !executionRequest.isStreamingDisabled() && (format == OutputFormat.JSON || format == OutputFormat.CSV || format == OutputFormat.XLSX ||
//                (param != null && param.length == 1 && (PatternExecutor.func.equals(param[0]) || TDS_TO_JSON_OBJECT_FUNCTION.equals(param[0]))));
    }


    private org.finos.legend.engine.ide.api.execution.function.manager.OutputFormat getOutputFormat(Map<String, String[]> requestParams)
    {
        String[] outputFormatStrings = requestParams.get(OUTPUT_FORMAT_PARAMETER);
        if (outputFormatStrings == null)
        {
            return requestParams.containsKey(OUTPUT_PROCESSOR_PARAMETER) ? null : DEFAULT_OUTPUT_FORMAT;
        }
        if (outputFormatStrings.length != 1)
        {
            throw new IllegalArgumentException("Parameter '" + OUTPUT_FORMAT_PARAMETER + "' must have at most one value");
        }
        String formatString = outputFormatStrings[0].trim().toLowerCase();
        org.finos.legend.engine.ide.api.execution.function.manager.OutputFormat format = OUTPUT_FORMATS.get(formatString);
        if (format == null)
        {
            throw new IllegalArgumentException("Unknown output format \"" + outputFormatStrings[0] + "\" (valid formats: " + OUTPUT_FORMATS.keysView().toSortedList().makeString(", ") + ")");
        }
        return format;
    }
}
