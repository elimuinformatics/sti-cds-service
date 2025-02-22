// Copyright 2018-2024 Elimu Informatics
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.elimu.kogito;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.text.SimpleDateFormat;

import org.apache.http.client.HttpClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import io.elimu.kogito.cql.CachingHttpClient;
import io.elimu.kogito.util.AuthHelper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.Response;

@QuarkusTest
public class ConfirmedGcTest {
    @ConfigProperty(name = "FHIR_TERMINOLOGY_SERVER_URL")
    String fhirTermUrl;

    @Test
    @TestSecurity(authorizationEnabled = false)
    void testDiscovery() throws IOException, URISyntaxException, InterruptedException {
    	Response response = given()
    			.header("Content-type", "application/json")
                .when().get("/cds-hooks")
                .then()
                .statusCode(200)
                .extract()
                .response();
    	List<?> services = response.jsonPath().getList("services");
    	assertNotNull(services);
    	assertEquals(2, services.size());
    	assertTrue(services.toString().contains("confirmed_gc_treatment"));
    }
    
    @Test
    @TestSecurity(authorizationEnabled = false)
    void testConfirmedGCCards() throws IOException, URISyntaxException, InterruptedException {
        String fhirUrl = "https://fhir-test.com/r4";
    	HttpClient client = new TestHttpClientBuilder().
            expectCall("GET", fhirTermUrl + "/PlanDefinition/12056?_format=json", 200, getClass().getResource("/confirmed-calls/plan-def.json")).
            expectCall("GET", fhirTermUrl + "/Library?name=SimpleGonorrheaCDS2&_format=json&_elements=name%2Cversion", 200, getClass().getResource("/confirmed-calls/simple-gonorrhea-library.json")).
            expectCall("GET", fhirTermUrl + "/Library/12055/_history/57?_format=json&_elements=name%2Ctype%2Cversion%2Ccontent", 200, getClass().getResource("/confirmed-calls/simple-gonorrhea-library-direct.json")).
            expectCall("GET", fhirTermUrl + "/Library?name=FHIRHelpers&_format=json&_elements=name%2Cversion", 200, getClass().getResource("/confirmed-calls/fhir-helper-library.json")).
            expectCall("GET", fhirUrl + "/Patient/123457", 200, getClass().getResource("/confirmed-calls/patient.json")).
            expectCall("GET", fhirTermUrl + "/PlanDefinition?url=http://elimu.io/PlanDefinition/GonorrheaCDSLaboratoryConfirmed&_format=json", 200, getClass().getResource("/confirmed-calls/gonorrhea-cds-plandef.json")).
            expectCall("GET", fhirTermUrl + "/Library?name=GonorrheaLabConfirmedCDS&_format=json&_elements=name%2Cversion", 200, getClass().getResource("/confirmed-calls/gonorrhea-lab-confirmed-library.json")).
            expectCall("GET", fhirTermUrl + "/Library/GonorrheaLabConfirmedCDS/_history/54?_format=json&_elements=name%2Ctype%2Cversion%2Ccontent", 200, getClass().getResource("/confirmed-calls/gonorrhea-lab-confirmed-library-direct.json")).
            expectCall("GET", fhirUrl + "/AllergyIntolerance?patient=Patient%2F123457", 200, getClass().getResource("/confirmed-calls/allergy-intollerances.json")).
            expectCall("GET", fhirTermUrl + "/ValueSet?url=http%3A%2F%2Fcts.nlm.nih.gov%2Ffhir%2FValueSet%2F2.16.840.1.113762.1.4.1213.1500", 200, getClass().getResource("/confirmed-calls/valueset-1.json")).
            expectCall("GET", fhirTermUrl + "/ValueSet/2.16.840.1.113762.1.4.1213.1500/$expand", 200, getClass().getResource("/confirmed-calls/valueset-2.json")).
            expectCall("GET", fhirTermUrl + "/ValueSet?url=http%3A%2F%2Fcts.nlm.nih.gov%2Ffhir%2FValueSet%2F2.16.840.1.113762.1.4.1196.4448", 200, getClass().getResource("/confirmed-calls/valueset-3.json")).
            expectCall("GET", fhirTermUrl + "/ValueSet/2.16.840.1.113762.1.4.1196.4448/$expand", 200, getClass().getResource("/confirmed-calls/valueset-4.json")).
            expectCall("GET", fhirTermUrl + "/ValueSet?url=http%3A%2F%2Fcts.nlm.nih.gov%2Ffhir%2FValueSet%2F2.16.840.1.113762.1.4.1196.1352", 200, getClass().getResource("/confirmed-calls/valueset-5.json")).
            expectCall("GET", fhirTermUrl + "/ValueSet/2.16.840.1.113762.1.4.1196.1352/$expand", 200, getClass().getResource("/confirmed-calls/valueset-6.json")).
            expectCall("GET", fhirUrl + "/Observation?code=http%3A%2F%2Floinc.org%7C18833-4%2Chttp%3A%2F%2Floinc.org%7C29463-7%2Chttp%3A%2F%2Floinc.org%7C3141-9%2Chttp%3A%2F%2Floinc.org%7C3142-7%2Chttp%3A%2F%2Floinc.org%7C75292-3%2Chttp%3A%2F%2Floinc.org%7C79348-9%2Chttp%3A%2F%2Floinc.org%7C8335-2%2Chttp%3A%2F%2Floinc.org%7C8340-2%2Chttp%3A%2F%2Floinc.org%7C8341-0%2Chttp%3A%2F%2Floinc.org%7C8350-1%2Chttp%3A%2F%2Floinc.org%7C8351-9&subject=Patient%2F123457", 200, getClass().getResource("/confirmed-calls/observations.json")).
            expectCall("GET", fhirTermUrl + "/ValueSet?url=http%3A%2F%2Fcts.nlm.nih.gov%2Ffhir%2FValueSet%2FGonorrheaTestsUnspecSite", 200, getClass().getResource("/confirmed-calls/valueset-7.json")).
            expectCall("GET", fhirTermUrl + "/ValueSet/GonorrheaTestsUnspecSite/$expand", 200, getClass().getResource("/confirmed-calls/valueset-8.json")).
            expectCall("GET", fhirUrl + "/Observation?code=http%3A%2F%2Floinc.org%7C24111-7%2Chttp%3A%2F%2Floinc.org%7C29311-8%2Chttp%3A%2F%2Floinc.org%7C31906-1%2Chttp%3A%2F%2Floinc.org%7C36902-5%2Chttp%3A%2F%2Floinc.org%7C43305-2%2Chttp%3A%2F%2Floinc.org%7C43403-5%2Chttp%3A%2F%2Floinc.org%7C43406-8%2Chttp%3A%2F%2Floinc.org%7C45073-4%2Chttp%3A%2F%2Floinc.org%7C45076-7%2Chttp%3A%2F%2Floinc.org%7C5028-6%2Chttp%3A%2F%2Floinc.org%7C698-1%2Chttp%3A%2F%2Floinc.org%7C91781-5%2Chttp%3A%2F%2Floinc.org%7C36903-3%2Chttp%3A%2F%2Floinc.org%7C43405-0&subject=Patient%2F123457", 200, transformData("/confirmed-calls/observations-2.json", "\\{\\{yesterday\\}\\}", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date(System.currentTimeMillis() - 86400000)))).
            expectCall("GET", fhirUrl + "/MedicationRequest?subject=Patient%2F123457", 200, getClass().getResource("/confirmed-calls/med-requests.json")).
            expectCall("GET", fhirUrl + "/Condition?subject=Patient%2F123457", 200, getClass().getResource("/confirmed-calls/conditions.json")).
    		expectCall("GET", fhirUrl + "/Medication/et6kIqz57048TZ-zI", 200, getClass().getResource("/confirmed-calls/medication-1.json")).
    		expectCall("GET", fhirUrl + "/Medication/eGVdHiKbEpRO.9kB3xR", 200, getClass().getResource("/confirmed-calls/medication-2.json")).
    		expectCall("GET", fhirTermUrl + "/ValueSet?url=http%3A%2F%2Fcts.nlm.nih.gov%2Ffhir%2FValueSet%2F2.16.840.1.113762.1.4.1196.4469", 200, getClass().getResource("/confirmed-calls/valueset-9.json")).
    		expectCall("GET", fhirTermUrl + "/ValueSet/2.16.840.1.113762.1.4.1196.4469/$expand", 200, getClass().getResource("/confirmed-calls/valueset-10.json")).
    		expectCall("GET", fhirTermUrl + "/ValueSet?url=http%3A%2F%2Fcts.nlm.nih.gov%2Ffhir%2FValueSet%2F2.16.840.1.113762.1.4.1196.4474", 200, getClass().getResource("/confirmed-calls/valueset-11.json")).
    		expectCall("GET", fhirTermUrl + "/ValueSet/2.16.840.1.113762.1.4.1196.4474/$expand", 200, getClass().getResource("/confirmed-calls/valueset-12.json")).
    		expectCall("GET", fhirUrl + "/Observation?code=http%3A%2F%2Floinc.org%7C14092-1%2Chttp%3A%2F%2Floinc.org%7C16975-5%2Chttp%3A%2F%2Floinc.org%7C18396-2%2Chttp%3A%2F%2Floinc.org%7C24012-7%2Chttp%3A%2F%2Floinc.org%7C29893-5%2Chttp%3A%2F%2Floinc.org%7C30361-0%2Chttp%3A%2F%2Floinc.org%7C31201-7%2Chttp%3A%2F%2Floinc.org%7C33660-2%2Chttp%3A%2F%2Floinc.org%7C33806-1%2Chttp%3A%2F%2Floinc.org%7C33807-9%2Chttp%3A%2F%2Floinc.org%7C40732-0%2Chttp%3A%2F%2Floinc.org%7C40733-8%2Chttp%3A%2F%2Floinc.org%7C43009-0%2Chttp%3A%2F%2Floinc.org%7C43011-6%2Chttp%3A%2F%2Floinc.org%7C44873-8%2Chttp%3A%2F%2Floinc.org%7C5221-7%2Chttp%3A%2F%2Floinc.org%7C5222-5%2Chttp%3A%2F%2Floinc.org%7C5225-8%2Chttp%3A%2F%2Floinc.org%7C56888-1%2Chttp%3A%2F%2Floinc.org%7C7917-8%2Chttp%3A%2F%2Floinc.org%7C7918-6%2Chttp%3A%2F%2Floinc.org%7C7919-4%2Chttp%3A%2F%2Floinc.org%7C85686-4%2Chttp%3A%2F%2Floinc.org%7C86233-4%2Chttp%3A%2F%2Floinc.org%7C9821-0&subject=Patient%2F123457", 200, getClass().getResource("/confirmed-calls/observations-3.json")).
    		expectCall("GET", fhirTermUrl + "/ActivityDefinition?url=http%3A%2F%2Felimu.io%2FActivityDefinition%2FHIVTestOrderProposal", 200, getClass().getResource("/confirmed-calls/hiv-test-activity-def.json")).
    		expectCall("GET", fhirTermUrl + "/ActivityDefinition/HIVTestOrderProposal/_history/10", 200, getClass().getResource("/confirmed-calls/hiv-test-activity-def-direct.json")).
    		expectCall("GET", fhirTermUrl + "/Library?name=CDSHooksSupport&_format=json&_elements=name%2Cversion", 200, getClass().getResource("/confirmed-calls/cds-hooks-support-library.json")).
    		expectCall("GET", fhirTermUrl + "/Library/CDSHooksSupport/_history/1?_format=json&_elements=name%2Ctype%2Cversion%2Ccontent", 200, getClass().getResource("/confirmed-calls/cds-hooks-support-library-direct.json")).
		expectCall("GET", fhirTermUrl + "/ValueSet?url=http%3A%2F%2Fcts.nlm.nih.gov%2Ffhir%2FValueSet%2F2.16.840.1.113883.3.464.1003.120.12.1003", 200, getClass().getResource("/confirmed-calls/valueset-13.json")).
		expectCall("GET", fhirTermUrl + "/ValueSet/2.16.840.1.113883.3.464.1003.120.12.1003/$expand", 200, getClass().getResource("/confirmed-calls/valueset-14.json")).
		expectCall("GET", fhirUrl + "/Specimen/663693", 200, getClass().getResource("/confirmed-calls/specimen.json")).
		expectCall("GET", fhirTermUrl + "/ValueSet?url=http%3A%2F%2Fcts.nlm.nih.gov%2Ffhir%2FValueSet%2F2.16.840.1.113762.1.4.1196.4467", 200, getClass().getResource("/confirmed-calls/valueset-15.json")).
		expectCall("GET", fhirTermUrl + "/ValueSet/2.16.840.1.113762.1.4.1196.4467/$expand", 200, getClass().getResource("/confirmed-calls/valueset-16.json")).
		expectCall("GET", fhirTermUrl + "/ValueSet?url=http%3A%2F%2Fcts.nlm.nih.gov%2Ffhir%2FValueSet%2F2.16.840.1.113762.1.4.1196.4466", 200, getClass().getResource("/confirmed-calls/valueset-17.json")).
		expectCall("GET", fhirTermUrl + "/ValueSet/2.16.840.1.113762.1.4.1196.4466/$expand", 200, getClass().getResource("/confirmed-calls/valueset-18.json")).
		expectCall("GET", fhirTermUrl + "/ActivityDefinition?url=http%3A%2F%2Felimu.io%2FActivityDefinition%2FCeftriaxone500OrderProposal", 200, getClass().getResource("/confirmed-calls/actdef-2.json")).
		expectCall("GET", fhirTermUrl + "/ActivityDefinition/Ceftriaxone500OrderProposal/_history/25", 200, getClass().getResource("/confirmed-calls/actdef-2-direct.json")).
		expectCall("GET", fhirTermUrl + "/ActivityDefinition?url=http%3A%2F%2Felimu.io%2FActivityDefinition%2FGentamicin240OrderProposal", 200, getClass().getResource("/confirmed-calls/actdef-3.json")).
		expectCall("GET", fhirTermUrl + "/ActivityDefinition/Gentamicin240OrderProposal/_history/13", 200, getClass().getResource("/confirmed-calls/actdef-3-direct.json")).
		expectCall("GET", fhirTermUrl + "/ActivityDefinition?url=http%3A%2F%2Felimu.io%2FActivityDefinition%2FAzithromycin2GmOrderProposal", 200, getClass().getResource("/confirmed-calls/actdef-4.json")).
		expectCall("GET", fhirTermUrl + "/ActivityDefinition/Azithromycin2GmOrderProposal/_history/3", 200, getClass().getResource("/confirmed-calls/actdef-4-direct.json")).
    		build();

        AuthHelper.setTestClient(client);
        CachingHttpClient.setTestClient(client);
        String jsonBody = transformData("/gc-confirmed-request-sample.json", "\\{\\{fhirBaseUrl\\}\\}", fhirUrl);;
        Response response = given()
            .body(jsonBody)
            .header("Content-type", "application/json")
            .when().post("/cds-hooks/confirmed_gc_treatment")
            .then()
            .statusCode(200)
            .extract()
            .response();
        List<?> cards = response.jsonPath().getList("cards");
        assertNotNull(cards);
        assertEquals(1, cards.size());
    }

    String transformData(String file, String text, String replaceWith) throws IOException {
	String content = new String(getClass().getResourceAsStream(file).readAllBytes());
	String retval = content.replaceAll(text, replaceWith);
	return retval;
    }
}

