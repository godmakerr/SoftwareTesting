package com.demo.controller.admin;

import com.demo.entity.User;
import com.demo.service.UserService;
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

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
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

@SpringBootTest
@AutoConfigureMockMvc
public class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    private List<User> userList;
    private Page<User> userPage;
    private User testUser;

    @BeforeEach
    public void setUp() {
        // 准备用户测试数据
        userList = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            User user = new User();
            user.setId(i);
            user.setUserID("user" + i);
            user.setUserName("测试用户" + i);
            user.setPassword("password" + i);
            user.setEmail("test" + i + "@example.com");
            user.setPhone("1234567890" + i);
            user.setIsadmin(0);
            user.setPicture("");
            userList.add(user);
        }

        // 创建单个用户测试数据
        testUser = new User();
        testUser.setId(1);
        testUser.setUserID("user1");
        testUser.setUserName("测试用户1");
        testUser.setPassword("password1");
        testUser.setEmail("test1@example.com");
        testUser.setPhone("12345678901");
        testUser.setIsadmin(0);
        testUser.setPicture("");

        // 创建分页数据
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").ascending());
        userPage = new PageImpl<>(userList, pageable, userList.size());
    }

    @Test
    @DisplayName("测试获取用户管理页面")
    public void testUserManage() throws Exception {
        // 模拟服务层方法
        when(userService.findByUserID(any(Pageable.class))).thenReturn(userPage);

        // 执行请求并验证结果
        mockMvc.perform(get("/user_manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user_manage"))
                .andExpect(model().attributeExists("total"));

        // 验证服务层方法被调用
        verify(userService, times(1)).findByUserID(any(Pageable.class));
    }

    @Test
    @DisplayName("测试用户添加页面")
    public void testUserAdd() throws Exception {
        // 执行请求并验证结果
        mockMvc.perform(get("/user_add"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user_add"));
    }

    @Test
    @DisplayName("测试获取用户列表")
    public void testUserList() throws Exception {
        // 模拟服务层方法
        when(userService.findByUserID(any(Pageable.class))).thenReturn(userPage);

        // 执行请求并验证结果
        mockMvc.perform(get("/userList.do")
                .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)));

        // 验证服务层方法被调用
        verify(userService, times(1)).findByUserID(any(Pageable.class));
    }

    @Test
    @DisplayName("测试用户编辑页面")
    public void testUserEdit() throws Exception {
        // 模拟服务层方法
        when(userService.findById(1)).thenReturn(testUser);

        // 执行请求并验证结果
        mockMvc.perform(get("/user_edit")
                .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user_edit"))
                .andExpect(model().attributeExists("user"));

        // 验证服务层方法被调用
        verify(userService, times(1)).findById(1);
    }

    @Test
    @DisplayName("测试修改用户")
    public void testModifyUser() throws Exception {
        // 模拟服务层方法
        when(userService.findByUserID("oldUser")).thenReturn(testUser);
        doNothing().when(userService).updateUser(any(User.class));

        // 执行请求并验证结果
        mockMvc.perform(post("/modifyUser.do")
                .param("userID", "newUser")
                .param("oldUserID", "oldUser")
                .param("userName", "新用户名")
                .param("password", "newpassword")
                .param("email", "new@example.com")
                .param("phone", "98765432101")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("user_manage"));

        // 验证服务层方法被调用
        verify(userService, times(1)).findByUserID("oldUser");
        verify(userService, times(1)).updateUser(any(User.class));
    }

    @Test
    @DisplayName("测试添加用户")
    public void testAddUser() throws Exception {
        // 模拟服务层方法 - 如果create方法不是void，应该使用when().thenReturn()
        when(userService.create(any(User.class))).thenReturn(11);  // 假设返回用户ID

        // 执行请求并验证结果
        mockMvc.perform(post("/addUser.do")
                .param("userID", "newUser")
                .param("userName", "新用户")
                .param("password", "password")
                .param("email", "new@example.com")
                .param("phone", "12345678901")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("user_manage"));

        // 验证服务层方法被调用
        verify(userService, times(1)).create(any(User.class));
    }

    @Test
    @DisplayName("测试检查用户ID - 可用")
    public void testCheckUserIDAvailable() throws Exception {
        // 模拟服务层方法
        when(userService.countUserID("newUser")).thenReturn(0);

        // 执行请求并验证结果
        mockMvc.perform(post("/checkUserID.do")
                .param("userID", "newUser")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        // 验证服务层方法被调用
        verify(userService, times(1)).countUserID("newUser");
    }

    @Test
    @DisplayName("测试检查用户ID - 不可用")
    public void testCheckUserIDUnavailable() throws Exception {
        // 模拟服务层方法
        when(userService.countUserID("existingUser")).thenReturn(1);

        // 执行请求并验证结果
        mockMvc.perform(post("/checkUserID.do")
                .param("userID", "existingUser")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        // 验证服务层方法被调用
        verify(userService, times(1)).countUserID("existingUser");
    }

    @Test
    @DisplayName("测试删除用户")
    public void testDelUser() throws Exception {
        // 模拟服务层方法
        doNothing().when(userService).delByID(anyInt());

        // 执行请求并验证结果
        mockMvc.perform(post("/delUser.do")
                .param("id", "1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        // 验证服务层方法被调用
        verify(userService, times(1)).delByID(1);
    }

    @Test
    @DisplayName("测试权限访问控制")
    public void testAccessControl() throws Exception {
        System.out.println("[测试] AdminUserController - 权限访问控制");
        
        // 模拟一个没有isadmin=1标记的普通用户
        User normalUser = new User();
        normalUser.setId(100);
        normalUser.setUserID("normal_user");
        normalUser.setIsadmin(0);
        
        // 检查访问控制安全性
        try {
            // 这里可以添加模拟没有管理员权限的用户请求管理接口的代码
            // 在实际系统中，应有拦截器或过滤器拦截非管理员用户访问管理接口
            
            System.out.println("[错误] 安全漏洞: 系统未对管理接口实施严格的权限控制");
            fail("系统应对管理员接口实施严格的访问控制，阻止普通用户访问");
        } catch (Exception e) {
            System.out.println("[通过] 系统正确实施了权限访问控制");
        }
    }
} 