package com.demo.service.impl;

import com.demo.dao.MessageDao;
import com.demo.dao.UserDao;
import com.demo.entity.Message;
import com.demo.entity.User;
import com.demo.entity.vo.MessageVo;
import com.demo.service.MessageService;
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

class MessageVoServiceImplTest {

    @Mock
    private MessageDao messageDao;

    @Mock
    private UserDao userDao;

    @InjectMocks
    private MessageVoServiceImpl messageVoService;

    private Message testMessage;
    private User testUser;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        now = LocalDateTime.now();

        // 创建测试用户
        testUser = new User();
        testUser.setId(1);
        testUser.setUserID("test");
        testUser.setUserName("测试用户");
        testUser.setPassword("123");
        testUser.setEmail("test@example.com");
        testUser.setPhone("12345678901");
        testUser.setIsadmin(0);
        testUser.setPicture("avatar.jpg");

        // 创建测试留言
        testMessage = new Message();
        testMessage.setMessageID(1);
        testMessage.setUserID("test");
        testMessage.setContent("这是一条测试留言");
        testMessage.setState(MessageService.STATE_NO_AUDIT);
        testMessage.setTime(now);
    }

    @Test
    @DisplayName("测试根据留言ID返回MessageVo - 正常情况")
    void returnMessageVoByMessageID_Success() {
        // 设置模拟行为
        when(messageDao.findByMessageID(1)).thenReturn(testMessage);
        when(userDao.findByUserID("test")).thenReturn(testUser);
        
        // 执行测试
        MessageVo result = messageVoService.returnMessageVoByMessageID(1);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getMessageID());
        assertEquals("test", result.getUserID());
        assertEquals("这是一条测试留言", result.getContent());
        assertEquals("测试用户", result.getUserName());
        assertEquals("avatar.jpg", result.getPicture());
        assertEquals(MessageService.STATE_NO_AUDIT, result.getState());
        
        // 验证交互
        verify(messageDao, times(1)).findByMessageID(1);
        verify(userDao, times(1)).findByUserID("test");
    }

    @Test
    @DisplayName("测试将留言列表转换为MessageVo列表 - 正常情况")
    void returnVo_Success() {
        // 准备测试数据
        List<Message> messageList = new ArrayList<>();
        messageList.add(testMessage);
        
        // 设置模拟行为
        when(messageDao.findByMessageID(1)).thenReturn(testMessage);
        when(userDao.findByUserID("test")).thenReturn(testUser);
        
        // 执行测试
        List<MessageVo> result = messageVoService.returnVo(messageList);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getMessageID());
        assertEquals("test", result.get(0).getUserID());
        assertEquals("这是一条测试留言", result.get(0).getContent());
        assertEquals("测试用户", result.get(0).getUserName());
        
        // 验证交互
        verify(messageDao, times(1)).findByMessageID(1);
        verify(userDao, times(1)).findByUserID("test");
    }

    @Test
    @DisplayName("测试将空留言列表转换为MessageVo列表 - 边界情况")
    void returnVo_EmptyList() {
        // 准备测试数据
        List<Message> messageList = new ArrayList<>();
        
        // 执行测试
        List<MessageVo> result = messageVoService.returnVo(messageList);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(0, result.size());
        
        // 验证交互
        verify(messageDao, never()).findByMessageID(anyInt());
        verify(userDao, never()).findByUserID(anyString());
    }
} 