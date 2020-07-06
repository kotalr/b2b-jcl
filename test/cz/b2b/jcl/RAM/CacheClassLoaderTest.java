/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.b2b.jcl.RAM;

import cz.b2b.jcl.RAM.CacheClassLoader;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.Provider;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import java.lang.reflect.Method;

/**
 *
 * @author richard
 */
public class CacheClassLoaderTest {
    private String HOME_DIR = "/home/richard/NetBeansProjects/O2/b2b-jcl/test/cz/b2b/jcl/RAM/resource";
    
    
    public CacheClassLoaderTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testAddJAR() throws Exception {
        String file_name = HOME_DIR + "/jar" + "/Test3.jar";
        System.out.println("addJAR = " + file_name);
        CacheClassLoader childClassLoader = new CacheClassLoader(Thread.currentThread().getContextClassLoader());
        childClassLoader.addJAR(file_name);
        final Class<?> test = Class.forName("cz.b2b.jcl.RAM.resource.jar.Test3", true, childClassLoader);
        Object o = test.getDeclaredConstructor(new Class[]{}).newInstance(new Object[]{});

        Method print = o.getClass().getMethod("print", String.class);
        System.out.println("class = " + o.getClass().getCanonicalName());
        print.invoke(o, "JAR");



    }

    @Test
    public void testAddDir() throws Exception {
        String dir_name = HOME_DIR;
        System.out.println("addDir (recursive finding jar) = " + dir_name);
        CacheClassLoader childClassLoader = new CacheClassLoader(Thread.currentThread().getContextClassLoader());
        childClassLoader.addDir(dir_name);
        final Class<?> test = Class.forName("cz.b2b.jcl.RAM.resource.jar.Test3", true, childClassLoader);
        Object o = test.getDeclaredConstructor(new Class[]{}).newInstance(new Object[]{});

        Method print = o.getClass().getMethod("print", String.class);
        System.out.println("class = " + o.getClass().getCanonicalName());
        print.invoke(o, "DIR");
    }
    
    
    @Test
    public void testAddClass() throws Exception {
        String path = HOME_DIR + "/class";
        String packageName = "cz.b2b.jcl.RAM.resource";
        String className = "Test";
        String fullClassName = packageName + "." + className;
        System.out.println("addClass, path = " + path + ", package =  " + packageName + ", class = " + className);
        CacheClassLoader childClassLoader = new CacheClassLoader(Thread.currentThread().getContextClassLoader());
        childClassLoader.addClass(path, packageName, className);
        final Class<?> test = Class.forName(fullClassName, true, childClassLoader);
        Object o = test.getDeclaredConstructor(new Class[]{}).newInstance(new Object[]{});

        Method print = o.getClass().getMethod("print", String.class);
        System.out.println("class = " + o.getClass().getCanonicalName());
        print.invoke(o, "CLASS");

    }

    @Test
    public void testAddjava() throws Exception {
        String path = HOME_DIR + "/java";
        String packageName = "cz.b2b.jcl.RAM.resource.java";
        String className = "Test2";
        String fullClassName = packageName + "." + className;
        System.out.println("addJava, path = " + path + ", package =  " + packageName + ", class = " + className);
        CacheClassLoader childClassLoader = new CacheClassLoader(Thread.currentThread().getContextClassLoader());
        childClassLoader.addJava(path, packageName, className,null);
        final Class<?> test = Class.forName(fullClassName, true, childClassLoader);
        Object o = test.getDeclaredConstructor(new Class[]{}).newInstance(new Object[]{});

        Method print = o.getClass().getMethod("print", String.class);
        System.out.println("class = " + o.getClass().getCanonicalName());
        print.invoke(o, "JAVA");
        childClassLoader.close();
        
    }
    
    
}
