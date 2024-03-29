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
package cz.b2b.jcl.RAM;

import java.net.*;
import java.util.*;
import java.util.jar.*;
import java.io.*;
import org.slf4j.*;
import cz.b2b.jcl.util.CONST;
import cz.b2b.jcl.util.ConcurrentSoftHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The CacheClassLoader class implements a class loader that loads classes from
 * jar file, class file java file, directories with jar files and stores these
 * in memory cache.
 * <p>
 * The code of individual classes is stored in the RAM cache and it is possible
 * to exchange source files without having to restart the entire application.
 * Simply change the appropriate file. Creates a new classloader that loads
 * these new resources and uses them later. The old one can either be forgotten
 * or used with the original code. Supported code sources:
 * <p>
 * - jar file
 * <p>
 * - directory (The directory is crawled recursively)
 * <p>
 * - .class file (byte code)
 * <p>
 * - .java file (source code)
 * <p>
 * Examples of usage:
 * <pre>
 * CacheClassLoader childClassLoader = new CacheClassLoader(Thread.currentThread().getContextClassLoader());
 *
 * String file_name = "/tmp/Test3.jar";
 * System.out.println("addJAR = " + file_name);
 * childClassLoader.addJAR(file_name);
 *
 * file_name = "/tmp/Test.jar";
 * childClassLoader.addJAR(file_name);
 * System.out.println("addJAR = " + file_name);
 *
 * final Class&#60;?&#62; test = Class.forName("cz.b2b.jcl.RAM.resource.jar.Test3", true, childClassLoader);
 * Object o = test.getDeclaredConstructor(new Class[]{}).newInstance(new Object[]{});
 *
 * Method print = o.getClass().getMethod("print", String.class);
 * System.out.println("class = " + o.getClass().getCanonicalName());
 * print.invoke(o, "JAR");
 *
 * </pre>
 * <pre>
 * String path = "/tmp/class";
 * String packageName = "cz.b2b.jcl.RAM.resource";
 * String className = "Test";
 * String fullClassName = packageName + "." + className;
 * System.out.println("addClass, path = " + path + ", package =  " + packageName + ", class = " + className);
 * CacheClassLoader childClassLoader = new CacheClassLoader(Thread.currentThread().getContextClassLoader());
 * childClassLoader.addClass(path, packageName, className);
 * final Class&#60;?&#62; test = Class.forName(fullClassName, true, childClassLoader);
 * Object o = test.getDeclaredConstructor(new Class[]{}).newInstance(new Object[]{});
 *
 * Method print = o.getClass().getMethod("print", String.class);
 * System.out.println("class = " + o.getClass().getCanonicalName());
 * print.invoke(o, "CLASS");
 * </pre>
 *
 * @author Richard Kotal &#60;richard.kotal@b2b.cz&#620;
 */
public class CacheClassLoader extends URLClassLoader {

    private final static String protocol = "x-mem-cache";
    private static final Logger logger = LoggerFactory.getLogger(CacheClassLoader.class);

    private final Map<String, byte[]> CACHE;
    private URL cacheURL = null;
    private final List<String> jars = new CopyOnWriteArrayList<>();

    private boolean loadAllJar = false;

    /**
     * Constructs a new CacheClassLoader for the given URLs of URLClassLoader
     * and the MEM cache stream protocol handler.
     * <p>
     * The url for the MEM cache stream protocol handler is added to the others
     * when the constructor is created.
     *
     * @param urls the Standard URLClassLoader URLs from which to load classes
     * and resources. The URLs will be searched in the order specified for
     * classes and resources after first searching in the specified parent class
     * loader.
     * @param parent the parent class loader for delegation
     * @param hardSize The number of "hard" references of class code to hold
     * internally. If equal -1 all references are still held internally.
     * @param softRef enable ConcurrentSoftHashMap for class byte representation
     * @param loadAllJar Allows loading of the entire spring content. Otherwise,
     * only the required class is loaded. Reduces memory requirements, can
     * significantly reduce loading speed.
     * @throws MalformedURLException Thrown to indicate that a malformed URL has
     * occurred. Either no legal protocol could be found in a specification
     * string or the string could not be parsed.
     */
    public CacheClassLoader(URL[] urls, ClassLoader parent, int hardSize, boolean softRef, boolean loadAllJar) throws MalformedURLException {
        super(urls, parent);

        if (softRef == true) {
            CACHE = new ConcurrentSoftHashMap<>(hardSize);
        } else {
            CACHE = new ConcurrentHashMap<>();
        }
        this.loadAllJar = loadAllJar;
        cacheURL = new URL(protocol, CONST.host, CONST.port, CONST.baseURI, new CacheURLStreamHandler());

        super.addURL(cacheURL);
    }

    /**
     * Constructs a new CacheClassLoader for the MEM cache stream protocol
     * handler.
     * <p>
     * The url for the MEM cache stream protocol handler is added when the
     * constructor is created.
     *
     * @param parent the parent class loader for delegation
     * @throws MalformedURLException Thrown to indicate that a malformed URL has
     * occurred. Either no legal protocol could be found in a specification
     * string or the string could not be parsed.
     */
    public CacheClassLoader(ClassLoader parent) throws MalformedURLException {
        this(new URL[]{}, parent, -1, false, false);
    }

    public CacheClassLoader(ClassLoader parent, boolean loadAllJar) throws MalformedURLException {
        this(new URL[]{}, parent, -1, false, loadAllJar);
    }

    @Override
    public void close() throws IOException {

        CACHE.clear();
        jars.clear();
        super.close();

    }

    /**
     * Add JAR file to CacheClassLoader URL path.
     *
     * @param jar jar file (ex.: /tmp/test.jar)
     * @throws IOException
     */
    public void addJAR(String jar) throws IOException {

        if (loadAllJar == true) {
            add_code(jar, null);
        } else {
            jars.add(jar);
        }

    }

    /**
     * Add java class byte code (.class file) to CacheClassLoader URL path.
     *
     * @param path Path where class file is located (ex.: /tmp)
     * @param packageName Package name of the class (ex.:
     * cz.b2b.jcl.RAM.resource)
     * @param className Class name (ex.: Test)
     * @throws IOException
     */
    public void addClass(String path, String packageName, String className) throws IOException {
        String class_name = path + java.io.File.separatorChar + className + CONST.CLASS_SUFFIX;
        add_class(class_name, packageName, className);
    }

    /**
     * Add java class source code (.java file) to CacheClassLoader URL path.
     * <p>
     * The source code is compiled dynamically at runtime.
     *
     * @param path Path where class file is located (ex.: /tmp)
     * @param packageName Package name of the class (ex.:
     * cz.b2b.jcl.RAM.resource)
     * @param className Class name (ex.: Test)
     * @param extraLib Fields of any additional resources (libraries) needed
     * during compilation
     * @throws IOException
     */
    public void addJava(String path, String packageName, String className, String[] extraLib) throws IOException {
        String class_name = cz.b2b.jcl.util.JavaRTCompiler.fileCompile(path, className, packageName, extraLib);
        if (class_name == null) {
            throw new FileNotFoundException("Java file does not exist.");
        }
        add_class(class_name, packageName, className);
        File fi = new File(class_name);
        if (fi != null) {
            fi.delete();
        }

    }

    /**
     * Adds jar files contained in the given directory and subdirectories to the
     * CacheClassLoader URL path.
     * <p>
     * The directory is crawled recursively.
     *
     * @param directory Directory with jar files (ex.: /tmp)
     * @throws IOException
     */
    public void addDir(String directory) throws IOException {
        if (directory == null) {
            throw new FileNotFoundException("Directory name is empty.");
        }

        List<File> jars = Arrays.asList(new File(directory).listFiles());
        if (jars == null) {
            logger.debug("Directory " + directory + " does not contain files.");
            return;
        }
        for (File jar : jars) {
            logger.debug("Jar file = " + jar);
            if (jar == null) {
                continue;
            }
            if (jar.isDirectory() == true) {
                addDir(jar.getAbsolutePath());
                return;
            }

            addJAR(jar.getAbsolutePath());
        }

    }

    /**
     * Adds jar files contained in the given directories and subdirectories to
     * the CacheClassLoader URL path.
     * <p>
     * The directories are crawled recursively.
     * <p>
     * The individual directories are separated by a specified delimiter.
     *
     * @param directory List of directories with jar files separated by
     * directorySeparator (ex.: /tmp;/home;/root)
     * @param directorySeparator Directory list delimiter (ex.: ;)
     * @throws IOException
     */
    public void addDir(String directory, String directorySeparator) throws IOException {
        if (directory == null) {
            throw new FileNotFoundException("Directory name is empty.");
        }

        String[] dirs = directory.split(directorySeparator);
        if (dirs == null) {
            throw new FileNotFoundException("Directories name are empty.");
        }

        for (String dir : dirs) {
            logger.debug("Directory = " + dir);

            if (dir == null) {
                continue;
            }
            addDir(dir);
        }

    }

    /**
     * Special MEM cache stream protocol handler knows how to make a connection
     * for the protocol type x-mem-cache.
     * <p>
     * This handler loads jar, class, java files and jar files contained in
     * directories into the RAM cache.
     */
    private class CacheURLStreamHandler extends URLStreamHandler {

        @Override
        protected URLConnection openConnection(URL url) throws IOException {
            return new CacheURLConnection(url);
        }

    }

    private class CacheURLConnection extends URLConnection {

        public CacheURLConnection(URL url) {
            super(url);
        }

        @Override
        public void connect() throws IOException {
        }

        @Override
        public InputStream getInputStream() throws IOException {
            String file_name = url.getFile();

            byte[] data = CACHE.get(file_name);

            if (loadAllJar != true) {
                if (data == null) {
                    add_code(file_name);
                }

                data = CACHE.get(file_name);
            }

            if (data == null) {
                throw new FileNotFoundException(file_name);
            }

            return new ByteArrayInputStream(data);
        }

    }

    private boolean add_code(String jar, String file_name) throws IOException {
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        JarInputStream jis = null;
        ByteArrayOutputStream out = null;
        String name = null;
        byte[] b = new byte[CONST.BUFFER_SIZE];
        int len = 0;

        if (jar == null) {
            throw new FileNotFoundException("Jar file is empty.");
        }

        try {
            fis = new FileInputStream(jar);
            bis = new BufferedInputStream(fis);
            jis = new JarInputStream(bis);

            JarEntry jarEntry = null;
            while ((jarEntry = jis.getNextJarEntry()) != null) {
                name = CONST.baseURI + jarEntry.getName();

                if (jarEntry.isDirectory()) {
                    logger.debug("Ignoring directory " + name + CONST.DOT);
                    continue;
                }

                if (CACHE.containsKey(name)) {
                    logger.debug("Class/Resource " + name + " already loaded; ignoring entry...");
                    continue;
                }

                
                
                if (loadAllJar != true && file_name.equals(name) == false) {
                    continue;
                }

                out = new ByteArrayOutputStream();

                while ((len = jis.read(b)) > 0) {
                    out.write(b, 0, len);
                }

                logger.debug("Jar entry = " + name);

                CACHE.put(name, out.toByteArray());
                out.close();
                if (loadAllJar != true) {
                    return true;
                }

            }
        } finally {
            if (jis != null) {
                jis.close();
            }
            if (bis != null) {
                bis.close();
            }
            if (fis != null) {
                fis.close();
            }

        }
        return false;
    }

    private void add_code(String file_name) throws IOException {

        for (String jar : jars) {

            if (jar == null) {
                continue;
            }
            if (add_code(jar, file_name) == true) {
                return;
            }
        }

    }

    private void add_class(String class_name, String packageName, String className) throws IOException {
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        ByteArrayOutputStream out = null;
        byte[] b = new byte[CONST.BUFFER_SIZE];
        int len = 0;
        String name = null;

        if (class_name == null) {
            throw new FileNotFoundException("Class file is empty.");
        }
        if (packageName == null) {
            throw new FileNotFoundException("Package name is empty.");
        }

        name = CONST.baseURI + packageName.replace(CONST.DOT, CONST.baseURI) + CONST.baseURI + className + CONST.CLASS_SUFFIX;
        if (CACHE.containsKey(name)) {
            logger.debug("Class/Resource " + name + " already loaded; ignoring entry...");
            return;
        }

        try {
            fis = new FileInputStream(class_name);
            bis = new BufferedInputStream(fis);

            out = new ByteArrayOutputStream();

            while ((len = bis.read(b)) > 0) {
                out.write(b, 0, len);
            }
            CACHE.put(name, out.toByteArray());

            out.close();
        } finally {
            if (bis != null) {
                bis.close();
            }
            if (fis != null) {
                fis.close();
            }

        }
    }

}
