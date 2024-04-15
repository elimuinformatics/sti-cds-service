package io.elimu.kogito.util;

import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.digest.Md5Crypt;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class AuthHelper {

    private static final Logger LOG = LoggerFactory.getLogger(AuthHelper.class);
    private static final Map<String, Map<String, Object>> CACHE = new HashMap<>();

    private static HttpClient testClient;
    
    public static void setTestClient(HttpClient testClient) {
		AuthHelper.testClient = testClient;
	}
    
	public static String getAuthToken(String tokenUrl, String scope, String clientId, String clientSecret, String grantType, String username, String password) {
		StringBuilder body = new StringBuilder();
		addToBody(body, "token_url", tokenUrl);
		addToBody(body, "scope", scope);
		addToBody(body, "client_id", clientId);
		addToBody(body, "client_secret", clientSecret);
		addToBody(body, "grant_type", grantType);
		addToBody(body, "username", username);
		addToBody(body, "password", password);
		String keyUnhashed = "url=" + tokenUrl + "&" + body + "&client_id=" + clientId + "&client_secret=" + clientSecret;
        String key = Md5Crypt.md5Crypt(keyUnhashed.getBytes());
        Map<String, Object> retval = CACHE.get(key);
        if (retval != null) {
            Long expirationTimeMillis = (Long) retval.get("_timeutil_expiration_time_millis");
            if (expirationTimeMillis != null && expirationTimeMillis.longValue() < System.currentTimeMillis()) {
                return (String) (retval == null ? null : retval.get("access_token"));
            }
        }
        retval = _authenticate(body.toString(), tokenUrl, clientId, clientSecret);
        if (retval.containsKey("expires_in")) {
            Long expirationTime = ((Long) retval.get("expires_in")).longValue() * 1000 + System.currentTimeMillis();
            retval.put("_timeutil_expiration_time_millis", expirationTime);
            CACHE.put(key, retval);
        }
        return (String) (retval == null ? null : retval.get("access_token"));
	}

	private static void addToBody(StringBuilder sb, String key , String value) {
		if (value == null || "".equals(value.trim())) {
			return;
		}
		if (sb.length() > 0) {
			sb.append('&');
		}
		sb.append(key).append("=").append(value);
        
	}

    private static Map<String, Object> _authenticate(String body, String tokenUrl, String clientId, String clientSecret) {
    	HttpClient client = testClient == null ? HttpClientBuilder.create().build() : testClient;
        Map<String, Object> results = new HashMap<>();
        try {
        	HttpPost post = new HttpPost(tokenUrl);
        	post.setEntity(new StringEntity(body));
        	post.setHeader("Accept", "*/*");
        	post.setHeader("Content-Type", "application/x-www-form-urlencoded");
        	post.setHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString((clientId + ":" + (clientSecret == null ? "" : clientSecret)).getBytes()));  //NOSONAR
        	HttpResponse resp = client.execute(post);
            try (InputStream input = resp.getEntity().getContent()){
            	String response = new String(input.readAllBytes());
                results = new Gson().fromJson(response, new TypeToken<Map<String, Object>>() { }.getType());
                longify(results, "expires_in");
                longify(results, "refresh_expires_in");
            } catch (Exception e2) {
                throw e2;
            }
        } catch (Exception e) {
            results.put("error", e);
            results.put("errorMessage", e.getMessage());
        }
        return results;
    }

    private static void longify(Map<String, Object> results, String key) {
        if (results.containsKey(key)) {
            try {
                Object val = results.get(key);
                if (val instanceof Number) {
                    results.put(key, ((Number)val).longValue());
                } else {
                    String value = String.valueOf(val);
                    results.put(key, Long.parseLong(value));
                }
            } catch (Exception e) {
                LOG.warn("Couldn't parse value '" + results.get(key) + "' from key '" + key + "' as long");
            }
        }
    }
}
