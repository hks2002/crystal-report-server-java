/*******************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                      *
 * @CreatedDate           : 2026-08-12 09:59:17                                *
 * @LastEditors           : Robert Huang<56649783@qq.com>                      *
 * @LastEditDate          : 2026-08-17 18:50:48                                *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                    *
 ******************************************************************************/

package com.da.crystal.report.JNDI;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DataSourceConfig {
  private final String connectionURL;
  private final String driverName;
  private final String jndiName = "jdbc/crystal_report";
  private final String username;
  private final String password;

  public boolean useJNDI() {
    return true;
  }

  public String getServerType() {
    return "JDBC (JNDI)";
  }
}