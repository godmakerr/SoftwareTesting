package com.demo.controller;

import com.demo.entity.Message;
import com.demo.entity.News;
import com.demo.entity.Venue;
import com.demo.entity.vo.MessageVo;
import com.demo.service.MessageService;
import com.demo.service.MessageVoService;
import com.demo.service.NewsService;
import com.demo.service.VenueService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
public class IndexControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NewsService newsService;

    @MockBean
    private VenueService venueService;

    @MockBean
    private MessageService messageService;

    @MockBean
    private MessageVoService messageVoService;

    private List<News> newsList;
    private List<Venue> venueList;
    private List<Message> messageList;
    private List<MessageVo> messageVoList;
    private Page<Message> messagePage;

    @BeforeEach
    public void setUp() {
        // 准备新闻测试数据
        newsList = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            News news = new News();
            news.setNewsID(i);
            news.setTitle("测试新闻标题" + i);
            news.setContent("测试新闻内容" + i);
            news.setTime(LocalDateTime.now());
            newsList.add(news);
        }

        // 准备场馆测试数据
        venueList = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Venue venue = new Venue();
            venue.setVenueID(i);
            venue.setVenueName("测试场馆" + i);
            venue.setAddress("测试地址" + i);
            venue.setPrice(100 * i);
            venue.setDescription("测试描述" + i);
            venue.setOpen_time("09:00");
            venue.setClose_time("18:00");
            venueList.add(venue);
        }

        // 准备消息测试数据
        messageList = new ArrayList<>();
        messageVoList = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Message message = new Message();
            message.setMessageID(i);
            message.setUserID("user" + i);
            message.setContent("测试消息内容" + i);
            message.setTime(LocalDateTime.now());
            message.setState(2); // 通过状态
            messageList.add(message);

            MessageVo messageVo = new MessageVo();
            messageVo.setMessageID(i);
            messageVo.setUserID("user" + i);
            messageVo.setUserName("测试用户" + i);
            messageVo.setContent("测试消息内容" + i);
            messageVo.setTime(LocalDateTime.now());
            messageVo.setPicture("");
            messageVo.setState(2);
            messageVoList.add(messageVo);
        }

        // 创建分页数据
        Pageable pageable = PageRequest.of(0, 5, Sort.by("time").descending());
        messagePage = new PageImpl<>(messageList, pageable, messageList.size());
    }

    @Test
    @DisplayName("测试首页")
    public void testIndex() throws Exception {
        // 模拟服务层方法
        Pageable venue_pageable = PageRequest.of(0, 5, Sort.by("venueID").ascending());
        Pageable news_pageable = PageRequest.of(0, 5, Sort.by("time").descending());
        Pageable message_pageable = PageRequest.of(0, 5, Sort.by("time").descending());

        Page<Venue> venuePage = new PageImpl<>(venueList, venue_pageable, venueList.size());
        Page<News> newsPage = new PageImpl<>(newsList, news_pageable, newsList.size());

        when(venueService.findAll(any(Pageable.class))).thenReturn(venuePage);
        when(newsService.findAll(any(Pageable.class))).thenReturn(newsPage);
        when(messageService.findPassState(any(Pageable.class))).thenReturn(messagePage);
        when(messageVoService.returnVo(messageList)).thenReturn(messageVoList);

        System.out.println("[测试] IndexController.index - 基本功能");
        
        // 执行请求并验证结果
        MvcResult result = mockMvc.perform(get("/index"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("news_list"))
                .andExpect(model().attributeExists("venue_list"))
                .andExpect(model().attributeExists("message_list"))
                .andReturn();

        // 验证服务层方法被调用
        verify(venueService, times(1)).findAll(any(Pageable.class));
        verify(newsService, times(1)).findAll(any(Pageable.class));
        verify(messageService, times(1)).findPassState(any(Pageable.class));
        verify(messageVoService, times(1)).returnVo(messageList);
        
        // 检查模型中的数据是否正确
        if (result.getModelAndView().getModel().get("news_list") == null) {
            System.out.println("[错误] 首页缺少新闻列表数据");
            fail("首页模型中应包含新闻列表数据");
        } else {
            System.out.println("[通过] 首页包含了新闻列表数据");
        }
    }

    @Test
    @DisplayName("测试管理员首页")
    public void testAdminIndex() throws Exception {
        System.out.println("[测试] IndexController.admin_index - 管理员首页访问");
        
        // 执行请求并验证结果
        MvcResult result = mockMvc.perform(get("/admin_index"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/admin_index"))
                .andReturn();
        
        // 检查返回的视图名称是否正确
        String viewName = result.getModelAndView().getViewName();
        if (!"admin/admin_index".equals(viewName)) {
            System.out.println("[错误] 管理员首页返回了错误的视图: " + viewName);
            fail("管理员首页应返回'admin/admin_index'视图，实际返回: " + viewName);
        } else {
            System.out.println("[通过] 管理员首页返回了正确的视图");
        }
    }

    @Test
    @DisplayName("测试首页性能")
    public void testIndexPerformance() throws Exception {
        System.out.println("[测试] IndexController.index - 性能测试");
        
        // 创建大量数据测试性能
        List<Venue> largeVenueList = new ArrayList<>();
        List<News> largeNewsList = new ArrayList<>();
        List<Message> largeMessageList = new ArrayList<>();
        List<MessageVo> largeMessageVoList = new ArrayList<>();
        
        // 生成100个场馆数据
        for (int i = 1; i <= 100; i++) {
            Venue venue = new Venue();
            venue.setVenueID(i);
            venue.setVenueName("性能测试场馆" + i);
            venue.setAddress("性能测试地址" + i);
            venue.setPrice(100);
            venue.setDescription("性能测试描述" + i);
            venue.setOpen_time("09:00");
            venue.setClose_time("18:00");
            largeVenueList.add(venue);
            
            News news = new News();
            news.setNewsID(i);
            news.setTitle("性能测试新闻标题" + i);
            news.setContent("性能测试新闻内容" + i);
            news.setTime(LocalDateTime.now());
            largeNewsList.add(news);
            
            Message message = new Message();
            message.setMessageID(i);
            message.setUserID("user" + i);
            message.setContent("性能测试消息内容" + i);
            message.setTime(LocalDateTime.now());
            message.setState(2);
            largeMessageList.add(message);
            
            MessageVo messageVo = new MessageVo();
            messageVo.setMessageID(i);
            messageVo.setUserID("user" + i);
            messageVo.setUserName("性能测试用户" + i);
            messageVo.setContent("性能测试消息内容" + i);
            messageVo.setTime(LocalDateTime.now());
            messageVo.setPicture("");
            messageVo.setState(2);
            largeMessageVoList.add(messageVo);
        }
        
        // 创建分页数据
        Pageable venue_pageable = PageRequest.of(0, 100, Sort.by("venueID").ascending());
        Pageable news_pageable = PageRequest.of(0, 100, Sort.by("time").descending());
        Pageable message_pageable = PageRequest.of(0, 100, Sort.by("time").descending());
        
        Page<Venue> largeVenuePage = new PageImpl<>(largeVenueList, venue_pageable, largeVenueList.size());
        Page<News> largeNewsPage = new PageImpl<>(largeNewsList, news_pageable, largeNewsList.size());
        Page<Message> largeMessagePage = new PageImpl<>(largeMessageList, message_pageable, largeMessageList.size());
        
        // 模拟服务层方法返回大量数据
        when(venueService.findAll(any(Pageable.class))).thenReturn(largeVenuePage);
        when(newsService.findAll(any(Pageable.class))).thenReturn(largeNewsPage);
        when(messageService.findPassState(any(Pageable.class))).thenReturn(largeMessagePage);
        when(messageVoService.returnVo(largeMessageList)).thenReturn(largeMessageVoList);
        
        // 记录开始时间
        long startTime = System.currentTimeMillis();
        
        // 执行请求并捕获结果
        MvcResult result = mockMvc.perform(get("/index"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andReturn();
        
        // 计算执行时间
        long executionTime = System.currentTimeMillis() - startTime;
        System.out.println("[信息] 首页加载时间: " + executionTime + "毫秒");
        
        // 检查性能问题 - 假设我们期望首页加载时间不超过1000毫秒（这是一个示例阈值）
        // 注意：在实际环境中，这个值取决于系统性能要求
        if (executionTime > 1000) {
            System.out.println("[错误] 首页加载时间过长: " + executionTime + " 毫秒 > 1000 毫秒");
            fail("首页加载时间过长，可能存在性能问题。当前加载时间: " + executionTime + " 毫秒");
        } else {
            System.out.println("[通过] 首页加载时间在合理范围内");
        }
    }
} 