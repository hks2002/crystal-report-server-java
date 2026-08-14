/*******************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                      *
 * @CreatedDate           : 2026-08-14 15:32:31                                *
 * @LastEditors           : Robert Huang<56649783@qq.com>                      *
 * @LastEditDate          : 2026-08-14 15:33:08                                *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                    *
 ******************************************************************************/
package com.da.crystal.report.JNDI;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class DataSourceProxy implements DataSource {

  private final DataSource target;
  private final SqlRewriter rewriter;

  public DataSourceProxy(DataSource target, SqlRewriter rewriter) {
    this.target = target;
    this.rewriter = rewriter;
  }

  @Override
  public Connection getConnection() throws SQLException {
    Connection conn = target.getConnection();
    return SqlLoggingProxy.wrap(conn, "CR", rewriter);
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    Connection conn = target.getConnection(username, password);
    return SqlLoggingProxy.wrap(conn, "CR", rewriter);
  }

  @Override
  public PrintWriter getLogWriter() throws SQLException {
    return target.getLogWriter();
  }

  @Override
  public void setLogWriter(PrintWriter out) throws SQLException {
    target.setLogWriter(out);
  }

  @Override
  public void setLoginTimeout(int seconds) throws SQLException {
    target.setLoginTimeout(seconds);
  }

  @Override
  public int getLoginTimeout() throws SQLException {
    return target.getLoginTimeout();
  }

  @Override
  public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    return target.getParentLogger();
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    if (iface.isInstance(this)) {
      return iface.cast(this);
    }
    return target.unwrap(iface);
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return iface.isInstance(this) || target.isWrapperFor(iface);
  }

  public DataSource getTarget() {
    return target;
  }
}