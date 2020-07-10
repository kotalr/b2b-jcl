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
package cz.b2b.jcl.DB;

import java.net.*;
import java.util.*;
import java.io.*;
import org.slf4j.*;
import cz.b2b.jcl.util.CONST;
import java.sql.*;
import com.mchange.v2.c3p0.*;
import java.beans.PropertyVetoException;

/**
 The JdbcClassLoader class implements a class loader that loads classes from a
 database.
 <p>
 It is assumed that there is a table in the DB that has three columns of these
 names:
 <p>
 - package_name [varchar] (class name),
 <p>
 - class_name [varchar] (class name),
 <p>
 - class_code [blob] (byte code of the class).
 <p>
 The table name is arbitrary and is part of the configuration.
 <p>
 Example of the structure of the relevant table (mysql, mariadb):
 <p>
 {@code CREATE TABLE IF NOT EXISTS `jcl_db_jdbc` ( `package_name` varchar(512)
 CHARACTER SET ascii NOT NULL, `class_name` varchar(256) CHARACTER SET ascii
 NOT NULL, `class_code` longblob DEFAULT NULL, PRIMARY KEY
 (`package_name`,`class_name`) ) ENGINE=MyISAM DEFAULT CHARSET=utf8
 COLLATE=utf8_bin;}
 <p>
 Example of usage:
 <pre>
 String driver = "org.mariadb.jdbc.Driver";
 String dbUrl = "jdbc:mariadb://127.0.0.1:3306/test";
 String table = "jcl_db_jdbc";
 String username = "root";
 String password = "root";

 JdbcClassLoader childClassLoader = new JdbcClassLoader(Thread.currentThread().getContextClassLoader());
 childClassLoader.setConnection(driver, dbUrl, table, username, password);

 final Class&#60;?&#62; test = Class.forName("cz.b2b.jcl.RAM.resource.Test", true, childClassLoader);
 Object o = test.getDeclaredConstructor(new Class[]{}).newInstance(new Object[]{});

 Method print = o.getClass().getMethod("print", String.class);
 System.out.println("class = " + o.getClass().getCanonicalName());
 print.invoke(o, "JDBC");

 </pre>


 @author Richard Kotal &#60;richard.kotal@b2b.cz&#620;
 */
public class JdbcClassLoader extends URLClassLoader {

    private final static String protocol = "x-db-jdbc";
    private final static Logger logger = LoggerFactory.getLogger(JdbcClassLoader.class);
    private final static String package_name = "package_name";
    private final static String class_name = "class_name";
    private final static String class_code = "class_code";

    private String table = null;

    private final URL jdbcURL = new URL(protocol, CONST.host, CONST.port, CONST.baseURI, new JdbcURLStreamHandler());
    private final ComboPooledDataSource cpds = new ComboPooledDataSource();

    /**
     Constructs a new JdbcClassLoader for the given URLs of URLClassLoader and
     the JDBC stream protocol handler.
     <p>
     The url for the JDBC stream protocol handler is added to the others when
     the constructor is created.

     @param urls the Standard URLClassLoader URLs from which to load classes and
     resources. The URLs will be searched in the order specified for classes and
     resources after first searching in the specified parent class loader.
     @param parent the parent class loader for delegation
     @throws MalformedURLException Thrown to indicate that a malformed URL has
     occurred. Either no legal protocol could be found in a specification string
     or the string could not be parsed.
     */
    public JdbcClassLoader(URL[] urls, ClassLoader parent) throws MalformedURLException {
        super(urls, parent);
        super.addURL(jdbcURL);
    }

    /**
     Constructs a new JdbcClassLoader for the JDBC stream protocol handler.
     <p>
     The url for the JDBC stream protocol handler is added when the constructor
     is created.

     @param parent the parent class loader for delegation
     @throws MalformedURLException Thrown to indicate that a malformed URL has
     occurred. Either no legal protocol could be found in a specification string
     or the string could not be parsed.
     */
    public JdbcClassLoader(ClassLoader parent) throws MalformedURLException {
        this(new URL[]{}, parent);
    }

    @Override
    public void close() throws IOException {
        cpds.close();
        super.close();

    }

    /**
     Set Pool settings.

     @param minPoolSize minimal pool size
     @param maxPoolSize maximal pool size
     @param maxIdleTime maximal idle timeout
     */
    public void setPoolSettings(int minPoolSize, int maxPoolSize, int maxIdleTime) {
        cpds.setMinPoolSize(minPoolSize);
        cpds.setMaxPoolSize(maxPoolSize);
        cpds.setMaxIdleTime(maxIdleTime);

    }

    /**
     Set JDBC driver.

     @param driver JDBC driver (ex.: org.mariadb.jdbc.Driver)
     @throws ClassNotFoundException Thrown when JDBC driver class not found
     */
    public void setDriver(String driver) throws ClassNotFoundException, PropertyVetoException {
        cpds.setDriverClass(driver);
    }

    /**
     Set table name.
     @param table table name (ex.: jcl_db_jdbc)
     */
    public void setTable(String table) {
        this.table = table;

    }

    /**
     Set url string for JDBC connection.
     @param dbUrl url connection (ex.: jdbc:mariadb://127.0.0.1:3306/test)
     */
    public void setDbUrl(String dbUrl) {
        cpds.setJdbcUrl(dbUrl);

    }

    /**
     Set Login name to DB.
     @param username login name to DB (ex.: root)
     */
    public void setUsername(String username) {
        cpds.setUser(username);

    }

    /**
     Set password to DB.
     @param password password to DB (ex.: root)
     */
    public void setPassword(String password) {
        cpds.setPassword(password);

    }

    /**
     Set full connection to DB.
     @param driver JDBC driver (ex.: org.mariadb.jdbc.Driver)
     @param dbUrl url connection (ex.: jdbc:mariadb://127.0.0.1:3306/test)
     @param table table name (ex.: jcl_db_jdbc)
     @param username login name to DB (ex.: root)
     @param password password to DB (ex.: root)
     @throws ClassNotFoundException Thrown when JDBC driver class not found
     */
    public void setConnection(String driver, String dbUrl, String table, String username, String password) throws ClassNotFoundException, PropertyVetoException {
        setDriver(driver);
        setDbUrl(dbUrl);
        setTable(table);
        setUsername(username);
        setPassword(password);

    }

    /**
     Special JDBC stream protocol handler knows how to make a connection for the
     protocol type x-db-jdbc.
     <p>
     This handler connects to the DB via the JDBC connector and reads the class
     code from the DB table.
     */
    private class JdbcURLStreamHandler extends URLStreamHandler {

        @Override
        protected URLConnection openConnection(URL url) throws IOException {
            return new JdbcURLConnection(url);
        }

    }

    private class JdbcURLConnection extends URLConnection {

        public JdbcURLConnection(URL url) {
            super(url);
        }

        @Override
        public void connect() throws IOException {
        }

        @Override
        public InputStream getInputStream() throws IOException {
            Map cols = parseURL(url);

            final byte[] data = class_code(cols);
            if (data == null) {
                throw new FileNotFoundException(url.getFile());
            }

            return new ByteArrayInputStream(data);
        }

        private byte[] class_code(Map cols) {
            byte[] class_code = null;
            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;
            if (cols == null || cols.isEmpty() == true) {
                return class_code;
            }

            String SQL = "SELECT " + JdbcClassLoader.class_code + " FROM " + table + " WHERE " + class_name + " ='" + escape((String) cols.get(class_name)) + "' AND package_name='" + escape((String) cols.get(package_name)) + "'";
            logger.debug(SQL);

            try {
                conn = cpds.getConnection();
                stmt = conn.createStatement();
                rs = stmt.executeQuery(SQL);
                if (rs.next() == true) {
                    class_code = rs.getBytes(JdbcClassLoader.class_code);
                }

            } catch (Exception e) {
                logger.error(e.toString());
            } finally {
                try {
                    if (rs != null) {
                        rs.close();
                    }
                    if (stmt != null) {
                        stmt.close();
                    }
                    if (conn != null) {
                        conn.close();
                    }
                } catch (SQLException ex) {
                    logger.error(ex.toString());
                }

            }

            return class_code;
        }

        private String escape(String in) {
            if (in == null) {
                return in;
            }

            return in.replace("'", "''").replace("\\", "\\\\");

        }

        private Map parseURL(URL url) {
            Map jdbcURI = new HashMap();
            if (url == null) {
                return jdbcURI;
            }
            String class_file_name = url.getFile();
            if (class_file_name == null) {
                return jdbcURI;
            }
            File fi = new File(class_file_name);
            jdbcURI.put(package_name, package_name(fi));
            jdbcURI.put(class_name, class_name(fi));

            return jdbcURI;
        }

        private String package_name(File fi) {
            String package_name = null;
            if (fi == null) {
                return package_name;
            }
            package_name = fi.getParent();
            if (package_name != null) {
                package_name = package_name.replace(CONST.baseURI, CONST.DOT).substring(1);
            }

            return package_name;
        }

        private String class_name(File fi) {
            String class_name = null;
            if (fi == null) {
                return class_name;
            }
            class_name = fi.getName();
            if (class_name != null) {
                class_name = class_name.replace(CONST.CLASS_SUFFIX, CONST.EMPTY);
            }

            return class_name;
        }

    }

}
