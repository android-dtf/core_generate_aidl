package com.jakev.genaidl;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.io.PrintWriter;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;


public class AidlFile {

    private List<String> imports;
    private List<String> methods;
    private String interfaceName = "";
    private String packageName = "";
    private String outputDirectory = "";

    public AidlFile(String className, String outputDir) {

        packageName = Utils.getPackage(className);
        interfaceName = Utils.getShort(className);
        imports = new ArrayList<String>();
        methods = new ArrayList<String>();
        outputDirectory = outputDir;

    }

    public void addMethod(String methodString) {

        methods.add(methodString);
    }

    public void addImport(String methodString) {

        if (Utils.needsImport(methodString) &&
                !imports.contains(methodString)) {

            imports.add(methodString);
        }
    }

    public int writeFile() {

        String fileName = outputDirectory + "/" + interfaceName+".aidl";

        System.out.println("Writing: "+fileName);

        PrintWriter writer;

        try {
            writer = new PrintWriter(fileName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            System.err.println("[ERROR] Unable to open file!");
            return -1;
        } catch (FileNotFoundException e) {
            System.err.println("[ERROR] Unable to open file!");
            return -1;
        }

        /* Write the package */
        writer.println("package "+packageName+";");
        writer.println("");

        /* Imports */
        for (String impt : imports) {
            writer.println("import "+impt+";");
        }
        writer.println("");

        /* Interface */
        writer.println("interface "+interfaceName+" {");
        writer.println("");

        for (String method : methods) {
            writer.println(method);
            writer.println("");
        }
        writer.println("}");

        writer.flush();
        writer.close();

        return 0;
    }
}
