package com.demo.controller.admin;

import com.demo.entity.Venue;
import com.demo.service.VenueService;
import com.demo.utils.FileUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.mock.web.MockHttpSession;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
public class AdminVenueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VenueService venueService;
    
    @MockBean
    private FileUtil fileUtil;

    private List<Venue> venueList;
    private Page<Venue> venuePage;
    private Venue testVenue;
    private com.demo.entity.User adminUser;

    @BeforeEach
    public void setUp() {
        // 准备测试数据
        venueList = new ArrayList<>();
        
        // 创建测试管理员用户
        adminUser = new com.demo.entity.User();
        adminUser.setUserID("admin");
        adminUser.setUserName("管理员");
        adminUser.setPassword("admin123");
        adminUser.setIsadmin(1); // 管理员类型
        
        // 创建测试场馆
        for (int i = 1; i <= 5; i++) {
            Venue venue = new Venue();
            venue.setVenueID(i);
            venue.setVenueName("测试场馆" + i);
            venue.setAddress("测试地址" + i);
            venue.setDescription("测试描述" + i);
            venue.setPrice(100 * i);
            venue.setPicture("venue_" + i + ".jpg");
            venue.setOpen_time("08:00");
            venue.setClose_time("22:00");
            venueList.add(venue);
        }
        
        // 创建测试场馆对象
        testVenue = new Venue();
        testVenue.setVenueID(1);
        testVenue.setVenueName("测试场馆");
        testVenue.setAddress("测试地址");
        testVenue.setDescription("测试描述");
        testVenue.setPrice(100);
        testVenue.setPicture("venue.jpg");
        testVenue.setOpen_time("08:00");
        testVenue.setClose_time("22:00");
        
        // 创建分页数据
        Pageable pageable = PageRequest.of(0, 10, Sort.by("venueID").ascending());
        venuePage = new PageImpl<>(venueList, pageable, venueList.size());
    }

    @Test
    @DisplayName("测试获取场馆管理页面")
    public void testVenueManage() throws Exception {
        System.out.println("[测试] AdminVenueController.venue_manage - 获取场馆管理页面");
        
        // 模拟服务层方法
        when(venueService.findAll(any(Pageable.class))).thenReturn(venuePage);
        
        // 执行请求并验证结果
        MvcResult result = mockMvc.perform(get("/venue_manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/venue_manage"))
                .andExpect(model().attributeExists("total"))
                .andReturn();
        
        // 验证服务层方法被调用
        verify(venueService, times(1)).findAll(any(Pageable.class));
        
        // 检查模型中是否包含正确的数据
        if (result.getModelAndView().getModel().get("total") == null) {
            System.out.println("[错误] 场馆管理页面缺少total属性");
            fail("场馆管理页面模型中应包含total属性");
        } else {
            System.out.println("[通过] 场馆管理页面包含了total属性");
        }
    }

    @Test
    @DisplayName("测试场馆编辑页面")
    public void testVenueEdit() throws Exception {
        System.out.println("[测试] AdminVenueController.venue_edit - 场馆编辑页面");
        
        // 模拟服务层方法
        when(venueService.findByVenueID(1)).thenReturn(testVenue);
        
        // 执行请求并验证结果
        MvcResult result = mockMvc.perform(get("/venue_edit")
                .param("venueID", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("/admin/venue_edit"))
                .andExpect(model().attributeExists("venue"))
                .andReturn();
        
        // 验证服务层方法被调用
        verify(venueService, times(1)).findByVenueID(1);
        
        // 检查返回的场馆对象是否正确
        Object venueObject = result.getModelAndView().getModel().get("venue");
        if (venueObject == null || !(venueObject instanceof Venue)) {
            System.out.println("[错误] 场馆编辑页面没有正确返回场馆对象");
            fail("场馆编辑页面应返回有效的场馆对象");
        } else {
            System.out.println("[通过] 场馆编辑页面返回了正确的场馆对象");
        }
    }

    @Test
    @DisplayName("测试场馆添加页面")
    public void testVenueAdd() throws Exception {
        System.out.println("[测试] AdminVenueController.venue_add - 场馆添加页面");
        
        // 执行请求并验证结果
        MvcResult result = mockMvc.perform(get("/venue_add"))
                .andExpect(status().isOk())
                .andExpect(view().name("/admin/venue_add"))
                .andReturn();
        
        // 检查返回的视图名称是否正确
        String viewName = result.getModelAndView().getViewName();
        if (!"/admin/venue_add".equals(viewName)) {
            System.out.println("[错误] 场馆添加页面返回了错误的视图名称: " + viewName);
            fail("场馆添加页面应返回'/admin/venue_add'视图，实际返回: " + viewName);
        } else {
            System.out.println("[通过] 场馆添加页面返回了正确的视图名称");
        }
    }

    @Test
    @DisplayName("测试获取场馆列表")
    public void testGetVenueList() throws Exception {
        System.out.println("[测试] AdminVenueController.venueList - 获取场馆列表");
        
        // 模拟服务层方法
        when(venueService.findAll(any(Pageable.class))).thenReturn(venuePage);
        
        // 执行请求并验证结果
        MvcResult result = mockMvc.perform(get("/venueList.do")
                .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andReturn();
        
        // 验证服务层方法被调用
        verify(venueService, times(1)).findAll(any(Pageable.class));
        
        // 检查返回的数据数量是否正确
        String responseContent = result.getResponse().getContentAsString();
        if (responseContent == null || responseContent.isEmpty()) {
            System.out.println("[错误] 获取场馆列表返回了空数据");
            fail("获取场馆列表API应返回有效的JSON数据");
        } else if (!responseContent.contains("\"venueID\"")) {
            System.out.println("[错误] 获取场馆列表返回的数据格式不正确: " + responseContent);
            fail("获取场馆列表API返回的JSON数据应包含场馆信息");
        } else {
            System.out.println("[通过] 获取场馆列表返回了正确的数据");
        }
    }

    @Test
    @DisplayName("测试删除场馆")
    public void testDelVenue() throws Exception {
        System.out.println("[测试] AdminVenueController.delVenue - 删除场馆");
        
        // 模拟服务层方法
        doNothing().when(venueService).delById(anyInt());
        
        // 执行请求并验证结果
        MvcResult result = mockMvc.perform(post("/delVenue.do")
                .param("venueID", "1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true))
                .andReturn();
        
        // 验证服务层方法被调用
        verify(venueService, times(1)).delById(1);
        
        // 检查返回结果是否正确
        String responseContent = result.getResponse().getContentAsString();
        if (!"true".equals(responseContent)) {
            System.out.println("[错误] 删除场馆操作应返回'true'，实际返回: " + responseContent);
            fail("删除场馆操作应返回'true'，实际返回: " + responseContent);
        } else {
            System.out.println("[通过] 删除场馆操作返回了正确的结果");
        }
    }

    @Test
    @DisplayName("测试检查场馆名称 - 可用")
    public void testCheckVenueNameAvailable() throws Exception {
        System.out.println("[测试] AdminVenueController.checkVenueName - 检查场馆名称可用性");
        
        // 模拟服务层方法
        when(venueService.countVenueName("新场馆")).thenReturn(0);
        
        // 执行请求并验证结果
        MvcResult result = mockMvc.perform(post("/checkVenueName.do")
                .param("venueName", "新场馆")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true))
                .andReturn();
        
        // 验证服务层方法被调用
        verify(venueService, times(1)).countVenueName("新场馆");
        
        // 检查返回结果是否正确
        String responseContent = result.getResponse().getContentAsString();
        if (!"true".equals(responseContent)) {
            System.out.println("[错误] 检查可用场馆名称应返回'true'，实际返回: " + responseContent);
            fail("检查可用场馆名称应返回'true'，实际返回: " + responseContent);
        } else {
            System.out.println("[通过] 检查可用场馆名称返回了正确的结果");
        }
    }
    
    @Test
    @DisplayName("测试检查场馆名称 - 不可用")
    public void testCheckVenueNameUnavailable() throws Exception {
        System.out.println("[测试] AdminVenueController.checkVenueName - 检查场馆名称不可用");
        
        // 模拟服务层方法
        when(venueService.countVenueName("测试场馆1")).thenReturn(1);
        
        // 执行请求并验证结果
        MvcResult result = mockMvc.perform(post("/checkVenueName.do")
                .param("venueName", "测试场馆1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(false))
                .andReturn();
        
        // 验证服务层方法被调用
        verify(venueService, times(1)).countVenueName("测试场馆1");
        
        // 检查返回结果是否正确
        String responseContent = result.getResponse().getContentAsString();
        if (!"false".equals(responseContent)) {
            System.out.println("[错误] 检查不可用场馆名称应返回'false'，实际返回: " + responseContent);
            fail("检查不可用场馆名称应返回'false'，实际返回: " + responseContent);
        } else {
            System.out.println("[通过] 检查不可用场馆名称返回了正确的结果");
        }
    }


    @Test
    @DisplayName("测试场馆管理页面权限")
    public void testVenueManagePermission() throws Exception {
        System.out.println("[测试] AdminVenueController - 场馆管理页面权限");
        
        // 模拟未登录用户的会话
        MockHttpSession noLoginSession = new MockHttpSession();
        
        try {
            MvcResult result = mockMvc.perform(get("/admin/venue/manage")
                    .session(noLoginSession))
                    .andReturn();
            
            int status = result.getResponse().getStatus();
            if (status == 302) {
                String redirectUrl = result.getResponse().getRedirectedUrl();
                if (redirectUrl != null && redirectUrl.contains("login")) {
                    System.out.println("[通过] 系统正确重定向未登录用户到登录页面");
                } else {
                    System.out.println("[警告] 未登录用户访问场馆管理页面重定向到非登录页面: " + redirectUrl);
                }
            } else if (status == 403 || status == 401) {
                System.out.println("[通过] 系统返回" + status + "状态码，禁止未授权访问");
            } else {
                System.out.println("[警告] 未登录用户访问场馆管理页面返回非预期状态码: " + status);
                fail("未登录用户应该无法访问场馆管理页面");
            }
            
            // 验证服务层方法未被调用
            verify(venueService, times(0)).findAll(any(Pageable.class));
        } catch (Exception e) {
            System.out.println("[错误] 测试场馆管理页面权限时抛出异常: " + e.getMessage());
            fail("权限测试不应抛出异常: " + e.getMessage());
        }
    }
    
    @Test
    @DisplayName("测试非法ID删除场馆")
    public void testDeleteVenueWithInvalidId() throws Exception {
        System.out.println("[测试] AdminVenueController - 非法ID删除场馆");
        
        // 设置模拟admin登录会话
        MockHttpSession adminSession = new MockHttpSession();
        adminSession.setAttribute("login_user", adminUser);
        
        // 测试负数ID
        try {
            MvcResult result = mockMvc.perform(get("/admin/venue/del.do?id=-1")
                    .session(adminSession))
                    .andReturn();
            
            verify(venueService, times(0)).delById(-1);
            
            int status = result.getResponse().getStatus();
            String redirectUrl = result.getResponse().getRedirectedUrl();
            
            if (status == 302 && redirectUrl != null && redirectUrl.contains("error")) {
                System.out.println("[通过] 系统正确处理了负数ID的场馆删除请求");
            } else if (status >= 400) {
                System.out.println("[通过] 系统返回错误状态码 " + status + " 处理了负数ID的场馆删除请求");
            } else {
                System.out.println("[警告] 系统未正确处理负数ID的场馆删除请求，状态码: " + status);
                fail("系统应拒绝处理负数ID的场馆删除请求");
            }
        } catch (Exception e) {
            System.out.println("[通过] 系统通过异常处理了负数ID的场馆删除请求: " + e.getMessage());
        }
    }
} 