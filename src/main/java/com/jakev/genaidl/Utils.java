package com.jakev.genaidl;

import java.util.Arrays;

public class Utils {

    public static String getShort(String className) {

        try {
            String[] bits = className.split("\\.");
            String lastOne = bits[bits.length-1];
            return lastOne;
        } catch (ArrayIndexOutOfBoundsException e) {
            return className;
        }
    }

    public static String getPackage(String className) {

        return className.substring(0, className.lastIndexOf("."));
    }

    public static boolean needsImport(String className) {

        /* Prims */
        if (Arrays.asList("byte", "char", "double", "float",
                          "int", "long", "short", "void", "boolean")
                                                        .contains(className)) {
            return false;
        }
        
        /* String, CharSequence */
        else if (Arrays.asList("java.lang.String",
                               "java.lang.CharSequence").contains(className)) {
            return false;
        }

        /* Maps and Lists */
        else if (className.startsWith("java.util.Map") ||
                        className.startsWith("java.util.List")) {
            return false;
        }
        else {
            return true;
        }
    }
}
