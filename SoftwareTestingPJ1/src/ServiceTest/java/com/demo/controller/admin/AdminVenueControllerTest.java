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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @BeforeEach
    public void setUp() {
        // 准备测试数据
        venueList = new ArrayList<>();
        
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
        // 模拟服务层方法
        when(venueService.findAll(any(Pageable.class))).thenReturn(venuePage);
        
        // 执行请求并验证结果
        mockMvc.perform(get("/venue_manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/venue_manage"))
                .andExpect(model().attributeExists("total"));
        
        // 验证服务层方法被调用
        verify(venueService, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("测试场馆编辑页面")
    public void testVenueEdit() throws Exception {
        // 模拟服务层方法
        when(venueService.findByVenueID(1)).thenReturn(testVenue);
        
        // 执行请求并验证结果
        mockMvc.perform(get("/venue_edit")
                .param("venueID", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("/admin/venue_edit"))
                .andExpect(model().attributeExists("venue"));
        
        // 验证服务层方法被调用
        verify(venueService, times(1)).findByVenueID(1);
    }

    @Test
    @DisplayName("测试场馆添加页面")
    public void testVenueAdd() throws Exception {
        // 执行请求并验证结果
        mockMvc.perform(get("/venue_add"))
                .andExpect(status().isOk())
                .andExpect(view().name("/admin/venue_add"));
    }

    @Test
    @DisplayName("测试获取场馆列表")
    public void testGetVenueList() throws Exception {
        // 模拟服务层方法
        when(venueService.findAll(any(Pageable.class))).thenReturn(venuePage);
        
        // 执行请求并验证结果
        mockMvc.perform(get("/venueList.do")
                .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));
        
        // 验证服务层方法被调用
        verify(venueService, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("测试删除场馆")
    public void testDelVenue() throws Exception {
        // 模拟服务层方法
        doNothing().when(venueService).delById(anyInt());
        
        // 执行请求并验证结果
        mockMvc.perform(post("/delVenue.do")
                .param("venueID", "1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
        
        // 验证服务层方法被调用
        verify(venueService, times(1)).delById(1);
    }

    @Test
    @DisplayName("测试检查场馆名称 - 可用")
    public void testCheckVenueNameAvailable() throws Exception {
        // 模拟服务层方法
        when(venueService.countVenueName("新场馆")).thenReturn(0);
        
        // 执行请求并验证结果
        mockMvc.perform(post("/checkVenueName.do")
                .param("venueName", "新场馆")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
        
        // 验证服务层方法被调用
        verify(venueService, times(1)).countVenueName("新场馆");
    }
    
    @Test
    @DisplayName("测试检查场馆名称 - 不可用")
    public void testCheckVenueNameUnavailable() throws Exception {
        // 模拟服务层方法
        when(venueService.countVenueName("测试场馆1")).thenReturn(1);
        
        // 执行请求并验证结果
        mockMvc.perform(post("/checkVenueName.do")
                .param("venueName", "测试场馆1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(false));
        
        // 验证服务层方法被调用
        verify(venueService, times(1)).countVenueName("测试场馆1");
    }
} 