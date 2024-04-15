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
