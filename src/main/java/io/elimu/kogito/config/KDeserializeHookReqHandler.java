package io.elimu.kogito.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.kie.kogito.internal.process.runtime.KogitoWorkItem;
import org.kie.kogito.internal.process.runtime.KogitoWorkItemHandler;
import org.kie.kogito.internal.process.runtime.KogitoWorkItemManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import ca.uhn.fhir.context.FhirContext;
import io.elimu.kogito.exception.WorkItemHandlerException;
import io.elimu.kogito.model.CDSHookRequest;
import io.elimu.kogito.model.FHIRAuthorization;

public class KDeserializeHookReqHandler implements KogitoWorkItemHandler {

    private static final Logger log = LoggerFactory.getLogger(KDeserializeHookReqHandler.class);

    @Override
    @SuppressWarnings("unchecked")
    public void executeWorkItem(KogitoWorkItem workItem, KogitoWorkItemManager manager) {
      try {
         Map<String, Object> workItemResult = workItem.getResults();
         String hook = (String) workItem.getParameter("hook");
         String hookInstance = (String) workItem.getParameter("hookInstance");
         String fhirServer = (String) workItem.getParameter("fhirServer");
         Map<String, Object> fhirAuthorization = (Map<String, Object>) workItem.getParameter("fhirAuthorization");
         Map<String, Object> context = (Map<String, Object>) workItem.getParameter("context");
         Map<String, Object> prefetch = (Map<String, Object>) workItem.getParameter("prefetch");
         Map<String, Object> cdshook = new HashMap<>();
         cdshook.put("hook", hook);
         cdshook.put("hookInstance", hookInstance);
         cdshook.put("fhirServer", fhirServer);
         cdshook.put("fhirAuthorization", fhirAuthorization);
         cdshook.put("context", context);
         cdshook.put("prefetch", prefetch);
         String requestJson = new Gson().toJson(cdshook);
         CDSHookRequest cdsHookRequest = new Gson().fromJson(requestJson, CDSHookRequest.class);
         FHIRAuthorization fhirAuth = cdsHookRequest.getFhirAuthorization();
         context = cdsHookRequest.getContext();
         prefetch = cdsHookRequest.getPrefetch();
         if (hook == null) {
            log.error("Missing parameter hook");
            throw new WorkItemHandlerException("Missing parameter hook");
         }
         if (hookInstance == null) {
            log.error("Missing parameter hookInstance");
            throw new WorkItemHandlerException("Missing parameter hookInstance");
         }
         setWorkItemParams(fhirServer, workItemResult, "fhirServer");
         setWorkItemParams(fhirAuth, workItemResult, "fhirAuthorization");
            if (context == null) {
            log.error("Missing parameter context");
            throw new WorkItemHandlerException("Missing parameter context");
         } else {
            setWorkItemResource(context, workItemResult, "context");
         }
         if (prefetch != null && !prefetch.isEmpty()) { // OPTIONAL PARAM
            setWorkItemResource(prefetch, workItemResult, "prefetch");
         } else {
            log.warn("Missing or empty parameter prefetch");
         }
         workItemResult.put("cdsHookRequest", cdsHookRequest);
         workItemResult.put("hook", hook);
         workItemResult.put("hookInstance", hookInstance);
         manager.completeWorkItem(workItem.getStringId(), workItemResult);
      } catch (IllegalArgumentException ex) {
         log.error("Missing or empty parameter prefetch " + ex.getMessage(), ex);
         throw new WorkItemHandlerException("Missing or empty parameter");
      } catch (UnsupportedOperationException | ClassCastException | NullPointerException ex) {
         log.error("Error in deserializing json string", ex);
         throw new WorkItemHandlerException("Error in deserializing json string");
      } catch (WorkItemHandlerException ex) {
         // rethrow exception without any change or additional logging
         throw ex;
      } catch (Exception ex) {
         log.error("Unknown Error in KDeserializeCDSHooksReqHandler", ex);
         throw new WorkItemHandlerException(ex);
      }
      log.trace("KDeserializeCDSHooksReqHandler execution completed");
   }
   
   private void setWorkItemParams(Object item, Map<String, Object> workItemResult, String param) {
      if (item != null) {
         workItemResult.put(param, item);
      } else {
         log.warn("Missing parameter {}", param);
      }
   }

   private void setWorkItemResource(Map<String, Object> item, Map<String, Object> workItemResult, String param) {
      for (Entry<String, Object> entry : item.entrySet()) {
         workItemResult.put(param.concat("_").concat(entry.getKey()), entry.getValue());
         Object resource = parseResource(entry.getValue());
         if (resource != null) {
            workItemResult.put(param.concat("Resource_").concat(entry.getKey()), resource);
            log.debug("Added  the {} Resource ::{}", param, entry.getKey());
         }
      }
   }

   private Object parseResource(Object jsonObject) {
      try {
         JSONObject parsedJsonObj = null;
         if(jsonObject instanceof Map) {
            parsedJsonObj = (JSONObject) new JSONParser().
                  parse(JSONObject.toJSONString((Map<?,?>)jsonObject));
         }
         if((parsedJsonObj!=null) && parsedJsonObj.get("resourceType") != null) {
            String resourceData =  parsedJsonObj.toJSONString();
            resourceData = resourceData.replaceAll("(\\:\\d+)\\.0(\\}|\\,|\\]|\\s)","$1 $2");
            return FhirContext.forR4Cached().newJsonParser().parseResource(resourceData);
         }
      }
      catch(Exception ex) { // If the jsonObject is not a json String like plain String
         return null;
      }
      return null;
   }

    @Override
    public void abortWorkItem(KogitoWorkItem workItem, KogitoWorkItemManager manager) {
        // do nothing
    }

}
