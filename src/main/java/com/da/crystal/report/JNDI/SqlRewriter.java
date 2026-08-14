/*******************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                      *
 * @CreatedDate           : 2026-08-14 15:32:27                                *
 * @LastEditors           : Robert Huang<56649783@qq.com>                      *
 * @LastEditDate          : 2026-08-14 15:32:47                                *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                    *
 ******************************************************************************/
package com.da.crystal.report.JNDI;

@FunctionalInterface
public interface SqlRewriter {

  String rewrite(String sql);
}