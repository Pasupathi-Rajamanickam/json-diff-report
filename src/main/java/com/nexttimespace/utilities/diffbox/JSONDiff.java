package com.nexttimespace.utilities.diffbox;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class JSONDiff {

    public Map<String, String> createCompareHTML(String sourceJSON, String compareJSON, String index, String sourceURL, String compareURL) throws JSONException, IOException {
        Map<String, String> compareHTML = new LinkedHashMap<>();
        Map<String, String> pathList = getMeAll(sourceJSON);
        Map<String, String> pathList1 = getMeAll(compareJSON);
        Map<String, Object> reportDiff = prettyPrintDiff(pathList, pathList1);
        List<DiffResponse> listReport = (List<DiffResponse>) reportDiff.get("diff");
        String status = (boolean) reportDiff.get("status") ? "Passed" : "Failed";
        Map<String, Object> finalReport = new LinkedHashMap<>();
        finalReport.put("diffResponse", listReport);
        finalReport.put("index", index);
        finalReport.put("sourceURL", sourceURL);
        finalReport.put("compareURL", compareURL);
        finalReport.put("overAllStatus", status);

        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile("report-main.html");
        StringWriter writer = new StringWriter();
        mustache.execute(writer, finalReport).flush();
        compareHTML.put("compareReport", writer.toString());
        compareHTML.put("status", status);
        return compareHTML;
    }

    public Map<String, Object> prettyPrintDiff(Map<String, String> sourceList, Map<String, String> compareWithList) {
        Map<String, Object> finalReport = new LinkedHashMap<>();
        boolean overall = true;

        List<DiffResponse> differences = new ArrayList<>();

        for (Map.Entry<String, String> entry : sourceList.entrySet()) {
            DiffResponse diffResponse = new DiffResponse();
            diffResponse.setSourceTag(entry.getKey());
            diffResponse.setSourceValue(entry.getValue() + "");

            if (compareWithList.get(entry.getKey()) != null) {
                diffResponse.setCompareWithTag(entry.getKey());
                diffResponse.setCompareWithValue(compareWithList.get(entry.getKey()) + "");
                if (entry.getValue() != null && compareWithList.get(entry.getKey()) != null) {
                    boolean compareStatus = entry.getValue().equals(compareWithList.get(entry.getKey()));
                    diffResponse.setStatus(compareStatus);
                    if (!compareStatus) {
                        overall = false;
                    }
                } else {
                    overall = false;
                    diffResponse.setStatus(false);
                }
            } else {
                overall = false;
                diffResponse.setStatus(false);
            }
            differences.add(diffResponse);
        }

        for (Map.Entry<String, String> entry : compareWithList.entrySet()) {
            if (!sourceList.containsKey(entry.getKey())) {
                overall = false;
                DiffResponse diffResponse = new DiffResponse();
                diffResponse.setSourceTag("NULL");
                diffResponse.setSourceValue("NULL");
                diffResponse.setCompareWithTag(entry.getKey());
                diffResponse.setCompareWithValue(compareWithList.get(entry.getKey()) + "");
                differences.add(diffResponse);
            }
        }

        finalReport.put("diff", differences);
        finalReport.put("status", overall);

        return finalReport;
    }

    private static String getMeAll(Map<String, String> pathList) throws JSONException {
        JSONObject json = new JSONObject();
        for (Map.Entry<String, String> entry : pathList.entrySet()) {
            String dolorStripped = entry.getKey().replace("$.", "");
            if (dolorStripped.contains(".")) {
                String[] dotSeparated = dolorStripped.split("\\.");
                int depth = 0;
                Object jsonParent = json;
                for (int indexMain = 0; indexMain < dotSeparated.length - 1; indexMain++) {
                    String pathFixed = dotSeparated[indexMain].replaceAll("\\[.*\\]", "");
                    String arrayIndex = StringUtils.substringBetween(dotSeparated[indexMain], "[", "]");
                    if ((jsonParent instanceof JSONObject && ((JSONObject) jsonParent).has(pathFixed))) {
                        if (((JSONObject) jsonParent).get(pathFixed) instanceof JSONObject) {
                            jsonParent = (JSONObject) ((JSONObject) jsonParent).get(pathFixed);
                            depth++;
                        } else if (json.get(pathFixed) instanceof JSONArray) {
                            int arrayIndexNumber = 0;
                            arrayIndexNumber = Integer.parseInt(arrayIndex);
                            JSONArray parrentArray = ((JSONArray) json.get(pathFixed));
                            if (arrayIndexNumber == -1) {
                                arrayIndexNumber = -1;
                            }
                            if (parrentArray.length() > arrayIndexNumber) {
                                jsonParent = ((JSONArray) json.get(pathFixed)).get(arrayIndexNumber);
                            } else {
                                jsonParent = ((JSONArray) json.get(pathFixed));
                            }
                            depth++;

                        }

                    } else if ((jsonParent instanceof JSONArray)) {
                        JSONArray array = (JSONArray) jsonParent;
                        for (int index = 0; index < array.length(); index++) {
                            Object child = array.get(index);
                            if (child instanceof JSONObject && ((JSONObject) child).has(pathFixed)) {
                                jsonParent = ((JSONObject) child).get(pathFixed);
                                depth++;
                                break;
                            } else if (child instanceof JSONArray) {
                                JSONArray array1 = (JSONArray) child;
                                jsonParent = (JSONArray) json.get(pathFixed);
                                depth++;
                                break;
                            }

                        }
                    } else {
                        break;
                    }
                }

                if (!dotSeparated[dotSeparated.length - 1].contains("[")) {
                    JSONObject previous = new JSONObject();
                    previous.put(dotSeparated[dotSeparated.length - 1], entry.getValue());
                    if (depth == 0) {
                        for (int splitIndex = dotSeparated.length - 2; splitIndex >= 0; splitIndex--) {
                            if (dotSeparated[splitIndex].contains("[")) {
                                JSONArray newone = new JSONArray();
                                newone.put(previous);
                                JSONObject newone1 = new JSONObject();
                                newone1.put(dotSeparated[splitIndex].replaceAll("\\[.*\\]", ""), newone);
                                previous = newone1;
                            } else {
                                JSONObject newone = new JSONObject();
                                newone.put(dotSeparated[splitIndex], previous);
                                previous = newone;
                            }
                        }
                    } else {
                        for (int splitIndex = dotSeparated.length - 2; splitIndex >= depth; splitIndex--) {
                            JSONObject newone = new JSONObject();
                            newone.put(dotSeparated[splitIndex], previous);
                            previous = newone;
                        }
                    }

                    if (jsonParent instanceof JSONObject && ((JSONObject) jsonParent).keys().hasNext()) {
                        ((JSONObject) jsonParent).put(previous.keys().next().toString(), previous.get(previous.keys().next().toString()));
                    } else if (jsonParent instanceof JSONArray) {
                        // ((JSONArray)
                        // jsonParent).get(0).put(previous.keys().next().toString(),
                        // previous.get(previous.keys().next().toString()));
                        ((JSONArray) jsonParent).put(previous);
                    } else {
                        json = previous;
                    }
                } else {
                    JSONArray jsonDot = new JSONArray();
                    ((JSONArray) jsonDot).put(entry.getValue());
                    Object previous = new JSONArray();
                    String endKey = dotSeparated[dotSeparated.length - 1].replaceAll("\\[.*\\]", "");
                    if (depth == 0) {
                        for (int splitIndex = dotSeparated.length - 2; splitIndex >= 0; splitIndex--) {
                            if (dotSeparated[splitIndex].contains("[")) {
                                JSONArray newone = new JSONArray();
                                newone.put(previous);
                                JSONObject newone1 = new JSONObject();
                                newone1.put(dotSeparated[splitIndex].replaceAll("\\[.*\\]", ""), newone);
                                previous = newone1;
                            } else {
                                JSONObject newone = new JSONObject();
                                newone.put(dotSeparated[splitIndex], previous);
                                previous = newone;
                            }
                        }
                    } else {
                        for (int splitIndex = dotSeparated.length - 2; splitIndex >= depth; splitIndex--) {
                            JSONObject newone = new JSONObject();
                            newone.put(dotSeparated[splitIndex], previous);
                            previous = newone;
                        }
                    }

                    if (jsonParent instanceof JSONObject && ((JSONObject) jsonParent).keys().hasNext()) {
                        if (((JSONObject) jsonParent).has(endKey)) {
                            ((JSONArray) ((JSONObject) jsonParent).get(endKey)).put(entry.getValue());
                        } else {
                            ((JSONObject) jsonParent).put(endKey, jsonDot);
                        }
                        // ((JSONObject) jsonParent).put(previous.keys().next().toString(),
                        // previous.get(previous.keys().next().toString()));
                    } else if (jsonParent instanceof JSONArray) {
                        // ((JSONArray)
                        // jsonParent).get(0).put(previous.keys().next().toString(),
                        // previous.get(previous.keys().next().toString()));
                        ((JSONArray) jsonParent).put(previous);
                    } else {
                        json = (JSONObject) previous;
                    }

                }
                // json = previous;

                /*
                 * for(int splitIndex=dotSeparated.length -1; splitIndex > 0; splitIndex--){
                 * JSONObject jsonDot1 = new JSONObject(); jsonDot1 }
                 */
            } else {
                if (entry.getKey().contains("[")) {
                    String key = entry.getKey().replace("$.", "").replaceAll("\\[.*\\]", "");
                    if (json.has(key)) {
                        ((JSONArray) json.get(key)).put(entry.getValue());
                    } else {

                        JSONArray jsonDot = new JSONArray();
                        ((JSONArray) jsonDot).put(entry.getValue());
                        json.put(entry.getKey().replace("$.", "").replaceAll("\\[.*\\]", ""), jsonDot);
                    }
                } else {
                    json.put(entry.getKey().replace("$.", ""), entry.getValue());
                }
            }

        }
        return prettyFormatJSON(json.toString());
    }

    public static String prettyFormatJSON(String input) {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonParser jp = new JsonParser();
            JsonElement je = jp.parse(input);
            return gson.toJson(je);
        } catch (Exception e) {
            return input;
        }
    }

    private Map<String, String> getMeAll(String json) throws JSONException {
        Map<String, String> pathList = new LinkedHashMap<>();
        if (json.startsWith("{")) {
            JSONObject object = new JSONObject(json);

            String jsonPath = "$";
            if (json != JSONObject.NULL) {
                readObject(object, jsonPath, pathList);
            }
        } else if (json.startsWith("[")) {
            System.out.println("Json starts with [ does not support now.");
        }
        return pathList;
    }

    private void readObject(JSONObject object, String jsonPath, Map<String, String> pathList) throws JSONException {

        Iterator<String> keysItr = object.keys();
        String parentPath = jsonPath;
        while (keysItr.hasNext()) {
            String key = keysItr.next();
            Object value = object.get(key);
            jsonPath = parentPath + "." + key;

            if (value instanceof JSONArray) {
                readArray((JSONArray) value, jsonPath, pathList);
            } else if (value instanceof JSONObject) {
                readObject((JSONObject) value, jsonPath, pathList);
            } else { // is a value
                pathList.put(jsonPath, value + "");
            }
        }

    }

    private void readArray(JSONArray array, String jsonPath, Map<String, String> pathList) throws JSONException {
        String parentPath = jsonPath;
        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            jsonPath = parentPath + "[" + i + "]";

            if (value instanceof JSONArray) {
                readArray((JSONArray) value, jsonPath, pathList);
            } else if (value instanceof JSONObject) {
                readObject((JSONObject) value, jsonPath, pathList);
            } else { // is a value
                pathList.put(jsonPath, value + "");
            }
        }
    }
}
