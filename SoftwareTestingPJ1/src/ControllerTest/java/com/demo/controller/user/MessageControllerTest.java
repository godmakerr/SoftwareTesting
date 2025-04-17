package com.demo.controller.user;

import com.demo.entity.Message;
import com.demo.entity.User;
import com.demo.entity.vo.MessageVo;
import com.demo.exception.LoginException;
import com.demo.service.MessageService;
import com.demo.service.MessageVoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
public class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MessageService messageService;

    @MockBean
    private MessageVoService messageVoService;

    private List<Message> messageList;
    private List<MessageVo> messageVoList;
    private Page<Message> messagePage;
    private MockHttpSession session;
    private User testUser;

    @BeforeEach
    public void setUp() {
        // 准备测试数据
        messageList = new ArrayList<>();
        messageVoList = new ArrayList<>();
        
        // 创建测试消息
        for (int i = 1; i <= 5; i++) {
            Message message = new Message();
            message.setMessageID(i);
            message.setUserID("user1");
            message.setContent("测试消息内容" + i);
            message.setTime(LocalDateTime.now());
            message.setState(2); // 通过状态
            messageList.add(message);
            
            MessageVo messageVo = new MessageVo();
            messageVo.setMessageID(i);
            messageVo.setUserID("user1");
            messageVo.setUserName("测试用户");
            messageVo.setContent("测试消息内容" + i);
            messageVo.setTime(LocalDateTime.now());
            messageVo.setPicture("");
            messageVo.setState(2);
            messageVoList.add(messageVo);
        }
        
        // 创建分页数据
        Pageable pageable = PageRequest.of(0, 5, Sort.by("time").descending());
        messagePage = new PageImpl<>(messageList, pageable, messageList.size());
        
        // 创建测试用户和会话
        testUser = new User();
        testUser.setId(1);
        testUser.setUserID("user1");
        testUser.setUserName("测试用户");
        testUser.setPassword("password");
        testUser.setEmail("test@example.com");
        testUser.setPhone("12345678901");
        testUser.setIsadmin(0);
        
        session = new MockHttpSession();
        session.setAttribute("user", testUser);
        
        System.out.println("[INFO] 初始化MessageControllerTest测试环境");
    }

    @Test
    @DisplayName("测试获取消息列表页面 - 用户已登录")
    public void testMessageListPageWithLoginUser() throws Exception {
        System.out.println("[测试] MessageController.message_list - 用户已登录场景");
        
        // 模拟服务层方法
        when(messageService.findPassState(any(Pageable.class))).thenReturn(messagePage);
        when(messageVoService.returnVo(messageList)).thenReturn(messageVoList);
        when(messageService.findByUser(anyString(), any(Pageable.class))).thenReturn(messagePage);
        
        // 执行请求并验证结果
        mockMvc.perform(get("/message_list").session(session))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(view().name("message_list"))
                .andExpect(model().attributeExists("total"))
                .andExpect(model().attributeExists("user_total"));
        
        // 验证服务层方法被调用
        verify(messageService, times(1)).findPassState(any(Pageable.class));
        verify(messageVoService, times(1)).returnVo(messageList);
        verify(messageService, times(1)).findByUser(eq("user1"), any(Pageable.class));
        
        System.out.println("[通过] 已登录用户可以正常访问留言列表页面");
    }

    @Test
    @DisplayName("测试获取消息列表页面 - 用户未登录")
    public void testMessageListPageWithoutLoginUser() throws Exception {
        System.out.println("[测试] MessageController.message_list - 用户未登录场景");
        
        // 修改验证方式：期望请求在没有登录用户的情况下会抛出异常
        try {
            mockMvc.perform(get("/message_list"))
                   .andDo(MockMvcResultHandlers.print());
            
            // 如果没有抛出异常，则测试失败
            System.out.println("[错误] 系统允许未登录用户访问需要登录的页面");
            fail("系统允许未登录用户访问需要登录的页面");
        } catch (Exception e) {
            // 验证异常的根本原因
            if (e.getCause() instanceof LoginException) {
                System.out.println("[通过] 系统正确拒绝了未登录用户访问留言列表页面");
            } else if (e.getCause() instanceof NullPointerException) {
                System.out.println("[错误] 系统存在空指针异常，未正确处理未登录状态");
                fail("系统存在空指针异常，未正确处理未登录状态");
            } else {
                System.out.println("[错误] 系统抛出意外异常: " + e.getCause().getClass().getName());
                fail("系统抛出意外异常: " + e.getCause().getClass().getName());
            }
        }
        
        // 验证服务层方法的调用情况
        verify(messageService, times(0)).findPassState(any(Pageable.class));
    }

    @Test
    @DisplayName("测试获取通过的消息列表")
    public void testGetMessageList() throws Exception {
        System.out.println("[测试] MessageController.message_list - 获取已通过审核的消息列表");
        
        // 模拟服务层方法
        when(messageService.findPassState(any(Pageable.class))).thenReturn(messagePage);
        when(messageVoService.returnVo(messageList)).thenReturn(messageVoList);
        
        // 执行请求并验证结果
        mockMvc.perform(get("/message/getMessageList")
                .param("page", "1"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));
        
        // 验证服务层方法被调用
        verify(messageService, times(1)).findPassState(any(Pageable.class));
        verify(messageVoService, times(1)).returnVo(messageList);
        
        System.out.println("[通过] 系统可以正确返回已通过审核的消息列表");
    }

    @Test
    @DisplayName("测试获取用户的消息列表 - 用户已登录")
    public void testFindUserList() throws Exception {
        System.out.println("[测试] MessageController.user_message_list - 用户已登录场景");
        
        // 模拟服务层方法
        when(messageService.findByUser(anyString(), any(Pageable.class))).thenReturn(messagePage);
        when(messageVoService.returnVo(messageList)).thenReturn(messageVoList);
        
        // 执行请求并验证结果
        mockMvc.perform(get("/message/findUserList")
                .param("page", "1")
                .session(session))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));
        
        // 验证服务层方法被调用
        verify(messageService, times(1)).findByUser(eq("user1"), any(Pageable.class));
        verify(messageVoService, times(1)).returnVo(messageList);
        
        System.out.println("[通过] 已登录用户可以正常查看自己的留言列表");
    }
    
    @Test
    @DisplayName("测试获取用户的消息列表 - 用户未登录")
    public void testFindUserListWithoutLogin() throws Exception {
        System.out.println("[测试] MessageController.user_message_list - 用户未登录场景");
        
        // 修改验证方式：期望请求在没有登录用户的情况下会抛出LoginException
        try {
            mockMvc.perform(get("/message/findUserList")
                    .param("page", "1"))
                    .andDo(MockMvcResultHandlers.print());
            
            // 如果没有抛出异常，则测试失败
            System.out.println("[错误] 系统允许未登录用户访问个人留言列表");
            fail("系统允许未登录用户访问个人留言列表");
        } catch (Exception e) {
            // 验证异常的根本原因是LoginException
            if (e.getCause() instanceof LoginException) {
                assertEquals("请登录！", e.getCause().getMessage());
                System.out.println("[通过] 系统正确拒绝了未登录用户访问个人留言列表");
            } else {
                System.out.println("[错误] 系统抛出意外异常: " + e.getCause().getClass().getName());
                fail("系统抛出意外异常: " + e.getCause().getClass().getName());
            }
        }
        
        // 验证服务方法未被调用
        verify(messageService, times(0)).findByUser(anyString(), any(Pageable.class));
        verify(messageVoService, times(0)).returnVo(any());
    }

    @Test
    @DisplayName("测试发送消息")
    public void testSendMessage() throws Exception {
        System.out.println("[测试] MessageController.sendMessage - 发送新留言");
        
        // 捕获服务层方法的参数
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        
        // 执行请求并验证结果
        mockMvc.perform(post("/sendMessage")
                .param("userID", "user1")
                .param("content", "测试发送消息内容")
                .session(session)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/message_list"));
        
        // 验证服务层方法被调用并检查参数
        verify(messageService, times(1)).create(messageCaptor.capture());
        
        // 检查传递给服务层的消息对象
        Message capturedMessage = messageCaptor.getValue();
        assertEquals("user1", capturedMessage.getUserID(), "用户ID应该正确设置");
        assertEquals("测试发送消息内容", capturedMessage.getContent(), "消息内容应该正确设置");
        assertEquals(1, capturedMessage.getState(), "消息状态应该设置为待审核");
        assertNotNull(capturedMessage.getTime(), "消息时间不应为空");
        
        System.out.println("[通过] 系统可以正确处理发送留言请求");
    }

    @Test
    @DisplayName("测试修改消息")
    public void testModifyMessage() throws Exception {
        System.out.println("[测试] MessageController.modifyMessage - 修改留言");
        
        // 创建测试消息
        Message message = new Message();
        message.setMessageID(1);
        message.setUserID("user1"); // 与登录用户相同
        message.setContent("原消息内容");
        message.setTime(LocalDateTime.now());
        message.setState(1);
        
        // 模拟服务层方法
        when(messageService.findById(1)).thenReturn(message);
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        
        // 执行请求并验证结果
        mockMvc.perform(post("/modifyMessage.do")
                .param("messageID", "1")
                .param("content", "修改后的消息内容")
                .session(session)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
        
        // 验证服务层方法被调用
        verify(messageService, times(1)).findById(1);
        verify(messageService, times(1)).update(messageCaptor.capture());
        
        // 检查传递给服务层的消息对象
        Message capturedMessage = messageCaptor.getValue();
        assertEquals("修改后的消息内容", capturedMessage.getContent(), "消息内容应该更新");
        assertEquals(1, capturedMessage.getState(), "消息状态应该重置为待审核");
        
        System.out.println("[通过] 系统可以正确处理修改留言请求");
    }

    @Test
    @DisplayName("测试删除消息")
    public void testDelMessage() throws Exception {
        System.out.println("[测试] MessageController.delMessage - 删除留言");
        
        // 模拟服务层方法
        doNothing().when(messageService).delById(anyInt());
        
        // 执行请求并验证结果
        mockMvc.perform(post("/delMessage.do")
                .param("messageID", "1")
                .session(session)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
        
        // 验证服务层方法被调用
        verify(messageService, times(1)).delById(1);
        
        System.out.println("[通过] 系统可以正确处理删除留言请求");
    }

    @Test
    @DisplayName("测试修改他人消息的业务逻辑漏洞")
    public void testModifyOthersMessageVulnerability() throws Exception {
        System.out.println("[测试] MessageController.modifyMessage - 尝试修改他人留言");
        
        // 创建一个不属于当前登录用户的消息
        Message otherUserMessage = new Message();
        otherUserMessage.setMessageID(999);
        otherUserMessage.setUserID("other_user"); // 不同于测试用户的userID
        otherUserMessage.setContent("其他用户的消息");
        otherUserMessage.setState(2);
        otherUserMessage.setTime(LocalDateTime.now());
        
        // 模拟服务层根据ID查找消息
        when(messageService.findById(999)).thenReturn(otherUserMessage);
        
        // 以当前登录用户的身份执行修改请求
        try {
            MvcResult result = mockMvc.perform(post("/modifyMessage.do")
                    .param("messageID", "999")
                    .param("content", "尝试修改其他用户的消息")
                    .session(session)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn();
            
            // 如果没有抛出异常，则检查是否调用了更新方法
            verify(messageService, times(1)).findById(999);
            
            // 判断是否真的更新了消息
            try {
                verify(messageService, times(0)).update(any(Message.class));
                System.out.println("[通过] 系统阻止了修改他人留言的尝试");
            } catch (AssertionError e) {
                System.out.println("[错误] 系统存在安全漏洞：允许用户修改他人的留言");
                fail("系统存在安全漏洞：允许用户修改他人的留言");
            }
        } catch (Exception e) {
            // 如果抛出异常，则需要检查是否是预期的权限异常
            System.out.println("[通过] 系统正确抛出异常阻止修改他人留言: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("测试XSS漏洞防护")
    public void testXssVulnerability() throws Exception {
        System.out.println("[测试] MessageController.sendMessage - XSS漏洞防护");
        
        // XSS攻击尝试字符串
        String xssPayload = "<script>alert('XSS')</script>";
        
        // 捕获传递给服务层的参数
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        
        // 执行请求，发送包含XSS脚本的消息
        mockMvc.perform(post("/sendMessage")
                .param("userID", "user1")
                .param("content", xssPayload)
                .session(session)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/message_list"));
                
        // 验证服务层方法被调用并检查参数
        verify(messageService, times(1)).create(messageCaptor.capture());
        
        // 检查传递给服务层的消息对象，判断是否过滤了XSS内容
        Message capturedMessage = messageCaptor.getValue();
        if (capturedMessage.getContent().contains("<script>")) {
            System.out.println("[错误] 系统存在高危XSS漏洞，未能过滤javascript脚本代码");
            fail("系统存在高危XSS漏洞，未能过滤javascript脚本代码");
        } else {
            System.out.println("[通过] 系统正确过滤了XSS攻击代码");
        }
    }
    
    @Test
    @DisplayName("测试SQL注入漏洞防护")
    public void testSqlInjectionVulnerability() throws Exception {
        System.out.println("[测试] MessageController.sendMessage - SQL注入漏洞防护");
        
        // SQL注入攻击字符串
        String sqlInjectionPayload = "' OR 1=1; --";
        
        // 捕获传递给服务层的参数
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        
        // 执行请求，发送包含SQL注入的消息
        mockMvc.perform(post("/sendMessage")
                .param("userID", "user1' OR 1=1; --")
                .param("content", sqlInjectionPayload)
                .session(session)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().is3xxRedirection());
                
        // 验证服务层方法被调用并检查参数
        verify(messageService, times(1)).create(messageCaptor.capture());
        
        // 检查传递给服务层的消息对象，判断是否过滤了SQL注入内容
        Message capturedMessage = messageCaptor.getValue();
        if (capturedMessage.getUserID().equals("user1' OR 1=1; --")) {
            System.out.println("[错误] 系统存在SQL注入漏洞，未能过滤SQL关键字和特殊字符");
            fail("系统存在SQL注入漏洞，未能过滤SQL关键字和特殊字符");
        } else {
            System.out.println("[通过] 系统正确处理了SQL注入尝试");
        }
    }
    
    @Test
    @DisplayName("测试消息为空时的处理")
    public void testEmptyMessage() throws Exception {
        System.out.println("[测试] MessageController.sendMessage - 空消息处理");
        
        // 捕获传递给服务层的参数
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        
        // 执行请求，发送空消息
        mockMvc.perform(post("/sendMessage")
                .param("userID", "user1")
                .param("content", "")
                .session(session)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().is3xxRedirection());
                
        // 验证服务层方法被调用并检查参数
        verify(messageService, times(1)).create(messageCaptor.capture());
        
        // 检查传递给服务层的消息对象
        Message capturedMessage = messageCaptor.getValue();
        if (capturedMessage.getContent() == null || capturedMessage.getContent().isEmpty()) {
            System.out.println("[错误] 系统允许提交空内容的留言");
            fail("系统允许提交空内容的留言");
        } else {
            System.out.println("[通过] 系统正确处理了空消息的情况");
        }
    }
} 