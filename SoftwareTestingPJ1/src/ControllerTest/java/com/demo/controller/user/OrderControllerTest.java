package com.demo.controller.user;

import com.demo.entity.Order;
import com.demo.entity.User;
import com.demo.entity.Venue;
import com.demo.entity.vo.OrderVo;
import com.demo.entity.vo.VenueOrder;
import com.demo.exception.LoginException;
import com.demo.service.OrderService;
import com.demo.service.OrderVoService;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

import org.mockito.ArgumentCaptor;

@SpringBootTest
@AutoConfigureMockMvc
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private OrderVoService orderVoService;

    @MockBean
    private VenueService venueService;

    private User testUser;
    private MockHttpSession session;
    private List<Order> orderList;
    private List<OrderVo> orderVoList;
    private Page<Order> orderPage;
    private Venue testVenue;
    private Order testOrder;
    private VenueOrder venueOrder;

    @BeforeEach
    public void setUp() {
        // 准备测试用户
        testUser = new User();
        testUser.setId(1);
        testUser.setUserID("user1");
        testUser.setUserName("测试用户");
        testUser.setPassword("password");
        testUser.setEmail("test@example.com");
        testUser.setPhone("12345678901");
        testUser.setIsadmin(0);
        testUser.setPicture("");

        // 创建用户会话
        session = new MockHttpSession();
        session.setAttribute("user", testUser);

        // 准备测试场馆
        testVenue = new Venue();
        testVenue.setVenueID(1);
        testVenue.setVenueName("测试场馆");
        testVenue.setAddress("测试地址");
        testVenue.setPrice(100);
        testVenue.setDescription("测试描述");
        testVenue.setOpen_time("09:00");
        testVenue.setClose_time("18:00");

        // 准备测试订单数据
        orderList = new ArrayList<>();
        orderVoList = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();
        for (int i = 1; i <= 5; i++) {
            Order order = new Order();
            order.setOrderID(i);
            order.setUserID("user1");
            order.setVenueID(1);
            order.setOrderTime(now.minusDays(i));
            order.setStartTime(now.plusDays(i));
            order.setHours(2);
            order.setState(1);
            order.setTotal(200);
            orderList.add(order);

            OrderVo orderVo = new OrderVo();
            orderVo.setOrderID(i);
            orderVo.setUserID("user1");
            orderVo.setVenueName("测试场馆");
            orderVo.setOrderTime(now.minusDays(i));
            orderVo.setStartTime(now.plusDays(i));
            orderVo.setHours(2);
            orderVo.setState(1);
            orderVo.setTotal(200);
            orderVoList.add(orderVo);
        }

        // 创建测试订单
        testOrder = new Order();
        testOrder.setOrderID(1);
        testOrder.setUserID("user1");
        testOrder.setVenueID(1);
        testOrder.setOrderTime(now);
        testOrder.setStartTime(now.plusDays(1));
        testOrder.setHours(2);
        testOrder.setState(1);
        testOrder.setTotal(200);

        // 创建分页数据
        Pageable pageable = PageRequest.of(0, 5, Sort.by("orderTime").descending());
        orderPage = new PageImpl<>(orderList, pageable, orderList.size());

        // 创建场馆订单信息
        venueOrder = new VenueOrder();
        venueOrder.setVenue(testVenue);
        venueOrder.setOrders(orderList);

        System.out.println("[INFO] 初始化OrderControllerTest测试环境");
    }

    @Test
    @DisplayName("测试获取订单管理页面 - 用户已登录")
    public void testOrderManageWithLoginUser() throws Exception {
        // 模拟服务层方法
        when(orderService.findUserOrder(eq("user1"), any(Pageable.class))).thenReturn(orderPage);

        // 执行请求并验证结果
        mockMvc.perform(get("/order_manage").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("order_manage"))
                .andExpect(model().attributeExists("total"));

        // 验证服务层方法被调用
        verify(orderService, times(1)).findUserOrder(eq("user1"), any(Pageable.class));
    }

    @Test
    @DisplayName("测试获取订单管理页面 - 用户未登录")
    public void testOrderManageWithoutLoginUser() throws Exception {
        // 执行请求并验证结果
        try {
            mockMvc.perform(get("/order_manage"));
        } catch (Exception e) {
            // 验证抛出的异常类型
            assertTrue(e.getCause() instanceof LoginException);
        }
    }

    @Test
    @DisplayName("测试获取下单页面 - 指定场馆")
    public void testOrderPlaceWithVenueID() throws Exception {
        // 模拟服务层方法
        when(venueService.findByVenueID(1)).thenReturn(testVenue);

        // 执行请求并验证结果
        mockMvc.perform(get("/order_place.do").param("venueID", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("order_place"))
                .andExpect(model().attributeExists("venue"));

        // 验证服务层方法被调用
        verify(venueService, times(1)).findByVenueID(1);
    }

    @Test
    @DisplayName("测试获取下单页面 - 未指定场馆")
    public void testOrderPlaceWithoutVenueID() throws Exception {
        // 执行请求并验证结果
        mockMvc.perform(get("/order_place"))
                .andExpect(status().isOk())
                .andExpect(view().name("order_place"));
    }

    @Test
    @DisplayName("测试获取用户订单列表 - 用户已登录")
    public void testGetOrderListWithLoginUser() throws Exception {
        // 模拟服务层方法
        when(orderService.findUserOrder(eq("user1"), any(Pageable.class))).thenReturn(orderPage);
        when(orderVoService.returnVo(orderList)).thenReturn(orderVoList);

        // 执行请求并验证结果
        mockMvc.perform(get("/getOrderList.do")
                .param("page", "1")
                .session(session))
                .andExpect(status().isOk());

        // 验证服务层方法被调用
        verify(orderService, times(1)).findUserOrder(eq("user1"), any(Pageable.class));
        verify(orderVoService, times(1)).returnVo(orderList);
    }

    @Test
    @DisplayName("测试获取用户订单列表 - 用户未登录")
    public void testGetOrderListWithoutLoginUser() throws Exception {
        // 执行请求并验证结果
        try {
            mockMvc.perform(get("/getOrderList.do").param("page", "1"));
        } catch (Exception e) {
            // 验证抛出的异常类型
            assertTrue(e.getCause() instanceof LoginException);
        }
    }

    @Test
    @DisplayName("测试添加订单 - 用户已登录")
    public void testAddOrderWithLoginUser() throws Exception {
        // 模拟服务层方法
        doNothing().when(orderService).submit(anyString(), any(LocalDateTime.class), anyInt(), anyString());

        // 执行请求并验证结果
        mockMvc.perform(post("/addOrder.do")
                .param("venueName", "测试场馆")
                .param("date", "2023-05-01")
                .param("startTime", "2023-05-01 14:00")
                .param("hours", "2")
                .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("order_manage"));

        // 验证服务层方法被调用
        verify(orderService, times(1)).submit(anyString(), any(LocalDateTime.class), anyInt(), anyString());
    }

    @Test
    @DisplayName("测试添加订单 - 用户未登录")
    public void testAddOrderWithoutLoginUser() throws Exception {
        // 执行请求并验证结果
        try {
            mockMvc.perform(post("/addOrder.do")
                    .param("venueName", "测试场馆")
                    .param("date", "2023-05-01")
                    .param("startTime", "2023-05-01 14:00")
                    .param("hours", "2"));
        } catch (Exception e) {
            // 验证抛出的异常类型
            assertTrue(e.getCause() instanceof LoginException);
        }
    }

    @Test
    @DisplayName("测试完成订单")
    public void testFinishOrder() throws Exception {
        // 模拟服务层方法
        doNothing().when(orderService).finishOrder(1);

        // 执行请求并验证结果
        mockMvc.perform(post("/finishOrder.do")
                .param("orderID", "1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk());

        // 验证服务层方法被调用
        verify(orderService, times(1)).finishOrder(1);
    }

    @Test
    @DisplayName("测试获取订单编辑页面")
    public void testEditOrder() throws Exception {
        // 模拟服务层方法
        when(orderService.findById(1)).thenReturn(testOrder);
        when(venueService.findByVenueID(1)).thenReturn(testVenue);

        // 执行请求并验证结果
        mockMvc.perform(get("/modifyOrder.do")
                .param("orderID", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("order_edit"))
                .andExpect(model().attributeExists("venue"))
                .andExpect(model().attributeExists("order"));

        // 验证服务层方法被调用
        verify(orderService, times(1)).findById(1);
        verify(venueService, times(1)).findByVenueID(1);
    }

    @Test
    @DisplayName("测试修改订单 - 用户已登录")
    public void testModifyOrderWithLoginUser() throws Exception {
        // 模拟服务层方法
        doNothing().when(orderService).updateOrder(anyInt(), anyString(), any(LocalDateTime.class), anyInt(), anyString());

        // 执行请求并验证结果
        mockMvc.perform(post("/modifyOrder")
                .param("orderID", "1")
                .param("venueName", "测试场馆")
                .param("date", "2023-05-01")
                .param("startTime", "2023-05-01 14:00")
                .param("hours", "2")
                .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("order_manage"));

        // 验证服务层方法被调用
        verify(orderService, times(1)).updateOrder(anyInt(), anyString(), any(LocalDateTime.class), anyInt(), anyString());
    }

    @Test
    @DisplayName("测试修改订单 - 用户未登录")
    public void testModifyOrderWithoutLoginUser() throws Exception {
        // 执行请求并验证结果
        try {
            mockMvc.perform(post("/modifyOrder")
                    .param("orderID", "1")
                    .param("venueName", "测试场馆")
                    .param("date", "2023-05-01")
                    .param("startTime", "2023-05-01 14:00")
                    .param("hours", "2"));
        } catch (Exception e) {
            // 验证抛出的异常类型
            assertTrue(e.getCause() instanceof LoginException);
        }
    }

    @Test
    @DisplayName("测试删除订单")
    public void testDelOrder() throws Exception {
        // 模拟服务层方法
        doNothing().when(orderService).delOrder(1);

        // 执行请求并验证结果
        mockMvc.perform(post("/delOrder.do")
                .param("orderID", "1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        // 验证服务层方法被调用
        verify(orderService, times(1)).delOrder(1);
    }

    @Test
    @DisplayName("测试获取场馆订单列表")
    public void testGetVenueOrderList() throws Exception {
        // 模拟服务层方法
        when(venueService.findByVenueName("测试场馆")).thenReturn(testVenue);
        when(orderService.findDateOrder(eq(1), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(orderList);

        // 执行请求并验证结果
        mockMvc.perform(get("/order/getOrderList.do")
                .param("venueName", "测试场馆")
                .param("date", "2023-05-01"))
                .andExpect(status().isOk());

        // 验证服务层方法被调用
        verify(venueService, times(1)).findByVenueName("测试场馆");
        verify(orderService, times(1)).findDateOrder(eq(1), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("测试整数溢出漏洞")
    public void testIntegerOverflowVulnerability() throws Exception {
        System.out.println("[测试] OrderController.addOrder - 整数溢出漏洞");
        
        // 创建超大小时数量
        String hugeHours = "2147483647"; // Integer.MAX_VALUE
        
        // 模拟服务层方法
        ArgumentCaptor<Integer> hoursCaptor = ArgumentCaptor.forClass(Integer.class);
        doNothing().when(orderService).submit(anyString(), any(LocalDateTime.class), hoursCaptor.capture(), anyString());
        
        // 执行请求
        mockMvc.perform(post("/addOrder.do")
                .param("venueName", "场馆1")
                .param("date", "2025-06-01")
                .param("startTime", "2025-06-01 10:00")
                .param("hours", hugeHours)
                .session(session))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().is3xxRedirection());
                
        // 验证捕获的参数
        Integer capturedHours = hoursCaptor.getValue();
        if (capturedHours == Integer.MAX_VALUE) {
            System.out.println("[错误] 系统存在整数溢出漏洞: " + capturedHours + " 小时 * 100 价格 = " + (capturedHours * 100L));
            fail("系统存在整数溢出漏洞: 允许使用MAX_VALUE作为小时数，可能导致价格计算错误");
        } else if (capturedHours > 24) {
            System.out.println("[错误] 系统允许非常规的预订时长: " + capturedHours + " 小时");
            fail("系统允许非常规的预订时长: " + capturedHours + " 小时");
        } else {
            System.out.println("[通过] 系统正确处理了异常大的预订时长");
        }
    }

    @Test
    @DisplayName("测试负值处理漏洞")
    public void testNegativeValueVulnerability() throws Exception {
        System.out.println("[测试] OrderController.addOrder - 负值处理漏洞");
        
        // 使用负小时数
        String negativeHours = "-5";
        
        // 模拟服务层方法
        ArgumentCaptor<Integer> hoursCaptor = ArgumentCaptor.forClass(Integer.class);
        doNothing().when(orderService).submit(anyString(), any(LocalDateTime.class), hoursCaptor.capture(), anyString());
        
        // 执行请求
        try {
            mockMvc.perform(post("/addOrder.do")
                    .param("venueName", "场馆1")
                    .param("date", "2025-06-01")
                    .param("startTime", "2025-06-01 10:00")
                    .param("hours", negativeHours)
                    .session(session))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().is3xxRedirection());
                    
            // 验证捕获的参数
            Integer capturedHours = hoursCaptor.getValue();
            if (capturedHours < 0) {
                System.out.println("[错误] 系统不应接受负数小时数(-5)或计算负 数总价(-500)");
                fail("系统不应接受负数小时数: " + capturedHours);
            }
        } catch (Exception e) {
            System.out.println("[通过] 系统正确拒绝了负值小时数");
        }
    }

    @Test
    @DisplayName("测试非营业时间预订漏洞")
    public void testOutsideBusinessHoursVulnerability() throws Exception {
        System.out.println("[测试] OrderController.addOrder - 非营业时间预订漏洞");
        
        // 使用非营业时间 - 假设营业时间为9:00-22:00
        String lateHour = "2025-06-01 23:00"; // 超出营业时间
        
        // 模拟服务层方法
        when(venueService.findByVenueName("场馆1")).thenReturn(testVenue);
        ArgumentCaptor<LocalDateTime> timeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        doNothing().when(orderService).submit(anyString(), timeCaptor.capture(), anyInt(), anyString());
        
        // 执行请求
        mockMvc.perform(post("/addOrder.do")
                .param("venueName", "场馆1")
                .param("date", "2025-06-01")
                .param("startTime", lateHour)
                .param("hours", "2")
                .session(session))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().is3xxRedirection());
                
        // 验证捕获的参数
        LocalDateTime capturedTime = timeCaptor.getValue();
        int hour = capturedTime.getHour();
        if (hour >= 22 || hour < 9) {
            System.out.println("[错误] 系统不应接受营业时间(9:00-22:00) 外的订单预订(23:00)");
            fail("系统不应接受营业时间外的订单预订: " + capturedTime);
        } else {
            System.out.println("[通过] 系统正确处理了非营业时间预订");
        }
    }

    @Test
    @DisplayName("测试过去时间预订漏洞")
    public void testPastTimeVulnerability() throws Exception {
        System.out.println("[测试] OrderController.addOrder - 过去时间预订漏洞");
        
        // 使用过去的时间
        LocalDateTime pastTime = LocalDateTime.now().minusDays(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String pastTimeStr = pastTime.format(formatter);
        
        // 模拟服务层方法
        ArgumentCaptor<LocalDateTime> timeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        doNothing().when(orderService).submit(anyString(), timeCaptor.capture(), anyInt(), anyString());
        
        // 执行请求
        mockMvc.perform(post("/addOrder.do")
                .param("venueName", "场馆1")
                .param("date", pastTime.toLocalDate().toString())
                .param("startTime", pastTimeStr)
                .param("hours", "2")
                .session(session))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().is3xxRedirection());
                
        // 验证捕获的参数
        LocalDateTime capturedTime = timeCaptor.getValue();
        LocalDateTime now = LocalDateTime.now();
        if (capturedTime.isBefore(now)) {
            System.out.println("[错误] 系统不应接受过去时间(" + pastTimeStr + ")的订单预订");
            fail("系统不应接受过去时间的订单预订: " + capturedTime);
        } else {
            System.out.println("[通过] 系统正确处理了过去时间预订");
        }
    }

    @Test
    @DisplayName("测试访问他人订单的安全漏洞")
    public void testOrderSecurityVulnerability() throws Exception {
        System.out.println("[测试] OrderController.modifyOrder - 访问他人订单漏洞");
        
        // 创建一个不属于当前登录用户的订单
        Order otherUserOrder = new Order();
        otherUserOrder.setOrderID(999);
        otherUserOrder.setUserID("other_user"); // 不同于测试用户的userID
        otherUserOrder.setVenueID(1);
        otherUserOrder.setOrderTime(LocalDateTime.now());
        otherUserOrder.setStartTime(LocalDateTime.now().plusDays(1));
        otherUserOrder.setHours(2);
        otherUserOrder.setState(1);
        otherUserOrder.setTotal(200);
        
        // 模拟服务层根据ID查找订单
        when(orderService.findById(999)).thenReturn(otherUserOrder);
        when(venueService.findByVenueID(1)).thenReturn(testVenue);
        
        // 检查修改订单页面是否检查订单所有者
        MvcResult result = mockMvc.perform(get("/modifyOrder.do")
                .param("orderID", "999")
                .session(session))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();
                
        int status = result.getResponse().getStatus();
        if (status == 200) {
            System.out.println("[错误] 系统存在安全漏洞：允许用户访问他人的订单");
            fail("系统存在安全漏洞：允许用户访问他人的订单");
        } else {
            System.out.println("[通过] 系统正确阻止了访问他人订单的尝试");
        }
    }

    @Test
    @DisplayName("测试订单为空场馆的处理")
    public void testNullVenueOrder() throws Exception {
        System.out.println("[测试] OrderController.addOrder - 空场馆处理");
        
        // 执行请求，场馆名为空
        try {
            mockMvc.perform(post("/addOrder.do")
                    .param("venueName", "")
                    .param("date", "2025-06-01")
                    .param("startTime", "2025-06-01 10:00")
                    .param("hours", "2")
                    .session(session))
                    .andDo(MockMvcResultHandlers.print());
                    
            System.out.println("[错误] 系统应主动检查并抛出带有明确错误信息的NullPointerException, 而不是在访问null对象时才抛出");
            fail("系统应主动检查并抛出带有明确错误信息的异常");
        } catch (Exception e) {
            // 检查异常类型
            if (e.getCause() instanceof NullPointerException) {
                System.out.println("[警告] 系统抛出NullPointerException，但未提供明确的错误信息");
            } else {
                System.out.println("[通过] 系统正确处理了空场馆的情况");
            }
        }
    }
} 