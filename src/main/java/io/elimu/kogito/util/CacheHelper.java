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

package io.elimu.kogito.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class CacheHelper {

	private static final CacheHelper INSTANCE = new CacheHelper();
	
	private Map<String, Long> cache = new HashMap<>();
	
	private CacheHelper() {
	}
	
	public static CacheHelper getInstance() {
		return INSTANCE;
	}
	
	public void register(String key, int timeoutInSeconds) {
		long expirationEpoch = System.currentTimeMillis() + (timeoutInSeconds * 1000L);
		cache.put(key, expirationEpoch);
	}

	public boolean containsKey(String key) {
		if (cache.containsKey(key)) {
			if (cache.get(key).longValue() > System.currentTimeMillis()) {
				cache.remove(key);
				return true;
			}
		}
		return false;
	}
	
	public String hashPatient(String patientId) {
		try {
			String salt = System.getenv("SALT"); // this needs to be added
			if (salt == null) {
				salt = "";
			}
			byte[] hashBytes = MessageDigest.getInstance("SHA-256").digest((patientId + salt).getBytes(StandardCharsets.UTF_8));
			StringBuilder hexString = new StringBuilder(2 * hashBytes.length);
			for (byte h : hashBytes) {
				String hex = Integer.toHexString(0xff & h);
				if (hex.length() == 1)
					hexString.append('0');
				hexString.append(hex);
			}
			return hexString.toString();
		} catch (NoSuchAlgorithmException e) {
			//algorithm is part of every JVM so this shouldnt happen
			return patientId;
		}
	}
}
