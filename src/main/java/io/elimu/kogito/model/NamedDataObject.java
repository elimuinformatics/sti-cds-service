package io.elimu.kogito.model;

import java.util.Objects;

public class NamedDataObject {

	private String name;
	private Object value;
	
	public NamedDataObject() {
	}

	public NamedDataObject(String name, Object value) {
		super();
		this.name = name;
		this.value = value;
	}

	@Override
	public String toString() {
		return "NamedDataObject [name=" + name + ", value=" + value + "]";
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NamedDataObject other = (NamedDataObject) obj;
		return Objects.equals(name, other.name) && Objects.equals(value, other.value);
	}
	
	
}
