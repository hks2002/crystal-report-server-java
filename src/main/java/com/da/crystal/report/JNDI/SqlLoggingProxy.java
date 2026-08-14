/*******************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                      *
 * @CreatedDate           : 2026-08-14 16:49:38                                *
 * @LastEditors           : Robert Huang<56649783@qq.com>                      *
 * @LastEditDate          : 2026-08-14 17:28:26                                *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                    *
 ******************************************************************************/
package com.da.crystal.report.JNDI;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Statement;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class SqlLoggingProxy implements InvocationHandler {

  private static final java.util.Set<String> EXECUTE_METHODS = java.util.Set.of(
      "execute", "executeQuery", "executeUpdate", "executeLargeUpdate", "addBatch");

  private final Object delegate;
  private final String source;
  private final SqlRewriter rewriter;

  private SqlLoggingProxy(Object delegate, String source, SqlRewriter rewriter) {
    this.delegate = delegate;
    this.source = source;
    this.rewriter = rewriter;
  }

  @SuppressWarnings("unchecked")
  public static <T> T wrap(T obj, String source, SqlRewriter rewriter) {
    if (obj == null) {
      return null;
    }
    if (obj instanceof Connection) {
      return (T) Proxy.newProxyInstance(
          Connection.class.getClassLoader(),
          getAllInterfaces(obj.getClass()),
          new SqlLoggingProxy(obj, source, rewriter));
    }
    if (obj instanceof Statement) {
      return (T) Proxy.newProxyInstance(
          Statement.class.getClassLoader(),
          getAllInterfaces(obj.getClass()),
          new SqlLoggingProxy(obj, source, rewriter));
    }
    return obj;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    String name = method.getName();

    Object specialResult = handleSpecialMethods(proxy, method, name, args);
    if (specialResult != null) {
      return specialResult;
    }

    rewriteSqlIfNeeded(name, args);

    Object result = method.invoke(delegate, args);

    if ("executeBatch".equals(name) || "executeLargeBatch".equals(name)) {
      logSql(source, name, "<batch>");
    }

    return wrapResult(result, name, args);
  }

  private Object handleSpecialMethods(Object proxy, Method method, String name, Object[] args) throws Throwable {
    if ("unwrap".equals(name) && hasArg(args)) {
      Class<?> iface = (Class<?>) args[0];
      if (iface.isInstance(delegate)) {
        return iface.cast(delegate);
      }
    }
    if ("isWrapperFor".equals(name) && hasArg(args)) {
      Class<?> iface = (Class<?>) args[0];
      return iface.isInstance(delegate) || (boolean) method.invoke(delegate, args);
    }
    if ("hashCode".equals(name)) {
      return System.identityHashCode(proxy);
    }
    if ("equals".equals(name) && hasArg(args)) {
      return proxy == args[0];
    }
    if ("toString".equals(name)) {
      return "SqlLoggingProxy[" + source + "] -> " + delegate;
    }
    return null;
  }

  private void rewriteSqlIfNeeded(String name, Object[] args) {
    if (!hasArg(args) || !(args[0] instanceof String)) {
      return;
    }

    String originalSql = (String) args[0];
    String rewrittenSql = rewriter.rewrite(originalSql);
    boolean changed = !originalSql.equals(rewrittenSql);

    if (EXECUTE_METHODS.contains(name)) {
      logSql(source, name, originalSql);
      if (changed) {
        log.info("[SQL] REWRITTEN\n{}", rewrittenSql);
        args[0] = rewrittenSql;
      }
    } else if (("prepareStatement".equals(name) || "prepareCall".equals(name)) && changed) {
      log.info("[SQL] PREPARE REWRITTEN\n{}", rewrittenSql);
      args[0] = rewrittenSql;
    }
  }

  private Object wrapResult(Object result, String name, Object[] args) {
    if (result instanceof Statement) {
      return Proxy.newProxyInstance(
          Statement.class.getClassLoader(),
          getAllInterfaces(result.getClass()),
          new SqlLoggingProxy(result, source + "/" + name, rewriter));
    }

    if (result instanceof Connection && !Proxy.isProxyClass(result.getClass())) {
      return Proxy.newProxyInstance(
          Connection.class.getClassLoader(),
          getAllInterfaces(result.getClass()),
          new SqlLoggingProxy(result, source + "/conn", rewriter));
    }

    return result;
  }

  private static boolean hasArg(Object[] args) {
    return args != null && args.length > 0;
  }

  private static void logSql(String source, String method, String sql) {
    if (sql == null) {
      return;
    }

    if (!sql.toUpperCase().contains("WHERE")) {
      log.warn("SQL does not contain [WHERE] clause, it may cause performance issue!\n{}", sql);
    } else {
      log.info("[SQL] {} {}\n{}", source, method, (sql));
    }
  }

  private static Class<?>[] getAllInterfaces(Class<?> clazz) {
    java.util.Set<Class<?>> interfaces = new java.util.LinkedHashSet<>();
    Class<?> current = clazz;
    while (current != null) {
      for (Class<?> iface : current.getInterfaces()) {
        if (iface.getName().startsWith("java.sql")
            || iface.getName().startsWith("javax.sql")) {
          interfaces.add(iface);
        }
      }
      current = current.getSuperclass();
    }
    return interfaces.toArray(new Class<?>[0]);
  }
}