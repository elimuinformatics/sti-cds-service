package io.elimu.kogito.cql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.CarePlan.CarePlanActivityComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.RequestGroup;
import org.hl7.fhir.r4.model.RequestGroup.ActionPrecheckBehavior;
import org.hl7.fhir.r4.model.RequestGroup.RequestGroupActionComponent;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Type;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;
import io.elimu.kogito.model.Card;
import io.elimu.kogito.model.LinkCard;
import io.elimu.kogito.model.Suggestion;

public class CardCreator {

	private static final Logger LOG = LoggerFactory.getLogger(CardCreator.class);
	
	public static List<Card> convert(CarePlan carePlan) throws ParseException, ReflectiveOperationException {
        List<Card> cards = new ArrayList<>();
        for (CarePlanActivityComponent activity : carePlan.getActivity()) {
        	Resource refTarget = activity.getReferenceTarget();
            if (refTarget != null && refTarget instanceof RequestGroup) {
                cards = _convert((RequestGroup) refTarget);
            }
        }

        return cards;
    }

    public static List<Card> _convert(RequestGroup requestGroup) throws ParseException, ReflectiveOperationException {
    	List<Card> cards = new ArrayList<>();
    	// links
    	List<LinkCard> links = new ArrayList<>();
        if (requestGroup.hasExtension()) {
            for (Extension extension : requestGroup.getExtension()) {
            	LinkCard link = new LinkCard();
            	Type extValue = extension.getValue();
                if (extValue instanceof Attachment) {
                	Attachment attExtValue = (Attachment) extValue;
                	if (attExtValue.hasUrl()) {
                    	link.setUrl(attExtValue.getUrl());
                	}
                	if (attExtValue.hasTitle()) {
                		link.setTitle(attExtValue.getTitle());
                	}
                	if (extValue.hasExtension()) {
                    	link.setType(extValue.getExtensionFirstRep().getValue().primitiveValue());
                    } 
                } else {
                	throw new RuntimeException("Invalid link extension type: " + extValue.getClass().getMethod("fhirType").invoke(extValue));
                }
                links.add(link);
            }
        }

        if (requestGroup.hasAction()) {
            for (RequestGroupActionComponent action : requestGroup.getAction()) {
            	List<Suggestion> suggestions = new ArrayList<>();
            	boolean isValidCard = false;
            	//Class<?> r4actionClass = action.getClass();
            	IParser parser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser().setPrettyPrint(true);
            	Card card = new Card();
                // basic
                if (action.hasTitle()) {
                	isValidCard = true;
                	card.setSummary(action.getTitle());
                }
                if (action.hasDescription()) {
                	isValidCard = true;
                	card.setDetail(action.getDescription());
                }
                if (action.hasExtension()) {
                	isValidCard = true;
                	for (Extension ext : action.getExtension()) {
	                	Type extValue = ext.getValue();
	                	if (ext.getUrl().toString().endsWith(".override")) {
	                		if (extValue != null) {
	                			List<JSONObject> overrideReasons = new ArrayList<>();
	                			if (extValue instanceof CodeableConcept) {
	                				for (Coding coding : ((CodeableConcept) extValue).getCoding()) {
	                					Map<String, Object> json = new HashMap<>();
	                					json.put("code", coding.getCode());
	                					json.put("system", coding.getSystem());
	                					json.put("display", coding.getDisplay());
	                					overrideReasons.add(new JSONObject(json));
	                				}
	                			} else if (extValue instanceof Coding) {
	            					Map<String, Object> json = new HashMap<>();
	            					Coding coding = (Coding) extValue;
	                				json.put("code", coding.getCode());
	                				json.put("system", coding.getSystem());
	                				json.put("display", coding.getDisplay());
	            					overrideReasons.add(new JSONObject(json));
	            					//TODO we need to add here something fro the case it comes as a String, or as a list of strings?
	                			} else {
	                				LOG.warn("Cannot parse extension of type " + extValue.getClass().getName() + " as overrideReasons element");
	                			}
	                			card.setOverrideReasons(overrideReasons);
	                		}
	                	} else {
		                	card.setIndicator(String.valueOf(extValue));
	                	}
                	}
                }
                // source
                Map<String, Object> source = new HashMap<>();
                if (action.hasDocumentation()) {
                	//isValidCard = true;
                    // Assuming first related artifact has everything
                	RelatedArtifact documentation = action.getDocumentationFirstRep();
                    if (documentation.hasDisplay()) {
                    	source.put("label", documentation.getDisplay());
                    } else if (documentation.hasLabel()) {
                    	source.put("label", documentation.getLabel());
                    }
                    if (documentation.hasUrl()) {
                    	source.put("url", documentation.getUrl());
                    }
                    if (documentation.hasDocument()) {
                    	Attachment doc = documentation.getDocument();
                    	if (doc.hasUrl()) {
                    		source.put("icon", doc.getUrl());
                    	}
                    }
                }
                card.setSource(new JSONObject(source));
                if (action.hasSelectionBehavior()) {
                	card.setSelectionBehavior(action.getSelectionBehavior().toCode());
                }
                if (action.hasAction()) {
                	for (RequestGroupActionComponent act2 : action.getAction()) {
                		Suggestion sug = new Suggestion();
                		sug.setLabel(act2.getPrefix());
                		Map<String, Object> actionsRet = new HashMap<>();
                		sug.setUuid(UUID.randomUUID().toString());
                        if (act2.hasId()) {
                        	actionsRet.put("resourceId", act2.getId());
                        }
                        if (act2.hasTextEquivalent()) {
                        	actionsRet.put("description", act2.getTextEquivalent());
                        } else {
                        	actionsRet.put("description", act2.getDescription());
                        }
                        if (act2.hasType()) {
                        	Object code = act2.getType().getCodingFirstRep().getCode();
                        	if("fire-event".equals(code)) {
                        		actionsRet.put("type", "create");
                        	} else {
                        		actionsRet.put("type", "remove".equals(code) ? "delete" : code);
                        	}
                        }
                        if (act2.hasResource()) {
                        	IBaseResource resourceTarget = act2.getResource().getResource();
                        	if (resourceTarget != null) {
    	                    	String json = (String) parser.encodeResourceToString(resourceTarget);
    	                    	JSONObject obj = (JSONObject) new JSONParser().parse(json);
    	                    	actionsRet.put("resource", obj);
                        	}
                        }
                        sug.setActions(Arrays.asList(new JSONObject(actionsRet)));
                        ActionPrecheckBehavior precheck = act2.getPrecheckBehavior();
                        if (precheck != null) {
                        	sug.setIsRecommended("yes".equalsIgnoreCase(precheck.toCode()));
                        } else {
                        	sug.setIsRecommended(false);
                        }
                        suggestions.add(sug);
                	}
                }
                card.setSuggestions(suggestions);
                if (!links.isEmpty()) {
                	card.setLinks(links);
                }
                if (isValidCard) {
                	card.setUuid(UUID.randomUUID().toString());
                	cards.add(card);
                }
            }
        }
        return cards;
    }
}
