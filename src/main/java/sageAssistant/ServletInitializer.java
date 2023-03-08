/**************************************************************************************************
 * @Author                : hks2002<56649783@qq.com>                                              *
 * @CreatedDate           : 2023-03-06 21:04:02                                                   *
 * @LastEditors           : hks2002<56649783@qq.com>                                              *
 * @LastEditDate          : 2023-03-06 21:19:33                                                   *
 * @FilePath              : rptSrv/src/main/java/sageAssistant/ServletInitializer.java            *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                       *
 *************************************************************************************************/

package sageAssistant;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

public class ServletInitializer extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(RptSrvApplication.class);
	}

}
