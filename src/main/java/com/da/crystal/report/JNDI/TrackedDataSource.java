package com.da.crystal.report.JNDI;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.HashSet;
import java.util.Set;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;

import lombok.extern.log4j.Log4j2;

// TODO Currently CR SDK-2025 can't close(release) the conection after document.close() when using JNDI, 
// if it fixed, could removing the `tracedDataSource` class.

@Log4j2
public class TrackedDataSource implements DataSource, AutoCloseable {

  private final HikariDataSource delegate;
  private final ThreadLocal<Set<Connection>> threadConnections = ThreadLocal.withInitial(HashSet::new);

  public TrackedDataSource(HikariDataSource delegate) {
    this.delegate = delegate;
  }

  public HikariDataSource getDelegate() {
    return delegate;
  }

  @Override
  public Connection getConnection() throws SQLException {
    Connection conn = delegate.getConnection();
    threadConnections.get().add(conn);
    log.debug("Connection borrowed: {} (thread: {}, total: {})",
        conn, Thread.currentThread().getName(), threadConnections.get().size());
    return conn;
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    Connection conn = delegate.getConnection(username, password);
    threadConnections.get().add(conn);
    log.debug("Connection borrowed: {} (thread: {}, total: {})",
        conn, Thread.currentThread().getName(), threadConnections.get().size());
    return conn;
  }

  public void releaseBorrowedConnections() {
    Set<Connection> borrowed = threadConnections.get();
    if (borrowed.isEmpty()) {
      return;
    }
    for (Connection conn : borrowed) {
      try {
        if (!conn.isClosed()) {
          conn.close();
          log.debug("Forcibly released connection: {} (thread: {})",
              conn, Thread.currentThread().getName());
        }
      } catch (SQLException e) {
        log.warn("Failed to release connection: {}", conn, e);
      }
    }
    log.debug("Released {} connection(s) on thread: {}", borrowed.size(), Thread.currentThread().getName());
    borrowed.clear();
  }

  public int getBorrowedCount() {
    return threadConnections.get().size();
  }

  @Override
  public void close() {
    releaseBorrowedConnections();
    threadConnections.remove();
    delegate.close();
  }

  @Override
  public java.io.PrintWriter getLogWriter() throws SQLException {
    return delegate.getLogWriter();
  }

  @Override
  public void setLogWriter(java.io.PrintWriter out) throws SQLException {
    delegate.setLogWriter(out);
  }

  @Override
  public void setLoginTimeout(int seconds) throws SQLException {
    delegate.setLoginTimeout(seconds);
  }

  @Override
  public int getLoginTimeout() throws SQLException {
    return delegate.getLoginTimeout();
  }

  @Override
  public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
    return delegate.getParentLogger();
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    if (iface.isInstance(this)) {
      return iface.cast(this);
    }
    return delegate.unwrap(iface);
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return iface.isInstance(this) || delegate.isWrapperFor(iface);
  }
}