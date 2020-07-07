/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License") +  you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package cz.b2b.jcl.util;

import java.io.*;
import java.util.*;
import javax.tools.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 Class used to dynamically compile Java classes.
 @author Richard Kotal &#60;richard.kotal@b2b.cz&#620;
 */
public class JavaRTCompiler {

//    private static final String TMP_DIR = System.getProperty("java.io.tmpdir");
    private static final String JAVA_CLASS_PATH = System.getProperty("java.class.path");

    private static final Logger logger = LoggerFactory.getLogger(JavaRTCompiler.class);

    /**
     Compile class from file.
     <p>
     Setting for StandardLocation.CLASS_OUTPUT does not work!

     @param path Path to access to java source file or java file (file with
     .java suffix)
     @param classname The class name
     @param packageName The package defined for the class
     @param extraLib additional libraries path
     @return path to compiled class file. Same as path for source code. Only
     suffix is changed from .java to .class
     @throws java.io.IOException Throw if location CLASS_PATH is an output location and path does not represent an existing directory
     */
    @SuppressWarnings("unchecked")
    public static String fileCompile(
            String path,
            String classname,
            String packageName,
            String[] extraLib
    ) throws IOException  {
        String java_file = null;
        String class_file = null;
        // String tmp_dir = TMP_DIR + java.io.File.separatorChar;
        StandardJavaFileManager fileManager = null;
        try {

            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            fileManager = compiler.getStandardFileManager(diagnostics, null, null);

            fileManager.setLocation(StandardLocation.CLASS_PATH, classPath(extraLib));
            /* !!!! This is not work !!!! */
 /*
            java.io.File outputdir = new java.io.File(tmp_dir);
            if (!outputdir.exists()) {
                outputdir.mkdir();
            }

            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(outputdir));
             */
            java_file = null;
            if (path == null) {
                return null;
            }
            if (path.endsWith(CONST.JAVA_SUFFIX) == true) {
                java_file = path;
            } else {
                java_file = path + java.io.File.separatorChar + classname + CONST.JAVA_SUFFIX;

            }
            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromStrings(
                    Arrays.asList(java_file)
            );
            JavaCompiler.CompilationTask task = compiler.getTask(
                    null,
                    fileManager,
                    diagnostics,
                    null,
                    null,
                    compilationUnits
            );

            boolean success = task.call();

            if (!success) {
                if (diagnostics.getDiagnostics() != null) {
                    logger.error(diagnostics.getDiagnostics().toString());
                }
                return null;
            }

        } finally {
            if (fileManager != null) {
                fileManager.close();
            }

        }
        if (java_file == null) {
            return null;
        }
        class_file = java_file.replace(CONST.JAVA_SUFFIX, CONST.CLASS_SUFFIX);
        return class_file;

    }

    private static Iterable<? extends java.io.File> classPath(String[] extraLib) {

        List<java.io.File> out = new ArrayList<>();

        String class_path = JAVA_CLASS_PATH;
        if (class_path == null || class_path.equals("") == true) {
            class_path = ".";
        }
        //add standard path
        for (String item : class_path.split(java.io.File.pathSeparator)) {
            if (item == null || item.equals("") == true) {
                item = ".";
            }
            out.add(new java.io.File(item));
        }

        if (extraLib != null) {
            for (String item : extraLib) {
                if (item == null || item.equals("") == true) {
                    item = ".";
                }
                out.add(new java.io.File(item));
            }

        }

        return out;

    }

}
