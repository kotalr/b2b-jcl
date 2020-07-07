/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.b2b.jcl.DB;

import cz.b2b.jcl.RAM.CacheClassLoader;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import java.lang.reflect.Method;

/**
 *
 * @author richard
 */
public class JdbcClassLoaderTest {

    public JdbcClassLoaderTest() {
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
    public void testJDBC() throws Exception {

        String driver = "org.mariadb.jdbc.Driver";
        String dbUrl = "jdbc:mariadb://127.0.0.1:3306/test";
        String table = "jcl_db_jdbc";
        String username = "root";
        String password = "root";
        System.out.println("jdbc :: drive = " + driver + ", dbUrl = " + dbUrl + ", table = " + table + ", username = " + username + ", password = " + password);

        JdbcClassLoader childClassLoader = new JdbcClassLoader(Thread.currentThread().getContextClassLoader());
        childClassLoader.setConnection(driver, dbUrl, table, username, password);

        Class<?> test = Class.forName("cz.b2b.jcl.RAM.resource.Test", true, childClassLoader);
        Object o = test.getDeclaredConstructor(new Class[]{}).newInstance(new Object[]{});

        Method print = o.getClass().getMethod("print", String.class);
        System.out.println("class = " + o.getClass().getCanonicalName());
        print.invoke(o, "JDBC");

    }


    @Test
    public void testDelegate() throws Exception {

        String driver = "org.mariadb.jdbc.Driver";
        String dbUrl = "jdbc:mariadb://127.0.0.1:3306/test";
        String table = "jcl_db_jdbc";
        String username = "root";
        String password = "root";
        System.out.println("delegate classLaoder jdbc :: drive = " + driver + ", dbUrl = " + dbUrl + ", table = " + table + ", username = " + username + ", password = " + password);


//        String HOME_DIR ="/home/richard/NetBeansProjects/b2b-jcl/test/cz/b2b/jcl/RAM/resource";
        String HOME_DIR ="XXX/b2b-jcl/test/cz/b2b/jcl/RAM/resource";
        String file_name = HOME_DIR + "/jar" + "/Test3.jar";
        CacheClassLoader childClassLoader1 = new CacheClassLoader(Thread.currentThread().getContextClassLoader());
        childClassLoader1.addJAR(file_name);
        
        
        JdbcClassLoader childClassLoader = new JdbcClassLoader(childClassLoader1);
        childClassLoader.setConnection(driver, dbUrl, table, username, password);

        Class<?> test = Class.forName("cz.b2b.jcl.RAM.resource.jar.Test3", true, childClassLoader);
        Object o = test.getDeclaredConstructor(new Class[]{}).newInstance(new Object[]{});

        Method print = o.getClass().getMethod("print", String.class);
        System.out.println("class = " + o.getClass().getCanonicalName());
        print.invoke(o, "JDBC+JAR");

    }


}
