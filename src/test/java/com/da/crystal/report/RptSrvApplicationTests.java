/*********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                            *
 * @CreatedDate           : 2023-03-06 21:25:06                                                                      *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                            *
 * @LastEditDate          : 2023-04-17 15:06:11                                                                      *
 * @FilePath              : src/test/java/com/da/crystal/report/RptSrvApplicationTests.java                          *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                          *
 ********************************************************************************************************************/

package com.da.crystal.report;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
class RptSrvApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
        log.debug("contextLoads");
    }

    /**
     * Mock report controller request
     */
    @Test
    public void reportControllerTest() {
        try {
            mockMvc
                .perform(
                    MockMvcRequestBuilders.get("/Report/TEST/pdf?InvoiceNO=ZFC1901001").servletPath("/Report/TEST/pdf")
                )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType("application/pdf"));

            mockMvc
                .perform(
                    MockMvcRequestBuilders.get("/Report/TEST/doc?InvoiceNO=ZFC1901001").servletPath("/Report/TEST/doc")
                )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType("application/word"));
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
