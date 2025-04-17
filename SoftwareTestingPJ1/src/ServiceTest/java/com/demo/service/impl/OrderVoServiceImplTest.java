package com.demo.service.impl;

import com.demo.dao.OrderDao;
import com.demo.dao.VenueDao;
import com.demo.entity.Order;
import com.demo.entity.Venue;
import com.demo.entity.vo.OrderVo;
import com.demo.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderVoServiceImplTest {

    @Mock
    private OrderDao orderDao;

    @Mock
    private VenueDao venueDao;

    @InjectMocks
    private OrderVoServiceImpl orderVoService;

    private Order testOrder;
    private Venue testVenue;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        now = LocalDateTime.now();
        
        // 创建测试场馆
        testVenue = new Venue();
        testVenue.setVenueID(1);
        testVenue.setVenueName("测试场馆");
        testVenue.setPrice(100);
        testVenue.setAddress("测试地址");
        testVenue.setDescription("测试描述");
        testVenue.setOpen_time("09:00");
        testVenue.setClose_time("21:00");
        
        // 创建测试订单
        testOrder = new Order();
        testOrder.setOrderID(1);
        testOrder.setUserID("test");
        testOrder.setVenueID(1);
        testOrder.setState(OrderService.STATE_NO_AUDIT);
        testOrder.setOrderTime(now);
        testOrder.setStartTime(now.plusDays(1));
        testOrder.setHours(2);
        testOrder.setTotal(200);
    }

    @Test
    @DisplayName("测试根据订单ID返回OrderVo - 正常情况")
    void returnOrderVoByOrderID_Success() {
        // 设置模拟行为
        when(orderDao.findByOrderID(1)).thenReturn(testOrder);
        when(venueDao.findByVenueID(1)).thenReturn(testVenue);
        
        // 执行测试
        OrderVo result = orderVoService.returnOrderVoByOrderID(1);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getOrderID());
        assertEquals("test", result.getUserID());
        assertEquals(1, result.getVenueID());
        assertEquals("测试场馆", result.getVenueName());
        assertEquals(OrderService.STATE_NO_AUDIT, result.getState());
        assertEquals(2, result.getHours());
        assertEquals(200, result.getTotal());
        
        // 验证交互
        verify(orderDao, times(1)).findByOrderID(1);
        verify(venueDao, times(1)).findByVenueID(1);
    }

    @Test
    @DisplayName("测试将订单列表转换为OrderVo列表 - 正常情况")
    void returnVo_Success() {
        // 准备测试数据
        List<Order> orderList = new ArrayList<>();
        orderList.add(testOrder);
        
        // 设置模拟行为
        when(orderDao.findByOrderID(1)).thenReturn(testOrder);
        when(venueDao.findByVenueID(1)).thenReturn(testVenue);
        
        // 执行测试
        List<OrderVo> result = orderVoService.returnVo(orderList);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getOrderID());
        assertEquals("test", result.get(0).getUserID());
        assertEquals("测试场馆", result.get(0).getVenueName());
        
        // 验证交互
        verify(orderDao, times(1)).findByOrderID(1);
        verify(venueDao, times(1)).findByVenueID(1);
    }

    @Test
    @DisplayName("测试将空订单列表转换为OrderVo列表 - 边界情况")
    void returnVo_EmptyList() {
        // 准备测试数据
        List<Order> orderList = new ArrayList<>();
        
        // 执行测试
        List<OrderVo> result = orderVoService.returnVo(orderList);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(0, result.size());
        
        // 验证交互
        verify(orderDao, never()).findByOrderID(anyInt());
        verify(venueDao, never()).findByVenueID(anyInt());
    }
} 