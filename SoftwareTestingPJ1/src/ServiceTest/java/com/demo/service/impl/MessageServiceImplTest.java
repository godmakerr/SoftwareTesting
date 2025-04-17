package com.demo.service.impl;

import com.demo.dao.MessageDao;
import com.demo.entity.Message;
import com.demo.service.MessageService;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MessageServiceImplTest {

    @Mock
    private MessageDao messageDao;

    @InjectMocks
    private MessageServiceImpl messageService;

    private Message testMessage;
    private List<Message> messageList;
    private Page<Message> messagePage;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        
        now = LocalDateTime.now();

        // 创建测试留言
        testMessage = new Message();
        testMessage.setMessageID(1);
        testMessage.setUserID("test");
        testMessage.setContent("这是一条测试留言");
        testMessage.setTime(now);
        testMessage.setState(MessageServiceImpl.STATE_NO_AUDIT);
        
        // 创建留言列表和分页
        messageList = new ArrayList<>();
        messageList.add(testMessage);
        
        messagePage = new PageImpl<>(messageList);
    }

    // 根据ID查询留言测试

    @Test
    @DisplayName("测试根据ID查询留言 - 留言存在")
    void findById_MessageExists() {
        // 设置模拟行为
        when(messageDao.getOne(1)).thenReturn(testMessage);

        // 执行测试
        Message result = messageService.findById(1);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getMessageID());
        assertEquals("test", result.getUserID());
        assertEquals("这是一条测试留言", result.getContent());
    }
    
    @Test
    @DisplayName("测试根据ID查询留言 - 留言不存在")
    void findById_MessageNotExists() {
        // 设置模拟行为 - 不存在的留言ID抛出异常
        when(messageDao.getOne(999)).thenThrow(new javax.persistence.EntityNotFoundException("Message not found"));
        
        // 执行测试并验证异常
        assertThrows(javax.persistence.EntityNotFoundException.class, () -> {
            messageService.findById(999);
        });
    }
    
    @Test
    @DisplayName("测试根据ID查询留言 - ID为负数")
    void findById_NegativeID() {
        // 设置模拟行为 - 负数ID抛出异常
        when(messageDao.getOne(-1)).thenThrow(new IllegalArgumentException("ID must be positive"));
        
        // 执行测试并验证异常
        assertThrows(IllegalArgumentException.class, () -> {
            messageService.findById(-1);
        });
    }
    
    @Test
    @DisplayName("测试根据ID查询留言 - ID为0")
    void findById_ZeroID() {
        // 设置模拟行为 - ID为0抛出异常
        when(messageDao.getOne(0)).thenThrow(new IllegalArgumentException("ID must be positive"));
        
        // 执行测试并验证异常
        assertThrows(IllegalArgumentException.class, () -> {
            messageService.findById(0);
        });
    }
    
    // 根据用户查询留言测试
    
    @Test
    @DisplayName("测试根据用户查询留言 - 正常情况")
    void findByUser_Normal() {
        // 创建分页请求
        Pageable pageable = PageRequest.of(0, 10);
        
        // 设置模拟行为
        when(messageDao.findAllByUserID("test", pageable)).thenReturn(messagePage);
        
        // 执行测试
        Page<Message> result = messageService.findByUser("test", pageable);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testMessage.getMessageID(), result.getContent().get(0).getMessageID());
        assertEquals("test", result.getContent().get(0).getUserID());
    }
    
    @Test
    @DisplayName("测试根据用户查询留言 - 用户无留言")
    void findByUser_NoMessages() {
        // 创建分页请求
        Pageable pageable = PageRequest.of(0, 10);
        
        // 创建空页面
        Page<Message> emptyPage = new PageImpl<>(new ArrayList<>());
        
        // 设置模拟行为
        when(messageDao.findAllByUserID("no_messages", pageable)).thenReturn(emptyPage);
        
        // 执行测试
        Page<Message> result = messageService.findByUser("no_messages", pageable);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }
    
    @Test
    @DisplayName("测试根据用户查询留言 - 用户ID为null")
    void findByUser_NullUserID() {
        // 创建分页请求
        Pageable pageable = PageRequest.of(0, 10);
        
        // 设置模拟行为 - 传入null时应返回空结果
        when(messageDao.findAllByUserID(null, pageable)).thenReturn(new PageImpl<>(new ArrayList<>()));
        
        // 执行测试
        Page<Message> result = messageService.findByUser(null, pageable);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }
    
    @Test
    @DisplayName("测试根据用户查询留言 - 用户ID为空字符串")
    void findByUser_EmptyUserID() {
        // 创建分页请求
        Pageable pageable = PageRequest.of(0, 10);
        
        // 设置模拟行为 - 传入空字符串时应返回空结果
        when(messageDao.findAllByUserID("", pageable)).thenReturn(new PageImpl<>(new ArrayList<>()));
        
        // 执行测试
        Page<Message> result = messageService.findByUser("", pageable);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }
    
    // 创建留言测试
    
    @Test
    @DisplayName("测试创建留言 - 正常情况")
    void create_Success() {
        // 创建一个新留言
        Message newMessage = new Message();
        newMessage.setUserID("test");
        newMessage.setContent("这是新创建的留言");
        newMessage.setTime(now);
        newMessage.setState(MessageServiceImpl.STATE_NO_AUDIT);
        
        // 设置模拟行为 - 保存后返回带ID的留言
        Message savedMessage = new Message();
        savedMessage.setMessageID(2);
        savedMessage.setUserID("test");
        savedMessage.setContent("这是新创建的留言");
        savedMessage.setTime(now);
        savedMessage.setState(MessageServiceImpl.STATE_NO_AUDIT);
        
        when(messageDao.save(any(Message.class))).thenReturn(savedMessage);
        
        // 执行测试
        int result = messageService.create(newMessage);
        
        // 验证结果
        assertEquals(2, result);
        verify(messageDao, times(1)).save(newMessage);
    }
    
    @Test
    @DisplayName("测试创建留言 - ID字段已设置")
    void create_WithID() {
        // 创建一个带ID的新留言
        Message newMessage = new Message();
        newMessage.setMessageID(100); // 预设ID
        newMessage.setUserID("test");
        newMessage.setContent("这是带ID的新留言");
        newMessage.setTime(now);
        newMessage.setState(MessageServiceImpl.STATE_NO_AUDIT);
        
        // 设置模拟行为 - 返回原始对象，保留其ID
        when(messageDao.save(newMessage)).thenReturn(newMessage);
        
        // 执行测试
        int result = messageService.create(newMessage);
        
        // 验证结果 - 应该返回预设的ID
        assertEquals(100, result);
        verify(messageDao, times(1)).save(newMessage);
    }
    
    @Test
    @DisplayName("测试创建留言 - 用户ID为null")
    void create_NullUserID() {
        // 创建用户ID为null的留言
        Message newMessage = new Message();
        newMessage.setUserID(null);
        newMessage.setContent("这是用户ID为null的留言");
        newMessage.setTime(now);
        newMessage.setState(MessageServiceImpl.STATE_NO_AUDIT);
        
        // 设置模拟行为 - 保存后返回带ID的留言
        Message savedMessage = new Message();
        savedMessage.setMessageID(2);
        savedMessage.setUserID(null);
        savedMessage.setContent("这是用户ID为null的留言");
        savedMessage.setTime(now);
        savedMessage.setState(MessageServiceImpl.STATE_NO_AUDIT);
        
        when(messageDao.save(any(Message.class))).thenReturn(savedMessage);
        
        // 执行测试
        int result = messageService.create(newMessage);
        
        // 验证结果 - 服务层没有验证用户ID不能为null
        assertEquals(2, result);
        verify(messageDao, times(1)).save(newMessage);
    }
    
    @Test
    @DisplayName("测试创建留言 - 内容为null")
    void create_NullContent() {
        // 创建内容为null的留言
        Message newMessage = new Message();
        newMessage.setUserID("test");
        newMessage.setContent(null);
        newMessage.setTime(now);
        newMessage.setState(MessageServiceImpl.STATE_NO_AUDIT);
        
        // 设置模拟行为 - 保存后返回带ID的留言
        Message savedMessage = new Message();
        savedMessage.setMessageID(2);
        savedMessage.setUserID("test");
        savedMessage.setContent(null);
        savedMessage.setTime(now);
        savedMessage.setState(MessageServiceImpl.STATE_NO_AUDIT);
        
        when(messageDao.save(any(Message.class))).thenReturn(savedMessage);
        
        // 执行测试
        int result = messageService.create(newMessage);
        
        // 验证结果 - 服务层没有验证内容不能为null
        assertEquals(2, result);
        verify(messageDao, times(1)).save(newMessage);
    }
    
    @Test
    @DisplayName("测试创建留言 - 时间为null")
    void create_NullTime() {
        // 创建时间为null的留言
        Message newMessage = new Message();
        newMessage.setUserID("test");
        newMessage.setContent("这是时间为null的留言");
        newMessage.setTime(null);
        newMessage.setState(MessageServiceImpl.STATE_NO_AUDIT);
        
        // 设置模拟行为 - 保存后返回带ID的留言
        Message savedMessage = new Message();
        savedMessage.setMessageID(2);
        savedMessage.setUserID("test");
        savedMessage.setContent("这是时间为null的留言");
        savedMessage.setTime(null);
        savedMessage.setState(MessageServiceImpl.STATE_NO_AUDIT);
        
        when(messageDao.save(any(Message.class))).thenReturn(savedMessage);
        
        // 执行测试
        int result = messageService.create(newMessage);
        
        // 验证结果 - 服务层没有验证时间不能为null
        assertEquals(2, result);
        verify(messageDao, times(1)).save(newMessage);
    }
    
    @Test
    @DisplayName("测试创建留言 - 内容超长")
    void create_ContentTooLong() {
        // 创建内容超长的留言（假设有限制，实际上代码中没有显式限制）
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            longContent.append("内容");
        }
        
        Message newMessage = new Message();
        newMessage.setUserID("test");
        newMessage.setContent(longContent.toString());
        newMessage.setTime(now);
        newMessage.setState(MessageServiceImpl.STATE_NO_AUDIT);
        
        // 设置模拟行为 - 假设DAO会拒绝过长的内容
        when(messageDao.save(newMessage)).thenThrow(new IllegalArgumentException("Content too long"));
        
        // 执行测试并验证异常
        assertThrows(IllegalArgumentException.class, () -> {
            messageService.create(newMessage);
        });
    }
    
    // 删除留言测试
    
    @Test
    @DisplayName("测试删除留言 - 正常情况")
    void delById_Success() {
        // 执行测试
        messageService.delById(1);
        
        // 验证行为
        verify(messageDao, times(1)).deleteById(1);
    }
    
    @Test
    @DisplayName("测试删除留言 - 留言不存在")
    void delById_MessageNotExists() {
        // 设置模拟行为 - 删除不存在的留言时抛出异常
        doThrow(new IllegalArgumentException("Message not found")).when(messageDao).deleteById(999);
        
        // 执行测试并验证异常
        assertThrows(IllegalArgumentException.class, () -> {
            messageService.delById(999);
        });
        
        // 验证行为
        verify(messageDao, times(1)).deleteById(999);
    }
    
    @Test
    @DisplayName("测试删除留言 - ID为负数")
    void delById_NegativeID() {
        // 设置模拟行为 - 删除ID为负数的留言时抛出异常
        doThrow(new IllegalArgumentException("ID must be positive")).when(messageDao).deleteById(-1);
        
        // 执行测试并验证异常
        assertThrows(IllegalArgumentException.class, () -> {
            messageService.delById(-1);
        });
        
        // 验证行为
        verify(messageDao, times(1)).deleteById(-1);
    }
    
    // 更新留言测试
    
    @Test
    @DisplayName("测试更新留言 - 正常情况")
    void update_Success() {
        // 更新测试留言
        testMessage.setContent("更新后的留言内容");
        testMessage.setTime(now.plusDays(1));
        
        // 执行测试
        messageService.update(testMessage);
        
        // 验证行为
        verify(messageDao, times(1)).save(testMessage);
    }
    
    @Test
    @DisplayName("测试更新留言 - 留言不存在")
    void update_MessageNotExists() {
        // 创建一个不存在的留言
        Message nonExistentMessage = new Message();
        nonExistentMessage.setMessageID(999);
        nonExistentMessage.setUserID("test");
        nonExistentMessage.setContent("这是不存在的留言");
        nonExistentMessage.setTime(now);
        nonExistentMessage.setState(MessageServiceImpl.STATE_NO_AUDIT);
        
        // 设置模拟行为 - 允许更新不存在的留言（实际上会创建新留言）
        when(messageDao.save(nonExistentMessage)).thenReturn(nonExistentMessage);
        
        // 执行测试
        messageService.update(nonExistentMessage);
        
        // 验证行为
        verify(messageDao, times(1)).save(nonExistentMessage);
    }
    
    @Test
    @DisplayName("测试更新留言 - ID为null")
    void update_NullID() {
        // 创建一个ID为null的留言
        Message messageWithNullID = new Message();
        // 不设置ID
        messageWithNullID.setUserID("test");
        messageWithNullID.setContent("这是ID为null的留言");
        messageWithNullID.setTime(now);
        messageWithNullID.setState(MessageServiceImpl.STATE_NO_AUDIT);
        
        // 设置模拟行为 - 保存时会自动生成ID（实际上会创建新留言）
        Message savedMessage = new Message();
        savedMessage.setMessageID(2);
        savedMessage.setUserID("test");
        savedMessage.setContent("这是ID为null的留言");
        savedMessage.setTime(now);
        savedMessage.setState(MessageServiceImpl.STATE_NO_AUDIT);
        
        when(messageDao.save(messageWithNullID)).thenReturn(savedMessage);
        
        // 执行测试
        messageService.update(messageWithNullID);
        
        // 验证行为
        verify(messageDao, times(1)).save(messageWithNullID);
    }
    
    // 确认留言测试
    
    @Test
    @DisplayName("测试确认留言 - 留言存在")
    void confirmMessage_MessageExists() {
        // 设置模拟行为
        when(messageDao.findByMessageID(1)).thenReturn(testMessage);
        
        // 执行测试
        messageService.confirmMessage(1);
        
        // 验证交互
        verify(messageDao, times(1)).findByMessageID(1);
        verify(messageDao, times(1)).updateState(MessageServiceImpl.STATE_PASS, 1);
    }
    
    @Test
    @DisplayName("测试确认留言 - 留言不存在")
    void confirmMessage_MessageNotExists() {
        // 设置模拟行为
        when(messageDao.findByMessageID(999)).thenReturn(null);
        
        // 执行测试并验证异常
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            messageService.confirmMessage(999);
        });
        
        // 验证异常消息
        assertEquals("留言不存在", exception.getMessage());
        
        // 验证交互
        verify(messageDao, times(1)).findByMessageID(999);
        verify(messageDao, never()).updateState(anyInt(), anyInt());
    }
    
    @Test
    @DisplayName("测试确认留言 - ID为负数")
    void confirmMessage_NegativeID() {
        // 设置模拟行为 - 查询ID为负数的留言时返回null
        when(messageDao.findByMessageID(-1)).thenReturn(null);
        
        // 执行测试并验证异常
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            messageService.confirmMessage(-1);
        });
        
        // 验证异常消息
        assertEquals("留言不存在", exception.getMessage());
        
        // 验证交互
        verify(messageDao, times(1)).findByMessageID(-1);
        verify(messageDao, never()).updateState(anyInt(), anyInt());
    }
    
    // 拒绝留言测试
    
    @Test
    @DisplayName("测试拒绝留言 - 留言存在")
    void rejectMessage_MessageExists() {
        // 设置模拟行为
        when(messageDao.findByMessageID(1)).thenReturn(testMessage);
        
        // 执行测试
        messageService.rejectMessage(1);
        
        // 验证交互
        verify(messageDao, times(1)).findByMessageID(1);
        verify(messageDao, times(1)).updateState(MessageServiceImpl.STATE_REJECT, 1);
    }
    
    @Test
    @DisplayName("测试拒绝留言 - 留言不存在")
    void rejectMessage_MessageNotExists() {
        // 设置模拟行为
        when(messageDao.findByMessageID(999)).thenReturn(null);
        
        // 执行测试并验证异常
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            messageService.rejectMessage(999);
        });
        
        // 验证异常消息
        assertEquals("留言不存在", exception.getMessage());
        
        // 验证交互
        verify(messageDao, times(1)).findByMessageID(999);
        verify(messageDao, never()).updateState(anyInt(), anyInt());
    }
    
    // 查询待审核留言测试
    
    @Test
    @DisplayName("测试查询待审核留言 - 有待审核留言")
    void findWaitState_WithMessages() {
        // 创建分页请求
        Pageable pageable = PageRequest.of(0, 10);
        
        // 设置模拟行为
        when(messageDao.findAllByState(MessageServiceImpl.STATE_NO_AUDIT, pageable)).thenReturn(messagePage);
        
        // 执行测试
        Page<Message> result = messageService.findWaitState(pageable);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testMessage.getMessageID(), result.getContent().get(0).getMessageID());
        assertEquals(MessageServiceImpl.STATE_NO_AUDIT, result.getContent().get(0).getState());
    }
    
    @Test
    @DisplayName("测试查询待审核留言 - 无待审核留言")
    void findWaitState_NoMessages() {
        // 创建分页请求
        Pageable pageable = PageRequest.of(0, 10);
        
        // 创建空页面
        Page<Message> emptyPage = new PageImpl<>(new ArrayList<>());
        
        // 设置模拟行为
        when(messageDao.findAllByState(MessageServiceImpl.STATE_NO_AUDIT, pageable)).thenReturn(emptyPage);
        
        // 执行测试
        Page<Message> result = messageService.findWaitState(pageable);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }
    
    // 查询通过留言测试
    
    @Test
    @DisplayName("测试查询通过留言 - 有通过留言")
    void findPassState_WithMessages() {
        // 创建分页请求
        Pageable pageable = PageRequest.of(0, 10);
        
        // 创建通过状态的留言
        Message passedMessage = new Message();
        passedMessage.setMessageID(2);
        passedMessage.setUserID("test");
        passedMessage.setContent("这是一条通过审核的留言");
        passedMessage.setTime(now);
        passedMessage.setState(MessageServiceImpl.STATE_PASS);
        
        List<Message> passedMessages = new ArrayList<>();
        passedMessages.add(passedMessage);
        Page<Message> passedPage = new PageImpl<>(passedMessages);
        
        // 设置模拟行为
        when(messageDao.findAllByState(MessageServiceImpl.STATE_PASS, pageable)).thenReturn(passedPage);
        
        // 执行测试
        Page<Message> result = messageService.findPassState(pageable);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(2, result.getContent().get(0).getMessageID());
        assertEquals(MessageServiceImpl.STATE_PASS, result.getContent().get(0).getState());
    }
    
    @Test
    @DisplayName("测试查询通过留言 - 无通过留言")
    void findPassState_NoMessages() {
        // 创建分页请求
        Pageable pageable = PageRequest.of(0, 10);
        
        // 创建空页面
        Page<Message> emptyPage = new PageImpl<>(new ArrayList<>());
        
        // 设置模拟行为
        when(messageDao.findAllByState(MessageServiceImpl.STATE_PASS, pageable)).thenReturn(emptyPage);
        
        // 执行测试
        Page<Message> result = messageService.findPassState(pageable);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }
    
    // 边界状态转换测试
    
    @Test
    @DisplayName("测试已拒绝留言再次确认")
    void confirmMessage_AlreadyRejected() {
        // 创建一个已拒绝状态的留言
        Message rejectedMessage = new Message();
        rejectedMessage.setMessageID(3);
        rejectedMessage.setUserID("test");
        rejectedMessage.setContent("这是一条已拒绝的留言");
        rejectedMessage.setTime(now);
        rejectedMessage.setState(MessageServiceImpl.STATE_REJECT);
        
        // 设置模拟行为
        when(messageDao.findByMessageID(3)).thenReturn(rejectedMessage);
        
        // 执行测试 - 服务层没有验证状态转换的合法性
        messageService.confirmMessage(3);
        
        // 验证交互 - 允许将拒绝状态的留言更新为通过状态
        verify(messageDao, times(1)).findByMessageID(3);
        verify(messageDao, times(1)).updateState(MessageServiceImpl.STATE_PASS, 3);
    }
    
    @Test
    @DisplayName("测试已通过留言再次拒绝")
    void rejectMessage_AlreadyPassed() {
        // 创建一个已通过状态的留言
        Message passedMessage = new Message();
        passedMessage.setMessageID(4);
        passedMessage.setUserID("test");
        passedMessage.setContent("这是一条已通过的留言");
        passedMessage.setTime(now);
        passedMessage.setState(MessageServiceImpl.STATE_PASS);
        
        // 设置模拟行为
        when(messageDao.findByMessageID(4)).thenReturn(passedMessage);
        
        // 执行测试 - 服务层没有验证状态转换的合法性
        messageService.rejectMessage(4);
        
        // 验证交互 - 允许将通过状态的留言更新为拒绝状态
        verify(messageDao, times(1)).findByMessageID(4);
        verify(messageDao, times(1)).updateState(MessageServiceImpl.STATE_REJECT, 4);
    }
} 