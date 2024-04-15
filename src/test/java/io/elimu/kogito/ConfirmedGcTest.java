package io.elimu.kogito;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.elimu.kogito.cql.CachingHttpClient;
import io.elimu.kogito.util.AuthHelper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;

@QuarkusTest
@Slf4j
public class ConfirmedGcTest {

    @Test
    @TestSecurity(authorizationEnabled = false)
    void testConfig() throws IOException, InterruptedException {
        HttpClient client = Mockito.mock(HttpClient.class);
        AuthHelper.setTestClient(client);
        CachingHttpClient.setTestClient(client);
        String jsonBody = IOUtils.toString(getClass().getResourceAsStream("/gc-confirmed-request-sample.json"), StandardCharsets.UTF_8);
        String expectedResourceType = "OperationOutcome";
        Response response = given()
            .body(jsonBody)
            .header("Content-type", "application/json")
            .when().post("/confirmed_gc_treatment")
            .then()
            .statusCode(201)
            .extract()
            .response();
        String outcome = response.prettyPrint();
	System.out.println("OUTCOME = " + outcome);
        assertTrue(outcome != null && outcome.contains(expectedResourceType));
        List<?> cards = response.jsonPath().getList("cards");
        assertNotNull(cards);
        assertEquals(0, cards.size());
    }
}

