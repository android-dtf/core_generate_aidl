/*
 * Android DEX to Sqlite3 DB utility
 * Copyright 2013-2014 Jake Valletta (@jake_valletta)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jakev.genaidl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;


import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.ParseException;

import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.DexBackedClassDef;
import org.jf.dexlib2.dexbacked.DexBackedMethod;
import org.jf.dexlib2.DexFileFactory;

import org.jf.dexlib2.iface.MethodParameter;

public class App {

    private static final String gProgramName = "GenerateAIDL";
    private static final String gCmdName = "generateaidl";
    private static final String gProgramVersion = "1.0";

    private static DexBackedDexFile gDexFile = null;

    private static final String IINTERFACE_CLASS = "android.os.IInterface";

    private static final String AS_BINDER_METHOD_NAME = "asBinder";
    private static final String GET_INT_DESC_METHOD_NAME = "getInterfaceDescriptor";

    private static Options gOptions = new Options();
    private static boolean gDebug = false;

    private static void usage() {

        HelpFormatter formatter = new HelpFormatter();

        System.out.println(gProgramName + " v" + gProgramVersion + " Command Line Utility");
        formatter.printHelp(gCmdName, gOptions);
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

    private static boolean isFile(String filePathString) {

        File f = new File(filePathString);
        if(f.exists() && !f.isDirectory()) {
            return true;
        } else {
            return false;
        }
    }

    private static DexBackedClassDef getStubProxy(
                                Set<? extends DexBackedClassDef> classDefs,
                                String stubProxyName) {

        for (DexBackedClassDef classDef: classDefs) {

            String className = descriptorToDot(classDef.getType());

            if (className.equals(stubProxyName)) {
                return classDef;
            }
        }

        return null;
    }

    private static int processDex(String outputDirectory) {

        int rtn = 0;
        int i = 0;

        Set<? extends DexBackedClassDef> classDefs = gDexFile.getClasses();

        /* Find all IInterfaces first */
        for (DexBackedClassDef classDef: classDefs) {

            String className = descriptorToDot(classDef.getType());

            /* No support AIDL */
            if (className.startsWith("android.support")) {
                continue;
            }

            SortedSet<String> interfaces = new TreeSet(classDef.getInterfaces());
            if (interfaces.size() != 1) {
                continue;           
            }

            if (descriptorToDot(interfaces.first()).equals(IINTERFACE_CLASS)) {

                /* Now grab the Stub.Proxy, to get the protocols */
                String stubProxyName = className + ".Stub.Proxy";
                DexBackedClassDef stubProxyDef = getStubProxy(classDefs, stubProxyName);
                if (stubProxyDef == null) {
                    System.err.println("[ERROR] Unable to find Stub.Proxy for class: "
                                                            + stubProxyName + ", Skiping!");
                    continue;
                }

                AidlFile aidl = new AidlFile(className, outputDirectory);

                String shortClassName = Utils.getShort(className);

                /* Parse methods */
                for (DexBackedMethod method : stubProxyDef.getVirtualMethods()) {

                    String methodName = method.getName();

                    if (methodName.equals(GET_INT_DESC_METHOD_NAME) ||
                            methodName.equals(AS_BINDER_METHOD_NAME)) {
                        continue;
                    }

                    String returnType = descriptorToDot(method.getReturnType());

                    /* Try to add returnType to imports */
                    aidl.addImport(returnType);

                    String shortReturnType = Utils.getShort(returnType);
                    StringBuilder paramStringBuilder = new StringBuilder();

                    int paramOffset = 0;

                    for (MethodParameter param : method.getParameters()) {

                        String dottedName = descriptorToDot(param.getType());

                        /* Try to add returnType to imports */
                        aidl.addImport(dottedName);

                        String shortName = Utils.getShort(dottedName);

                        String argName = "";
                        /* Is the name saved? */
                        if (param.getName() != null) {
                            argName = param.getName();
                        } else {
                            argName = "arg" + Integer.toString(paramOffset);
                        }

                        paramStringBuilder.append(shortName + " " + argName + ", ");
                        paramOffset++;
                    }

                    String paramString = paramStringBuilder.toString().replaceAll(",\\s$", "");                    
                    /* Let's build the import list */
                    aidl.addMethod("    " + shortReturnType + " " + methodName + "(" + paramString + ");");


                }

                /* Write it out. */
                aidl.writeFile();
            }
        }

        return rtn;
    }

    public static void main(String[] args) {

        int rtn = 0;
    
        String dexFileName = "";
        String dexDbName = "";
        String outputDirectory = ".";
        int sdkVersion = 23;

        CommandLineParser parser = new BasicParser();
        CommandLine cmd = null;

        gOptions.addOption("a", true, "Android API level to use.");
        gOptions.addOption("d", false, "Show debugging information.");
        gOptions.addOption("h", false, "Show help screen.");
        gOptions.addOption("i", true, "Input DEX/ODEX file.");
        gOptions.addOption("o", true, "Output directory for AIDL files.");

        try {
            cmd = parser.parse(gOptions, args);

            if (cmd.hasOption("h")) {
                usage();
                System.exit(0);
            }

            if (cmd.hasOption("d"))
                gDebug = true;

            if (cmd.hasOption("a")) {
                try {
                    sdkVersion = Integer.parseInt(cmd.getOptionValue("a"));
                } catch (NumberFormatException e) {
                    System.err.println("[ERROR] Numeric API level required!");
                    System.exit(-2);
                }
            }

            if (cmd.hasOption("o")) {
                outputDirectory = cmd.getOptionValue("o");
            }

            if (!cmd.hasOption("i")) {
                System.err.println("[ERROR] Input (-i) parameter is required!");
                usage();
                System.exit(-1);
            }

        } catch (ParseException e) {
            System.err.println("[ERROR] Unable to parse command line properties: " + e);
            System.exit(-1);
        }

        dexFileName = cmd.getOptionValue("i");

        if (!isFile(dexFileName)) {
            System.err.println("[ERROR] File '" + dexFileName + "' does not exist!");
            System.exit(-3);
        }

        if (gDebug) { System.out.println("Loading DEX into object."); }
        try {
            gDexFile = DexFileFactory.loadDexFile(dexFileName, sdkVersion, true);
        } catch (IOException e){
            System.err.println("[ERROR] Unable to load DEX file!");
            System.exit(-4);
        }

        if (gDebug) { System.out.println("About to process DEX..."); }
        rtn = processDex(outputDirectory);
        if (rtn != 0) {
            System.err.println("[ERROR] Error processing DEX!");
        }

        /* Close it down. */
        System.exit(rtn);
    }
}
