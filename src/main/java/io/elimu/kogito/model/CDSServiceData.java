package io.elimu.kogito.model;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public class CDSServiceData {

	String id;
	String title;
	String description;
	String hook;
	Map<String, String> prefetch = new HashMap<>();
}
