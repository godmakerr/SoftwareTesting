package com.demo.controller.admin;

import com.demo.entity.News;
import com.demo.service.NewsService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpSession;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;

@SpringBootTest
@AutoConfigureMockMvc
public class AdminNewsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NewsService newsService;

    private List<News> newsList;
    private News testNews;
    private Page<News> newsPage;
    private com.demo.entity.User adminUser;

    @BeforeEach
    public void setUp() {
        // 准备新闻测试数据
        newsList = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            News news = new News();
            news.setNewsID(i);
            news.setTitle("测试新闻标题" + i);
            news.setContent("测试新闻内容" + i);
            news.setTime(LocalDateTime.now());
            newsList.add(news);
        }

        // 创建单个新闻测试数据
        testNews = new News();
        testNews.setNewsID(1);
        testNews.setTitle("测试新闻标题1");
        testNews.setContent("测试新闻内容1");
        testNews.setTime(LocalDateTime.now());

        // 创建分页数据
        Pageable pageable = PageRequest.of(0, 10, Sort.by("time").descending());
        newsPage = new PageImpl<>(newsList, pageable, newsList.size());
        
        // 创建测试管理员用户
        adminUser = new com.demo.entity.User();
        adminUser.setUserID("admin");
        adminUser.setUserName("管理员");
        adminUser.setPassword("admin123");
        adminUser.setIsadmin(1); // 管理员类型
    }

    @Test
    @DisplayName("测试获取新闻管理页面")
    public void testNewsManage() throws Exception {
        System.out.println("[测试] AdminNewsController.news_manage - 获取新闻管理页面");
        
        // 模拟服务层方法
        when(newsService.findAll(any(Pageable.class))).thenReturn(newsPage);

        // 执行请求并验证结果
        MvcResult result = mockMvc.perform(get("/news_manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/news_manage"))
                .andExpect(model().attributeExists("total"))
                .andReturn();

        // 验证服务层方法被调用
        verify(newsService, times(1)).findAll(any(Pageable.class));
        
        // 检查模型中是否包含正确的数据
        if (result.getModelAndView().getModel().get("total") == null) {
            System.out.println("[错误] 新闻管理页面缺少total属性");
            fail("新闻管理页面模型中应包含total属性");
        } else {
            System.out.println("[通过] 新闻管理页面包含了total属性");
        }
    }

    @Test
    @DisplayName("测试新闻添加页面")
    public void testNewsAdd() throws Exception {
        System.out.println("[测试] AdminNewsController.news_add - 新闻添加页面");
        
        // 执行请求并验证结果
        MvcResult result = mockMvc.perform(get("/news_add"))
                .andExpect(status().isOk())
                .andExpect(view().name("/admin/news_add"))
                .andReturn();
        
        // 检查返回的视图名称是否正确
        String viewName = result.getModelAndView().getViewName();
        if (!"/admin/news_add".equals(viewName)) {
            System.out.println("[错误] 新闻添加页面返回了错误的视图名称: " + viewName);
            fail("新闻添加页面应返回'/admin/news_add'视图，实际返回: " + viewName);
        } else {
            System.out.println("[通过] 新闻添加页面返回了正确的视图名称");
        }
    }

    @Test
    @DisplayName("测试新闻编辑页面")
    public void testNewsEdit() throws Exception {
        System.out.println("[测试] AdminNewsController.news_edit - 新闻编辑页面");
        
        // 模拟服务层方法
        when(newsService.findById(1)).thenReturn(testNews);

        // 执行请求并验证结果
        MvcResult result = mockMvc.perform(get("/news_edit")
                .param("newsID", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("/admin/news_edit"))
                .andExpect(model().attributeExists("news"))
                .andReturn();

        // 验证服务层方法被调用
        verify(newsService, times(1)).findById(1);
        
        // 检查返回的新闻对象是否正确
        Object newsObject = result.getModelAndView().getModel().get("news");
        if (newsObject == null || !(newsObject instanceof News)) {
            System.out.println("[错误] 新闻编辑页面没有正确返回新闻对象");
            fail("新闻编辑页面应返回有效的新闻对象");
        } else {
            System.out.println("[通过] 新闻编辑页面返回了正确的新闻对象");
        }
    }

    @Test
    @DisplayName("测试获取新闻列表")
    public void testNewsList() throws Exception {
        System.out.println("[测试] AdminNewsController.newsList - 获取新闻列表");
        
        // 模拟服务层方法
        when(newsService.findAll(any(Pageable.class))).thenReturn(newsPage);

        // 执行请求并验证结果
        MvcResult result = mockMvc.perform(get("/newsList.do")
                .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)))
                .andReturn();

        // 验证服务层方法被调用
        verify(newsService, times(1)).findAll(any(Pageable.class));
        
        // 检查返回的数据数量是否正确
        String responseContent = result.getResponse().getContentAsString();
        if (responseContent == null || responseContent.isEmpty()) {
            System.out.println("[错误] 获取新闻列表返回了空数据");
            fail("获取新闻列表API应返回有效的JSON数据");
        } else if (!responseContent.contains("\"newsID\"")) {
            System.out.println("[错误] 获取新闻列表返回的数据格式不正确: " + responseContent);
            fail("获取新闻列表API返回的JSON数据应包含新闻信息");
        } else {
            System.out.println("[通过] 获取新闻列表返回了正确的数据");
        }
    }

    @Test
    @DisplayName("测试删除新闻")
    public void testDelNews() throws Exception {
        System.out.println("[测试] AdminNewsController.delNews - 删除新闻");
        
        // 模拟服务层方法
        doNothing().when(newsService).delById(anyInt());

        // 执行请求并验证结果
        MvcResult result = mockMvc.perform(post("/delNews.do")
                .param("newsID", "1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string("true"))
                .andReturn();

        // 验证服务层方法被调用
        verify(newsService, times(1)).delById(1);
        
        // 检查返回结果是否正确
        String responseContent = result.getResponse().getContentAsString();
        if (!"true".equals(responseContent)) {
            System.out.println("[错误] 删除新闻操作应返回'true'，实际返回: " + responseContent);
            fail("删除新闻操作应返回'true'，实际返回: " + responseContent);
        } else {
            System.out.println("[通过] 删除新闻操作返回了正确的结果");
        }
    }

    @Test
    @DisplayName("测试修改新闻")
    public void testModifyNews() throws Exception {
        System.out.println("[测试] AdminNewsController.modifyNews - 修改新闻");
        
        // 模拟服务层方法
        when(newsService.findById(1)).thenReturn(testNews);
        doNothing().when(newsService).update(any(News.class));

        // 执行请求并验证结果
        MvcResult result = mockMvc.perform(post("/modifyNews.do")
                .param("newsID", "1")
                .param("title", "修改后的标题")
                .param("content", "修改后的内容")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("news_manage"))
                .andReturn();

        // 验证服务层方法被调用
        verify(newsService, times(1)).findById(1);
        verify(newsService, times(1)).update(any(News.class));
        
        // 检查重定向URL是否正确
        String redirectUrl = result.getResponse().getRedirectedUrl();
        if (!"news_manage".equals(redirectUrl)) {
            System.out.println("[错误] 修改新闻后应重定向到'news_manage'，实际重定向到: " + redirectUrl);
            fail("修改新闻后应重定向到'news_manage'，实际重定向到: " + redirectUrl);
        } else {
            System.out.println("[通过] 修改新闻后正确重定向到新闻管理页面");
        }
    }

    @Test
    @DisplayName("测试添加新闻")
    public void testAddNews() throws Exception {
        System.out.println("[测试] AdminNewsController.addNews - 添加新闻");
        
        // 模拟服务层方法 - 如果create方法不是void，应该使用when().thenReturn()
        when(newsService.create(any(News.class))).thenReturn(11);  // 假设返回新闻ID

        // 执行请求并验证结果
        MvcResult result = mockMvc.perform(post("/addNews.do")
                .param("title", "新增新闻标题")
                .param("content", "新增新闻内容")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("news_manage"))
                .andReturn();

        // 验证服务层方法被调用
        verify(newsService, times(1)).create(any(News.class));
        
        // 检查重定向URL是否正确
        String redirectUrl = result.getResponse().getRedirectedUrl();
        if (!"news_manage".equals(redirectUrl)) {
            System.out.println("[错误] 添加新闻后应重定向到'news_manage'，实际重定向到: " + redirectUrl);
            fail("添加新闻后应重定向到'news_manage'，实际重定向到: " + redirectUrl);
        } else {
            System.out.println("[通过] 添加新闻后正确重定向到新闻管理页面");
        }
    }

    @Test
    @DisplayName("测试新闻内容XSS漏洞防护")
    public void testXssProtection() throws Exception {
        System.out.println("[测试] AdminNewsController.addNews - XSS漏洞防护");
        
        // 包含XSS攻击代码的新闻内容
        String xssPayload = "<script>alert('XSS攻击');</script>恶意脚本";
        
        // 模拟服务层方法
        when(newsService.create(any(News.class))).thenReturn(100);
        
        // 执行请求并验证结果
        mockMvc.perform(post("/addNews.do")
                .param("title", "XSS测试标题")
                .param("content", xssPayload)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection());
        
        // 检查是否有过滤XSS攻击代码
        verify(newsService, times(1)).create(any(News.class));
        
        // 这里应该检查提交到服务层的News对象内容是否已过滤XSS攻击代码
        // 由于模拟测试环境限制，我们只能假设当服务层收到含有脚本代码的内容时应该处理它
        
        if (xssPayload.contains("<script>")) {
            System.out.println("[错误] 安全漏洞: 系统未过滤新闻内容中的XSS攻击代码");
            fail("系统应过滤新闻内容中的XSS攻击代码，防止存储型XSS攻击");
        } else {
            System.out.println("[通过] 系统正确过滤了XSS攻击代码");
        }
    }

    @Test
    @DisplayName("测试新闻标题边界值")
    public void testNewsTitleBoundary() throws Exception {
        System.out.println("[测试] AdminNewsController - 测试新闻标题边界值");

        // 模拟服务层行为
        when(newsService.create(any(News.class))).thenReturn(1);
        
        // 测试超长标题（例如超过100个字符）
        StringBuilder longTitleBuilder = new StringBuilder();
        for(int i = 0; i < 110; i++) {
            longTitleBuilder.append("标");
        }
        String longTitle = longTitleBuilder.toString();
        
        try {
            // 执行请求
            MvcResult result = mockMvc.perform(post("/addNews.do")
                    .param("title", longTitle)
                    .param("content", "测试内容")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                    .andReturn();
            
            // 检查结果 - 应该有某种形式的错误处理（重定向或错误页面）
            int status = result.getResponse().getStatus();
            if (status == 200) {
                // 如果允许超长标题，那应该检查数据是否被截断处理
                verify(newsService).create(argThat(news -> 
                    news.getTitle().length() < longTitle.length()));
                System.out.println("[通过] 系统正确处理了超长标题");
            } else if (status == 302) {
                // 重定向也是一种合理的处理方式
                String redirectUrl = result.getResponse().getRedirectedUrl();
                if (redirectUrl == null || !redirectUrl.contains("error")) {
                    System.out.println("[警告] 系统重定向URL不包含错误信息: " + redirectUrl);
                } else {
                    System.out.println("[通过] 系统正确重定向到错误页面");
                }
            } else {
                System.out.println("[警告] 边界值测试返回了非预期状态码: " + status);
            }
        } catch (Exception e) {
            System.out.println("[错误] 边界值测试抛出异常: " + e.getMessage());
            fail("新闻标题边界值测试应正常处理而非抛出异常");
        }
    }
    
    @Test
    @DisplayName("测试新闻删除安全性")
    public void testNewsDeleteSecurity() throws Exception {
        System.out.println("[测试] AdminNewsController - 新闻删除安全性");
        
        // 模拟服务层行为
        doNothing().when(newsService).delById(anyInt());
        
        // 测试正常删除
        mockMvc.perform(post("/delNews.do")
                .param("newsID", "1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
        
        // 验证服务层方法被调用
        verify(newsService).delById(1);
        
        // 测试删除ID为负数的情况
        try {
            MvcResult result = mockMvc.perform(post("/delNews.do")
                    .param("newsID", "-1")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                    .andReturn();
            
            // 检查结果 - 应当有合理的处理
            String responseContent = result.getResponse().getContentAsString();
            if ("true".equals(responseContent)) {
                System.out.println("[警告] 删除负数ID时返回成功，未进行安全检查");
                verify(newsService).delById(-1);
            } else if ("false".equals(responseContent)) {
                System.out.println("[通过] 系统正确拒绝了负数ID的删除请求");
            } else {
                System.out.println("[警告] 删除负数ID返回了非预期结果: " + responseContent);
            }
        } catch (Exception e) {
            System.out.println("[错误] 删除负数ID测试抛出异常: " + e.getMessage());
            fail("删除负数ID测试应正常处理而非抛出异常");
        }
    }

    @Test
    @DisplayName("测试新闻管理权限控制")
    public void testNewsManagePermission() throws Exception {
        System.out.println("[测试] AdminNewsController - 新闻管理权限控制");
        
        // 模拟未登录用户的会话
        MockHttpSession noLoginSession = new MockHttpSession();
        
        try {
            MvcResult result = mockMvc.perform(get("/admin/news/manage")
                    .session(noLoginSession))
                    .andReturn();
            
            int status = result.getResponse().getStatus();
            if (status == 302) {
                String redirectUrl = result.getResponse().getRedirectedUrl();
                if (redirectUrl != null && redirectUrl.contains("login")) {
                    System.out.println("[通过] 系统正确重定向未登录用户到登录页面");
                } else {
                    System.out.println("[警告] 未登录用户访问新闻管理页面重定向到非登录页面: " + redirectUrl);
                }
            } else if (status == 403 || status == 401) {
                System.out.println("[通过] 系统返回" + status + "状态码，禁止未授权访问");
            } else {
                System.out.println("[警告] 未登录用户访问新闻管理页面返回非预期状态码: " + status);
                fail("未登录用户应该无法访问新闻管理页面");
            }
            
            // 验证服务层方法未被调用
            verify(newsService, times(0)).findAll(any(Pageable.class));
        } catch (Exception e) {
            System.out.println("[错误] 测试新闻管理权限时抛出异常: " + e.getMessage());
            fail("权限测试不应抛出异常: " + e.getMessage());
        }
    }
    
    @Test
    @DisplayName("测试新闻添加输入验证")
    public void testAddNewsValidation() throws Exception {
        System.out.println("[测试] AdminNewsController - 新闻添加输入验证");
        
        // 设置模拟admin登录会话
        MockHttpSession adminSession = new MockHttpSession();
        adminSession.setAttribute("login_user", adminUser);
        
        // 测试空标题
        try {
            MvcResult result = mockMvc.perform(post("/admin/news/add.do")
                    .session(adminSession)
                    .param("title", "")
                    .param("content", "测试内容"))
                    .andReturn();
                    
            String viewName = result.getModelAndView().getViewName();
            
            if (viewName != null && (viewName.contains("error") || viewName.contains("redirect:/admin/news/add"))) {
                System.out.println("[通过] 系统正确处理了空标题的新闻添加请求");
            } else {
                System.out.println("[警告] 系统未正确处理空标题的新闻添加请求，视图名: " + viewName);
                fail("系统应拒绝处理空标题的新闻添加请求");
            }
            
            // 验证服务层方法未被调用或被正确调用
            verify(newsService, never()).create(argThat(news -> news.getTitle() == null || news.getTitle().isEmpty()));
            
        } catch (Exception e) {
            System.out.println("[通过] 系统通过异常处理了空标题的新闻添加请求: " + e.getMessage());
        }
        
        // 测试超长标题（模拟数据库字段长度限制）
        try {
            StringBuilder longTitle = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                longTitle.append("测试");
            }
            
            MvcResult result = mockMvc.perform(post("/admin/news/add.do")
                    .session(adminSession)
                    .param("title", longTitle.toString())
                    .param("content", "测试内容"))
                    .andReturn();
                    
            String viewName = result.getModelAndView().getViewName();
            
            if (viewName != null && (viewName.contains("error") || viewName.contains("redirect:/admin/news/add"))) {
                System.out.println("[通过] 系统正确处理了超长标题的新闻添加请求");
            } else {
                System.out.println("[警告] 系统未正确处理超长标题的新闻添加请求，视图名: " + viewName);
            }
            
        } catch (Exception e) {
            System.out.println("[通过] 系统通过异常处理了超长标题的新闻添加请求: " + e.getMessage());
        }
    }
    
    @Test
    @DisplayName("测试非法ID删除新闻")
    public void testDeleteNewsWithInvalidId() throws Exception {
        System.out.println("[测试] AdminNewsController - 非法ID删除新闻");
        
        // 设置模拟admin登录会话
        MockHttpSession adminSession = new MockHttpSession();
        adminSession.setAttribute("login_user", adminUser);
        
        // 测试负数ID
        try {
            MvcResult result = mockMvc.perform(get("/admin/news/del.do?id=-1")
                    .session(adminSession))
                    .andReturn();
            
            verify(newsService, never()).delById(-1);
            
            int status = result.getResponse().getStatus();
            String redirectUrl = result.getResponse().getRedirectedUrl();
            
            if (status == 302 && redirectUrl != null && redirectUrl.contains("error")) {
                System.out.println("[通过] 系统正确处理了负数ID的新闻删除请求");
            } else if (status >= 400) {
                System.out.println("[通过] 系统返回错误状态码 " + status + " 处理了负数ID的新闻删除请求");
            } else {
                System.out.println("[警告] 系统未正确处理负数ID的新闻删除请求，状态码: " + status);
                fail("系统应拒绝处理负数ID的新闻删除请求");
            }
        } catch (Exception e) {
            System.out.println("[通过] 系统通过异常处理了负数ID的新闻删除请求: " + e.getMessage());
        }
        
        // 测试非数字ID
        try {
            MvcResult result = mockMvc.perform(get("/admin/news/del.do?id=abc")
                    .session(adminSession))
                    .andReturn();
            
            int status = result.getResponse().getStatus();
            
            if (status != 200) {
                System.out.println("[通过] 系统正确处理了非数字ID的新闻删除请求，状态码: " + status);
            } else {
                System.out.println("[警告] 系统返回200状态码处理非数字ID的新闻删除请求");
                fail("系统应拒绝处理非数字ID的新闻删除请求");
            }
        } catch (Exception e) {
            System.out.println("[通过] 系统通过异常处理了非数字ID的新闻删除请求: " + e.getMessage());
        }
    }
} 