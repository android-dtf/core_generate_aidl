package com.jakev.genaidl;

import java.util.Arrays;
import java.io.File;

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

    /* From AOSP : dalvik/tools/dexdeps/src/com/android/dexdeps/Output.java */
    static String primitiveTypeLabel(char typeChar) {
        /* primitive type; substitute human-readable name in */
        switch (typeChar) {
            case 'B':   return "byte";
            case 'C':   return "char";
            case 'D':   return "double";
            case 'F':   return "float";
            case 'I':   return "int";
            case 'J':   return "long";
            case 'S':   return "short";
            case 'V':   return "void";
            case 'Z':   return "boolean";
            default:
                /* huh? */
                System.err.println("Unexpected class char " + typeChar);
                assert false;
                return "UNKNOWN";
        }
    }

    static String descriptorToDot(String descr) {
        int targetLen = descr.length();
        int offset = 0;
        int arrayDepth = 0;

        if (descr == null) {
            return null;
        }

        /* strip leading [s; will be added to end */
        while (targetLen > 1 && descr.charAt(offset) == '[') {
            offset++;
            targetLen--;
        }
        arrayDepth = offset;

        if (targetLen == 1) {
            descr = primitiveTypeLabel(descr.charAt(offset));
            offset = 0;
            targetLen = descr.length();
        } else {
            /* account for leading 'L' and trailing ';' */
            if (targetLen >= 2 && descr.charAt(offset) == 'L' &&
                descr.charAt(offset+targetLen-1) == ';')
            {
                targetLen -= 2;     /* two fewer chars to copy */
                offset++;           /* skip the 'L' */
            }
        }

        char[] buf = new char[targetLen + arrayDepth * 2];

        /* copy class name over */
        int i;
        for (i = 0; i < targetLen; i++) {
            char ch = descr.charAt(offset + i);
            buf[i] = (ch == '/' || ch == '$') ? '.' : ch;
        }

        /* add the appopriate number of brackets for arrays */
        while (arrayDepth-- > 0) {
            buf[i++] = '[';
            buf[i++] = ']';
        }
        assert i == buf.length;

        return new String(buf);
    }
    /* End from AOSP */

    static boolean isFile(String filePathString) {

        File f = new File(filePathString);
        if(f.exists() && !f.isDirectory()) {
            return true;
        } else {
            return false;
        }
    }
}
