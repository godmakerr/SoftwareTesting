package com.demo.service.impl;

import com.demo.dao.UserDao;
import com.demo.entity.User;
import com.demo.service.UserService;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserServiceImplTest {

    @Mock
    private UserDao userDao;

    @InjectMocks
    private UserServiceImpl userService;

    private User normalUser;
    private User adminUser;
    private List<User> userList;
    private Page<User> userPage;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        
        // 创建普通用户
        normalUser = new User();
        normalUser.setId(1);
        normalUser.setUserID("test");
        normalUser.setUserName("Test User");
        normalUser.setPassword("password");
        normalUser.setEmail("test@example.com");
        normalUser.setPhone("12345678901");
        normalUser.setIsadmin(0);
        
        // 创建管理员用户
        adminUser = new User();
        adminUser.setId(2);
        adminUser.setUserID("admin");
        adminUser.setUserName("Admin User");
        adminUser.setPassword("adminpass");
        adminUser.setEmail("admin@example.com");
        adminUser.setPhone("98765432109");
        adminUser.setIsadmin(1);
        
        // 创建用户列表
        userList = new ArrayList<>();
        userList.add(normalUser);
        userList.add(adminUser);
        
        // 创建分页对象
        userPage = new PageImpl<>(userList);
    }

    // 登录测试

    @Test
    @DisplayName("测试不同方法重载 - 避免方法调用模糊")
    void test_FindByUserID_MethodOverloadAmbiguity() {
        // 设置模拟行为
        when(userDao.findByUserID(anyString())).thenReturn(normalUser);
        when(userDao.findAllByIsadmin(eq(0), any(Pageable.class))).thenReturn(userPage);
        
        // 执行测试 - 显式类型转换避免方法调用模糊
        User result1 = userService.findByUserID("test");
        User result2 = userService.findByUserID((String)null);
        Page<User> result3 = userService.findByUserID(PageRequest.of(0, 10));
        
        // 验证结果
        assertNotNull(result1);
        assertNull(result2);
        assertNotNull(result3);
    }

    // 创建用户测试
    
    @Test
    @DisplayName("测试创建用户 - 数据库保存异常")
    void test_Create_DatabaseException() {
        // 创建用户
        User newUser = new User();
        newUser.setUserID("new_user");
        newUser.setUserName("New User");
        newUser.setPassword("password");
        
        // 设置模拟行为 - 模拟数据库异常
        when(userDao.save(any(User.class))).thenThrow(new RuntimeException("Database error"));
        
        // 执行测试并验证异常 - 服务层没有处理异常
        assertThrows(RuntimeException.class, () -> {
            userService.create(newUser);
        });
    }
    
    @Test
    @DisplayName("测试用户创建 - userDao.findAll()抛出异常")
    void test_Create_FindAllException() {
        // 创建用户
        User newUser = new User();
        newUser.setUserID("new_user");
        newUser.setUserName("New User");
        newUser.setPassword("password");
        
        // 设置模拟行为 - save成功但findAll抛出异常
        when(userDao.save(any(User.class))).thenReturn(newUser);
        when(userDao.findAll()).thenThrow(new RuntimeException("Database connection lost"));
        
        // 执行测试并验证异常
        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.create(newUser);
        });
        
        assertEquals("Database connection lost", exception.getMessage());
        
        // 验证交互 - 尽管发生异常，用户应该已被保存
        verify(userDao, times(1)).save(newUser);
    }
    
    @Test
    @DisplayName("测试创建用户 - SQL注入漏洞")
    void test_Create_SQLInjection() {
        // 创建带有SQL注入尝试的用户
        User sqlInjectionUser = new User();
        sqlInjectionUser.setUserID("'; DROP TABLE users; --");
        sqlInjectionUser.setUserName("Hacker");
        sqlInjectionUser.setPassword("password");
        
        // 设置模拟行为 - 模拟保存成功
        when(userDao.save(any(User.class))).thenReturn(sqlInjectionUser);
        when(userDao.findAll()).thenReturn(userList);
        
        // 执行测试 - 应该正常保存，但在实际应用中应该进行输入验证
        int result = userService.create(sqlInjectionUser);
        
        // 验证结果
        assertEquals(2, result);
        verify(userDao, times(1)).save(sqlInjectionUser);
    }
    
    @Test
    @DisplayName("测试密码安全 - 系统应拒绝弱密码")
    void test_Create_WeakPassword() {
        // 创建一个带有极弱密码的用户
        User weakPasswordUser = new User();
        weakPasswordUser.setUserID("new_user");
        weakPasswordUser.setUserName("New User");
        weakPasswordUser.setPassword("123456"); // 极弱密码
        
        // 设置模拟行为
        when(userDao.save(any(User.class))).thenReturn(weakPasswordUser);
        when(userDao.findAll()).thenReturn(userList);
        
        try {
            // 执行测试 - 应该拒绝弱密码，但系统接受了
            int result = userService.create(weakPasswordUser);
            
            // 使用ArgumentCaptor捕获传给save方法的User对象
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userDao).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            
            // 如果密码过短或过于简单，测试应该失败
            if (savedUser.getPassword() != null && 
                (savedUser.getPassword().equals("123456") || savedUser.getPassword().length() < 8)) {
                fail("系统应该拒绝弱密码(123456)，但接受了它");
            }
        } catch (IllegalArgumentException e) {
            // 如果系统正确实现，应该抛出IllegalArgumentException，测试通过
            assertTrue(e.getMessage().contains("弱密码") || 
                       e.getMessage().contains("密码必须") ||
                       e.getMessage().contains("密码强度不够"),
                      "系统应该明确说明密码不符合强度要求");
        }
    }
    
    @Test
    @DisplayName("测试用户名验证 - 系统应拒绝非法用户ID")
    void test_Create_InvalidUserID() {
        // 创建一个带有非法用户ID的用户（包含特殊字符）
        User invalidUser = new User();
        invalidUser.setUserID("user@#$%^&*()");  // 包含特殊字符的用户ID
        invalidUser.setUserName("Invalid User");
        invalidUser.setPassword("password123");
        
        // 设置模拟行为
        when(userDao.save(any(User.class))).thenReturn(invalidUser);
        when(userDao.findAll()).thenReturn(userList);
        
        try {
            // 执行测试 - 应该拒绝非法用户ID，但系统接受了
            int result = userService.create(invalidUser);
            
            // 使用ArgumentCaptor捕获传给save方法的User对象
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userDao).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            
            // 如果用户ID包含特殊字符，测试应该失败
            if (savedUser.getUserID() != null && 
                (savedUser.getUserID().contains("@") || 
                 savedUser.getUserID().contains("#") || 
                 savedUser.getUserID().contains("$"))) {
                fail("系统应该拒绝包含特殊字符的用户ID(user@#$%^&*())，但接受了它");
            }
        } catch (IllegalArgumentException e) {
            // 如果系统正确实现，应该抛出IllegalArgumentException，测试通过
            assertTrue(e.getMessage().contains("非法用户ID") || 
                       e.getMessage().contains("用户ID只能") ||
                       e.getMessage().contains("不允许特殊字符"),
                      "系统应该明确说明用户ID格式不符合要求");
        }
    }
    
    @Test
    @DisplayName("测试明文密码存储 - 应使用密码哈希")
    void test_Create_PlaintextPasswordStored() {
        // 创建一个新用户
        String plaintextPassword = "SecurePassword123!";
        User newUser = new User();
        newUser.setUserID("secure_user");
        newUser.setUserName("Secure User");
        newUser.setPassword(plaintextPassword);
        
        // 设置模拟行为
        when(userDao.save(any(User.class))).thenReturn(newUser);
        when(userDao.findAll()).thenReturn(userList);
        
        // 执行测试 - 应该存储哈希密码，但系统存储了明文
        userService.create(newUser);
        
        // 使用ArgumentCaptor捕获传给save方法的User对象
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userDao).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        
        // 判断密码是否被哈希处理：
        // 1. 如果密码完全等于原始密码，则明显未经哈希
        // 2. 常见哈希算法输出通常长度固定且不同于原始密码
        if (savedUser.getPassword().equals(plaintextPassword)) {
            fail("安全漏洞：系统将密码以明文形式存储，应使用密码哈希算法！");
        }
        
        // 检查密码格式是否符合常见哈希格式（可选）
        String hashedPassword = savedUser.getPassword();
        boolean appearsHashed = 
            (hashedPassword.length() >= 32 && hashedPassword.length() <= 128) || // 长度符合哈希输出
            hashedPassword.matches("^\\$2[ayb]\\$.{56}$") || // BCrypt
            hashedPassword.matches("^[a-f0-9]{32}$") || // MD5
            hashedPassword.matches("^[a-f0-9]{40}$") || // SHA-1
            hashedPassword.matches("^[a-f0-9]{64}$"); // SHA-256
        
        if (!appearsHashed) {
            fail("密码可能未经适当哈希处理，不符合常见哈希算法的输出模式");
        }
    }
    
    // 删除用户测试
    
    @Test
    @DisplayName("测试删除用户 - deleteById抛出EmptyResultDataAccessException")
    void test_DelByID_EmptyResultException() {
        // 设置模拟行为 - 当尝试删除不存在的用户时抛出异常
        doThrow(new EmptyResultDataAccessException("User not found", 1)).when(userDao).deleteById(999);
        
        // 执行测试并验证异常 - 服务层应该捕获并转换此异常，但当前实现直接传播
        assertThrows(EmptyResultDataAccessException.class, () -> {
            userService.delByID(999);
        });
    }
    
    // 更新用户测试
    
    @Test
    @DisplayName("测试updateUser - 用户为null (严格检测抛出NullPointerException并包含指定消息)")
    void test_UpdateUser_NullUser_WithSpecificMessage() {
        // 执行测试并验证异常 - 传入null用户应该抛出特定消息的NullPointerException
        NullPointerException exception = assertThrows(NullPointerException.class, () -> {
            userService.updateUser(null);
        });
        
        // 验证异常消息 - 必须包含具体的错误原因
        String errorMsg = exception.getMessage();
        if (errorMsg == null || errorMsg.isEmpty()) {
            fail("系统抛出的是默认NullPointerException，而不是带有明确错误消息的异常，表明缺乏主动参数验证");
        } else if (!errorMsg.contains("null") && !errorMsg.contains("为空")) {
            fail("异常消息不够明确，应该包含'User cannot be null'或类似说明，实际为: " + errorMsg);
        }
    }
    
    @Test
    @DisplayName("测试更新用户时DAO异常")
    void test_UpdateUser_DaoException() {
        // 设置模拟行为 - 模拟DAO层异常
        doThrow(new RuntimeException("Database error during update")).when(userDao).save(any(User.class));
        
        // 执行测试并验证异常
        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.updateUser(normalUser);
        });
        
        assertEquals("Database error during update", exception.getMessage());
    }
    
    // 并发测试
    
    @Test
    @DisplayName("测试并发创建相同UserID - 违反唯一约束")
    void test_Create_DuplicateUserID() {
        // 创建一个与现有用户ID相同的用户
        User duplicateUser = new User();
        duplicateUser.setUserID("test"); // 与normalUser相同的ID
        duplicateUser.setUserName("Duplicate User");
        duplicateUser.setPassword("password");
        
        // 模拟findAll返回已包含相同userID的用户列表
        when(userDao.findAll()).thenReturn(userList);
        
        // 设置模拟行为 - 当尝试保存时，如果dao层没有进行检查，将不会抛出异常
        when(userDao.save(any(User.class))).thenReturn(duplicateUser);
        
        try {
            // 执行测试 - 应该检查用户ID的唯一性
            int result = userService.create(duplicateUser);
            
            // 检查是否在业务层进行了userID唯一性检查
            if (userDao.findAll().stream().anyMatch(u -> u.getUserID().equals(duplicateUser.getUserID()))) {
                // 如果userList中已存在相同ID的用户，服务层应该进行检查并拒绝创建
                fail("系统应该检查并拒绝创建重复的userID，但未进行检查");
            }
            
            // 验证调用了userDao.save - 这表明系统未拒绝重复userID
            verify(userDao).save(duplicateUser);
            
            fail("系统未能检测到userID '" + duplicateUser.getUserID() + "' 已存在，存在并发安全风险");
        } catch (DataIntegrityViolationException e) {
            // 如果是依赖数据库唯一约束抛出异常，说明服务层没有主动检查
            fail("系统没有在业务层检查userID唯一性，而是依赖数据库唯一约束，这会导致用户体验差");
        } catch (RuntimeException e) {
            // 如果系统正确实现，应该在服务层抛出异常，说明userID已存在
            assertTrue(e.getMessage().contains("已存在") || 
                       e.getMessage().contains("重复") ||
                       e.getMessage().contains("已被使用"),
                      "系统应该明确说明用户ID已存在");
        }
    }
} 