package com.demo.controller.admin;

import com.demo.entity.Order;
import com.demo.entity.vo.OrderVo;
import com.demo.service.OrderService;
import com.demo.service.OrderVoService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
public class AdminOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private OrderVoService orderVoService;

    private List<Order> orderList;
    private List<OrderVo> orderVoList;
    private Page<Order> orderPage;

    @BeforeEach
    public void setUp() {
        // 准备订单测试数据
        orderList = new ArrayList<>();
        orderVoList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 1; i <= 10; i++) {
            Order order = new Order();
            order.setOrderID(i);
            order.setUserID("user" + i);
            order.setVenueID(i);
            order.setOrderTime(now.minusDays(i));
            order.setStartTime(now.plusDays(i));
            order.setHours(2);
            order.setState(1); // 待审核状态
            order.setTotal(200 * i);
            orderList.add(order);

            OrderVo orderVo = new OrderVo();
            orderVo.setOrderID(i);
            orderVo.setUserID("user" + i);
            orderVo.setVenueName("测试场馆" + i);
            orderVo.setOrderTime(now.minusDays(i));
            orderVo.setStartTime(now.plusDays(i));
            orderVo.setHours(2);
            orderVo.setState(1);
            orderVo.setTotal(200 * i);
            orderVoList.add(orderVo);
        }

        // 创建分页数据
        Pageable pageable = PageRequest.of(0, 10, Sort.by("orderTime").descending());
        orderPage = new PageImpl<>(orderList, pageable, orderList.size());
    }

    @Test
    @DisplayName("测试获取预约管理页面")
    public void testReservationManage() throws Exception {
        // 模拟服务层方法
        when(orderService.findAuditOrder()).thenReturn(orderList);
        when(orderVoService.returnVo(orderList)).thenReturn(orderVoList);
        when(orderService.findNoAuditOrder(any(Pageable.class))).thenReturn(orderPage);

        // 执行请求并验证结果
        mockMvc.perform(get("/reservation_manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reservation_manage"))
                .andExpect(model().attributeExists("order_list"))
                .andExpect(model().attributeExists("total"));

        // 验证服务层方法被调用
        verify(orderService, times(1)).findAuditOrder();
        verify(orderVoService, times(1)).returnVo(orderList);
        verify(orderService, times(1)).findNoAuditOrder(any(Pageable.class));
    }

    @Test
    @DisplayName("测试获取未审核订单列表")
    public void testGetNoAuditOrder() throws Exception {
        // 模拟服务层方法
        when(orderService.findNoAuditOrder(any(Pageable.class))).thenReturn(orderPage);
        when(orderVoService.returnVo(orderList)).thenReturn(orderVoList);

        // 执行请求并验证结果
        mockMvc.perform(get("/admin/getOrderList.do")
                .param("page", "1"))
                .andExpect(status().isOk());

        // 验证服务层方法被调用
        verify(orderService, times(1)).findNoAuditOrder(any(Pageable.class));
        verify(orderVoService, times(1)).returnVo(orderList);
    }

    @Test
    @DisplayName("测试通过订单")
    public void testPassOrder() throws Exception {
        // 模拟服务层方法
        doNothing().when(orderService).confirmOrder(anyInt());

        // 执行请求并验证结果
        mockMvc.perform(post("/passOrder.do")
                .param("orderID", "1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        // 验证服务层方法被调用
        verify(orderService, times(1)).confirmOrder(1);
    }

    @Test
    @DisplayName("测试拒绝订单")
    public void testRejectOrder() throws Exception {
        // 模拟服务层方法
        doNothing().when(orderService).rejectOrder(anyInt());

        // 执行请求并验证结果
        mockMvc.perform(post("/rejectOrder.do")
                .param("orderID", "1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        // 验证服务层方法被调用
        verify(orderService, times(1)).rejectOrder(1);
    }
} 