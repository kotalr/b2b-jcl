/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.b2b.jcl.DB;

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

        final Class<?> test = Class.forName("cz.b2b.jcl.RAM.resource.Test", true, childClassLoader);
        Object o = test.getDeclaredConstructor(new Class[]{}).newInstance(new Object[]{});

        Method print = o.getClass().getMethod("print", String.class);
        System.out.println("class = " + o.getClass().getCanonicalName());
        print.invoke(o, "JDBC");

    }

}
