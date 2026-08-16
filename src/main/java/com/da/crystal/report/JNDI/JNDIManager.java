/*******************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                      *
 * @CreatedDate           : 2026-08-03 00:00:00                                *
 * @LastEditors           : Robert Huang<56649783@qq.com>                      *
 * @LastEditDate          : 2026-08-16 17:58:30                                *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                    *
 ******************************************************************************/

package com.da.crystal.report.JNDI;

import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.NamingManager;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class JNDIManager {

  private static final Map<String, Object> store = new ConcurrentHashMap<>();
  private static volatile boolean initialized = false;

  private JNDIManager() {
  }

  public static synchronized void init(JsonObject dsConfig) {
    if (initialized) {
      log.warn("JNDIManager already initialized, skipping");
      return;
    }

    try {
      NamingManager.setInitialContextFactoryBuilder(env -> new SimpleJNDIFactory());
      initialized = true;
      log.info("JNDI InitialContextFactory registered");

      setupDataSource(dsConfig);
    } catch (NamingException e) {
      log.error("Failed to initialize JNDI", e);
    }
  }

  private static void setupDataSource(JsonObject dsConfig) {
    try {
      String jndiName = dsConfig.getString("jndiName", "jdbc/crystal_report");
      HikariConfig hikariConfig = new HikariConfig();

      hikariConfig.setJdbcUrl(dsConfig.getString("url"));
      hikariConfig.setDriverClassName(dsConfig.getString("driverClassName"));
      hikariConfig.setUsername(dsConfig.getString("user"));
      hikariConfig.setPassword(dsConfig.getString("password"));

      JsonObject pool = dsConfig.getJsonObject("pool");
      if (pool != null) {
        if (pool.containsKey("maximumPoolSize")) {
          hikariConfig.setMaximumPoolSize(pool.getInteger("maximumPoolSize"));
        }
        if (pool.containsKey("minimumIdle")) {
          hikariConfig.setMinimumIdle(pool.getInteger("minimumIdle"));
        }
        if (pool.containsKey("connectionTimeout")) {
          hikariConfig.setConnectionTimeout(pool.getLong("connectionTimeout"));
        }
        if (pool.containsKey("idleTimeout")) {
          hikariConfig.setIdleTimeout(pool.getLong("idleTimeout"));
        }
        if (pool.containsKey("maxLifetime")) {
          hikariConfig.setMaxLifetime(pool.getLong("maxLifetime"));
        }
        if (pool.containsKey("leakDetectionThreshold")) {
          hikariConfig.setLeakDetectionThreshold(pool.getLong("leakDetectionThreshold"));
        }
        if (pool.containsKey("poolName")) {
          hikariConfig.setPoolName(pool.getString("poolName"));
        }
      }

      HikariDataSource hikariDataSource = new HikariDataSource(hikariConfig);

      boolean sqlIntercept = dsConfig.getBoolean("sqlIntercept", false);
      if (sqlIntercept) {
        DataSourceProxy dataSource = new DataSourceProxy(hikariDataSource, sql -> sql);
        bind(jndiName, dataSource);
        log.info("SQL intercept ENABLED");
      } else {
        bind(jndiName, hikariDataSource);
        log.info("SQL intercept disabled");
      }

      log.info("HikariCP DataSource bound to JNDI: {}", jndiName);
      log.info("URL: {}", hikariConfig.getJdbcUrl());
      log.info("MaxPoolSize: {}", hikariConfig.getMaximumPoolSize());
      log.info("MinIdle: {}", hikariConfig.getMinimumIdle());
    } catch (Exception e) {
      log.error("Failed to setup HikariCP DataSource in JNDI", e);
    }
  }

  public static void bind(String name, Object obj) throws NamingException {
    InitialContext ctx = new InitialContext();
    ctx.bind(name, obj);
  }

  public static Object lookup(String name) throws NamingException {
    return new InitialContext().lookup(name);
  }

  public static void shutdown() {
    for (Object obj : store.values()) {
      if (obj instanceof DataSourceProxy) {
        ((HikariDataSource) ((DataSourceProxy) obj).getTarget()).close();
      } else if (obj instanceof HikariDataSource) {
        ((HikariDataSource) obj).close();
      }
      log.info("HikariCP DataSource closed");
    }
    store.clear();
  }

  public static class SimpleJNDIFactory implements InitialContextFactory {
    @Override
    public Context getInitialContext(Hashtable<?, ?> environment) {
      return new SimpleJNDIContext();
    }
  }

  public static class SimpleJNDIContext implements Context {
    private final Map<String, Object> bindings;

    public SimpleJNDIContext() {
      this.bindings = store;
    }

    public SimpleJNDIContext(Map<String, Object> bindings) {
      this.bindings = bindings;
    }

    @Override
    public Object lookup(Name name) throws NamingException {
      return lookup(name.toString());
    }

    @Override
    public Object lookup(String name) throws NamingException {
      log.info("JNDI lookup: {}", name);
      Object result = bindings.get(name);
      if (result != null) {
        log.info("JNDI found: {} -> {}", name, result.getClass().getName());
        return result;
      }

      if (name.startsWith("java:comp/env/")) {
        result = bindings.get(name.substring("java:comp/env/".length()));
        if (result != null) {
          log.info("JNDI found (stripped prefix): {} -> {}", name, result.getClass().getName());
          return result;
        }
      }
      log.warn("JNDI not found: {} (available: {})", name, bindings.keySet());
      throw new NameNotFoundException("Name not found: " + name);
    }

    @Override
    public void bind(Name name, Object obj) throws NamingException {
      bind(name.toString(), obj);
    }

    @Override
    public void bind(String name, Object obj) throws NamingException {
      if (bindings.containsKey(name)) {
        throw new NameAlreadyBoundException("Name already bound: " + name);
      }
      bindings.put(name, obj);
    }

    @Override
    public void rebind(Name name, Object obj) throws NamingException {
      rebind(name.toString(), obj);
    }

    @Override
    public void rebind(String name, Object obj) throws NamingException {
      bindings.put(name, obj);
    }

    @Override
    public void unbind(Name name) throws NamingException {
      unbind(name.toString());
    }

    @Override
    public void unbind(String name) throws NamingException {
      bindings.remove(name);
    }

    @Override
    public void rename(Name oldName, Name newName) throws NamingException {
      rename(oldName.toString(), newName.toString());
    }

    @Override
    public void rename(String oldName, String newName) throws NamingException {
      Object obj = bindings.remove(oldName);
      if (obj == null) {
        throw new NameNotFoundException("Name not found: " + oldName);
      }
      bindings.put(newName, obj);
    }

    @Override
    public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
      return list(name.toString());
    }

    @Override
    public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
      throw new UnsupportedOperationException("list not supported");
    }

    @Override
    public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
      return listBindings(name.toString());
    }

    @Override
    public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
      throw new UnsupportedOperationException("listBindings not supported");
    }

    @Override
    public void destroySubcontext(Name name) throws NamingException {
      destroySubcontext(name.toString());
    }

    @Override
    public void destroySubcontext(String name) throws NamingException {
      bindings.remove(name);
    }

    @Override
    public Context createSubcontext(Name name) throws NamingException {
      return createSubcontext(name.toString());
    }

    @Override
    public Context createSubcontext(String name) throws NamingException {
      Map<String, Object> sub = new ConcurrentHashMap<>();
      bindings.put(name, sub);
      return new SimpleJNDIContext(sub);
    }

    @Override
    public Object lookupLink(Name name) throws NamingException {
      return lookup(name);
    }

    @Override
    public Object lookupLink(String name) throws NamingException {
      return lookup(name);
    }

    @Override
    public NameParser getNameParser(Name name) throws NamingException {
      throw new UnsupportedOperationException("getNameParser not supported");
    }

    @Override
    public NameParser getNameParser(String name) throws NamingException {
      throw new UnsupportedOperationException("getNameParser not supported");
    }

    @Override
    public Name composeName(Name name, Name prefix) throws NamingException {
      throw new UnsupportedOperationException("composeName not supported");
    }

    @Override
    public String composeName(String name, String prefix) throws NamingException {
      throw new UnsupportedOperationException("composeName not supported");
    }

    @Override
    public Object addToEnvironment(String propName, Object propVal) throws NamingException {
      throw new UnsupportedOperationException("addToEnvironment not supported");
    }

    @Override
    public Object removeFromEnvironment(String propName) throws NamingException {
      throw new UnsupportedOperationException("removeFromEnvironment not supported");
    }

    @Override
    public Hashtable<?, ?> getEnvironment() throws NamingException {
      return new Hashtable<>();
    }

    @Override
    public void close() throws NamingException {
    }

    @Override
    public String getNameInNamespace() throws NamingException {
      return "";
    }
  }
}