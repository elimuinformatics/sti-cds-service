package io.elimu.kogito.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.cqframework.cql.elm.execution.Library;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.ParameterDefinition;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RequestGroup;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.json.simple.parser.ParseException;
import org.kie.kogito.internal.process.runtime.KogitoWorkItem;
import org.kie.kogito.internal.process.runtime.KogitoWorkItemHandler;
import org.kie.kogito.internal.process.runtime.KogitoWorkItemManager;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverter;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverterFactory;
import org.opencds.cqf.cql.evaluator.CqlOptions;
import org.opencds.cqf.cql.evaluator.activitydefinition.r4.ActivityDefinitionProcessor;
import org.opencds.cqf.cql.evaluator.builder.CqlEvaluatorBuilder;
import org.opencds.cqf.cql.evaluator.builder.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.EndpointConverter;
import org.opencds.cqf.cql.evaluator.builder.LibraryContentProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.ModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.TerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.dal.FhirRestFhirDalFactory;
import org.opencds.cqf.cql.evaluator.builder.data.FhirModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.data.TypedRetrieveProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.library.FhirRestLibraryContentProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.library.TypedLibraryContentProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.terminology.FhirRestTerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.terminology.TypedTerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.cql.evaluator.engine.CqlEngineOptions;
import org.opencds.cqf.cql.evaluator.expression.ExpressionEvaluator;
import org.opencds.cqf.cql.evaluator.fhir.ClientFactory;
import org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;
import org.opencds.cqf.cql.evaluator.library.CqlFhirParametersConverter;
import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.IRestfulClientFactory;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.impl.GenericClient;
import ca.uhn.fhir.rest.client.interceptor.SimpleRequestHeaderInterceptor;
import io.elimu.kogito.exception.WorkItemHandlerException;
import io.elimu.kogito.model.Card;
import io.elimu.kogito.cql.CachingHttpClient;
import io.elimu.kogito.cql.CardCreator;
import io.elimu.kogito.cql.CustomFhirRestRetrieveProviderFactory;
import io.elimu.kogito.cql.DecoratedPlanDefinitionProcessor;
import io.elimu.kogito.cql.MyCqlEvaluatorBuilder;
import io.elimu.kogito.cql.OpenOperationParametersParser;
import io.elimu.kogito.cql.TimeObject;
import io.elimu.kogito.cql.TimeoutMap;

public class PlanDefInlineHandler implements KogitoWorkItemHandler {

	private static final Logger LOG = LoggerFactory.getLogger(PlanDefInlineHandler.class);
	private static final long MAX_AGE_CACHE = Long.valueOf(System.getProperty("plandef.max.lifetime", "900000"));
	
	private static Map<String, TimeObject<DecoratedPlanDefinitionProcessor>> CACHED_PROCESSORS = new HashMap<>();
	private static Map<String, TimeObject<PlanDefinition>> CACHED_PLANDEFS = new HashMap<>();
	private static HashMap<org.cqframework.cql.elm.execution.VersionedIdentifier, Library> LIBRARY_CACHE = new TimeoutMap<>(MAX_AGE_CACHE);

	private FhirContext ctx;
	private ClientFactory clientFactory;
	private AdapterFactory adapterFactory;
	private FhirTypeConverter fhirTypeConverter;
	private CqlFhirParametersConverter cqlFhirParametersConverter;
	private LibraryVersionSelector libraryVersionSelector;
	private FhirRestLibraryContentProviderFactory frlcpFactory;
	private Set<TypedLibraryContentProviderFactory> libraryContentProviderFactories;
	private LibraryContentProviderFactory libraryLoaderFactory;
	private FhirModelResolverFactory mrFactory;
	private Set<ModelResolverFactory> modelResolverFactories;
	private CustomFhirRestRetrieveProviderFactory trpFactory;
	private Set<TypedRetrieveProviderFactory> retrieveProviderFactories;
	private DataProviderFactory dataProviderFactory;
	private Set<TypedTerminologyProviderFactory> terminologyProviderFactories;
	private TerminologyProviderFactory terminologyProviderFactory;
	private EndpointConverter endpointConverter;
	private LibraryProcessor libProcessor;

	private ExpressionEvaluator expressionEvaluator;

	private OpenOperationParametersParser operationParametersParser;

	public PlanDefInlineHandler() {
		this.ctx = FhirContext.forR4Cached();
		IRestfulClientFactory restClientFactory = this.ctx.getRestfulClientFactory();
		restClientFactory.setConnectTimeout(30000);
		restClientFactory.setSocketTimeout(30000);
		restClientFactory.setServerValidationMode(ServerValidationModeEnum.NEVER);
		restClientFactory.setHttpClient(new CachingHttpClient());
		
		this.clientFactory = new ClientFactory(this.ctx);
		this.adapterFactory = new AdapterFactory();
		this.fhirTypeConverter = new FhirTypeConverterFactory().create(this.ctx.getVersion().getVersion());
		this.cqlFhirParametersConverter = new CqlFhirParametersConverter(this.ctx, this.adapterFactory, this.fhirTypeConverter);
		this.libraryVersionSelector = new LibraryVersionSelector(this.adapterFactory);
		this.frlcpFactory = new FhirRestLibraryContentProviderFactory(this.clientFactory, this.adapterFactory, this.libraryVersionSelector);
		this.libraryContentProviderFactories = new HashSet<>();
		this.libraryContentProviderFactories.add(this.frlcpFactory);
		this.libraryLoaderFactory = new org.opencds.cqf.cql.evaluator.builder.library.LibraryContentProviderFactory(
				this.ctx, this.adapterFactory, this.libraryContentProviderFactories, this.libraryVersionSelector);
		this.mrFactory = new FhirModelResolverFactory();
		this.modelResolverFactories = Collections.singleton(this.mrFactory);
		this.trpFactory = new CustomFhirRestRetrieveProviderFactory(this.ctx, this.clientFactory);
		this.retrieveProviderFactories = new HashSet<>();
		this.retrieveProviderFactories.add(this.trpFactory);
		CqlOptions cqlOptions = new CqlOptions();
		CqlEngineOptions cqlEngineOptions = new CqlEngineOptions();
		cqlEngineOptions.setOptions(Collections.singleton(CqlEngine.Options.EnableExpressionCaching));
		cqlOptions.setCqlEngineOptions(cqlEngineOptions);
		this.dataProviderFactory = new org.opencds.cqf.cql.evaluator.builder.data.DataProviderFactory(this.ctx, this.modelResolverFactories, this.retrieveProviderFactories);
		TypedTerminologyProviderFactory frtpFactory = new FhirRestTerminologyProviderFactory(this.ctx, this.clientFactory);
		this.terminologyProviderFactories = Collections.singleton(frtpFactory);
		this.terminologyProviderFactory = new org.opencds.cqf.cql.evaluator.builder.terminology.TerminologyProviderFactory(this.ctx, this.terminologyProviderFactories);
		this.endpointConverter = new EndpointConverter(this.adapterFactory);
		this.libProcessor = new LibraryProcessor(this.ctx, this.cqlFhirParametersConverter, this.libraryLoaderFactory, this.dataProviderFactory, 
				this.terminologyProviderFactory, this.endpointConverter, this.mrFactory, new Supplier<CqlEvaluatorBuilder>() {
					@Override
					public CqlEvaluatorBuilder get() {
						return new MyCqlEvaluatorBuilder().withLibraryCache(LIBRARY_CACHE).withCqlOptions(cqlOptions);
					}
				});
		this.expressionEvaluator = new ExpressionEvaluator(this.ctx, this.cqlFhirParametersConverter, this.libraryLoaderFactory, this.dataProviderFactory, 
				this.terminologyProviderFactory, this.endpointConverter, this.mrFactory, new Supplier<CqlEvaluatorBuilder>() {
					@Override
					public CqlEvaluatorBuilder get() {
						return new MyCqlEvaluatorBuilder().withLibraryCache(LIBRARY_CACHE).withCqlOptions(cqlOptions);
					}
				});
		this.operationParametersParser = new OpenOperationParametersParser(this.adapterFactory, this.fhirTypeConverter);
		try {
		    operationParametersParser.getClass().getDeclaredMethod("getValueChild", IBaseParameters.class, String.class).setAccessible(true);
		    operationParametersParser.getClass().getDeclaredMethod("getResourceChild", IBaseParameters.class, String.class).setAccessible(true);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
	
	private boolean isEmpty(String value) {
		return value == null || "".equals(value.trim()) || "null".equalsIgnoreCase(value.trim());
	}
	
	@Override
	public void executeWorkItem(KogitoWorkItem workItem, KogitoWorkItemManager manager) {
		LOG.info("Entered PlanDefinition WIH");
		Map<String, Object> results = workItem.getResults();
		String fhirServerUrl = (String) workItem.getParameter("fhirServerUrl");
		String fhirTerminologyServerUrl = (String) workItem.getParameter("fhirTerminologyServerUrl");
		String planDefId = (String) workItem.getParameter("planDefinitionId");
		String planDefUrl = (String) workItem.getParameter("planDefinitionUrl");
		String planDefJson = (String) workItem.getParameter("planDefinitionJson");
		String patientId = (String) workItem.getParameter("patientId");
		String encounterId = (String) workItem.getParameter("encounterId");
		String practitionerId = (String) workItem.getParameter("practitionerId");
		String organizationId = (String) workItem.getParameter("");
		String userType = (String) workItem.getParameter("userType");
		String userLanguage = (String) workItem.getParameter("userLanguage");
		String userTaskContext = (String) workItem.getParameter("userTaskContext");
		String setting = (String) workItem.getParameter("setting");
		String settingContext = (String) workItem.getParameter("settingContext");
		List<String> missingItems = new ArrayList<>();
		//validate required fhirServerUrl
		if (isEmpty(fhirServerUrl)) {
			missingItems.add("fhirServerUrl");
		}
		//validate fhirTerminolgoyServerUrl or make it fhirServerUrl by default
		if (isEmpty(fhirTerminologyServerUrl)) {
			if (!isEmpty(fhirServerUrl)) {
				missingItems.add("fhirTerminologyServerUrl");
			} else {
				fhirTerminologyServerUrl = fhirServerUrl;
			}
		}
		//validate required patientId
		if (isEmpty(patientId)) {
			missingItems.add("patientId");
		}
		//validate required planDefinitionId or planDefinitionUrl
		if (isEmpty(planDefId) && isEmpty(planDefUrl) && isEmpty(planDefJson)) {
			missingItems.add("planDefinitionId|planDefinitionUrl|planDefinitionJson");
		}
		if (!missingItems.isEmpty()) {
			LOG.info("Missing items: " + missingItems);
			throw new WorkItemHandlerException("Missing items: " + missingItems);
		}
		LOG.debug("Have all the necessary inputs for PlanDefCdsInlineWorkItmeHandler");
		try {
			String fhirServerAuth = (String) workItem.getParameter("fhirServerAuth");
			String fhirTerminologyServerAuth = (String) workItem.getParameter("fhirTerminologyServerAuth");
			Map<String, Object> prefetchData = new HashMap<>();
			Map<String, Object> contextData = new HashMap<>();
			Map<String, String> patientHeaderData = new HashMap<>();
			Map<String, String> terminologyHeaderData = new HashMap<>();
			for (String key : workItem.getParameters().keySet()) {
				if (key.startsWith("prefetch_")) {
					prefetchData.put(key.replace("prefetch_", ""), workItem.getParameter(key));
				} else if (key.startsWith("context_")) {
					contextData.put(key.replace("context_", ""), workItem.getParameter(key));
				} else if (key.startsWith("fhirServer_header_")) {
					patientHeaderData.put(key.replace("fhirServer_header_", "").replaceAll("__", "-"), 
							String.valueOf(workItem.getParameter(key)));
				} else if (key.startsWith("fhirTerminologyServer_header_")) {
					terminologyHeaderData.put(key.replace("fhirTerminologyServer_header_", "").replaceAll("__", "-"), 
							String.valueOf(workItem.getParameter(key)));
				}
			}
			DecoratedPlanDefinitionProcessor pdProcessor = null;
			PlanDefinition planDefinition = null;
			String key = null;
			if (CACHED_PROCESSORS.containsKey(planDefId) && !CACHED_PROCESSORS.get(planDefId).olderThan(MAX_AGE_CACHE)) {
				key = planDefId;
				pdProcessor = CACHED_PROCESSORS.get(planDefId).getValue();
				LOG.debug("Processor already cached by planDefId key " + key);
			} else if (CACHED_PROCESSORS.containsKey(planDefUrl) && !CACHED_PROCESSORS.get(planDefUrl).olderThan(MAX_AGE_CACHE)) {	
				key = planDefUrl;
				pdProcessor = CACHED_PROCESSORS.get(planDefUrl).getValue();
				LOG.debug("Processor already cached by planDefUrl key " + key);
			} else {
				if (!isEmpty(planDefJson)) {
					planDefinition = this.ctx.newJsonParser().parseResource(PlanDefinition.class, planDefJson);
					key = planDefinition.getIdElement().getIdPart();
					CACHED_PLANDEFS.put(key, new TimeObject<>(planDefinition));
				} else {
					LOG.debug("Creating PlanDefinitionProcessor...");
					//Object fhirClient = initClient(fhirServerUrl, fhirServerAuth, patientHeaderData); 
					IGenericClient fhirTerminologyClient = initClient(fhirTerminologyServerUrl, fhirTerminologyServerAuth, terminologyHeaderData);
					if (planDefId != null) {
						LOG.debug("Fetching PlanDefinition by ID: " + fhirTerminologyServerUrl + "/PlanDefinition/" + planDefId);
						key = planDefId;
						planDefinition = fhirTerminologyClient.fetchResourceFromUrl(PlanDefinition.class, fhirTerminologyServerUrl + "/PlanDefinition/" + planDefId);
						if (planDefinition == null) {
							throw new WorkItemHandlerException("PlanDefinition cannot be found by ID " + planDefId);
						}
						CACHED_PLANDEFS.put(key, new TimeObject<>(planDefinition));
						String url = planDefinition.getUrl();
						if (url != null) {
							CACHED_PLANDEFS.put(url, new TimeObject<>(planDefinition));
						}
						LOG.debug("PlanDefinition fetch " + fhirTerminologyServerUrl + "/PlanDefinition/" + planDefId + " successful");
					} else {
						LOG.debug("Fetching PlanDefinition by URL: " + fhirTerminologyServerUrl + "/PlanDefinition?url=" + planDefUrl);
						key = planDefUrl;
						Bundle bundle = fhirTerminologyClient.fetchResourceFromUrl(Bundle.class, fhirTerminologyServerUrl + "/PlanDefinition?url=" + planDefUrl);
						if (!bundle.getEntryFirstRep().hasResource()) {
							throw new WorkItemHandlerException("PlanDefinition cannot be found by Url " + planDefUrl);
						}
						planDefinition = (PlanDefinition) bundle.getEntryFirstRep().getResource();
						CACHED_PLANDEFS.put(key, new TimeObject<>(planDefinition));
						String id = planDefinition.getIdElement().getIdPart();
						if (id != null) {
							CACHED_PLANDEFS.put(id, new TimeObject<>(planDefinition));
						}
						LOG.debug("PlanDefinition fetch " + fhirTerminologyServerUrl + "/PlanDefinition?url=" + planDefUrl + " successful");
					}
				}
				FhirRestFhirDalFactory fdFactory = new FhirRestFhirDalFactory(this.clientFactory);
				List<String> headers = new ArrayList<>();
				headers.add("Content-Type: application/json");
				if (fhirTerminologyServerAuth != null) {
					headers.add("Authorization:" + fhirTerminologyServerAuth);
				}
				if (terminologyHeaderData != null && !terminologyHeaderData.isEmpty()) {
					for (String headerName : terminologyHeaderData.keySet()) {
						headers.add(headerName + ": " + terminologyHeaderData.get(headerName));
					}
				}
				FhirDal fhirDal = fdFactory.create(fhirTerminologyServerUrl, headers);
				ActivityDefinitionProcessor activityDefinitionProcessor = new ActivityDefinitionProcessor(this.ctx, fhirDal, this.libProcessor);
				pdProcessor = new DecoratedPlanDefinitionProcessor(this.ctx, fhirDal,
						this.libProcessor, this.expressionEvaluator, activityDefinitionProcessor, this.operationParametersParser);
				CACHED_PROCESSORS.put(key, new TimeObject<>(pdProcessor));
				LOG.debug("PlanDefinitionProcessor for key " + key + " created");
			}
			Endpoint terminologyEndpoint = new Endpoint().setAddress(fhirTerminologyServerUrl).setConnectionType(new Coding().setCode("hl7-fhir-rest"));
			if (fhirTerminologyServerAuth != null) {
				terminologyEndpoint.addHeader("Authorization:" + fhirTerminologyServerAuth);
			}
			if (terminologyHeaderData != null && !terminologyHeaderData.isEmpty()) {
				for (String headerName : terminologyHeaderData.keySet()) {
					terminologyEndpoint.addHeader(headerName + ": " + terminologyHeaderData.get(headerName));
				}
			}
			Endpoint dataEndpoint = new Endpoint().setAddress(fhirServerUrl).setConnectionType(new Coding().setCode("hl7-fhir-rest"));
			if (fhirServerAuth != null) {
				dataEndpoint.addHeader("Authorization:" + fhirServerAuth);
			}
			if (patientHeaderData != null && !patientHeaderData.isEmpty()) {
				for (String headerName : patientHeaderData.keySet()) {
					dataEndpoint.addHeader(headerName + ": " + patientHeaderData.get(headerName));
				}
			}
			planDefinition = CACHED_PLANDEFS.get(key).getValue();
			IdType planIdType = planDefinition.getIdElement();
			LOG.debug("PlanDefinitionProcessor apply call starting...");
			CarePlan carePlan = pdProcessor.apply(planDefinition, planIdType, 
				"Patient/"+patientId, encounterId == null ? null : "Encounter/" + encounterId, 
				practitionerId == null ? null : "Practitioner/" + practitionerId,
				organizationId == null ? null : "Organization/" + organizationId, 
				userType, userLanguage, userTaskContext, setting, settingContext, Boolean.TRUE, 
				asParameters(contextData), Boolean.TRUE, null, asParameters(prefetchData), dataEndpoint, 
				terminologyEndpoint, terminologyEndpoint);
			Map<String, Object> cqlResults = pdProcessor.getEvaluatedCqlResults();
			//populate CQL results
			if (cqlResults != null) {
				for (String cqlResultKey : cqlResults.keySet()) {
					results.put("cql_" + cqlResultKey, cqlResults.get(cqlResultKey));
				}
			}
			LOG.debug("PlanDefinitionProcessor apply call done");
			List<Card> cards = convert(carePlan);
			results.put("cards", cards);
			results.put("cardsJson", new Gson().toJson(cards));
		} catch (RuntimeException e) {
			e.printStackTrace();
			LOG.warn("PlanDefinitionProcessor execution threw a RuntimeException. Returning error output", e);
			results.put("error", e.getMessage() == null ? "null message" : e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			LOG.warn("PlanDefinitionProcessor execution threw an Exception. Returning error output", e);
			results.put("error", e.getMessage() == null ? "null message" : e.getMessage());
		} finally {
			LOG.info("Exiting PlanDefinition WIH");
			manager.completeWorkItem(workItem.getStringId(), results);
		}
	}
	
	private IBaseParameters asParameters(Map<String, Object> prefetchData) {
		Parameters retval = new Parameters();
		if (prefetchData != null) {
			for (Map.Entry<String, Object> entry : prefetchData.entrySet()) {
				if (entry.getValue() instanceof Collection) {
					//enter each element as a different ParameterComponent with same name
					Collection<?> col = (Collection<?>) entry.getValue();
					Extension extension = new Extension();
					extension.setUrl("http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-parameterDefinition");
					ParameterDefinition paramDef = new ParameterDefinition();
					paramDef.setMin(0).setMax(String.valueOf(col.size() < 2 ? 2 : col.size())).setType("item");
					extension.setValue(paramDef);
					for (Object item : col) {
						Map<String, Object> hmap = new HashMap<>();
						hmap.put(entry.getKey(), item);
						Map.Entry<String, Object> mockEntry = hmap.entrySet().iterator().next();
						addSingleParam(retval, mockEntry, extension);
					}
				} else {
					addSingleParam(retval, entry, null);
				}
			}
		}
		return retval;
	}

	private void addSingleParam(Parameters retval, Map.Entry<String, Object> entry, Extension extension) {
		Parameters.ParametersParameterComponent  param = null;
		if (entry.getValue() instanceof Resource || entry.getValue() instanceof String || entry.getValue() instanceof Number || entry.getValue() instanceof Boolean || entry.getValue() instanceof Date) {
			param = retval.addParameter();
			param.setName(entry.getKey());
			if (extension != null) {
				param.addExtension(extension);
			}
		}
		if (entry.getValue() instanceof Resource) {
			param.setResource((Resource) entry.getValue());
		} else if (entry.getValue() instanceof String) {
			param.setValue(new StringType(String.valueOf(entry.getValue())));
		} else if (entry.getValue() instanceof Number) {
			Number n = (Number) entry.getValue();
			if (n.toString().indexOf(".") == -1) {
				//integer
				param.setValue(new IntegerType(n.intValue()));
			} else {
				//decimal
				param.setValue(new DecimalType(n.doubleValue()));
			}
		} else if (entry.getValue() instanceof Boolean) {
			//boolean
			param.setValue(new BooleanType(entry.getValue().toString()));
		} else if (entry.getValue() instanceof Date) {
			//datetime
			param.setValue(new DateTimeType((Date) entry.getValue()));
		} else {
			//log parameter type not handled: type
			if (entry.getValue() == null ) {
				LOG.warn("Parameter type not supported for key '" + entry.getKey() + "': null");
			} else {
				LOG.warn("Parameter type not supported for key '" + entry.getKey() + "': " + entry.getValue().getClass().getName());
			}
		}

	}

	private List<Card> convert(CarePlan carePlan) throws ParseException, ReflectiveOperationException {
		for (CarePlan.CarePlanActivityComponent activity : carePlan.getActivity()) {
			Resource refTarget = activity.getReferenceTarget();
			if (refTarget == null) {
				Reference ref = activity.getReference();
				if (ref != null) {
					IBaseResource res = ref.getResource();
					if (res != null && res instanceof RequestGroup) {
						activity.setReferenceTarget((Resource) res);
					}
				}
			}
		}
		return CardCreator.convert(carePlan);
	}
	
	private IGenericClient initClient(String fhirServerUrl, String auth, Map<String, String> headerData) throws ReflectiveOperationException {
		GenericClient fhirClient = (GenericClient) this.ctx.newRestfulGenericClient(fhirServerUrl);
		fhirClient.setDontValidateConformance(true);
		fhirClient.setEncoding(EncodingEnum.JSON);
		if (auth != null && !"".equals(auth.trim())) {
			fhirClient.registerInterceptor(new SimpleRequestHeaderInterceptor("Authorization", auth));
		} 
		if (headerData != null && !headerData.isEmpty()) {
			for (String key : headerData.keySet()) {
				String value = headerData.get(key);
				fhirClient.registerInterceptor(new SimpleRequestHeaderInterceptor(key, value));
			}
		}
		return fhirClient;
	}

	@Override
	public void abortWorkItem(KogitoWorkItem workItem, KogitoWorkItemManager manager) {
		// do nothing
	}
}
