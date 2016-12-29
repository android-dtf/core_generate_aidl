package com.jakev.genaidl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.io.PrintWriter;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.jf.dexlib2.dexbacked.DexBackedMethod;
import org.jf.dexlib2.iface.MethodParameter;

public class AidlFile {

    private List<String> imports;
    private Map<Integer, String> methods;
    private String interfaceName = "";
    private String packageName = "";
    private String outputDirectory = "";

    public AidlFile(String className, String outputDir) {

        this.packageName = Utils.getPackage(className);
        this.interfaceName = Utils.getShort(className);
        this.imports = new ArrayList<String>();
        this.methods = new TreeMap<Integer, String>();
        this.outputDirectory = outputDir;
    }

    private String makeParamString(DexBackedMethod method) {

        StringBuilder paramStringBuilder = new StringBuilder();
        int paramOffset = 0;

        for (MethodParameter param : method.getParameters()) {

            String dottedReturnName = Utils.descriptorToDot(param.getType());

            this.addImport(dottedReturnName);

            String shortName = Utils.getShort(dottedReturnName);

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

        return paramStringBuilder.toString().replaceAll(",\\s$", "");
    }

    public void addMethod(int transactionId, String methodName, DexBackedMethod method) {

        String dottedReturnType = Utils.descriptorToDot(method.getReturnType());

        /* Update imports for return type */
        this.addImport(dottedReturnType);

        String shortReturnType = Utils.getShort(dottedReturnType);

        /* Args */
        String paramString = makeParamString(method);

        String methodString = "    " + shortReturnType + " " + methodName + "(" + paramString + ");";

        /* Add to the array list */
        this.methods.put(transactionId, methodString);
    }

    public void addImport(String methodString) {

        if (Utils.needsImport(methodString) &&
                !this.imports.contains(methodString)) {

            this.imports.add(methodString);
        }
    }

    public int writeFile() {

        String fileName = this.outputDirectory + "/" + this.interfaceName+".aidl";

        System.out.println("Writing: "+fileName);

        /* Create directories above file */
        new File(fileName).getParentFile().mkdirs();

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
        writer.println("package "+this.packageName+";");
        writer.println("");

        /* Imports */
        for (String impt : imports) {
            writer.println("import "+impt+";");
        }
        writer.println("");

        /* Interface */
        writer.println("interface "+this.interfaceName+" {");
        writer.println("");

        for (Map.Entry<Integer,String> entry : this.methods.entrySet()) {

          writer.println(entry.getValue());
          writer.println("");
        }

        writer.println("}");

        writer.flush();
        writer.close();

        return 0;
    }
}
