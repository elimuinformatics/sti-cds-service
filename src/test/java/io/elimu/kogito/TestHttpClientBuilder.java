package io.elimu.kogito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class TestHttpClientBuilder {

	private static final Logger log = LoggerFactory.getLogger(TestHttpClientBuilder.class);
	
    private class CallItem {
        private String method;
        private String url;
        private int responseStatus;
        private String responseBody;
        private boolean expectAuth;

        CallItem(String method, String url, int responseStatus, String responseBody, boolean expectAuth) {
            this.method = method;
            this.url = url;
            this.responseStatus = responseStatus;
            this.responseBody = responseBody;
            this.expectAuth = expectAuth;
        }

        public boolean getExpectAuth() {
            return expectAuth;
        }

        public String getMethod() {
            return method;
        }

        public String getResponseBody() {
            return responseBody;
        }

        public int getResponseStatus() {
            return responseStatus;
        }

        public String getUrl() {
            return url;
        }

        @Override
        public boolean equals(Object obj) {
            return super.equals(obj);
        }
    }

    private final HttpClient httpClient;
    private Map<String, CallItem> calls = new HashMap<>();

    @SuppressWarnings("unchecked")
    public TestHttpClientBuilder() throws IOException {
        this.httpClient = Mockito.mock(HttpClient.class);
        Answer<HttpResponse> answer = new Answer<HttpResponse>() {
            @Override
            public HttpResponse answer(InvocationOnMock invocation) throws Throwable {
                HttpUriRequest req = (HttpUriRequest) invocation.getArgument(0);
                String key = req.getMethod() + "_" + req.getURI().toASCIIString();
                log.info("Received call for " + key + " on mock HTTP client");
                assertTrue(calls.containsKey(key), "Expected to be waiting for URL " + key);
                CallItem item = calls.get(key);
                assertEquals(item.getMethod(), req.getMethod());
                assertEquals(item.getUrl(), req.getURI().toASCIIString());
                if (item.getExpectAuth()) {
                    assertNotNull(req.getFirstHeader("Authorization"));
                }
                HttpResponse response = Mockito.mock(HttpResponse.class);
                StatusLine statusLine = Mockito.mock(StatusLine.class);
                Mockito.when(statusLine.getStatusCode()).thenReturn(item.getResponseStatus());
                Mockito.when(response.getStatusLine()).thenReturn(statusLine);
                Mockito.when(response.getEntity()).thenReturn(new StringEntity(item.getResponseBody(), ContentType.APPLICATION_JSON));
                Mockito.when(response.getAllHeaders()).thenReturn(new Header[0]);
                return response;
            }
        };
        Mockito.when(httpClient.execute(Mockito.any(HttpUriRequest.class))).thenAnswer(answer);
        Mockito.when(httpClient.execute(Mockito.any(HttpUriRequest.class), Mockito.any(ResponseHandler.class))).thenAnswer(new Answer<Object>() {
        	@Override
        	public Object answer(InvocationOnMock invocation) throws Throwable {
        		HttpResponse resp1 = answer.answer(invocation);
        		ResponseHandler<?> handler = (ResponseHandler<?>) invocation.getArgument(1);
        		return handler.handleResponse(resp1);
        	}
        });
    }

    public TestHttpClientBuilder expectCall(String method, String url, int responseStatus, URL responseFile) throws IOException, URISyntaxException {
        String responseBody = Files.readString(Paths.get(responseFile.toURI()));
        return expectCall(method, url, responseStatus, responseBody);
    }

    public TestHttpClientBuilder expectCall(String method, String url, int responseStatus, URL responseFile, boolean expectAuth) throws IOException, URISyntaxException {
        String responseBody = Files.readString(Paths.get(responseFile.toURI()));
        return expectCall(method, url, responseStatus, responseBody, expectAuth);
    }

    public TestHttpClientBuilder expectCall(String method, String url, int responseStatus, String responseBody) {
        return expectCall(method, url, responseStatus, responseBody, false);
    }

    public TestHttpClientBuilder expectCall(String method, String url, int responseStatus, String responseBody, boolean expectAuth) {
        String key = method + "_" + url;
        calls.put(key, new CallItem(method, url, responseStatus, responseBody, expectAuth));
        return this;
    }

    public HttpClient build() {
        return httpClient;
    }
}
