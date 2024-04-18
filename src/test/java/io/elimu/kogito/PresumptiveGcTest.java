package io.elimu.kogito;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpClient;
import org.junit.jupiter.api.Test;

import io.elimu.kogito.cql.CachingHttpClient;
import io.elimu.kogito.util.AuthHelper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.Response;

@QuarkusTest
public class PresumptiveGcTest {

    @Test
    @TestSecurity(authorizationEnabled = false)
    void testPresumptiveGCCards() throws IOException, URISyntaxException, InterruptedException {
    	HttpClient client = new TestHttpClientBuilder().
            expectCall("POST", "https://auth-internal.elimuinformatics.com/auth/realms/product/protocol/openid-connect/token", 200, getClass().getResource("/presumptive-calls/auth-token.json")).
            expectCall("GET", "https://fhir4-terminology-sandbox-internal.elimuinformatics.com/baseR4/PlanDefinition?url=http://elimu.io/PlanDefinition/GonorrheaCDSPresumptiveTreatment&_format=json", 200, getClass().getResource("/presumptive-calls/plan-def.json")).
            expectCall("GET", "https://fhir4-terminology-sandbox-internal.elimuinformatics.com/baseR4/Library?name=GonorrheaTxCDS&_format=json&_elements=name%2Cversion", 200, getClass().getResource("/presumptive-calls/gonorrhea-tx-library.json")).
            expectCall("GET", "https://fhir4-terminology-sandbox-internal.elimuinformatics.com/baseR4/Library/GonorrheaTxCDS/_history/60?_format=json&_elements=name%2Ctype%2Cversion%2Ccontent", 200, getClass().getResource("/presumptive-calls/gonorrhea-tx-library-direct.json")).
            expectCall("GET", "https://fhir4-terminology-sandbox-internal.elimuinformatics.com/baseR4/Library?name=FHIRHelpers&_format=json&_elements=name%2Cversion", 200, getClass().getResource("/presumptive-calls/fhir-helper-library.json")).
            expectCall("GET", "https://fhir4-internal.elimuinformatics.com/fhir/Patient/754321", 200, getClass().getResource("/presumptive-calls/patient.json")).
            expectCall("GET", "https://fhir4-internal.elimuinformatics.com/fhir/Medication/et6kIqz57048TZ-zI", 200, getClass().getResource("/presumptive-calls/medication-1.json")).
            expectCall("GET", "https://fhir4-internal.elimuinformatics.com/fhir/Medication/eGVdHiKbEpRO.9kB3xR", 200, getClass().getResource("/presumptive-calls/medication-2.json")).
            expectCall("GET", "https://fhir4-terminology-sandbox-internal.elimuinformatics.com/baseR4/ValueSet?url=http%3A%2F%2Fcts.nlm.nih.gov%2Ffhir%2FValueSet%2F2.16.840.1.113762.1.4.1196.4502", 200, getClass().getResource("/presumptive-calls/valueset-1.json")).
            expectCall("GET", "https://fhir4-terminology-sandbox-internal.elimuinformatics.com/baseR4/ValueSet/2.16.840.1.113762.1.4.1196.4502/$expand", 200, getClass().getResource("/presumptive-calls/valueset-2.json")).
            expectCall("GET", "https://fhir4-terminology-sandbox-internal.elimuinformatics.com/baseR4/ValueSet?url=http%3A%2F%2Fcts.nlm.nih.gov%2Ffhir%2FValueSet%2F2.16.840.1.113762.1.4.1196.4448", 200, getClass().getResource("/presumptive-calls/valueset-3.json")).
            expectCall("GET", "https://fhir4-terminology-sandbox-internal.elimuinformatics.com/baseR4/ValueSet/2.16.840.1.113762.1.4.1196.4448/$expand", 200, getClass().getResource("/presumptive-calls/valueset-4.json")).
            expectCall("GET", "https://fhir4-internal.elimuinformatics.com/fhir/AllergyIntolerance?patient=Patient%2F754321", 200, getClass().getResource("/presumptive-calls/allergy-intollerances.json")).
            expectCall("GET", "https://fhir4-terminology-sandbox-internal.elimuinformatics.com/baseR4/ValueSet?url=http%3A%2F%2Fcts.nlm.nih.gov%2Ffhir%2FValueSet%2F2.16.840.1.113762.1.4.1213.1500", 200, getClass().getResource("/presumptive-calls/valueset-5.json")).
            expectCall("GET", "https://fhir4-terminology-sandbox-internal.elimuinformatics.com/baseR4/ValueSet/2.16.840.1.113762.1.4.1213.1500/$expand", 200, getClass().getResource("/presumptive-calls/valueset-6.json")).
            expectCall("GET", "https://fhir4-terminology-sandbox-internal.elimuinformatics.com/baseR4/ValueSet?url=http%3A%2F%2Fcts.nlm.nih.gov%2Ffhir%2FValueSet%2F2.16.840.1.113762.1.4.1196.1352", 200, getClass().getResource("/presumptive-calls/valueset-7.json")).
            expectCall("GET", "https://fhir4-terminology-sandbox-internal.elimuinformatics.com/baseR4/ValueSet/2.16.840.1.113762.1.4.1196.1352/$expand", 200, getClass().getResource("/presumptive-calls/valueset-8.json")).
            expectCall("GET", "https://fhir4-internal.elimuinformatics.com/fhir/Observation?code=http%3A%2F%2Floinc.org%7C18833-4%2Chttp%3A%2F%2Floinc.org%7C29463-7%2Chttp%3A%2F%2Floinc.org%7C3141-9%2Chttp%3A%2F%2Floinc.org%7C3142-7%2Chttp%3A%2F%2Floinc.org%7C75292-3%2Chttp%3A%2F%2Floinc.org%7C79348-9%2Chttp%3A%2F%2Floinc.org%7C8335-2%2Chttp%3A%2F%2Floinc.org%7C8340-2%2Chttp%3A%2F%2Floinc.org%7C8341-0%2Chttp%3A%2F%2Floinc.org%7C8350-1%2Chttp%3A%2F%2Floinc.org%7C8351-9&subject=Patient%2F754321", 200, getClass().getResource("/presumptive-calls/observations.json")).
            expectCall("GET", "https://fhir4-internal.elimuinformatics.com/fhir/Condition?subject=Patient%2F754321", 200, getClass().getResource("/presumptive-calls/conditions.json")).
            expectCall("GET", "https://fhir4-terminology-sandbox-internal.elimuinformatics.com/baseR4/ValueSet?url=http%3A%2F%2Fcts.nlm.nih.gov%2Ffhir%2FValueSet%2F2.16.840.1.113883.3.464.1003.120.12.1003", 200, getClass().getResource("/presumptive-calls/valueset-9.json")).
            expectCall("GET", "https://fhir4-terminology-sandbox-internal.elimuinformatics.com/baseR4/ValueSet/2.16.840.1.113883.3.464.1003.120.12.1003/$expand", 200, getClass().getResource("/presumptive-calls/valueset-10.json")).
            expectCall("GET", "https://fhir4-internal.elimuinformatics.com/fhir/MedicationRequest?subject=Patient%2F754321", 200, getClass().getResource("/presumptive-calls/med-requests.json")).
            expectCall("GET", "https://fhir4-terminology-sandbox-internal.elimuinformatics.com/baseR4/ValueSet?url=http%3A%2F%2Fcts.nlm.nih.gov%2Ffhir%2FValueSet%2F2.16.840.1.113762.1.4.1196.4469", 200, getClass().getResource("/presumptive-calls/valueset-11.json")).
            expectCall("GET", "https://fhir4-terminology-sandbox-internal.elimuinformatics.com/baseR4/ValueSet/2.16.840.1.113762.1.4.1196.4469/$expand", 200, getClass().getResource("/presumptive-calls/valueset-12.json")).
            expectCall("GET", "https://fhir4-terminology-sandbox-internal.elimuinformatics.com/baseR4/ValueSet?url=http%3A%2F%2Fcts.nlm.nih.gov%2Ffhir%2FValueSet%2F2.16.840.1.113762.1.4.1196.4474", 200, getClass().getResource("/presumptive-calls/valueset-13.json")).
            expectCall("GET", "https://fhir4-terminology-sandbox-internal.elimuinformatics.com/baseR4/ValueSet/2.16.840.1.113762.1.4.1196.4474/$expand", 200, getClass().getResource("/presumptive-calls/valueset-14.json")).
            expectCall("GET", "https://fhir4-internal.elimuinformatics.com/fhir/Observation?code=http%3A%2F%2Floinc.org%7C14092-1%2Chttp%3A%2F%2Floinc.org%7C16975-5%2Chttp%3A%2F%2Floinc.org%7C18396-2%2Chttp%3A%2F%2Floinc.org%7C24012-7%2Chttp%3A%2F%2Floinc.org%7C29893-5%2Chttp%3A%2F%2Floinc.org%7C30361-0%2Chttp%3A%2F%2Floinc.org%7C31201-7%2Chttp%3A%2F%2Floinc.org%7C33660-2%2Chttp%3A%2F%2Floinc.org%7C33806-1%2Chttp%3A%2F%2Floinc.org%7C33807-9%2Chttp%3A%2F%2Floinc.org%7C40732-0%2Chttp%3A%2F%2Floinc.org%7C40733-8%2Chttp%3A%2F%2Floinc.org%7C43009-0%2Chttp%3A%2F%2Floinc.org%7C43011-6%2Chttp%3A%2F%2Floinc.org%7C44873-8%2Chttp%3A%2F%2Floinc.org%7C5221-7%2Chttp%3A%2F%2Floinc.org%7C5222-5%2Chttp%3A%2F%2Floinc.org%7C5225-8%2Chttp%3A%2F%2Floinc.org%7C56888-1%2Chttp%3A%2F%2Floinc.org%7C7917-8%2Chttp%3A%2F%2Floinc.org%7C7918-6%2Chttp%3A%2F%2Floinc.org%7C7919-4%2Chttp%3A%2F%2Floinc.org%7C85686-4%2Chttp%3A%2F%2Floinc.org%7C86233-4%2Chttp%3A%2F%2Floinc.org%7C9821-0&subject=Patient%2F754321", 200, getClass().getResource("/presumptive-calls/observations-3.json")).
            expectCall("GET", "https://fhir4-terminology-sandbox-internal.elimuinformatics.com/baseR4/ActivityDefinition?url=http%3A%2F%2Felimu.io%2FActivityDefinition%2FCeftriaxone500NoLidoOrderProposal", 200, getClass().getResource("/presumptive-calls/act-def-500.json")).
            expectCall("GET", "https://fhir4-terminology-sandbox-internal.elimuinformatics.com/baseR4/ActivityDefinition/Ceftriaxone500NoLidoOrderProposal/_history/5", 200, getClass().getResource("/presumptive-calls/act-def-500-direct.json")).
            expectCall("GET", "https://fhir4-terminology-sandbox-internal.elimuinformatics.com/baseR4/Library?name=CDSHooksSupport&_format=json&_elements=name%2Cversion", 200, getClass().getResource("/presumptive-calls/cds-hooks-support-library.json")).
            expectCall("GET", "https://fhir4-terminology-sandbox-internal.elimuinformatics.com/baseR4/Library/CDSHooksSupport/_history/1?_format=json&_elements=name%2Ctype%2Cversion%2Ccontent", 200, getClass().getResource("/presumptive-calls/cds-hooks-support-library-direct.json")).
            expectCall("GET", "https://fhir-terminology-sandbox-internal.elimuinformatics.com/baseR4/ActivityDefinition?url=http%3A%2F%2Felimu.io%2FActivityDefinition%2FGentamicin240OrderProposal", 200, getClass().getResource("/presumptive-calls/act-def-2400.json")).
            expectCall("GET", "https://fhir-terminology-sandbox-internal.elimuinformatics.com/baseR4/ActivityDefinition/Gentamicin240OrderProposal/_history/13", 200, getClass().getResource("/presumptive-calls/act-def-2400-direct.json")).
            expectCall("GET", "https://fhir4-terminology-sandbox-internal.elimuinformatics.com/baseR4/ActivityDefinition?url=http%3A%2F%2Felimu.io%2FActivityDefinition%2FGentamicin240OrderProposal", 200, getClass().getResource("/presumptive-calls/actdef-3.json")).
            expectCall("GET", "https://fhir4-terminology-sandbox-internal.elimuinformatics.com/baseR4/ActivityDefinition/Gentamicin240OrderProposal/_history/13", 200, getClass().getResource("/presumptive-calls/actdef-3-direct.json")).
            expectCall("GET", "https://fhir4-terminology-sandbox-internal.elimuinformatics.com/baseR4/ActivityDefinition?url=http%3A%2F%2Felimu.io%2FActivityDefinition%2FAzithromycin2GmOrderProposal", 200, getClass().getResource("/presumptive-calls/actdef-4.json")).
            expectCall("GET", "https://fhir4-terminology-sandbox-internal.elimuinformatics.com/baseR4/ActivityDefinition/Azithromycin2GmOrderProposal/_history/3", 200, getClass().getResource("/presumptive-calls/actdef-4-direct.json")).
            expectCall("GET", "https://fhir4-terminology-sandbox-internal.elimuinformatics.com/baseR4/ActivityDefinition?url=http%3A%2F%2Felimu.io%2FActivityDefinition%2FHIVTestOrderProposal", 200, getClass().getResource("/presumptive-calls/hiv-test-activity-def.json")).
            expectCall("GET", "https://fhir4-terminology-sandbox-internal.elimuinformatics.com/baseR4/ActivityDefinition/HIVTestOrderProposal/_history/10", 200, getClass().getResource("/presumptive-calls/hiv-test-activity-def-direct.json")).
    		build();

        AuthHelper.setTestClient(client);
        CachingHttpClient.setTestClient(client);
        String jsonBody = IOUtils.toString(getClass().getResourceAsStream("/gc-presumptive-request-sample.json"), StandardCharsets.UTF_8);
        Response response = given()
            .body(jsonBody)
            .header("Content-type", "application/json")
            .when().post("/presumptive_gc_treatment")
            .then()
            .statusCode(201)
            .extract()
            .response();
        List<?> cards = response.jsonPath().getList("cards");
        assertNotNull(cards);
        assertEquals(1, cards.size());
    }

    String transformData(String file, String text, String replaceWith) throws IOException {
        String content = new String(getClass().getResourceAsStream(file).readAllBytes());
        String retval = content.replaceAll(text, replaceWith);
        org.slf4j.LoggerFactory.getLogger(ConfirmedGcTest.class).info("TRAANSFORMED DATA = " + retval);
        return retval;
    }

}
