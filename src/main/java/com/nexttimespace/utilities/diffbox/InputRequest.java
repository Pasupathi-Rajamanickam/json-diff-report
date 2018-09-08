package com.nexttimespace.utilities.diffbox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InputRequest {
    
    private List<Input> inputs = new ArrayList<>();
    
    public List<Input> getInputs() {
        return inputs;
    }

    public void setInputs(List<Input> inputs) {
        this.inputs = inputs;
    }


    public static class Input {
        private String urlSource;
        private String urlCompare;
        private String method;
        private Map<String, String> headersSource;
        private Map<String, String> headersCompare;
        private String requestSource;
        private String requestCompare;
        public String getUrlSource() {
            return urlSource;
        }
        public void setUrlSource(String urlSource) {
            this.urlSource = urlSource;
        }
        public String getUrlCompare() {
            return urlCompare;
        }
        public void setUrlCompare(String urlCompare) {
            this.urlCompare = urlCompare;
        }
        public String getMethod() {
            return method;
        }
        public void setMethod(String method) {
            this.method = method;
        }
        public Map<String, String> getHeadersSource() {
            return headersSource;
        }
        public void setHeadersSource(Map<String, String> headersSource) {
            this.headersSource = headersSource;
        }
        public Map<String, String> getHeadersCompare() {
            return headersCompare;
        }
        public void setHeadersCompare(Map<String, String> headersCompare) {
            this.headersCompare = headersCompare;
        }
        public String getRequestSource() {
            return requestSource;
        }
        public void setRequestSource(String requestSource) {
            this.requestSource = requestSource;
        }
        public String getRequestCompare() {
            return requestCompare;
        }
        public void setRequestCompare(String requestCompare) {
            this.requestCompare = requestCompare;
        }
        
    }

}
