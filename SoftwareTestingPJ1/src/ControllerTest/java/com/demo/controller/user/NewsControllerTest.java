package com.demo.controller.user;

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
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
public class NewsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NewsService newsService;

    private List<News> newsList;
    private Page<News> newsPage;
    private News testNews;

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

        // 创建单个新闻测试数据
        testNews = new News();
        testNews.setNewsID(1);
        testNews.setTitle("测试新闻标题1");
        testNews.setContent("测试新闻内容1");
        testNews.setTime(LocalDateTime.now());

        // 创建分页数据
        Pageable pageable = PageRequest.of(0, 5, Sort.by("time").descending());
        newsPage = new PageImpl<>(newsList, pageable, newsList.size());
    }

    @Test
    @DisplayName("测试获取新闻详情页面")
    public void testNewsDetail() throws Exception {
        // 模拟服务层方法
        when(newsService.findById(1)).thenReturn(testNews);

        // 执行请求并验证结果
        mockMvc.perform(get("/news").param("newsID", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("news"))
                .andExpect(model().attributeExists("news"));

        // 验证服务层方法被调用
        verify(newsService, times(1)).findById(1);
    }

    @Test
    @DisplayName("测试获取新闻列表页面")
    public void testNewsListPage() throws Exception {
        // 模拟服务层方法
        when(newsService.findAll(any(Pageable.class))).thenReturn(newsPage);

        // 执行请求并验证结果
        mockMvc.perform(get("/news_list"))
                .andExpect(status().isOk())
                .andExpect(view().name("news_list"))
                .andExpect(model().attributeExists("news_list"))
                .andExpect(model().attributeExists("total"));

        // 验证服务层方法被调用
        verify(newsService, times(2)).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("测试获取新闻列表 - 正常分页")
    public void testGetNewsList() throws Exception {
        // 模拟服务层方法
        when(newsService.findAll(any(Pageable.class))).thenReturn(newsPage);

        // 执行请求并验证结果
        mockMvc.perform(get("/news/getNewsList")
                .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)));

        // 验证服务层方法被调用
        verify(newsService, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("测试获取新闻列表 - 超出范围页码")
    public void testGetNewsListOutOfRange() throws Exception {
        // 创建空页面数据
        Pageable pageable = PageRequest.of(9998, 5, Sort.by("time").descending());
        Page<News> emptyPage = new PageImpl<>(new ArrayList<>(), pageable, 0);

        // 模拟服务层方法
        when(newsService.findAll(any(Pageable.class))).thenReturn(emptyPage);

        // 执行请求并验证结果
        mockMvc.perform(get("/news/getNewsList")
                .param("page", "9999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        // 验证服务层方法被调用
        verify(newsService, times(1)).findAll(any(Pageable.class));
    }
} 