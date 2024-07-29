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

public class RuleHelper {

    public static String getResourceType(String selectedOrder) {
        if (selectedOrder == null) {
            return null;
        }
        String[] parts = selectedOrder.split("/");
        if (parts.length==2) {
            return parts[0];
        }
        return null;
    }

    public static String getResourceId(String selectedOrder) {
        if (selectedOrder == null) {
            return null;
        }
        String[] parts = selectedOrder.split("/");
        if (parts.length==2) {
            return parts[1];
        }
        return null;
    }
}
