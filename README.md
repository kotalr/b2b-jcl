# b2b-jcl

Dynamic java class loader (in-memory and JDBC ClassLoader).

Load class code from DB, load class code into MEM cache from jar files, directories with jar files, class file, java file.

This is an extension of URLClassLoader with the following features:

- Loads java classes (byte code) directly from the table stored in the DB (JDBC connector).
- Loads java classes (byte code) from jar files, directories with jar files (recursively), .class files and .java files (compiled at runtime) into RAM (in-memory cache).

It allows you to change the system code at runtime without having to restart it, replacing only the necessary part. You can also replace jar files at runtime without errors. And load the new code from this new jar file and possibly keep the old one already loaded.

Examples of usage:

```
 String driver = "org.mariadb.jdbc.Driver";
 String dbUrl = "jdbc:mariadb://127.0.0.1:3306/test";
 String table = "jcl_db_jdbc";
 String username = "root";
 String password = "root";

 JdbcClassLoader childClassLoader = new JdbcClassLoader(Thread.currentThread().getContextClassLoader());
 childClassLoader.setConnection(driver, dbUrl, table, username, password);

 final Class<?> test = Class.forName("cz.b2b.jcl.RAM.resource.Test", true, childClassLoader);
 Object o = test.getDeclaredConstructor(new Class[]{}).newInstance(new Object[]{});

 Method print = o.getClass().getMethod("print", String.class);
 System.out.println("class = " + o.getClass().getCanonicalName());
 print.invoke(o, "JDBC");
```

------------------------------------------

```
 CacheClassLoader childClassLoader = new CacheClassLoader(Thread.currentThread().getContextClassLoader());

 String file_name = "/tmp/Test3.jar";
 System.out.println("addJAR = " + file_name);
 childClassLoader.addJAR(file_name);

 file_name = "/tmp/Test.jar";
 childClassLoader.addJAR(file_name);
 System.out.println("addJAR = " + file_name);

 final Class<?>  test = Class.forName("cz.b2b.jcl.RAM.resource.jar.Test3", true, childClassLoader);
 Object o = test.getDeclaredConstructor(new Class[]{}).newInstance(new Object[]{});

 Method print = o.getClass().getMethod("print", String.class);
 System.out.println("class = " + o.getClass().getCanonicalName());
 print.invoke(o, "JAR");
```

------------------------------------------

```
 String path = "/tmp/class";
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
```