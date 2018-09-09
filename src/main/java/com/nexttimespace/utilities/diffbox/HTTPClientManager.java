package com.nexttimespace.utilities.diffbox;

import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;
import java.security.cert.X509Certificate;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

public class HTTPClientManager {

	RestTemplate restTemplate = null;
	Map<String, String> commonHeaders = new LinkedHashMap<>();
	
	@PostConstruct
	public void setup() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException{
		
		HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = null;
		clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(
		            HttpClientBuilder.create().build());
		if(Boolean.valueOf(UtilityFunction.appProperties.getProperty("disablessl"))) {
			try {
				TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;

			    SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
			                    .loadTrustMaterial(null, acceptingTrustStrategy)
			                    .build();

			    SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);

			    CloseableHttpClient httpClient = HttpClients.custom()
			                    .setSSLSocketFactory(csf)
			                    .build();
				clientHttpRequestFactory.setHttpClient(httpClient);
				System.out.println("INFO: SSL Validations disabled");
			} catch(Exception e) {
			    System.out.println("ERROR: Exception disabling SSL validation");
			}
			
		}
		clientHttpRequestFactory.setReadTimeout(5 * 1000);
		clientHttpRequestFactory.setConnectTimeout(5 * 1000);
		
		restTemplate = new RestTemplate(clientHttpRequestFactory);
		registerCommonHeaders();
	}
	

	private void registerCommonHeaders() {
		try {
			String headers = UtilityFunction.appProperties.getProperty("common.headers");
			
			String[] headersArray = headers.split(";", -1);
			
			for(String heads : headersArray) {
				String[] head = heads.split(":", -1);
				if(head.length > 1) {
					commonHeaders.put(head[0].trim(), head[0].trim());
				}
			}
			System.out.println("INFO: Common header Enabled for" + commonHeaders);
		} catch(Exception e) {
		    System.out.println("WARN: Error parsing common headers, ignoring all common headers");
		}
		
	}
	public static class Const {
		public static final String REQUEST = "request";
		public static final String URL = "url";
		public static final String CHAR_SET = "charSet";
		public static final String TARGET_CHAR_SET = "targetcharSet";
		public static final String RESPONSE = "response";
		public static final String TIME_ELASPED = "timeElasped";
		public static final String METHOD = "method";
		public static final String REQUEST_HEADER = "reqHeader";
		public static final String RESPONSE_HEADER = "resHeader";
		public static final String RESPONSE_STATUS = "resStatus";
		public static final String RESPONSE_LENGTH = "resLength";
		public static final String ERROR_RESPONSE = "errorResponse";
	}
	
	

	public Map<String, Object> getResponse(Map<String, Object> properties) {
		HttpHeaders headers = new HttpHeaders();
		HttpEntity requestEntity = null;
		Map<String, Object> responseReturn = new LinkedHashMap<>();
		HttpMethod method = null;
		if (properties.get(Const.REQUEST_HEADER) != null && properties.get(Const.REQUEST_HEADER) instanceof String) {
			String header = properties.get(Const.REQUEST_HEADER).toString().trim();
			if (!header.isEmpty()) {
				String[] splitHeaderNewLine = header.split("\\r?\\n");
				if (splitHeaderNewLine.length > 0) {
					for (String headerIterator : splitHeaderNewLine) {
						if (headerIterator.contains(":")) {
							String[] splitHedaer = headerIterator.split(":", 2);
							if(splitHedaer[0].equalsIgnoreCase("accept")){
								if(splitHedaer.length > 1){
									headers.setAccept(MediaType.parseMediaTypes(splitHedaer[1]));
								}
							}else{
							if (splitHedaer.length >= 2) {
								headers.add(splitHedaer[0], splitHedaer[1]);
							} else {
								headers.add(splitHedaer[0], "");
							}
							}
						}
					}
				}
			}
		}
		
		if (properties.get(Const.REQUEST_HEADER) != null && properties.get(Const.REQUEST_HEADER) instanceof Map) {
		    Map<String, String> headersRequest =  (Map<String, String>) properties.get(Const.REQUEST_HEADER);
		    
		    for(Map.Entry<String, String> headerEntry : headersRequest.entrySet()) {
	            headers.add(headerEntry.getKey(), headerEntry.getValue());
		    }
		}
		
		if(!commonHeaders.isEmpty()) {
			for(Entry<String, String> entry: commonHeaders.entrySet()) {
				headers.add(entry.getKey(), entry.getValue());
			}
		}
		
		if (properties.get(Const.METHOD).toString().equals("GET")) {
			method = HttpMethod.GET;
			requestEntity = new HttpEntity<String>("", headers);
		} else if (properties.get(Const.METHOD).toString().equals("POST")) {
			method = HttpMethod.POST;
			requestEntity = new HttpEntity<String>(properties.get(Const.REQUEST).toString(), headers);
		}else if (properties.get(Const.METHOD).toString().equals("PUT")) {
			method = HttpMethod.PUT;
			requestEntity = new HttpEntity<String>(properties.get(Const.REQUEST).toString(), headers);
		}else if (properties.get(Const.METHOD).toString().equals("DELETE")) {
			method = HttpMethod.DELETE;
			requestEntity = new HttpEntity<String>(properties.get(Const.REQUEST).toString(), headers);
		}
		//StopWatch stopwatch = StopWatch.createStarted();
		ResponseEntity<String> response = null;
		try {
			response = restTemplate.exchange(properties.get(Const.URL).toString(), method, requestEntity, String.class);
			//stopwatch.stop();
			String body = response.getBody();
			if(properties.get(Const.CHAR_SET) != null) {
				try {
				    String targetCharSet = properties.get(Const.TARGET_CHAR_SET) != null ? properties.get(Const.TARGET_CHAR_SET).toString() : "UTF-8"; 
    				body = new String(body.getBytes(Charset.forName(properties.get(Const.CHAR_SET).toString())), targetCharSet);
    				body = body.replaceAll("[^\\x00-\\x7F]", "");
				}catch(Exception e) {
				    System.out.println("Error using charSet" + e);
				}
			}
			//responseReturn.put(Const.TIME_ELASPED, stopwatch.getTime(TimeUnit.MILLISECONDS));
			responseReturn.put(Const.RESPONSE_STATUS, response.getStatusCode());
			
			responseReturn.put(Const.RESPONSE_LENGTH, body!=null?body.getBytes().length:0);
			responseReturn.put(Const.RESPONSE, body!=null?body:"");
			responseReturn.put(Const.RESPONSE_HEADER, response.getHeaders());
			responseReturn.put(Const.RESPONSE_STATUS, response.getStatusCode());
		} catch (org.springframework.web.client.HttpClientErrorException |org.springframework.web.client.HttpServerErrorException  exception) {
			String body = exception.getResponseBodyAsString();
			responseReturn.put(Const.RESPONSE_LENGTH, body.getBytes().length);
			responseReturn.put(Const.RESPONSE, body);
			responseReturn.put(Const.RESPONSE_HEADER, exception.getResponseHeaders());
			responseReturn.put(Const.RESPONSE_STATUS, exception.getStatusCode());
			responseReturn.put(Const.ERROR_RESPONSE, "true");
		}catch(org.springframework.web.client.ResourceAccessException exception){
			responseReturn.put(Const.RESPONSE_LENGTH, "0");
			responseReturn.put(Const.RESPONSE, exception.getMessage());
			responseReturn.put(Const.RESPONSE_HEADER,"NA" );
			responseReturn.put(Const.RESPONSE_STATUS, "NA");
			responseReturn.put(Const.ERROR_RESPONSE, "true");
		}catch(Exception exception){
			responseReturn.put(Const.RESPONSE_LENGTH, "0");
			responseReturn.put(Const.RESPONSE, exception.getMessage());
			responseReturn.put(Const.RESPONSE_HEADER,"NA" );
			responseReturn.put(Const.RESPONSE_STATUS, "NA");
			responseReturn.put(Const.ERROR_RESPONSE, "true");
		}
		//responseReturn.put(Const.TIME_ELASPED, stopwatch.getTime(TimeUnit.MILLISECONDS));
		return responseReturn;
	}

}
