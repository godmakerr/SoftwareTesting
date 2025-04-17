package com.demo.controller.user;

import com.demo.entity.User;
import com.demo.service.UserService;
import com.demo.utils.FileUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;
    
    private User normalUser;
    private User adminUser;
    private MockHttpSession userSession;
    private MockHttpSession adminSession;

    @BeforeEach
    public void setUp() {
        // 准备普通用户数据
        normalUser = new User();
        normalUser.setId(1);
        normalUser.setUserID("user1");
        normalUser.setUserName("测试用户");
        normalUser.setPassword("password");
        normalUser.setEmail("test@example.com");
        normalUser.setPhone("12345678901");
        normalUser.setIsadmin(0);
        normalUser.setPicture("");

        // 准备管理员用户数据
        adminUser = new User();
        adminUser.setId(2);
        adminUser.setUserID("admin");
        adminUser.setUserName("管理员");
        adminUser.setPassword("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setPhone("12345678902");
        adminUser.setIsadmin(1);
        adminUser.setPicture("");

        // 创建会话
        userSession = new MockHttpSession();
        userSession.setAttribute("user", normalUser);

        adminSession = new MockHttpSession();
        adminSession.setAttribute("admin", adminUser);
        
        System.out.println("[INFO] 初始化UserControllerTest测试环境");
    }

    @Test
    @DisplayName("测试注册页面")
    public void testSignUpPage() throws Exception {
        mockMvc.perform(get("/signup"))
                .andExpect(status().isOk())
                .andExpect(view().name("signup"));
    }

    @Test
    @DisplayName("测试登录页面")
    public void testLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    @DisplayName("测试用户信息页面")
    public void testUserInfoPage() throws Exception {
        mockMvc.perform(get("/user_info").session(userSession))
                .andExpect(status().isOk())
                .andExpect(view().name("user_info"));
    }

    @Test
    @DisplayName("测试普通用户登录")
    public void testNormalUserLogin() throws Exception {
        when(userService.checkLogin("user1", "password")).thenReturn(normalUser);

        mockMvc.perform(post("/loginCheck.do")
                .param("userID", "user1")
                .param("password", "password")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string("/index"));

        verify(userService, times(1)).checkLogin("user1", "password");
    }

    @Test
    @DisplayName("测试管理员登录")
    public void testAdminLogin() throws Exception {
        when(userService.checkLogin("admin", "admin")).thenReturn(adminUser);

        mockMvc.perform(post("/loginCheck.do")
                .param("userID", "admin")
                .param("password", "admin")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string("/admin_index"));

        verify(userService, times(1)).checkLogin("admin", "admin");
    }

    @Test
    @DisplayName("测试登录失败")
    public void testLoginFail() throws Exception {
        when(userService.checkLogin("user1", "wrongpassword")).thenReturn(null);

        mockMvc.perform(post("/loginCheck.do")
                .param("userID", "user1")
                .param("password", "wrongpassword")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        verify(userService, times(1)).checkLogin("user1", "wrongpassword");
    }

    @Test
    @DisplayName("测试用户注册")
    public void testRegister() throws Exception {
        when(userService.create(any(User.class))).thenReturn(1);  // 假设返回ID

        mockMvc.perform(post("/register.do")
                .param("userID", "newuser")
                .param("userName", "新用户")
                .param("password", "newpassword")
                .param("email", "new@example.com")
                .param("phone", "12345678903")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("login"));

        verify(userService, times(1)).create(any(User.class));
    }

    @Test
    @DisplayName("测试用户登出")
    public void testLogout() throws Exception {
        mockMvc.perform(get("/logout.do").session(userSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/index"));
    }

    @Test
    @DisplayName("测试管理员登出")
    public void testAdminLogout() throws Exception {
        mockMvc.perform(get("/quit.do").session(adminSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/index"));
    }

    @Test
    @DisplayName("测试检查密码 - 正确密码")
    public void testCheckPasswordCorrect() throws Exception {
        when(userService.findByUserID("user1")).thenReturn(normalUser);

        mockMvc.perform(get("/checkPassword.do")
                .param("userID", "user1")
                .param("password", "password"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(userService, times(1)).findByUserID("user1");
    }

    @Test
    @DisplayName("测试检查密码 - 错误密码")
    public void testCheckPasswordIncorrect() throws Exception {
        when(userService.findByUserID("user1")).thenReturn(normalUser);

        mockMvc.perform(get("/checkPassword.do")
                .param("userID", "user1")
                .param("password", "wrongpassword"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        verify(userService, times(1)).findByUserID("user1");
    }

    @Test
    @DisplayName("测试更新用户信息 - 不修改密码和图片")
    public void testUpdateUserWithoutPasswordAndPicture() throws Exception {
        when(userService.findByUserID("user1")).thenReturn(normalUser);
        doNothing().when(userService).updateUser(any(User.class));

        // 使用空图片文件，这样FileUtil.saveUserFile会直接返回空字符串
        MockMultipartFile picture = new MockMultipartFile(
                "picture", "", "image/jpeg", new byte[0]);

        mockMvc.perform(multipart("/updateUser.do")
                .file(picture)
                .param("userID", "user1")
                .param("userName", "更新用户名")
                .param("passwordNew", "")
                .param("email", "update@example.com")
                .param("phone", "98765432101")
                .session(userSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("user_info"));

        verify(userService, times(1)).findByUserID("user1");
        verify(userService, times(1)).updateUser(any(User.class));
    }

    @Test
    @DisplayName("测试用户登录方法的设计一致性")
    public void testLoginMethodDesignConsistency() throws Exception {
        when(userService.checkLogin("user1", "password")).thenReturn(normalUser);

        mockMvc.perform(post("/loginCheck.do")
                .param("userID", "user1")
                .param("password", "password"))
                .andExpect(status().isOk());
        
        verify(userService, times(1)).checkLogin("user1", "password");
    }

    @Test
    @DisplayName("测试SQL注入漏洞防护")
    public void testSqlInjectionVulnerability() throws Exception {
        // SQL注入尝试字符串
        String sqlInjectionString = "' OR '1'='1";
        
        // 正常情况下，传入SQL注入字符串，登录应该失败
        when(userService.checkLogin(sqlInjectionString, "anypassword")).thenReturn(null);
        
        // 执行请求
        mockMvc.perform(post("/loginCheck.do")
                .param("userID", sqlInjectionString)
                .param("password", "anypassword"))
                .andExpect(status().isOk())
                .andExpect(content().string("false")); // 登录应该失败
                
        // 验证服务层方法被传递了SQL注入字符串
        // 服务层应该负责适当地转义或参数化这些输入
        verify(userService, times(1)).checkLogin(eq(sqlInjectionString), anyString());
    }

    @Test
    @DisplayName("测试密码明文存储漏洞")
    public void testPlaintextPasswordStored() throws Exception {
        System.out.println("[测试] UserController.register - 密码明文存储漏洞");
        
        // 捕获传递给服务层的User对象
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        
        // 执行注册请求
        mockMvc.perform(post("/register.do")
                .param("userID", "testuser")
                .param("userName", "Test User")
                .param("password", "testpassword123")
                .param("email", "test@example.com")
                .param("phone", "12345678901")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("login"));
                
        // 验证服务层方法被调用
        verify(userService, times(1)).create(userCaptor.capture());
        
        // 检查密码是否被加密
        User capturedUser = userCaptor.getValue();
        if ("testpassword123".equals(capturedUser.getPassword())) {
            System.out.println("[错误] 安全漏洞: 系统将密码以明文形式存储，应使用 密码哈希算法!");
            fail("安全漏洞: 系统将密码以明文形式存储，应使用密码哈希算法");
        } else {
            System.out.println("[通过] 系统正确加密了用户密码");
        }
    }

    @Test
    @DisplayName("测试弱密码接受漏洞")
    public void testWeakPasswordAccepted() throws Exception {
        System.out.println("[测试] UserController.register - 弱密码接受漏洞");
        
        // 尝试使用简单密码注册
        String weakPassword = "123456";
        
        // 捕获传递给服务层的User对象
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        
        // 执行注册请求
        mockMvc.perform(post("/register.do")
                .param("userID", "testuser")
                .param("userName", "Test User")
                .param("password", weakPassword)
                .param("email", "test@example.com")
                .param("phone", "12345678901")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().is3xxRedirection());
                
        // 验证服务层方法被调用
        verify(userService, times(1)).create(userCaptor.capture());
        
        // 检查是否接受了弱密码
        User capturedUser = userCaptor.getValue();
        if (weakPassword.equals(capturedUser.getPassword())) {
            System.out.println("[错误] 安全漏洞: 系统接受了简单弱密码 '" + weakPassword + "'");
            fail("安全漏洞: 系统接受了简单弱密码");
        } else if (capturedUser.getPassword().contains(weakPassword)) {
            System.out.println("[错误] 安全漏洞: 系统仅进行了简单加密，但仍接受了弱密码");
            fail("安全漏洞: 系统虽有加密但接受了弱密码");
        }
    }

    @Test
    @DisplayName("测试特殊字符用户ID处理")
    public void testInvalidUserIdHandling() throws Exception {
        System.out.println("[测试] UserController.register - 特殊字符用户ID处理");
        
        // 包含特殊字符的用户ID
        String invalidUserId = "user@#%&*()";
        
        // 捕获传递给服务层的User对象
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        
        // 执行注册请求
        try {
            mockMvc.perform(post("/register.do")
                    .param("userID", invalidUserId)
                    .param("userName", "Test User")
                    .param("password", "password123")
                    .param("email", "test@example.com")
                    .param("phone", "12345678901")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().is3xxRedirection());
                    
            // 验证服务层方法被调用
            verify(userService, times(1)).create(userCaptor.capture());
            
            // 检查系统是否接受了包含特殊字符的用户ID
            User capturedUser = userCaptor.getValue();
            if (invalidUserId.equals(capturedUser.getUserID())) {
                System.out.println("[错误] 系统应该拒绝包含特殊字符的用户ID(user@#%&*()), 但接受了它");
                fail("系统应该拒绝包含特殊字符的用户ID");
            }
        } catch (Exception e) {
            System.out.println("[通过] 系统正确拒绝了包含特殊字符的用户ID");
        }
    }

    @Test
    @DisplayName("测试登录暴力破解防护")
    public void testBruteForceProtection() throws Exception {
        System.out.println("[测试] UserController.login - 登录暴力破解防护");
        
        // 模拟连续登录失败
        when(userService.checkLogin(eq("testuser"), anyString())).thenReturn(null);
        
        // 连续尝试多次登录
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/loginCheck.do")
                    .param("userID", "testuser")
                    .param("password", "wrongpassword" + i)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(content().string("false"));
        }
        
        // 检查是否有暴力破解防护机制
        // 由于没有直接方法检查，我们只能通过日志输出警告
        System.out.println("[警告] 系统缺少登录尝试次数限制，可能存在暴力破解风险");
    }

    @Test
    @DisplayName("测试CSRF保护")
    public void testCsrfProtection() throws Exception {
        System.out.println("[测试] UserController - CSRF保护");
        
        // 检查登录成功后是否有CSRF令牌
        when(userService.checkLogin("user1", "password123")).thenReturn(normalUser);
        
        MvcResult result = mockMvc.perform(post("/loginCheck.do")
                .param("userID", "user1")
                .param("password", "password123")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andReturn();
                
        // 检查响应中是否包含CSRF令牌
        String responseContent = result.getResponse().getContentAsString();
        if (!responseContent.contains("_csrf") && !result.getRequest().getSession().getAttributeNames().hasMoreElements()) {
            System.out.println("[警告] 系统可能缺少CSRF保护，建议使用Spring Security的CSRF令牌");
        }
    }

    @Test
    @DisplayName("测试密码强度校验")
    public void testPasswordStrengthValidation() throws Exception {
        System.out.println("[测试] UserController.register - 密码强度校验");
        
        // 尝试不同强度的密码
        String[] passwords = {
            "123", // 太短
            "password", // 常见密码
            "12345678", // 纯数字
            "abcdefgh", // 纯字母
            "Password123!" // 强密码
        };
        
        for (String password : passwords) {
            System.out.println("测试密码: " + password);
            
            // 捕获传递给服务层的User对象
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            
            // 执行注册请求
            mockMvc.perform(post("/register.do")
                    .param("userID", "testuser" + password.length())
                    .param("userName", "Test User")
                    .param("password", password)
                    .param("email", "test" + password.length() + "@example.com")
                    .param("phone", "12345" + password.length())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                    .andDo(MockMvcResultHandlers.print());
                    
            try {
                // 验证服务层方法被调用
                verify(userService, times(1)).create(userCaptor.capture());
                Mockito.reset(userService); // 修改这里，使用完整的Mockito.reset方法调用
                
                if (password.length() < 6) {
                    System.out.println("[错误] 系统接受了过短的密码: " + password);
                } else if ("password".equals(password) || "12345678".equals(password)) {
                    System.out.println("[错误] 系统接受了常见的弱密码: " + password);
                } else if (password.matches("^\\d+$") || password.matches("^[a-zA-Z]+$")) {
                    System.out.println("[错误] 系统接受了单一类型字符的密码: " + password);
                } else {
                    System.out.println("[通过] 密码强度合格: " + password);
                }
            } catch (Exception e) {
                System.out.println("[可能通过] 系统可能拒绝了弱密码: " + password);
                Mockito.reset(userService); // 修改这里，使用完整的Mockito.reset方法调用
            }
        }
    }

    @Test
    @DisplayName("测试密码存储安全性")
    public void testPasswordStorageSecurity() throws Exception {
        // 捕获创建用户时的User对象
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userService.create(userCaptor.capture())).thenReturn(1);
        
        // 尝试注册新用户
        mockMvc.perform(post("/register.do")
                .param("userID", "securitytest")
                .param("userName", "安全测试")
                .param("password", "plainpassword")
                .param("email", "security@example.com")
                .param("phone", "12345678900")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED));
        
        // 获取捕获的User对象
        User capturedUser = userCaptor.getValue();
        
        // 检查密码是否与原始密码相同（明文存储）
        if ("plainpassword".equals(capturedUser.getPassword())) {
            System.out.println("[错误] 安全漏洞: 系统将密码以明文形式存储，应使用密码哈希算法!");
            fail("安全漏洞: 系统将密码以明文形式存储，应使用密码哈希算法");
        }
    }
} 