package com.demo.controller.admin;

import com.demo.entity.Message;
import com.demo.entity.vo.MessageVo;
import com.demo.service.MessageService;
import com.demo.service.MessageVoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.ui.Model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
public class AdminMessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MessageService messageService;

    @MockBean
    private MessageVoService messageVoService;

    private List<Message> messageList;
    private List<MessageVo> messageVoList;
    private Page<Message> messagePage;

    @BeforeEach
    public void setUp() {
        // 准备测试数据
        messageList = new ArrayList<>();
        messageVoList = new ArrayList<>();
        
        // 创建测试消息
        for (int i = 1; i <= 5; i++) {
            Message message = new Message();
            message.setMessageID(i);
            message.setUserID("user" + i);
            message.setContent("测试消息内容" + i);
            message.setTime(LocalDateTime.now());
            message.setState(1); // 待审核状态
            messageList.add(message);
            
            MessageVo messageVo = new MessageVo();
            messageVo.setMessageID(i);
            messageVo.setUserID("user" + i);
            messageVo.setUserName("用户" + i);
            messageVo.setContent("测试消息内容" + i);
            messageVo.setTime(LocalDateTime.now());
            messageVo.setPicture("");
            messageVo.setState(1);
            messageVoList.add(messageVo);
        }
        
        // 创建分页数据
        Pageable pageable = PageRequest.of(0, 10, Sort.by("time").descending());
        messagePage = new PageImpl<>(messageList, pageable, messageList.size());
    }

    @Test
    @DisplayName("测试获取消息管理页面")
    public void testMessageManage() throws Exception {
        // 模拟服务层方法
        when(messageService.findWaitState(any(Pageable.class))).thenReturn(messagePage);
        
        // 执行请求并验证结果
        mockMvc.perform(get("/message_manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/message_manage"))
                .andExpect(model().attributeExists("total"));
        
        // 验证服务层方法被调用
        verify(messageService, times(1)).findWaitState(any(Pageable.class));
    }

    @Test
    @DisplayName("测试获取消息列表 - 正常分页")
    public void testMessageList() throws Exception {
        // 模拟服务层方法
        when(messageService.findWaitState(any(Pageable.class))).thenReturn(messagePage);
        when(messageVoService.returnVo(messageList)).thenReturn(messageVoList);
        
        // 执行请求并验证结果
        mockMvc.perform(get("/messageList.do")
                .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));
        
        // 验证服务层方法被调用
        verify(messageService, times(1)).findWaitState(any(Pageable.class));
        verify(messageVoService, times(1)).returnVo(messageList);
    }
    
    @Test
    @DisplayName("测试获取消息列表 - 超出范围的页码")
    public void testMessageListWithInvalidPage() throws Exception {
        // 模拟空列表返回
        when(messageService.findWaitState(any(Pageable.class))).thenReturn(new PageImpl<>(new ArrayList<>()));
        when(messageVoService.returnVo(any())).thenReturn(new ArrayList<>());
        
        // 执行请求并验证结果
        mockMvc.perform(get("/messageList.do")
                .param("page", "9999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("测试通过消息")
    public void testPassMessage() throws Exception {
        // 模拟服务层方法
        doNothing().when(messageService).confirmMessage(anyInt());
        
        // 执行请求并验证结果
        mockMvc.perform(post("/passMessage.do")
                .param("messageID", "1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
        
        // 验证服务层方法被调用
        verify(messageService, times(1)).confirmMessage(1);
    }
    
    @Test
    @DisplayName("测试拒绝消息")
    public void testRejectMessage() throws Exception {
        // 模拟服务层方法
        doNothing().when(messageService).rejectMessage(anyInt());
        
        // 执行请求并验证结果
        mockMvc.perform(post("/rejectMessage.do")
                .param("messageID", "1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
        
        // 验证服务层方法被调用
        verify(messageService, times(1)).rejectMessage(1);
    }
    
    @Test
    @DisplayName("测试删除消息")
    public void testDelMessage() throws Exception {
        // 模拟服务层方法
        doNothing().when(messageService).delById(anyInt());
        
        // 执行请求并验证结果
        mockMvc.perform(post("/delMessage.do")
                .param("messageID", "1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
        
        // 验证服务层方法被调用
        verify(messageService, times(1)).delById(1);
    }
    
    @Test
    @DisplayName("测试删除消息 - 非法ID")
    public void testDelMessageWithInvalidId() throws Exception {
        // 模拟服务层方法，即使ID非法也会调用删除方法
        doNothing().when(messageService).delById(anyInt());
        
        // 执行请求并验证结果
        mockMvc.perform(post("/delMessage.do")
                .param("messageID", "-1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
        
        // 验证服务层方法被调用
        verify(messageService, times(1)).delById(-1);
    }
} 