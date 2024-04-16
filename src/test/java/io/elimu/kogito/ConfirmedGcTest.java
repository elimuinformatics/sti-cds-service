package io.elimu.kogito;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpClient;
import org.junit.jupiter.api.Test;

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
    void testConfig() throws IOException, URISyntaxException, InterruptedException {
    	// fhirTerminologyUrl = https://fhir4-test-terminology.com/r4
    	// fhirServerUrl = https://fhir4-test.com/r4
  	  
    	HttpClient client = new TestHttpClientBuilder().
            expectCall("POST", "https://oauth-test.com/auth/realms/product/protocol/openid-connect/token", 200, getClass().getResource("/confirmed-calls/auth-token.json")).
            expectCall("GET", "https://fhir4-test-terminology.com/r4/PlanDefinition/12056?_format=json", 200, getClass().getResource("/confirmed-calls/plan-def.json")).
            expectCall("GET", "https://fhir4-test-terminology.com/r4/Library?name=SimpleGonorrheaCDS2&_format=json&_elements=name%2Cversion", 200, getClass().getResource("/confirmed-calls/simple-gonorrhea-library.json")).
            expectCall("GET", "https://fhir4-test-terminology.com/r4/Library/12055/_history/57?_format=json&_elements=name%2Ctype%2Cversion%2Ccontent", 200, getClass().getResource("/confirmed-calls/simple-gonorrhea-library-direct.json")).
            expectCall("GET", "https://fhir4-test-terminology.com/r4/Library?name=FHIRHelpers&_format=json&_elements=name%2Cversion", 200, getClass().getResource("/confirmed-calls/fhir-helper-library.json")).
            expectCall("GET", "https://fhir4-test.com/r4/Patient/123457", 200, getClass().getResource("/confirmed-calls/patient.json")).
            expectCall("GET", "https://fhir4-test-terminology.com/r4/PlanDefinition?url=http://elimu.io/PlanDefinition/GonorrheaCDSLaboratoryConfirmed&_format=json", 200, getClass().getResource("/confirmed-calls/gonorrhea-cds-plandef.json")).
            expectCall("GET", "https://fhir4-test-terminology.com/r4/Library?name=GonorrheaLabConfirmedCDS&_format=json&_elements=name%2Cversion", 200, getClass().getResource("/confirmed-calls/gonorrhea-lab-confirmed-library.json")).
            expectCall("GET", "https://fhir4-test-terminology.com/r4/Library/GonorrheaLabConfirmedCDS/_history/54?_format=json&_elements=name%2Ctype%2Cversion%2Ccontent", 200, getClass().getResource("/confirmed-calls/gonorrhea-lab-confirmed-library-direct.json")).
            expectCall("GET", "https://fhir4-test.com/r4/AllergyIntolerance?patient=Patient%2F123457", 200, getClass().getResource("/confirmed-calls/allergy-intollerances.json")).
            expectCall("GET", "https://fhir4-test-terminology.com/r4/ValueSet?url=http%3A%2F%2Fcts.nlm.nih.gov%2Ffhir%2FValueSet%2F2.16.840.1.113762.1.4.1213.1500", 200, getClass().getResource("/confirmed-calls/valueset-1.json")).
            expectCall("GET", "https://fhir4-test-terminology.com/r4/ValueSet/2.16.840.1.113762.1.4.1213.1500/$expand", 200, getClass().getResource("/confirmed-calls/valueset-2.json")).
            expectCall("GET", "https://fhir4-test-terminology.com/r4/ValueSet?url=http%3A%2F%2Fcts.nlm.nih.gov%2Ffhir%2FValueSet%2F2.16.840.1.113762.1.4.1196.4448", 200, getClass().getResource("/confirmed-calls/valueset-3.json")).
            expectCall("GET", "https://fhir4-test-terminology.com/r4/ValueSet/2.16.840.1.113762.1.4.1196.4448/$expand", 200, getClass().getResource("/confirmed-calls/valueset-4.json")).
            expectCall("GET", "https://fhir4-test-terminology.com/r4/ValueSet?url=http%3A%2F%2Fcts.nlm.nih.gov%2Ffhir%2FValueSet%2F2.16.840.1.113762.1.4.1196.1352", 200, getClass().getResource("/confirmed-calls/valueset-5.json")).
            expectCall("GET", "https://fhir4-test-terminology.com/r4/ValueSet/2.16.840.1.113762.1.4.1196.1352/$expand", 200, getClass().getResource("/confirmed-calls/valueset-6.json")).
            expectCall("GET", "https://fhir4-test.com/r4/Observation?code=http%3A%2F%2Floinc.org%7C18833-4%2Chttp%3A%2F%2Floinc.org%7C29463-7%2Chttp%3A%2F%2Floinc.org%7C3141-9%2Chttp%3A%2F%2Floinc.org%7C3142-7%2Chttp%3A%2F%2Floinc.org%7C75292-3%2Chttp%3A%2F%2Floinc.org%7C79348-9%2Chttp%3A%2F%2Floinc.org%7C8335-2%2Chttp%3A%2F%2Floinc.org%7C8340-2%2Chttp%3A%2F%2Floinc.org%7C8341-0%2Chttp%3A%2F%2Floinc.org%7C8350-1%2Chttp%3A%2F%2Floinc.org%7C8351-9&subject=Patient%2F123457", 200, getClass().getResource("/confirmed-calls/observations.json")).
            expectCall("GET", "https://fhir4-test-terminology.com/r4/ValueSet?url=http%3A%2F%2Fcts.nlm.nih.gov%2Ffhir%2FValueSet%2FGonorrheaTestsUnspecSite", 200, getClass().getResource("/confirmed-calls/valueset-7.json")).
            expectCall("GET", "https://fhir4-test-terminology.com/r4/ValueSet/GonorrheaTestsUnspecSite/$expand", 200, getClass().getResource("/confirmed-calls/valueset-8.json")).
            expectCall("GET", "https://fhir4-test.com/r4/Observation?code=http%3A%2F%2Floinc.org%7C24111-7%2Chttp%3A%2F%2Floinc.org%7C29311-8%2Chttp%3A%2F%2Floinc.org%7C31906-1%2Chttp%3A%2F%2Floinc.org%7C36902-5%2Chttp%3A%2F%2Floinc.org%7C43305-2%2Chttp%3A%2F%2Floinc.org%7C43403-5%2Chttp%3A%2F%2Floinc.org%7C43406-8%2Chttp%3A%2F%2Floinc.org%7C45073-4%2Chttp%3A%2F%2Floinc.org%7C45076-7%2Chttp%3A%2F%2Floinc.org%7C5028-6%2Chttp%3A%2F%2Floinc.org%7C698-1%2Chttp%3A%2F%2Floinc.org%7C91781-5%2Chttp%3A%2F%2Floinc.org%7C36903-3%2Chttp%3A%2F%2Floinc.org%7C43405-0&subject=Patient%2F123457", 200, getClass().getResource("/confirmed-calls/observations-2.json")).
            expectCall("GET", "https://fhir4-test.com/r4/MedicationRequest?subject=Patient%2F123457", 200, getClass().getResource("/confirmed-calls/med-requests.json")).
            expectCall("GET", "https://fhir4-test.com/r4/Condition?subject=Patient%2F123457", 200, getClass().getResource("/confirmed-calls/conditions.json")).
    		expectCall("GET", "https://fhir4-test.com/r4/Medication/et6kIqz57048TZ-zI", 200, getClass().getResource("/confirmed-calls/medication-1.json")).
    		expectCall("GET", "https://fhir4-test.com/r4/Medication/eGVdHiKbEpRO.9kB3xR", 200, getClass().getResource("/confirmed-calls/medication-2.json")).
    		expectCall("GET", "https://fhir4-test-terminology.com/r4/ValueSet?url=http%3A%2F%2Fcts.nlm.nih.gov%2Ffhir%2FValueSet%2F2.16.840.1.113762.1.4.1196.4469", 200, getClass().getResource("/confirmed-calls/valueset-9.json")).
    		expectCall("GET", "https://fhir4-test-terminology.com/r4/ValueSet/2.16.840.1.113762.1.4.1196.4469/$expand", 200, getClass().getResource("/confirmed-calls/valueset-10.json")).
    		expectCall("GET", "https://fhir4-test-terminology.com/r4/ValueSet?url=http%3A%2F%2Fcts.nlm.nih.gov%2Ffhir%2FValueSet%2F2.16.840.1.113762.1.4.1196.4474", 200, getClass().getResource("/confirmed-calls/valueset-11.json")).
    		expectCall("GET", "https://fhir4-test-terminology.com/r4/ValueSet/2.16.840.1.113762.1.4.1196.4474/$expand", 200, getClass().getResource("/confirmed-calls/valueset-12.json")).
    		expectCall("GET", "https://fhir4-test.com/r4/Observation?code=http%3A%2F%2Floinc.org%7C14092-1%2Chttp%3A%2F%2Floinc.org%7C16975-5%2Chttp%3A%2F%2Floinc.org%7C18396-2%2Chttp%3A%2F%2Floinc.org%7C24012-7%2Chttp%3A%2F%2Floinc.org%7C29893-5%2Chttp%3A%2F%2Floinc.org%7C30361-0%2Chttp%3A%2F%2Floinc.org%7C31201-7%2Chttp%3A%2F%2Floinc.org%7C33660-2%2Chttp%3A%2F%2Floinc.org%7C33806-1%2Chttp%3A%2F%2Floinc.org%7C33807-9%2Chttp%3A%2F%2Floinc.org%7C40732-0%2Chttp%3A%2F%2Floinc.org%7C40733-8%2Chttp%3A%2F%2Floinc.org%7C43009-0%2Chttp%3A%2F%2Floinc.org%7C43011-6%2Chttp%3A%2F%2Floinc.org%7C44873-8%2Chttp%3A%2F%2Floinc.org%7C5221-7%2Chttp%3A%2F%2Floinc.org%7C5222-5%2Chttp%3A%2F%2Floinc.org%7C5225-8%2Chttp%3A%2F%2Floinc.org%7C56888-1%2Chttp%3A%2F%2Floinc.org%7C7917-8%2Chttp%3A%2F%2Floinc.org%7C7918-6%2Chttp%3A%2F%2Floinc.org%7C7919-4%2Chttp%3A%2F%2Floinc.org%7C85686-4%2Chttp%3A%2F%2Floinc.org%7C86233-4%2Chttp%3A%2F%2Floinc.org%7C9821-0&subject=Patient%2F123457", 200, getClass().getResource("/confirmed-calls/observations-3.json")).
    		expectCall("GET", "https://fhir4-test-terminology.com/r4/ActivityDefinition?url=http%3A%2F%2Felimu.io%2FActivityDefinition%2FHIVTestOrderProposal", 200, getClass().getResource("/confirmed-calls/hiv-test-activity-def.json")).
    		expectCall("GET", "https://fhir4-test-terminology.com/r4/ActivityDefinition/HIVTestOrderProposal/_history/10", 200, getClass().getResource("/confirmed-calls/hiv-test-activity-def-direct.json")).
    		expectCall("GET", "https://fhir4-test-terminology.com/r4/Library?name=CDSHooksSupport&_format=json&_elements=name%2Cversion", 200, getClass().getResource("/confirmed-calls/cds-hooks-support-library.json")).
    		expectCall("GET", "https://fhir4-test-terminology.com/r4/Library/CDSHooksSupport/_history/1?_format=json&_elements=name%2Ctype%2Cversion%2Ccontent", 200, getClass().getResource("/confirmed-calls/cds-hooks-support-library-direct.json")).
    		build();

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

