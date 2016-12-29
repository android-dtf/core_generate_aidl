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
import org.jf.dexlib2.dexbacked.DexBackedField;
import org.jf.dexlib2.dexbacked.DexBackedMethod;
import org.jf.dexlib2.DexFileFactory;

import org.jf.dexlib2.iface.value.IntEncodedValue; 
import org.jf.dexlib2.iface.value.EncodedValue; 

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

    private static DexBackedClassDef getClassDef(
                                Set<? extends DexBackedClassDef> classDefs,
                                String className) {

        for (DexBackedClassDef classDef: classDefs) {

            String matchName = Utils.descriptorToDot(classDef.getType());
            if (matchName.equals(className)) {
                return classDef;
            }
        }

        return null;
    }

    private static DexBackedMethod getMethod(
                                DexBackedClassDef classDef,
                                String methodName) {

         for (DexBackedMethod method : classDef.getVirtualMethods()) {

            String matchName = method.getName();
            if (matchName.equals(methodName)) {
                return method;
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

            String className = Utils.descriptorToDot(classDef.getType());

            /* No support AIDL */
            if (className.startsWith("android.support")) {
                continue;
            }

            SortedSet<String> interfaces = new TreeSet(classDef.getInterfaces());
            if (interfaces.size() != 1) {
                continue;           
            }

            /* Getting here is a valid AIDL (or so we think) */
            if (Utils.descriptorToDot(interfaces.first()).equals(IINTERFACE_CLASS)) {

                /* First find the Stub and get a list of transactions */
                String stubName = className + ".Stub";
                String stubProxyName = className + ".Stub.Proxy";

                DexBackedClassDef stubDef = getClassDef(classDefs, stubName);
                DexBackedClassDef stubProxyDef = getClassDef(classDefs, stubProxyName);

                if (stubDef == null) {
                    System.err.println("[ERROR] Unable to find Stub for class: "
                                                            + stubName + ", Skiping!");
                    continue;
                }

                if (stubProxyDef == null) {
                    System.err.println("[ERROR] Unable to find Stub.Proxy for class: "
                                                            + stubProxyName + ", Skiping!");
                    continue;
                }

                AidlFile aidl = new AidlFile(className, outputDirectory);

                /* Next, we need all the transactions for this. */
                for (DexBackedField field : stubDef.getStaticFields()) {
                    if (field.getName().startsWith("TRANSACTION_")) {

                        String methodName = field.getName().replace("TRANSACTION_", "");

                        /* Transaction ID for sorting */
                        EncodedValue iiev = field.getInitialValue();
                        int transactionId = ((IntEncodedValue) iiev).getValue();

                        /* Get the Stub.Proxy method (which has return + args) */
                        DexBackedMethod method = getMethod(stubProxyDef, methodName);

                        aidl.addMethod(transactionId, methodName, method);
                    }
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

        if (!Utils.isFile(dexFileName)) {
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
