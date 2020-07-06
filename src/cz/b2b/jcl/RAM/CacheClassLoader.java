/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.b2b.jcl.RAM;

import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Arrays;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.io.*;
import java.net.MalformedURLException;
import java.util.concurrent.ConcurrentHashMap;
import java.net.URLClassLoader;
import java.net.URL;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author richard
 */
public class CacheClassLoader extends URLClassLoader {

    private final static String protocol = "x-mem-cache";
    private final static String host = null;
    private final static int port = -1;
    private final static String baseURI = "/";
    private static final int BUFFER_SIZE = 8192;
    public static final String CLASS_SUFFIX = ".class";
    public static final String JAVA_SUFFIX = ".java";

    private final Map<String, byte[]> CACHE = new ConcurrentHashMap<>();
    private final URL cacheURL = new URL(protocol, host, port, baseURI, new CacheURLStreamHandler());
    private final Logger logger = LoggerFactory.getLogger(CacheClassLoader.class);

    public CacheClassLoader(URL[] urls, ClassLoader parent) throws MalformedURLException {
        super(urls, parent);
        super.addURL(cacheURL);
    }

    public CacheClassLoader(ClassLoader parent) throws MalformedURLException {
        this(new URL[]{}, parent);
    }

    @Override
    public void close() throws IOException {
        CACHE.clear();
        super.close();

    }

    public void addJAR(String jar) throws IOException {
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        JarInputStream jis = null;
        ByteArrayOutputStream out = null;
        String name = null;
        byte[] b = new byte[BUFFER_SIZE];
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

                if (jarEntry.isDirectory()) {
                    logger.debug("Ignoring directory " + jarEntry.getName() + ".");
                    continue;
                }

                if (CACHE.containsKey(baseURI + jarEntry.getName())) {
                    logger.debug("Class/Resource " + jarEntry.getName() + " already loaded; ignoring entry...");
                    continue;
                }

                out = new ByteArrayOutputStream();

                while ((len = jis.read(b)) > 0) {
                    out.write(b, 0, len);
                }
                name = jarEntry.getName();
                logger.debug("Jar entry = " + name);

                CACHE.put(baseURI + name, out.toByteArray());

                out.close();
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
    }

    public void addClass(String path, String packageName, String className) throws IOException {
        String class_name = path + java.io.File.separatorChar + className + CLASS_SUFFIX;
        add_class(class_name, packageName, className);
    }

    public void addJava(String path, String packageName, String className, String[] extraLib) throws IOException {
        String class_name = cz.b2b.jcl.util.JavaRTCompiler.fileCompile(path, className, packageName, extraLib);
        if (class_name == null) {
            return;
        }
        add_class(class_name, packageName, className);
        File fi = new File(class_name);
        if (fi != null) {
            fi.delete();
        }
        

    }

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

    protected class CacheURLStreamHandler extends URLStreamHandler {

        @Override
        protected URLConnection openConnection(URL url) throws IOException {
            final byte[] data = CACHE.get(url.getFile());
            if (data == null) {
                throw new FileNotFoundException(url.getFile());
            }
            return new URLConnection(url) {
                @Override
                public void connect() throws IOException {
                }

                @Override
                public InputStream getInputStream() throws IOException {
                    return new ByteArrayInputStream(data);
                }
            };
        }

    }

    private void add_class(String class_name, String packageName, String className) throws IOException {
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        ByteArrayOutputStream out = null;
        byte[] b = new byte[BUFFER_SIZE];
        int len = 0;
        String name = null;

        if (class_name == null) {
            throw new FileNotFoundException("Class file is empty.");
        }
        if (packageName == null) {
            throw new FileNotFoundException("Package name is empty.");
        }

        name = packageName.replace(".", "/") + "/" + className + CLASS_SUFFIX;

        try {
            fis = new FileInputStream(class_name);
            bis = new BufferedInputStream(fis);

            out = new ByteArrayOutputStream();

            while ((len = bis.read(b)) > 0) {
                out.write(b, 0, len);
            }

            CACHE.put(baseURI + name, out.toByteArray());

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
