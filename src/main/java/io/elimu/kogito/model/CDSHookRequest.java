package io.elimu.kogito.model;

import java.util.Map;

public class CDSHookRequest {

	private String hook;
	private String hookInstance;
	private String fhirServer;
	private FHIRAuthorization fhirAuthorization;
	private String user;
	private Map<String, Object> context;
	private Map<String, Object> prefetch;
	private Map<String, Object> contextResources;
	private Map<String, Object> prefetchResources;

	public String getHook() {
		return hook;
	}

	public void setHook(String hook) {
		this.hook = hook;
	}

	public String getHookInstance() {
		return hookInstance;
	}

	public void setHookInstance(String hookInstance) {
		this.hookInstance = hookInstance;
	}

	public String getFhirServer() {
		return fhirServer;
	}

	public void setFhirServer(String fhirServer) {
		this.fhirServer = fhirServer;
	}

	public FHIRAuthorization getFhirAuthorization() {
		return fhirAuthorization;
	}

	public void setFhirAuthorization(FHIRAuthorization fhirAuthorization) {
		this.fhirAuthorization = fhirAuthorization;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}



	/**
	 * @return the context
	 */
	public Map<String, Object> getContext() {
		return context;
	}

	/**
	 * @param context the context to set
	 */
	public void setContext(Map<String, Object> context) {
		this.context = context;
	}

	/**
	 * @return the prefetch
	 */
	public Map<String, Object> getPrefetch() {
		return prefetch;
	}

	/**
	 * @param prefetch the prefetch to set
	 */
	public void setPrefetch(Map<String, Object> prefetch) {
		this.prefetch = prefetch;
	}

	/**
	 * @return the contextResources
	 */
	public Map<String, Object> getContextResources() {
		return contextResources;
	}

	/**
	 * @param contextResources the contextResources to set
	 */
	public void setContextResources(Map<String, Object> contextResources) {
		this.contextResources = contextResources;
	}

	/**
	 * @return the prefetchResources
	 */
	public Map<String, Object> getPrefetchResources() {
		return prefetchResources;
	}

	/**
	 * @param prefetchResources the prefetchResources to set
	 */
	public void setPrefetchResources(Map<String, Object> prefetchResources) {
		this.prefetchResources = prefetchResources;
	}

}
