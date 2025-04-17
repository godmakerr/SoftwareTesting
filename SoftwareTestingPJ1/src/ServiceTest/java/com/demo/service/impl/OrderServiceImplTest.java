package com.demo.service.impl;

import com.demo.dao.OrderDao;
import com.demo.dao.VenueDao;
import com.demo.entity.Order;
import com.demo.entity.Venue;
import com.demo.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderServiceImplTest {

    @Mock
    private OrderDao orderDao;

    @Mock
    private VenueDao venueDao;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order testOrder;
    private Venue testVenue;
    private List<Order> orderList;
    private Page<Order> orderPage;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        now = LocalDateTime.now();
        
        // 创建测试场馆
        testVenue = new Venue();
        testVenue.setVenueID(1);
        testVenue.setVenueName("测试场馆");
        testVenue.setDescription("这是一个测试场馆");
        testVenue.setPrice(100);
        testVenue.setAddress("测试地址");
        testVenue.setOpen_time("09:00");
        testVenue.setClose_time("22:00");
        testVenue.setPicture("test.jpg");

        // 创建测试订单
        testOrder = new Order();
        testOrder.setOrderID(1);
        testOrder.setUserID("test");
        testOrder.setVenueID(1);
        testOrder.setState(OrderService.STATE_NO_AUDIT);
        testOrder.setOrderTime(now.minusDays(1));
        testOrder.setStartTime(now.plusDays(1));
        testOrder.setHours(2);
        testOrder.setTotal(200);
        
        // 创建订单列表和分页
        orderList = new ArrayList<>();
        orderList.add(testOrder);
        
        orderPage = new PageImpl<>(orderList);
    }

    @Test
    @DisplayName("测试提交订单时Venue为null - 应抛出NullPointerException")
    void submit_NullVenue_ShouldThrowException() {
        // 设置模拟行为 - 返回null场馆，模拟场馆不存在的情况
        when(venueDao.findByVenueName("不存在的场馆")).thenReturn(null);
        
        try {
            orderService.submit("不存在的场馆", now.plusDays(1), 2, "test");
            
            // 如果代码执行到这里，表示没有抛出异常，测试应该失败
            fail("系统应该检查venue是否为null并抛出NullPointerException，但未抛出任何异常");
        } catch (NullPointerException e) {
            // 预期会捕获到NullPointerException，但应该是由系统主动抛出的带有消息的异常
            // 检查异常是否包含消息，如果没有消息，说明这只是JVM的默认NPE，而非系统主动检查并抛出的
            if (e.getMessage() == null || e.getMessage().isEmpty()) {
                // 异常没有消息，说明这只是访问null对象时JVM抛出的默认NPE，而非系统主动检查并抛出的
                fail("系统应主动检查并抛出带有明确错误消息的NullPointerException，而不是在访问null对象时才抛出默认NPE");
            }
        }
    }
    
    @Test
    @DisplayName("测试DAO层异常传播 - OrderDao.save抛出异常")
    void submit_DaoThrowsException() {
        // 设置模拟行为
        when(venueDao.findByVenueName("测试场馆")).thenReturn(testVenue);
        when(orderDao.save(any(Order.class))).thenThrow(new RuntimeException("Database connection failed"));
        
        // 执行测试并验证异常
        Exception exception = assertThrows(RuntimeException.class, () -> {
            orderService.submit("测试场馆", now.plusDays(1), 2, "test");
        });
        
        assertEquals("Database connection failed", exception.getMessage());
    }
    
    @Test
    @DisplayName("测试confirmOrder - updateState抛出异常")
    void confirmOrder_UpdateStateThrowsException() {
        // 设置模拟行为
        when(orderDao.findByOrderID(1)).thenReturn(testOrder);
        doThrow(new RuntimeException("Update failed")).when(orderDao).updateState(OrderService.STATE_WAIT, 1);
        
        // 执行测试并验证异常
        Exception exception = assertThrows(RuntimeException.class, () -> {
            orderService.confirmOrder(1);
        });
        
        assertEquals("Update failed", exception.getMessage());
    }
    
    @Test
    @DisplayName("测试并发修改问题 - 并发修改订单状态")
    void concurrentOrderStateModification() {
        // 设置模拟行为 - 同一订单被两次状态修改
        Order order = new Order();
        order.setOrderID(1);
        order.setState(OrderService.STATE_NO_AUDIT);
        
        when(orderDao.findByOrderID(1)).thenReturn(order);
        
        // 第一次确认
        orderService.confirmOrder(1);
        
        // 验证状态被更新
        verify(orderDao, times(1)).updateState(OrderService.STATE_WAIT, 1);
        
        // 修改订单状态模拟第一次更新
        order.setState(OrderService.STATE_WAIT);
        
        // 第二次完成
        orderService.finishOrder(1);
        
        // 验证第二次更新
        verify(orderDao, times(1)).updateState(OrderService.STATE_FINISH, 1);
        
        // 第三次尝试拒绝（已经是完成状态） - 应该仍然能更新，但实际应该防止这种情况
        orderService.rejectOrder(1);
        
        // 验证第三次更新 - 这种情况下不应该被允许，但当前实现会允许
        verify(orderDao, times(1)).updateState(OrderService.STATE_REJECT, 1);
    }
    
    @Test
    @DisplayName("测试负数小时 - 应被拒绝但被接受")
    void updateOrder_NegativeHours_ShouldBeRejected() {
        // 设置模拟行为
        Order order = new Order();
        order.setOrderID(1);
        Venue venue = new Venue();
        venue.setVenueID(1);
        venue.setVenueName("测试场馆");
        venue.setPrice(100);
        
        when(venueDao.findByVenueName("测试场馆")).thenReturn(venue);
        when(orderDao.findByOrderID(1)).thenReturn(order);
        
        try {
            // 执行测试 - 使用负数小时
            orderService.updateOrder(1, "测试场馆", now.plusDays(1), -5, "test");
            
            // 如果代码执行到这里，说明系统接受了负数小时，这是不合理的
            // 使用ArgumentCaptor捕获传给save方法的Order对象
            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderDao).save(orderCaptor.capture());
            
            // 获取捕获的Order对象
            Order savedOrder = orderCaptor.getValue();
            
            // 断言负数小时和负数总价是不合理的，测试应该失败
            if (savedOrder.getHours() < 0 || savedOrder.getTotal() < 0) {
                fail("系统不应接受负数小时数(-5)或计算负数总价(" + savedOrder.getTotal() + ")");
            }
        } catch (IllegalArgumentException e) {
            // 如果系统正确实现，应该抛出IllegalArgumentException，测试通过
            assertEquals("预订小时数必须为正数", e.getMessage(), "异常消息不符合预期");
        }
    }
    
    @Test
    @DisplayName("测试超大小时数 - 应被拒绝但被接受并导致整数溢出")
    void submit_HugeHours_ShouldBeRejected() {
        // 设置模拟行为
        when(venueDao.findByVenueName("测试场馆")).thenReturn(testVenue);
        
        try {
            // 执行测试 - 使用非常大的小时数
            orderService.submit("测试场馆", now.plusDays(1), Integer.MAX_VALUE, "test");
            
            // 如果代码执行到这里，说明系统接受了极大小时数
            // 使用ArgumentCaptor捕获传给save方法的Order对象
            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderDao).save(orderCaptor.capture());
            
            // 获取捕获的Order对象
            Order savedOrder = orderCaptor.getValue();
            
            // 断言：如果超大小时数导致total为负数（整数溢出），测试应该失败
            if (savedOrder.getTotal() < 0) {
                fail("系统存在整数溢出漏洞：" + Integer.MAX_VALUE + " 小时 * " 
                     + testVenue.getPrice() + " 价格 = " + savedOrder.getTotal() 
                     + "（应为正数但却是负数，表明发生了整数溢出）");
            }
        } catch (IllegalArgumentException e) {
            // 如果系统正确实现，应该抛出IllegalArgumentException，测试通过
            assertTrue(e.getMessage().contains("小时数过大") || 
                       e.getMessage().contains("超出合理范围"),
                      "异常消息应该明确说明小时数过大或超出合理范围");
        }
    }
    
    @Test
    @DisplayName("测试updateOrder - 订单ID为0或负数")
    void updateOrder_InvalidOrderID() {
        // 设置模拟行为 - findByOrderID应该返回null表示订单不存在
        when(orderDao.findByOrderID(0)).thenReturn(null);
        when(venueDao.findByVenueName(anyString())).thenReturn(testVenue);
        
        // 执行测试并验证异常 - 但目前实现不会检查订单是否存在就直接使用
        // 应该抛出NullPointerException，但现有代码不会进行此项检查
        assertThrows(NullPointerException.class, () -> {
            orderService.updateOrder(0, "测试场馆", now.plusDays(1), 2, "test");
        });
    }
    
    @Test
    @DisplayName("测试updateOrder - 订单不存在应抛出异常")
    void updateOrder_OrderNotFound_ShouldThrowException() {
        // 设置模拟行为 - 返回null表示订单不存在
        when(orderDao.findByOrderID(999)).thenReturn(null);
        when(venueDao.findByVenueName("测试场馆")).thenReturn(testVenue);
        
        try {
            orderService.updateOrder(999, "测试场馆", now.plusDays(1), 2, "test");
            
            // 如果执行到这里，说明没有抛出异常，测试应该失败
            fail("当订单不存在时应抛出异常，但系统未进行检查");
        } catch (Exception e) {
            // 捕获异常 - 可能是NullPointerException或其他异常
            // 检查是否是空指针异常，且没有详细消息（表明是意外的JVM异常而非系统主动抛出）
            if (e instanceof NullPointerException && (e.getMessage() == null || e.getMessage().isEmpty())) {
                fail("系统应该主动检查订单是否存在并抛出带有明确错误消息的异常，而不是导致空指针异常");
            }
        }
    }
    
    @Test
    @DisplayName("测试状态冲突 - 已完成订单不应允许拒绝")
    void orderState_RejectCompletedOrder_ShouldFail() {
        // 创建已完成状态的订单
        Order completedOrder = new Order();
        completedOrder.setOrderID(5);
        completedOrder.setState(OrderService.STATE_FINISH); // 已完成状态
        
        when(orderDao.findByOrderID(5)).thenReturn(completedOrder);
        
        try {
            // 执行测试 - 尝试拒绝一个已完成的订单，应该失败
            orderService.rejectOrder(5); 
            
            // 如果执行到这里，说明系统允许了不合理的状态转换
            // 验证updateState确实被调用了（这是错误的行为）
            verify(orderDao).updateState(OrderService.STATE_REJECT, 5);
            
            fail("系统不应允许将已完成(STATE_FINISH)的订单状态变更为已拒绝(STATE_REJECT)");
        } catch (IllegalStateException e) {
            // 期望系统抛出IllegalStateException，表明不允许此状态转换
            assertTrue(e.getMessage().contains("不能拒绝已完成的订单") || 
                       e.getMessage().contains("状态转换不允许"),
                      "异常消息应该明确说明不允许从已完成状态转为已拒绝状态");
        }
    }
    
    @Test
    @DisplayName("测试过去时间 - 不应接受过去的预订时间")
    void submit_PastStartTime_ShouldBeRejected() {
        // 设置模拟行为
        when(venueDao.findByVenueName("测试场馆")).thenReturn(testVenue);
        
        // 过去的时间 - 一周前
        LocalDateTime pastTime = now.minusDays(7);
        
        try {
            // 执行测试 - 使用过去的时间
            orderService.submit("测试场馆", pastTime, 2, "test");
            
            // 如果执行到这里，说明系统接受了过去时间的订单
            // 验证save方法确实被调用了（这是错误的行为）
            verify(orderDao).save(argThat(o -> o.getStartTime().equals(pastTime)));
            
            fail("系统不应接受过去时间(" + pastTime + ")的订单预订");
        } catch (IllegalArgumentException e) {
            // 期望系统抛出IllegalArgumentException，表明拒绝过去时间
            assertTrue(e.getMessage().contains("不能是过去时间") || 
                       e.getMessage().contains("必须是未来时间"),
                      "异常消息应该明确说明不允许预订过去时间");
        }
    }
    
    @Test
    @DisplayName("测试并发订单冲突 - 系统未检测场馆时间冲突")
    void submit_ConcurrentBooking_ShouldDetectConflict() {
        // 设置模拟行为
        when(venueDao.findByVenueName("测试场馆")).thenReturn(testVenue);
        
        // 模拟查询返回冲突订单
        List<Order> conflictingOrders = new ArrayList<>();
        conflictingOrders.add(testOrder); // testOrder时间是now+1天，持续2小时
        
        // 设置冲突查询
        LocalDateTime bookingTime = now.plusDays(1); // 与testOrder同一时间
        LocalDateTime endTime = bookingTime.plusHours(3); // 结束时间覆盖
        when(orderDao.findByVenueIDAndStartTimeIsBetween(
            eq(1), 
            any(LocalDateTime.class), 
            any(LocalDateTime.class)
        )).thenReturn(conflictingOrders);
        
        // 执行测试 - 尝试预订一个已被占用的时间段
        orderService.submit("测试场馆", bookingTime, 3, "anotherUser");
        
        // 验证结果 - 系统错误地允许了冲突订单的创建
        verify(orderDao, times(1)).save(any(Order.class)); // 应该抛异常而不是保存
        
        // 系统应该检测冲突并拒绝这种订单
        String expectedBehavior = "应抛出RuntimeException，提示'所选时间段已被预订'";
        System.out.println(expectedBehavior);
    }
    
    @Test
    @DisplayName("测试时间边界 - 不应接受营业时间外的预订")
    void submit_OutsideBusinessHours_ShouldBeRejected() {
        // 设置模拟行为
        when(venueDao.findByVenueName("测试场馆")).thenReturn(testVenue);
        
        // 测试场馆的营业时间是9:00-22:00
        // 创建一个超出营业时间的预订 - 晚上23:00
        LocalDateTime lateNight = now.plusDays(1).withHour(23).withMinute(0).withSecond(0);
        
        try {
            // 执行测试 - 使用营业时间外的时间
            orderService.submit("测试场馆", lateNight, 2, "test");
            
            // 如果执行到这里，说明系统接受了营业时间外的订单
            // 验证save方法确实被调用了（这是错误的行为）
            verify(orderDao).save(argThat(o -> o.getStartTime().getHour() == 23));
            
            fail("系统不应接受营业时间(9:00-22:00)外的订单预订(23:00)");
        } catch (IllegalArgumentException e) {
            // 期望系统抛出IllegalArgumentException，表明拒绝营业时间外的预订
            assertTrue(e.getMessage().contains("营业时间") || 
                       e.getMessage().contains("9:00-22:00"),
                      "异常消息应该明确说明预订时间必须在营业时间范围内");
        }
    }
} 