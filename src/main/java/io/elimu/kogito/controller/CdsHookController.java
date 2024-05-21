package io.elimu.kogito.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.kie.kogito.Model;
import org.kie.kogito.process.Process;
import org.kie.kogito.process.ProcessInstance;
import org.kie.kogito.process.ProcessService;
import org.kie.kogito.process.Processes;

import com.google.gson.Gson;

import ca.uhn.fhir.context.FhirContext;
import io.elimu.kogito.model.CDSServiceData;
import lombok.extern.slf4j.Slf4j;

@Path(CdsHookController.BASE_URL)
@Slf4j
public class CdsHookController {

  public static final String X_CORRELATION_ID = "x-correlation-id";
  public static final String BASE_URL = "/cds-hooks";

  @Inject
  Processes processes;
  @Inject
  ProcessService psvc;
  
  @GET
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getProcessData(@Context UriInfo uriInfo, @Context HttpHeaders headers) {
	  log.info("Receiving request for discovery API");
	  Config configRaw = ConfigProvider.getConfig();
	  List<String> propNames = new LinkedList<>();
	  configRaw.getPropertyNames().forEach(propNames::add);
	  Map<String, List<CDSServiceData>> services = new HashMap<>();
	  services.put("services", new ArrayList<>());
	  for (String processId : processes.processIds()) {
		  log.info("Adding discovery data for process {}", processId);
		  CDSServiceData data = new CDSServiceData();
		  data.setId(configRaw.getConfigValue("discovery." + processId + ".id").getValue());
		  data.setHook(configRaw.getConfigValue("discovery." + processId + ".hook").getValue());
		  data.setDescription(configRaw.getConfigValue("discovery." + processId + ".description").getValue());
		  data.setTitle(configRaw.getConfigValue("discovery." + processId + ".title").getValue());
		  List<String> prefetches = propNames.stream().filter(s -> s.startsWith("discovery." + processId + ".prefetch.")).toList();
		  for (String prefetch : prefetches) {
			  String key = prefetch.replace("discovery."+processId + ".prefetch.", "");
			  data.getPrefetch().put(key, configRaw.getConfigValue(prefetch).getValue());
		  }
		  services.get("services").add(data);
	  }
	  return Response.status(200).entity(services).build();
  }
  
  @POST
  @Path("/{processId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response executeProcess(Map<String, Object> payload,
		  @PathParam("processId") String processId, 
		  @Context UriInfo uriInfo,
          @Context HttpHeaders headers) {
    try {
    	log.info("Receiving request for process {}", processId);
    	ProcessInstance<? extends Model> pi = createProcess(processes.processById(processId), payload, headers);
    	Map<String, Object> variables = pi.variables().toMap();
    	if (variables.containsKey("cards")) {
    		Map<String, Object> retval = Map.of("cards", variables.get("cards"));
    		String jsonRetval = new Gson().toJson(retval);
    		return Response.status(200).type(MediaType.APPLICATION_JSON).entity(jsonRetval).build();
    	} else {
    		String jsonRetval = createOperationOutcome(variables.get("issue"));
    		return Response.status(200).type(MediaType.APPLICATION_JSON).entity(jsonRetval).build();
    	}
    }catch (Exception t){
      t.printStackTrace();
      List<Map<String, Object>> issue = List.of(Map.of("diagnostics", t.getMessage(), 
    		  "code", OperationOutcome.IssueType.EXCEPTION.toCode(),
    		  "severity", OperationOutcome.IssueSeverity.ERROR.toCode()));
      String json = createOperationOutcome(issue);
	  return Response.status(400).type(MediaType.APPLICATION_JSON).entity(json).build();
    }
  }

  @SuppressWarnings("unchecked")
  private String createOperationOutcome(Object objIssue) {
	  List<Map<String, Object>> issue = (List<Map<String, Object>>) objIssue;
	  OperationOutcome outcome = new OperationOutcome();
	  for (Map<String, Object> i : issue) {
		  String code = (String) i.get("issue");
		  String severity = (String) i.get("severity");
		  String diagnostics = (String) i.get("diagnostics");
		  outcome.addIssue().setCode(OperationOutcome.IssueType.fromCode(code)).
	  		setSeverity(OperationOutcome.IssueSeverity.fromCode(severity)).
	  		setDiagnostics(diagnostics);
	  }
	  return FhirContext.forR4Cached().newJsonParser().encodeResourceToString(outcome);
  }

  private <T extends Model> ProcessInstance<T> createProcess(Process<T> p, Map<String, Object> payload, HttpHeaders headers) {
	  T model = p.createModel();
	  model.update(payload);
	  return psvc.createProcessInstance(p, null, model, headers.getRequestHeaders(), null);
  }
}
