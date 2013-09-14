package org.openconcerto.modules.extensionbuilder.table;

public class ForbiddenFieldName {
    public static boolean isAllowed(String field) {
        String f = field.toUpperCase();
        return !(f.equals("ARCHIVE") || f.equals("ORDRE") || f.equals("ID_USER_CREATE") || f.equals("ID_USER_COMMON_CREATE") || f.equals("ID_USER_MODIFY") || f.equals("ID_USER_COMMON_MODIFY")
                || f.equals("MODIFICATION_DATE") || f.equals("CREATION_DATE"));
    }
}
