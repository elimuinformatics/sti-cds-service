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

import java.util.HashMap;
import java.util.Map;

public class TimeoutMap<K, V> extends HashMap<K, V> {


	private static final long serialVersionUID = 1L;
	
	private final Map<K, Long> timeouts = new HashMap<>();
	private final long maxAgeCache;

	public TimeoutMap(long maxAgeCache) {
		this.maxAgeCache = maxAgeCache;
	}

	@Override
	public V get(Object key) {
		Long startTime = timeouts.get(key);
		if (startTime != null && System.currentTimeMillis() - startTime.longValue() > maxAgeCache) {
			remove(key);
			return null;
		}
		return super.get(key);
	}
	
	@Override
	public V put(K key, V value) {
		timeouts.put(key, System.currentTimeMillis());
		return super.put(key, value);
	}
	
	@Override
	public V putIfAbsent(K key, V value) {
		timeouts.putIfAbsent(key, System.currentTimeMillis());
		return super.putIfAbsent(key, value);
	}
	
	@Override
	public V getOrDefault(Object key, V defaultValue) {
		Long startTime = timeouts.get(key);
		if (startTime != null && System.currentTimeMillis() - startTime.longValue() > maxAgeCache) {
			remove(key);
			return defaultValue;
		}
		return super.getOrDefault(key, defaultValue);
	}
	
	@Override
	public V remove(Object key) {
		timeouts.remove(key);
		return super.remove(key);
	}
}
