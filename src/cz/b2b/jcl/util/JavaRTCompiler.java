package cz.b2b.jcl.util;

import java.io.*;
import java.util.*;
import javax.tools.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class used to dynamically compile Java classes. Let's go with some magic in
 * Java.
 *
 */
public class JavaRTCompiler {

//    private static final String TMP_DIR = System.getProperty("java.io.tmpdir");
    private static final String JAVA_CLASS_PATH = System.getProperty("java.class.path");

    private static final Logger logger = LoggerFactory.getLogger(JavaRTCompiler.class);

    /**
     * Compile class from file
     *
     * @param path Path to access to java source file or java file (file with
     * .java suffix)
     * @param classname The class name
     * @param packageName The package defined for the class
     * @param extraLib add extra libraries or path with libraries
     * @return path to compiled class file
     * @throws java.io.IOException
     */
    @SuppressWarnings("unchecked")
    public static String fileCompile(
            String path,
            String classname,
            String packageName,
            String[] extraLib
    ) throws IOException {
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
            if (path.endsWith(cz.b2b.jcl.RAM.CacheClassLoader.JAVA_SUFFIX) == true) {
                java_file = path;
            } else {
                java_file = path + java.io.File.separatorChar + classname + cz.b2b.jcl.RAM.CacheClassLoader.JAVA_SUFFIX;

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
        class_file = java_file.replace(cz.b2b.jcl.RAM.CacheClassLoader.JAVA_SUFFIX, cz.b2b.jcl.RAM.CacheClassLoader.CLASS_SUFFIX);
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
