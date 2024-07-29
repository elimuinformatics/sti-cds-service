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

package io.elimu.kogito.cql;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.CarePlan.CarePlanStatus;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DataRequirement;
import org.hl7.fhir.r4.model.DataRequirement.DataRequirementCodeFilterComponent;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Goal;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ParameterDefinition;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionConditionComponent;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionDynamicValueComponent;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionRelatedActionComponent;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionGoalComponent;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RequestGroup;
import org.hl7.fhir.r4.model.RequestGroup.RequestGroupActionComponent;
import org.hl7.fhir.r4.model.RequestGroup.RequestIntent;
import org.hl7.fhir.r4.model.RequestGroup.RequestStatus;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.UriType;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.evaluator.activitydefinition.r4.ActivityDefinitionProcessor;
import org.opencds.cqf.cql.evaluator.expression.ExpressionEvaluator;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;
import org.opencds.cqf.cql.evaluator.fhir.helper.ContainedHelper;
import org.opencds.cqf.cql.evaluator.fhir.util.FhirPathCache;
import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.IFhirPath;

public class DecoratedPlanDefinitionProcessor {

	private static final Logger logger = LoggerFactory.getLogger(DecoratedPlanDefinitionProcessor.class);
	private static final String alternateExpressionExtension = "http://hl7.org/fhir/us/ecr/StructureDefinition/us-ph-alternativeExpression";

	protected ActivityDefinitionProcessor activityDefinitionProcessor;
	protected LibraryProcessor libraryProcessor;
	protected ExpressionEvaluator expressionEvaluator;
	protected OpenOperationParametersParser operationParametersParser;
	protected FhirContext fhirContext;
	protected FhirDal fhirDal;
	protected IFhirPath fhirPath;

	private ThreadLocal<Map<String, Object>> libraryResults = new ThreadLocal<>() {
		protected Map<String,Object> initialValue() {
			return new HashMap<>();
		}
	};
  
	private ThreadLocal<Map<String, Object>> evaluatedCqlResults = new ThreadLocal<>() {
		protected Map<String, Object> initialValue() {
			return new HashMap<>();
		}
	};
	
	public DecoratedPlanDefinitionProcessor(FhirContext fhirContext, FhirDal fhirDal, LibraryProcessor libraryProcessor, 
			ExpressionEvaluator expressionEvaluator, ActivityDefinitionProcessor activityDefinitionProcessor, 
			OpenOperationParametersParser operationParametersParser) {
		requireNonNull(fhirContext, "fhirContext can not be null");
		requireNonNull(fhirDal, "fhirDal can not be null");
		requireNonNull(libraryProcessor, "LibraryProcessor can not be null");
		requireNonNull(operationParametersParser, "OperationParametersParser can not be null");
		this.fhirContext = fhirContext;
		this.fhirPath = FhirPathCache.cachedForContext(fhirContext);
		this.fhirDal = fhirDal;
		this.libraryProcessor = libraryProcessor;
		this.expressionEvaluator = expressionEvaluator;
		this.activityDefinitionProcessor = activityDefinitionProcessor;
		this.operationParametersParser = operationParametersParser;
	}

	public static <T> Optional<T> castOrThrow(Object obj, Class<T> type, String errorMessage) {
		if (obj == null) return Optional.empty();
		if (type.isInstance(obj)) {
			return Optional.of(type.cast(obj));
		}
		throw new IllegalArgumentException(errorMessage);
	}
	
	public CarePlan apply(PlanDefinition paramPlanDefinition, IdType theId, String patientId, String encounterId, String practitionerId,
			String organizationId, String userType, String userLanguage, String userTaskContext, String setting,
			String settingContext, Boolean mergeNestedCarePlans, IBaseParameters parameters, Boolean useServerData,
			IBaseBundle bundle, IBaseParameters prefetchData, IBaseResource dataEndpoint, IBaseResource contentEndpoint,
			IBaseResource terminologyEndpoint) {

		// warn if prefetchData exists
		// if no data anywhere blow up
		evaluatedCqlResults.get().clear();
		libraryResults.get().clear();
		requireNonNull(paramPlanDefinition, "Couldn't find PlanDefinition " + theId);
		PlanDefinition planDefinition = castOrThrow(paramPlanDefinition, PlanDefinition.class,
				"The planDefinition passed to FhirDal was not a valid instance of PlanDefinition.class").get();
		logger.info("Performing $apply operation on PlanDefinition/" + theId);
		CarePlan builder = new CarePlan();
		builder.addInstantiatesCanonical(planDefinition.getIdElement().getIdPart()).setSubject(new Reference(patientId)).setStatus(CarePlanStatus.DRAFT);
		for (PlanDefinitionGoalComponent goal : planDefinition.getGoal()) {
			builder.addGoal(new Reference(convertGoal(goal)));
		}
		if (encounterId != null) {
			builder.setEncounter(new Reference(encounterId));
		}
		if (practitionerId != null) {
			builder.setAuthor(new Reference(practitionerId));
		}
		if (organizationId != null) {
			builder.setAuthor(new Reference(organizationId));
		}
		if (userLanguage != null) {
			builder.setLanguage(userLanguage);
		}
		// Each Group of actions shares a RequestGroup
	    RequestGroup requestGroup = new RequestGroup().setStatus(RequestStatus.DRAFT).setIntent(RequestIntent.PROPOSAL);
	    IBaseDatatype valueChildKey = operationParametersParser.getValueChild(prefetchData, "key");
	    Optional<StringType> valueChildKeyOpt = castOrThrow(valueChildKey, StringType.class, "prefetchData key must be a String");
	    String prefetchDataKey = !valueChildKeyOpt.isPresent() ? null : (String) valueChildKeyOpt.get().asStringValue();
	    IBaseResource prefDataDesc = operationParametersParser.getResourceChild(prefetchData, "descriptor");
	    DataRequirement prefetchDataDescription = castOrThrow(prefDataDesc, DataRequirement.class, "prefetchData descriptor must be a DataRequirement").orElse(null);
	    IBaseResource prefDataData = operationParametersParser.getResourceChild(prefetchData, "data");
	    IBaseBundle prefetchDataData = castOrThrow(prefDataData, IBaseBundle.class, "prefetchData data must be a Bundle").orElse(null);
	    Session session = new Session(planDefinition, builder, patientId, encounterId, practitionerId, organizationId,
	    		userType, userLanguage, userTaskContext, setting, settingContext, requestGroup, parameters, prefetchData,
	    		contentEndpoint, terminologyEndpoint, dataEndpoint, bundle, useServerData, mergeNestedCarePlans,
	    		prefetchDataData, prefetchDataDescription, prefetchDataKey);
	    return (CarePlan) ContainedHelper.liftContainedResourcesToParent(resolveActions(session));
	}

	public Map<String, Object> getEvaluatedCqlResults() {
		return new HashMap<>(evaluatedCqlResults.get());
	}
	
	private Goal convertGoal(PlanDefinitionGoalComponent goal) {
		Goal myGoal = new Goal();
		myGoal.setCategory(Collections.singletonList(goal.getCategory()));
		myGoal.setDescription(goal.getDescription());
		myGoal.setPriority(goal.getPriority());
		myGoal.setStart(goal.getStart());
		myGoal.setTarget(goal.getTarget().stream().map((target) -> {
		      Goal.GoalTargetComponent myTarget = new Goal.GoalTargetComponent();
		      myTarget.setDetail(target.getDetail());
		      myTarget.setMeasure(target.getMeasure());
		      myTarget.setDue(target.getDue());
		      myTarget.setExtension(target.getExtension());
		      return myTarget;
		    } ).collect(Collectors.toList()));
		return myGoal;
	}

	private CarePlan resolveActions(Session session) {
	    Map<String, PlanDefinition.PlanDefinitionActionComponent> metConditions = new HashMap<String, PlanDefinition.PlanDefinitionActionComponent>();
		int index = 0;
	    for (PlanDefinition.PlanDefinitionActionComponent action : session.planDefinition.getAction()) {
			resolveAction(session, metConditions, action, index, false, -1);
			index++;
	    }
	    RequestGroup result = session.requestGroup;
		//result.setAction(toRequestGroupActions(session.planDefinition.getAction()));
	    session.carePlan.addActivity().setReference(new Reference(result));
	    session.carePlan.addContained(result);
	    return session.carePlan;
	}

	private void resolveAction(Session session, Map<String, PlanDefinition.PlanDefinitionActionComponent> metConditions, PlanDefinition.PlanDefinitionActionComponent action, int index, boolean isSubItem, int subIndex) {
		if (meetsConditions(session, action, index)) {
			String id = action.getId();
			if (action.hasRelatedAction()) {
				for (PlanDefinitionActionRelatedActionComponent relatedActionComponent : action.getRelatedAction()) {
					if (PlanDefinition.ActionRelationshipType.AFTER.equals(relatedActionComponent.getRelationship())) {
						String actionId = relatedActionComponent.getActionId();
						if (metConditions.containsKey(actionId)) {
							metConditions.put(id, action);
							resolveDynamicActions(session, action, index, isSubItem, subIndex);
							resolveDefinition(session, action, index, subIndex);
						}
					}
				}
			}
			metConditions.put(id, action);
			resolveDefinition(session, action, index, subIndex);
			resolveDynamicActions(session, action, index, isSubItem, subIndex);
		}
	}

	private void resolveDefinition(Session session, PlanDefinition.PlanDefinitionActionComponent action, int index, int subIndex) {
		if (action.hasDefinitionCanonicalType()) {
			String defCanTypeValue = action.getDefinitionCanonicalType().getValue();
			logger.debug("Resolving definition " + defCanTypeValue);
			switch (getResourceName(action.getDefinitionCanonicalType())) {
			case "PlanDefinition":
				applyNestedPlanDefinition(session, action.getDefinitionCanonicalType(), action, index, subIndex);
				break;
			case "ActivityDefinition":
				applyActivityDefinition(session, action.getDefinitionCanonicalType(), action, index, subIndex);
				break;
			case "Questionnaire":
				applyQuestionnaireDefinition(session, action.getDefinitionCanonicalType(), action, index, subIndex);
				break;
			default:
				throw new RuntimeException(String.format("Unknown action definition: ", action.getDefinitionCanonicalType()));
			}
		} else if (action.hasDefinitionUriType()) {
			applyUriDefinition(session, action.getDefinitionUriType(), action, index, subIndex);
		}
	}

	private void applyUriDefinition(Session session, UriType definition, PlanDefinition.PlanDefinitionActionComponent action, int index, int subIndex) {
		List<RequestGroupActionComponent> acts = session.requestGroup.getAction();
		while (index >= acts.size()) {
			session.requestGroup.addAction();
			acts = session.requestGroup.getAction();
		}
		RequestGroupActionComponent act = acts.get(index);
		RequestGroupActionComponent rgAction = act.addAction();
		rgAction.setResource(new Reference(definition.asStringValue()));
		rgAction.setTitle(action.getTitle());
		rgAction.setDescription(action.getDescription());
		rgAction.setTextEquivalent(action.getTextEquivalent());
		rgAction.setCode(action.getCode());
		rgAction.setTiming(action.getTiming());
		setPrecheckBehavior(rgAction, action, subIndex);
	}
	
	private void setPrecheckBehavior(RequestGroupActionComponent rgTarget, PlanDefinition.PlanDefinitionActionComponent pdefSource, int subIndex) {
		if (subIndex >= 0) {
			List<PlanDefinition.PlanDefinitionActionComponent> subActionList = pdefSource.getAction();
			if (subActionList != null && subActionList.size() > subIndex) {
				pdefSource = subActionList.get(subIndex);
			}
		}
		PlanDefinition.ActionPrecheckBehavior pdefPrecheck = pdefSource.getPrecheckBehavior();
		if (pdefPrecheck != null) {
			String precheck = pdefPrecheck.toCode();
			rgTarget.setPrecheckBehavior(RequestGroup.ActionPrecheckBehavior.fromCode(precheck));
		}
	}

	private void applyQuestionnaireDefinition(Session session, CanonicalType definition, PlanDefinition.PlanDefinitionActionComponent action, int index, int subIndex) {
		Resource result; //IBaseResource
		try {
			if (definition.getValue().startsWith("#")) {
				result = (Resource) resolveContained(session.planDefinition, definition.getValue());
			} else {
				Iterable<IBaseResource> iter = fhirDal.searchByUrl("Questionnaire", definition.asStringValue());
				Iterator<IBaseResource> iterator = iter.iterator();
				if (!iterator.hasNext()) {
					throw new RuntimeException("No questionnaire found for definition: " + definition);
				}
				result = (Resource) iterator.next();
			}
			applyAction(session, result, action);
			List<RequestGroupActionComponent> acts = session.requestGroup.getAction();
			while (index >= acts.size()) {
				session.requestGroup.addAction();
				acts = session.requestGroup.getAction();
			}
			RequestGroupActionComponent act = acts.get(index);
			RequestGroupActionComponent rgAction = act.addAction();
			String prefix = ((Questionnaire) result).getTitle();
			String desc = ((Questionnaire) result).getDescription();
			if (prefix == null) {
				prefix = desc;
			}
			rgAction.setPrefix(prefix).setDescription(desc).setResource(new Reference((Resource) result));
			session.requestGroup.addContained((Resource) result);
			setPrecheckBehavior(rgAction, action, subIndex);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("ERROR: Questionnaire {} could not be applied and threw exception {}", definition, e.toString());
		}
	}
	
	private void applyActivityDefinition(Session session, CanonicalType definition, PlanDefinition.PlanDefinitionActionComponent action, int index, int subIndex) {
		Resource result; //IBaseResource
		try {
			ActivityDefinition activityDefinition = null;
			if (definition.getValue().startsWith("#")) {
				activityDefinition = (ActivityDefinition) resolveContained(session.planDefinition, definition.getValue());
				result = this.activityDefinitionProcessor.resolveActivityDefinition(activityDefinition, 
						session.patientId, session.practitionerId, session.organizationId, session.parameters, 
						session.contentEndpoint, session.terminologyEndpoint, session.dataEndpoint);
			} else {	
				Iterable<IBaseResource> iter = fhirDal.searchByUrl("ActivityDefinition", definition.asStringValue());
				Iterator<IBaseResource> iterator = iter.iterator();
				if (!iterator.hasNext()) {
					throw new RuntimeException("No activity definition found for definition: " + definition);
				}
				activityDefinition = (ActivityDefinition) iterator.next();
				result = (Resource) this.activityDefinitionProcessor.apply(activityDefinition.getIdElement(), 
						session.patientId, session.encounterId, session.practitionerId, session.organizationId, 
						session.userType, session.userLanguage, session.userTaskContext, session.setting, 
						session.settingContext, session.parameters, session.contentEndpoint, 
						session.terminologyEndpoint, session.dataEndpoint);
				if (activityDefinition.hasIntent() && hasMethod(result, "getIntent")) {
					try {
						Class<?> intentClass = result.getClass().getMethod("getIntent").getReturnType();
						String intentCode = activityDefinition.getIntent().toCode();
						if (intentCode != null) {
							Object tgtIntent = intentClass.getMethod("fromCode", String.class).invoke(null, intentCode);
							result.getClass().getMethod("setIntent", intentClass).invoke(result, tgtIntent);
						}
					} catch (Exception e) {
						logger.warn("Cannot override intent enum for result " + result + ": " + e.getMessage());
					}
				}
			}
			applyAction(session, result, action);
			List<RequestGroupActionComponent> acts = session.requestGroup.getAction();
			while (index >= acts.size()) {
				session.requestGroup.addAction();
				acts = session.requestGroup.getAction();
			}
			RequestGroupActionComponent act = acts.get(index);
			RequestGroupActionComponent rgAction = act.addAction();
			rgAction.setResource(new Reference(result));
			String prefix = activityDefinition.getTitle();
			String desc = activityDefinition.getDescription();
			if (prefix == null) {
				prefix = desc;
			}
			rgAction.setPrefix(prefix);
			rgAction.setTextEquivalent(desc);
			setPrecheckBehavior(rgAction, action, subIndex);
			rgAction.getType().addCoding().setCode("fire-event");
			session.requestGroup.addContained(result);
		} catch (Exception e) {
			logger.error("ERROR: ActivityDefinition {} could not be applied and threw exception {}", definition, e.toString(), e);
		}
	}

	private boolean hasMethod(Object object, String methodName) {
		for (Method m : object.getClass().getMethods()) {
			if (m.getName().equals(methodName)) {
				return true;
			}
		}
		return false;
	}

	private void applyNestedPlanDefinition(Session session, CanonicalType definition, PlanDefinition.PlanDefinitionActionComponent action, int index, int subIndex) {
		CarePlan carePlan; //CarePlan
		Iterable<IBaseResource> iter = fhirDal.searchByUrl("PlanDefinition", definition.asStringValue());
		Iterator<IBaseResource> iterator = iter.iterator();
		if (!iterator.hasNext()) {
			throw new RuntimeException("No plan definition found for definition: " + definition);
		}
		PlanDefinition planDefinition = (PlanDefinition) iterator.next();
		carePlan = apply(planDefinition, planDefinition.getIdElement(), session.patientId, session.encounterId,
				session.practitionerId, session.organizationId, session.userType, session.userLanguage, session.userTaskContext,
				session.setting, session.settingContext, session.mergeNestedCarePlans, session.parameters,
				session.useServerData, session.bundle, session.prefetchData, session.dataEndpoint, session.contentEndpoint,
				session.terminologyEndpoint);
		applyAction(session, carePlan, action);
		// Add an action to the request group which points to this CarePlan
		List<RequestGroupActionComponent> acts = session.requestGroup.getAction();
		while (index >= acts.size()) {
			session.requestGroup.addAction();
			acts = session.requestGroup.getAction();
		}
		RequestGroupActionComponent act = acts.get(index);
		RequestGroupActionComponent rgAction = act.addAction();
		rgAction.setResource(new Reference(carePlan));
		session.requestGroup.addContained(carePlan);
		for (CanonicalType c : carePlan.getInstantiatesCanonical()) {
			session.carePlan.addInstantiatesCanonical(c.getValueAsString());
		}
		setPrecheckBehavior(act, action, subIndex);
	}

	private void applyAction(Session session, Resource result, PlanDefinition.PlanDefinitionActionComponent action) {
		switch (result.fhirType()) {
		case "Task":
			result = resolveTask(session, (CarePlan) result, action);
			break;
		}
	}

	private CarePlan resolveTask(Session session, CarePlan task, PlanDefinition.PlanDefinitionActionComponent action) {
		task.setId(new IdType(action.getId()));
	    if (action.hasRelatedAction()) {
	        for (PlanDefinitionActionRelatedActionComponent relatedAction : action.getRelatedAction()) {
	        	Extension next = new Extension();
	        	next.setUrl("http://hl7.org/fhir/aphl/StructureDefinition/next");
	        	if (relatedAction.hasOffset()) {
	        		Extension offsetExtension = new Extension();
	        		offsetExtension.setUrl("http://hl7.org/fhir/aphl/StructureDefinition/offset");
	        		offsetExtension.setValue(relatedAction.getOffset());
	        		next.addExtension(offsetExtension);
	        	}
	        	Extension target = new Extension();
	        	target.setUrl("http://hl7.org/fhir/aphl/StructureDefinition/target");
	        	target.setValue(new Reference("#" + relatedAction.getActionId()));
	        	next.addExtension(target);
	        	((DomainResource)task).addExtension(next);
	        }
	    }
	    if (action.hasCondition()) {
	    	for (PlanDefinitionActionConditionComponent conditionComponent : action.getCondition()) {
	    		Extension condition = new Extension();
	    		condition.setUrl("http://hl7.org/fhir/aphl/StructureDefinition/condition");
	    		Expression expression = conditionComponent.getExpression();
	    		condition.setValue(expression);
	    		if (conditionComponent.hasExtension(alternateExpressionExtension)) {
	    			condition.addExtension(conditionComponent.getExtensionByUrl(alternateExpressionExtension));
	    		}
	    		((DomainResource) task).addExtension(condition);
	    	}
	    }
	    if (action.hasInput()) {
	    	for (DataRequirement dataRequirement : action.getInput()) {
	    		Extension input = new Extension();
	    		input.setUrl("http://hl7.org/fhir/aphl/StructureDefinition/input");
	    		input.setValue(dataRequirement);
	    		((DomainResource) task).addExtension(input);
	      }
	    }
	    Reference basedOnRef = new Reference(session.requestGroup);
	    basedOnRef.setType(session.requestGroup.fhirType());
	    ((CarePlan) task).addBasedOn(basedOnRef);
	    return task;
	}

	private boolean resolveDynamicActions(Session session, PlanDefinition.PlanDefinitionActionComponent action, int index, boolean isSubItem, int subIndex) {
		boolean somethingFound = false;
		for (PlanDefinitionActionDynamicValueComponent dynamicValue : action.getDynamicValue()) {
			String path = dynamicValue.getPath();
			Expression expression = dynamicValue.getExpression();
			logger.info("Resolving dynamic value {} {}", path, expression);
			ensureDynamicValueExpression(dynamicValue);
			if (expression.hasLanguage()) {
				String libraryToBeEvaluated = ensureLibrary(session, expression);
				String language = expression.getLanguage();
				String exp = expression.getExpression();
				List<DataRequirement> input = action.getInput();
				Object result = evaluateConditionOrDynamicValue(exp, language, libraryToBeEvaluated, session, input);
				if (result == null && expression.hasExtension(alternateExpressionExtension)) {
					Extension altext = expression.getExtensionByUrl(alternateExpressionExtension);
					Type alternateExpressionValue = altext.getValue();
					if (!(alternateExpressionValue instanceof Expression)) {
						throw new RuntimeException("Expected alternateExpressionExtensionValue to be of type Expression");
					}
					Expression altExp = (Expression) alternateExpressionValue;
					if (altExp.hasLanguage()) {
						libraryToBeEvaluated = ensureLibrary(session, altExp);
						language = altExp.getLanguage();
						result = evaluateConditionOrDynamicValue(altExp.getExpression(), language, libraryToBeEvaluated, session, input);
					}
				}
				if (result instanceof DateTime) {
					OffsetDateTime dateTime = ((DateTime) result).getDateTime();
					Instant instant = dateTime.toInstant();
					result = Date.from(instant);
				}
				
				if (path != null && path.equals("$this")) {
					session.carePlan = (CarePlan) result;
				} else if (path != null && (path.startsWith("action") || path.startsWith("%action"))) {
					try {
						String propertyType = path.substring(path.indexOf(".")+1);
						if (propertyType.startsWith("extension") && propertyType.contains(".")) {
							propertyType = propertyType.substring(0, propertyType.indexOf("."));
						}
						if ("extension".equals(propertyType) && result instanceof IBaseDatatype) {
							result = new Extension().setValue((IBaseDatatype) result).setUrl(path);
						}
						if (result != null) {
							action.setProperty(propertyType, (Base) result);
						} else {
							logger.warn(String.format("Path %s attempted null value", path));
						}
						somethingFound = true;
					} catch (Exception e) {
						throw new RuntimeException(String.format("Could not set path %s to value: %s", path, result));
					}
				} else {
					try {
						session.carePlan.setProperty(path, (Base) result);
					} catch (Exception e) {
						throw new RuntimeException(String.format("Could not set path %s to value: %s", path, result));
					}
				}
			}
		}
		if (somethingFound == true) {
			List<RequestGroupActionComponent> acts = session.requestGroup.getAction();
			while (acts.size() <= index) {
				session.requestGroup.addAction();
				acts = session.requestGroup.getAction();
			}
			RequestGroupActionComponent act = acts.get(index);
			if (isSubItem) {
				act = act.addAction();
			}
			act.setTitle(action.getTitle());
			act.setDescription(action.getDescription());
			if (action.getPrefix() == null) {
				if (act.getPrefix() == null) {
					act.setPrefix(action.getDescription());
				}
			} else {
				act.setPrefix(action.getPrefix());
			}
			if (!act.hasTextEquivalent()) { // needed to preserve ActivityDefinition's description
				act.setTextEquivalent(action.getTextEquivalent());
			}
			if (action.getType()!= null) {
				act.setType(action.getType());
			}
			act.setId(action.getId());
			act.setCode(action.getCode());
			act.setTiming(action.getTiming());
			if (action.hasExtension()) {
				for (Extension ext : action.getExtension()) {
					act.addExtension(ext);
				}
			}
			if (action.hasDocumentation()) {
				if (action.getDocumentationFirstRep().getLabel() == null) {
					action.getDocumentationFirstRep().setLabel(session.planDefinition.getPublisher());
				}
				act.addDocumentation(action.getDocumentationFirstRep());
			} else if (session.planDefinition.getPublisher() != null) {
				act.addDocumentation().setLabel(session.planDefinition.getPublisher());
			}
			if (action.hasSelectionBehavior()) {
				act.setSelectionBehavior(RequestGroup.ActionSelectionBehavior.valueOf(action.getSelectionBehavior().name()));
			}
			setPrecheckBehavior(act, action, subIndex);
		}
		return somethingFound;
	}
	
	private Boolean meetsConditions(Session session, PlanDefinition.PlanDefinitionActionComponent action, int index) {
		if (action.hasAction()) {
			int subIndex = 0;
			for (PlanDefinition.PlanDefinitionActionComponent containedAction : action.getAction()) {
				Map<String, PlanDefinition.PlanDefinitionActionComponent>  metConditions = new HashMap<>();
				resolveAction(session, metConditions, containedAction, index, true, subIndex);
				subIndex++;
			}
		}
		CodeableConcept type = session.planDefinition.getType();
		if (type.hasCoding()) {
			for (Coding coding : type.getCoding()) {
				if (coding.getCode().equals("workflow-definition")) {
					// logger.info(String.format("Found a workflow definition type for PlanDefinition % conditions should be evaluated at task execution time."), session.planDefinition.getUrl());
					return true;
				}
			}
		}
		for (PlanDefinitionActionConditionComponent condition : action.getCondition()) {
			ensureConditionExpression(condition);
			Expression expression = condition.getExpression();
			if (expression.hasLanguage()) {
				String libraryToBeEvaluated = ensureLibrary(session, expression);
				String language = expression.getLanguage();
				String exp = expression.getExpression();
				List<DataRequirement> input = action.getInput();
				Object result = evaluateConditionOrDynamicValue(exp, language, libraryToBeEvaluated, session, input);
				if (result == null && expression.hasExtension(alternateExpressionExtension)) {
					Extension altext = expression.getExtensionByUrl(alternateExpressionExtension);
					Type alternateExpressionValue = altext.getValue();
					if (!(alternateExpressionValue instanceof Expression)) {
						throw new RuntimeException("Expected alternateExpressionExtensionValue to be of type Expression");
					}
					Expression altExp = (Expression) alternateExpressionValue;
					if (altExp.hasLanguage()) {
						libraryToBeEvaluated = ensureLibrary(session, altExp);
						language = altExp.getLanguage();
						result = evaluateConditionOrDynamicValue(altExp.getExpression(), language, libraryToBeEvaluated, session, input);
					}
				}
				if (result == null) {
					logger.warn("Expression Returned null");
					return false;
				}
				if (!(result instanceof BooleanType)) {
					logger.warn("The condition returned a non-boolean value: " + result.getClass().getSimpleName());
					continue;
				}
				boolean value = ((BooleanType) result).booleanValue();;
				if (!value) {
					String id = condition.getId();;
					logger.info("The result of condition id {} expression language {} is false", id, language);
					return false;
				}
			}
		}
		return true;
	}

	protected String ensureLibrary(Session session, Expression expression) {
		if (expression.hasReference()) {
			return expression.getReference();
		}
		String exp = expression.getExpression();
		logger.warn(String.format("No library reference for expression: %s", exp));
		List<CanonicalType> library = session.planDefinition.getLibrary();
		if (library.size() == 1) {
			return library.get(0).asStringValue();
		}
		logger.warn("No primary library defined");
		return null;
	}

	protected void ensureConditionExpression(PlanDefinitionActionConditionComponent condition) {
		if (!condition.hasExpression()) {
			logger.error("Missing condition expression");
			throw new RuntimeException("Missing condition expression");
		}
	}

	protected void ensureDynamicValueExpression(PlanDefinitionActionDynamicValueComponent dynamicValue) {
		if (!dynamicValue.hasExpression()) {
			logger.error("Missing dynamicValue expression");
			throw new RuntimeException("Missing dynamicValue expression");
		}
	}
	
	protected static String getResourceName(CanonicalType canonical) {
		if (canonical.hasValue()) {
			String id = canonical.getValue();
			if (id.contains("/")) {
				id = id.replace(id.substring(id.lastIndexOf("/")), "");
				return id.contains("/") ? id.substring(id.lastIndexOf("/") + 1) : id;
			}
			return null;
		}
		throw new RuntimeException("CanonicalType must have a value for resource name extraction");
	}

	public Object getParameterComponentByName(Parameters params, String name) {
		ParametersParameterComponent item = null;
		for (ParametersParameterComponent x : params.getParameter()) {
			if (name.equals(x.getName())) {
				item = x;
				break;
			}
		}
		if (item != null) {
			return item.hasValue() ? item.getValue() : item.getResource();
		}
		return null;
	}

	@SuppressWarnings({"unchecked"})
	protected Object evaluateConditionOrDynamicValue(String expression, String language, String libraryToBeEvaluated, Session session, List<DataRequirement> dataRequirements) { //List<DataRequirement> 
		Parameters params = resolveInputParameters(dataRequirements);
		if (session.parameters != null && (session.parameters instanceof Parameters)) {
			List<ParametersParameterComponent> paramsParams = params.getParameter();
			paramsParams.addAll(((Parameters) session.parameters).getParameter());
		}
		if (libraryResults.get().keySet().stream().noneMatch(k -> k.startsWith(session.patientId + "_"))) {
			IBaseParameters libraryResult = libraryProcessor.evaluate(libraryToBeEvaluated, session.patientId,
					params, session.contentEndpoint, session.terminologyEndpoint,
					session.dataEndpoint, session.bundle, null);
			if (libraryResult instanceof Parameters) {
				for (ParametersParameterComponent param : ((Parameters) libraryResult).getParameter()) {
					if (param.hasName()) {
						String name = param.getName();
						if (param.hasValue()) {
							Type value = param.getValue();
							libraryResults.get().put(session.patientId + "_" + name, value);
							logger.debug(" - " + name + ": " + value);
						} else if (param.hasResource()) {
							Resource resource = param.getResource();
							libraryResults.get().put(session.patientId + "_" + name, resource);
							logger.debug(" - " + name + ": " + resource);
						} else {
							logger.warn("Expression " + name + " has no value");
						}
					}
				}
			}
		}
		Object result = null;
		switch (language) {
		case "text/cql":
		case "text/cql.expression":
		case "text/cql-expression": 
			String key = generateExpressionKey(expression, params);
			if (libraryResults.get().containsKey(key)) {
				result = libraryResults.get().get(key);
			} else if (libraryResults.get().containsKey(session.patientId + "_" + expression)) {
				result = libraryResults.get().get(session.patientId + "_" + expression);
			} else {	
			    result = expressionEvaluator.evaluate(expression, params);
			    libraryResults.get().put(key, result);
			}
			break;
		case "text/cql-identifier":
		case "text/cql.identifier":
		case "text/cql.name":
		case "text/cql-name":
			String keyL = generateExpressionKey(expression, params);
			if (libraryResults.get().containsKey(keyL)) {
				result = libraryResults.get().get(keyL);
			} else if (libraryResults.get().containsKey(expression)) {
				result = libraryResults.get().get(expression);
			} else {	
				result = libraryProcessor.evaluate(libraryToBeEvaluated, session.patientId, params, session.contentEndpoint, 
					session.terminologyEndpoint, session.dataEndpoint, session.bundle, Collections.singleton(expression));
				libraryResults.get().put(keyL, result);
			}
			break;
		case "text/fhirpath":
			List<IBase> outputs;
			try {
				//<T extends IBase> List<T> evaluate(IBase theInput, String thePath, Class<T> theReturnType);
				outputs = fhirPath.evaluate(null, expression, IBase.class);
			} catch (RuntimeException e) {
				throw new IllegalArgumentException("Error evaluating FHIRPath expression", e);
			}
			if (outputs == null || outputs.isEmpty()) {
				result = null;
			} else if (outputs.size() == 1) {
				result = outputs.get(0);
			} else {
				throw new IllegalArgumentException("Expected only one value when evaluating FHIRPath expression: " + expression);
			}
			break;
		default:
			logger.warn("An action language other than CQL was found: " + language);
		}
		if (result != null) {
			if (result instanceof Parameters) {
				result = getParameterComponentByName((Parameters) result, expression);
			}
		}
		Map<String, Object> allresults = evaluatedCqlResults.get();
		String keyName = new IdType(libraryToBeEvaluated).getIdPart() + "_" + expression;
		//add keys to result
		for (String key : libraryResults.get().keySet().stream().filter(k -> k.startsWith(session.patientId + "_")).collect(Collectors.toList())) {
			if (!allresults.containsKey(key.replace(session.patientId + "_", ""))) {
				allresults.put(key.replace(session.patientId + "_", ""), libraryResults.get().get(key));
			}
		}
		if (allresults.containsKey(keyName)) {
			Object value = allresults.get(keyName);
			if (!allresults.containsKey(keyName + "_multiple")) {
				List<Object> list = new ArrayList<>();
				list.add(result);
				allresults.put(keyName, list);
				allresults.put(keyName + "_multiple", true);
			} else {
				List<Object> list = (List<Object>) value;
				list.add(result);
			}
		} else {
			allresults.put(keyName, result);
		}
		return result;
	}

	private String generateExpressionKey(String expression, Parameters params) {
		StringBuilder sb = new StringBuilder();
		for (ParametersParameterComponent param : params.getParameter()) {
			if (param.hasName() && param.hasValue()) {
				String name = param.getName();
				Type value = param.getValue();
				sb.append(name).append("=").append(value);
			} else if (param.hasName() && param.hasResource()) {
				String name = param.getName();
				Resource resource = param.getResource();
				ResourceType resourceType = resource.getResourceType();
				String id = resource.getId();
				sb.append(name).append("=>").append(resourceType).append("/").append(id);
			}
		}
		return expression + ":" + sb.toString();
	}

	private Parameters resolveInputParameters(List<DataRequirement> dataRequirements) {
		Parameters params = new Parameters();
		if (dataRequirements == null) {
			return params;
		}
		for (DataRequirement req : dataRequirements) {
			String dataReqType = req.getType();
			Iterable<IBaseResource> iter = fhirDal.search(dataReqType);
			Iterator<IBaseResource> resources = iter.iterator();
			if (resources != null && resources.hasNext()) {
				int index = 0;
				Boolean found = true;
				while (resources.hasNext()) {
					IBaseResource resource = resources.next();
					ParametersParameterComponent parameter = new ParametersParameterComponent();
					String reqId = req.getId();
					parameter.setName("%" + String.format("%s", reqId));
					if (req.hasCodeFilter()) {
						for (DataRequirementCodeFilterComponent filter : req.getCodeFilter()) {
							Parameters codeFilterParam = new Parameters();
							ParametersParameterComponent codeFilterParamParam = codeFilterParam.addParameter();
							codeFilterParamParam.setName("%resource");
							codeFilterParamParam.setResource((Resource) resource);
							if (filter != null) {
								if(filter.hasPath() && filter.hasValueSet()) {
									String filterValueSet = filter.getValueSet();
									Iterable<IBaseResource> valueset = fhirDal.searchByUrl("ValueSet", filterValueSet);
									if (valueset != null && valueset.iterator().hasNext()) {
										ParametersParameterComponent codeFilterparamItem = codeFilterParam.addParameter();
										codeFilterparamItem.setName("%valueset");
										codeFilterparamItem.setResource((Resource) valueset.iterator().next());
										String filterPath = filter.getPath();
										String codeFilterExpression = "%" + String.format("resource.%s.where(code.memberOf(\'%s\'))", filterPath, "%" + "valueset");
										IBaseParameters codeFilterResult = expressionEvaluator.evaluate(codeFilterExpression, codeFilterParam);
										IBaseDatatype tempResult = operationParametersParser.getValueChild(codeFilterResult, "return");
										if (tempResult != null && (tempResult instanceof BooleanType)) {
											found = ((BooleanType) tempResult).booleanValue();
										}
									}
									logger.debug(String.format("Could not find ValueSet with url %s on the local server.", filterValueSet));
								}
							}
						}
					}
					if (!resources.hasNext() && index == 0) {
						ParameterDefinition paramDef = new ParameterDefinition();
						paramDef.setMax("*").setName("%" + reqId);
						parameter.addExtension("http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-parameterDefinition", paramDef);
						if (found) {
							parameter.setResource((Resource) resource);
						}
					} else {
						if (!found) {
							index++;
							continue;
						}
						parameter.setResource((Resource) resource);
					} 
					params.addParameter(parameter);
					index++;
				}
			} else {
				ParametersParameterComponent parameter = params.addParameter();
				String reqId = req.getId();
				parameter.setName("%" + String.format("%s", reqId));
				ParameterDefinition paramDef = new ParameterDefinition().setMax("*").setName("%" + reqId);
				parameter.addExtension("http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-parameterDefinition", paramDef);
			}
		}
		return params;
	}

	protected IBaseResource resolveContained(DomainResource resource, String id) {
		for (Resource item : resource.getContained()) {
			if (item.hasIdElement()) {
				if (item.getIdElement().getIdPart().equals(id)) {
					return item;
				}
			}
		}
		return null;
	}
}

class Session {
	public final String patientId;
	public final PlanDefinition planDefinition;
	public final String practitionerId;
	public final String organizationId;
	public final String userType;
	public final String userLanguage;
	public final String userTaskContext;
	public final String setting;
	public final String settingContext;
	public final String prefetchDataKey;
	public CarePlan carePlan;
	public final String encounterId;
	public final RequestGroup requestGroup;
	  public IBaseParameters parameters, prefetchData;
	public IBaseResource contentEndpoint, terminologyEndpoint, dataEndpoint;
	public IBaseBundle bundle, prefetchDataData;
	public DataRequirement prefetchDataDescription;
	public Boolean useServerData, mergeNestedCarePlans;
	
	
    public Session(PlanDefinition planDefinition, CarePlan carePlan, String patientId, String encounterId,
	      String practitionerId, String organizationId, String userType, String userLanguage, String userTaskContext,
	      String setting, String settingContext, RequestGroup requestGroup, IBaseParameters parameters,
	      IBaseParameters prefetchData, IBaseResource contentEndpoint, IBaseResource terminologyEndpoint,
	      IBaseResource dataEndpoint, IBaseBundle bundle, Boolean useServerData, Boolean mergeNestedCarePlans,
	      IBaseBundle prefetchDataData, DataRequirement prefetchDataDescription, String prefetchDataKey) {

		this.patientId = patientId;
		this.planDefinition = planDefinition;
		this.carePlan = carePlan;
		this.encounterId = encounterId;
		this.practitionerId = practitionerId;
		this.organizationId = organizationId;
		this.userType = userType;
		this.userLanguage = userLanguage;
		this.userTaskContext = userTaskContext;
		this.setting = setting;
		this.settingContext = settingContext;
		this.requestGroup = requestGroup;
		this.parameters = parameters;
		this.contentEndpoint = contentEndpoint;
		this.terminologyEndpoint = terminologyEndpoint;
		this.dataEndpoint = dataEndpoint;
		this.bundle = bundle;
		this.useServerData = useServerData;
		this.mergeNestedCarePlans = mergeNestedCarePlans;
		this.prefetchDataData = prefetchDataData;
		this.prefetchDataDescription = prefetchDataDescription;
		this.prefetchData = prefetchData;
		this.prefetchDataKey = prefetchDataKey;
	}
}
