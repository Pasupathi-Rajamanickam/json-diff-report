package com.nexttimespace.utilities.diffbox;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

public class Executor {

    static ObjectMapper mapper = new ObjectMapper();
    static JSONDiff jsonDiff = new JSONDiff();
    static HTTPClientManager httpClient = new HTTPClientManager();

    public static void main(String... s) throws IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        httpClient.setup();
        String inputFile = "input.json";
        if (StringUtils.isNotBlank(System.getProperty("input"))) {
            inputFile = System.getProperty("input");
        }

        System.out.println("INFO: Started processing");
        InputRequest inputRequest = mapper.readValue(new File(inputFile), InputRequest.class);
        String reportFile = "";
        if (!inputRequest.getInputs().isEmpty()) {
            reportFile = prepareReport();
        }
        int index = 0;
        int lastPercent = 0;
        List<Map<String, String>> finalList = new ArrayList<>();
        for (InputRequest.Input request : inputRequest.getInputs()) {
            Map<String, String> list = new LinkedHashMap<>();
            index++;
            list.put("index", index + "");
            list.put("sourceURL", request.getUrlSource());
            list.put("compareURL", request.getUrlCompare());

            Map<String, Object> httpRequest = new LinkedHashMap<>();
            httpRequest.put(HTTPClientManager.Const.METHOD, StringUtils.isNotBlank(request.getMethod()) ? request.getMethod() : "GET");

            httpRequest.put(HTTPClientManager.Const.REQUEST_HEADER, request.getHeadersSource());
            httpRequest.put(HTTPClientManager.Const.URL, request.getUrlSource());

            if (StringUtils.isNotBlank(request.getRequestSource())) {
                httpRequest.put(HTTPClientManager.Const.REQUEST, request.getRequestSource());
            }

            Map<String, Object> response = null;

            if (Boolean.valueOf(UtilityFunction.appProperties.get("useURLConnection") + "")) {
                response = httpClient.getResponseV3(httpRequest);
            } else {
                response = httpClient.getResponseV2(httpRequest);
            }
            httpRequest = new LinkedHashMap<>();
            httpRequest.put(HTTPClientManager.Const.METHOD, StringUtils.isNotBlank(request.getMethod()) ? request.getMethod() : "GET");

            httpRequest.put(HTTPClientManager.Const.REQUEST_HEADER, request.getHeadersCompare());
            httpRequest.put(HTTPClientManager.Const.URL, request.getUrlCompare());

            if (StringUtils.isNotBlank(request.getRequestCompare())) {
                httpRequest.put(HTTPClientManager.Const.REQUEST, request.getRequestCompare());
            }
            Map<String, Object> responseCompare = null;

            if (Boolean.valueOf(UtilityFunction.appProperties.get("useURLConnection") + "")) {
                responseCompare = httpClient.getResponseV3(httpRequest);
            } else {
                responseCompare = httpClient.getResponseV2(httpRequest);
            }

            String status = "Failed";
            try {
                status = appendCompareReport(response, responseCompare, reportFile, "" + index, request.getUrlSource(), request.getUrlCompare());
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("ERROR: Error Processing " + request.getUrlSource());
            }
            if (status == null) {
                list.put("overAllStatus", "Error");
            } else {
                list.put("overAllStatus", status);
            }

            finalList.add(list);
            int percentOfClassPassed = (int) ((double) index * 100 / inputRequest.getInputs().size());

            if (lastPercent != percentOfClassPassed) {
                System.out.println(percentOfClassPassed + "% Completed");
            }
            lastPercent = percentOfClassPassed;
        }
        addListReport(finalList, reportFile);

        System.out.println("INFO: Compare process completed. Check report at " + reportFile);
    }

    private static void addListReport(List<Map<String, String>> listReport, String filename) throws IOException {
        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile("final-list.html");
        StringWriter writer = new StringWriter();
        Map<String, Object> objectToTemplate = new LinkedHashMap<>();
        objectToTemplate.put("listReport", listReport);
        mustache.execute(writer, objectToTemplate).flush();
        if (writer != null) {
            String listReportHTML = writer.toString();
            if (listReportHTML != null && !listReportHTML.isEmpty()) {
                try (FileWriter fw = new FileWriter(filename, true); BufferedWriter bw = new BufferedWriter(fw); PrintWriter out = new PrintWriter(bw)) {
                    out.println(listReportHTML);
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }
            }
        }

    }

    private static String appendCompareReport(Map<String, Object> responseSource, Map<String, Object> responseCompare, String filename, String index, String sourceURL, String compareURL) throws JSONException, IOException {
        String sourceResponse = (String) responseSource.get(HTTPClientManager.Const.RESPONSE);
        String compareResponse = (String) responseCompare.get(HTTPClientManager.Const.RESPONSE);
        Map<String, String> compareReport = null;
        String returnStatus = null;

        if (sourceResponse.startsWith("{") && compareResponse.startsWith("{")) {
            compareReport = jsonDiff.createCompareHTML(sourceResponse, compareResponse, index, sourceURL, compareURL);
        } else {
            System.err.println("WARN: Received Non JSON for " + sourceURL);
        }
        if (compareReport != null && !compareReport.isEmpty()) {
            try (FileWriter fw = new FileWriter(filename, true); BufferedWriter bw = new BufferedWriter(fw); PrintWriter out = new PrintWriter(bw)) {
                out.println(compareReport.get("compareReport"));
            } catch (Exception e) {
                throw e;
            }
            returnStatus = compareReport.get("status");
        }
        return returnStatus;
    }

    private static String getReportFileName() {
        return "Report/CompareReport_" + new SimpleDateFormat("yyyyMMddHHmmss'.html'").format(new Date());
    }

    private static String prepareReport() throws IOException {
        File reportFolder = new File("Report");
        if (!reportFolder.exists()) {
            reportFolder.mkdirs();
        }
        String reportFileName = getReportFileName();
        Resource resource = new ClassPathResource("/report-template.html");
        OutputStream outputStream = new FileOutputStream(reportFileName);
        IOUtils.copy(resource.getInputStream(), outputStream);
        outputStream.close();
        return reportFileName;
    }

}
