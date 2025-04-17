package com.demo.service.impl;

import com.demo.dao.VenueDao;
import com.demo.entity.Venue;
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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VenueServiceImplTest {

    @Mock
    private VenueDao venueDao;

    @InjectMocks
    private VenueServiceImpl venueService;

    private Venue testVenue;
    private List<Venue> venueList;
    private Page<Venue> venuePage;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

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
        
        // 创建场馆列表和分页
        venueList = new ArrayList<>();
        venueList.add(testVenue);
        
        venuePage = new PageImpl<>(venueList);
    }

    // 根据ID查询场馆测试

    @Test
    @DisplayName("测试根据ID查询场馆 - 场馆存在")
    void findByVenueID_VenueExists() {
        // 设置模拟行为
        when(venueDao.getOne(1)).thenReturn(testVenue);

        // 执行测试
        Venue result = venueService.findByVenueID(1);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getVenueID());
        assertEquals("测试场馆", result.getVenueName());
    }
    
    @Test
    @DisplayName("测试根据ID查询场馆 - 场馆不存在")
    void findByVenueID_VenueNotExists() {
        // 设置模拟行为 - 不存在的场馆ID抛出异常
        when(venueDao.getOne(999)).thenThrow(new javax.persistence.EntityNotFoundException("Venue not found"));
        
        // 执行测试并验证异常
        assertThrows(javax.persistence.EntityNotFoundException.class, () -> {
            venueService.findByVenueID(999);
        });
    }
    
    @Test
    @DisplayName("测试根据ID查询场馆 - ID为负数")
    void findByVenueID_NegativeID() {
        // 设置模拟行为 - 负数ID抛出异常
        when(venueDao.getOne(-1)).thenThrow(new IllegalArgumentException("ID must be positive"));
        
        // 执行测试并验证异常
        assertThrows(IllegalArgumentException.class, () -> {
            venueService.findByVenueID(-1);
        });
    }
    
    @Test
    @DisplayName("测试根据ID查询场馆 - ID为0")
    void findByVenueID_ZeroID() {
        // 设置模拟行为 - ID为0抛出异常
        when(venueDao.getOne(0)).thenThrow(new IllegalArgumentException("ID must be positive"));
        
        // 执行测试并验证异常
        assertThrows(IllegalArgumentException.class, () -> {
            venueService.findByVenueID(0);
        });
    }
    
    // 根据名称查询场馆测试
    
    @Test
    @DisplayName("测试根据名称查询场馆 - 场馆存在")
    void findByVenueName_VenueExists() {
        // 设置模拟行为
        when(venueDao.findByVenueName("测试场馆")).thenReturn(testVenue);
        
        // 执行测试
        Venue result = venueService.findByVenueName("测试场馆");
        
        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getVenueID());
        assertEquals("测试场馆", result.getVenueName());
    }
    
    @Test
    @DisplayName("测试根据名称查询场馆 - 场馆不存在")
    void findByVenueName_VenueNotExists() {
        // 设置模拟行为
        when(venueDao.findByVenueName("不存在的场馆")).thenReturn(null);
        
        // 执行测试
        Venue result = venueService.findByVenueName("不存在的场馆");
        
        // 验证结果
        assertNull(result);
    }
    
    @Test
    @DisplayName("测试根据名称查询场馆 - 名称为null")
    void findByVenueName_NullName() {
        // 设置模拟行为
        when(venueDao.findByVenueName(null)).thenReturn(null);
        
        // 执行测试
        Venue result = venueService.findByVenueName(null);
        
        // 验证结果
        assertNull(result);
    }
    
    @Test
    @DisplayName("测试根据名称查询场馆 - 名称为空字符串")
    void findByVenueName_EmptyName() {
        // 设置模拟行为
        when(venueDao.findByVenueName("")).thenReturn(null);
        
        // 执行测试
        Venue result = venueService.findByVenueName("");
        
        // 验证结果
        assertNull(result);
    }
    
    // 分页查询所有场馆测试
    
    @Test
    @DisplayName("测试分页查询所有场馆 - 正常情况")
    void findAll_Pagination() {
        // 创建分页请求
        Pageable pageable = PageRequest.of(0, 10);
        
        // 设置模拟行为
        when(venueDao.findAll(pageable)).thenReturn(venuePage);
        
        // 执行测试
        Page<Venue> result = venueService.findAll(pageable);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testVenue.getVenueID(), result.getContent().get(0).getVenueID());
        assertEquals(testVenue.getVenueName(), result.getContent().get(0).getVenueName());
    }
    
    @Test
    @DisplayName("测试分页查询所有场馆 - 空结果")
    void findAll_Pagination_Empty() {
        // 创建分页请求
        Pageable pageable = PageRequest.of(0, 10);
        
        // 创建空页面
        Page<Venue> emptyPage = new PageImpl<>(new ArrayList<>());
        
        // 设置模拟行为
        when(venueDao.findAll(pageable)).thenReturn(emptyPage);
        
        // 执行测试
        Page<Venue> result = venueService.findAll(pageable);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }
    
    @Test
    @DisplayName("测试分页查询所有场馆 - 页码超出范围")
    void findAll_Pagination_OutOfRange() {
        // 创建分页请求 - 页码超出范围
        Pageable pageable = PageRequest.of(100, 10);
        
        // 创建空页面
        Page<Venue> emptyPage = new PageImpl<>(new ArrayList<>());
        
        // 设置模拟行为
        when(venueDao.findAll(pageable)).thenReturn(emptyPage);
        
        // 执行测试
        Page<Venue> result = venueService.findAll(pageable);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }
    
    // 查询所有场馆测试
    
    @Test
    @DisplayName("测试查询所有场馆 - 有场馆")
    void findAll_WithVenues() {
        // 设置模拟行为
        when(venueDao.findAll()).thenReturn(venueList);
        
        // 执行测试
        List<Venue> result = venueService.findAll();
        
        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testVenue.getVenueID(), result.get(0).getVenueID());
        assertEquals(testVenue.getVenueName(), result.get(0).getVenueName());
    }
    
    @Test
    @DisplayName("测试查询所有场馆 - 无场馆")
    void findAll_NoVenues() {
        // 设置模拟行为
        when(venueDao.findAll()).thenReturn(new ArrayList<>());
        
        // 执行测试
        List<Venue> result = venueService.findAll();
        
        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    // 创建场馆测试
    
    @Test
    @DisplayName("测试创建场馆 - 正常情况")
    void create_Success() {
        // 创建一个新场馆
        Venue newVenue = new Venue();
        newVenue.setVenueName("新创建的场馆");
        newVenue.setDescription("这是新创建的场馆描述");
        newVenue.setPrice(200);
        newVenue.setAddress("新地址");
        newVenue.setOpen_time("10:00");
        newVenue.setClose_time("23:00");
        newVenue.setPicture("new.jpg");
        
        // 设置模拟行为 - 保存后返回带ID的场馆
        Venue savedVenue = new Venue();
        savedVenue.setVenueID(2);
        savedVenue.setVenueName("新创建的场馆");
        savedVenue.setDescription("这是新创建的场馆描述");
        savedVenue.setPrice(200);
        savedVenue.setAddress("新地址");
        savedVenue.setOpen_time("10:00");
        savedVenue.setClose_time("23:00");
        savedVenue.setPicture("new.jpg");
        
        when(venueDao.save(any(Venue.class))).thenReturn(savedVenue);
        
        // 执行测试
        int result = venueService.create(newVenue);
        
        // 验证结果
        assertEquals(2, result);
        verify(venueDao, times(1)).save(newVenue);
    }
    
    @Test
    @DisplayName("测试创建场馆 - ID字段已设置")
    void create_WithID() {
        // 创建一个带ID的新场馆
        Venue newVenue = new Venue();
        newVenue.setVenueID(100); // 预设ID
        newVenue.setVenueName("带ID的新场馆");
        newVenue.setDescription("这是带ID的新场馆描述");
        newVenue.setPrice(200);
        
        // 设置模拟行为 - 返回原始对象，保留其ID
        when(venueDao.save(newVenue)).thenReturn(newVenue);
        
        // 执行测试
        int result = venueService.create(newVenue);
        
        // 验证结果 - 应该返回预设的ID
        assertEquals(100, result);
        verify(venueDao, times(1)).save(newVenue);
    }
    
    @Test
    @DisplayName("测试创建场馆 - 名称为null")
    void create_NullName() {
        // 创建名称为null的场馆
        Venue newVenue = new Venue();
        newVenue.setVenueName(null);
        newVenue.setDescription("这是描述");
        newVenue.setPrice(200);
        
        // 设置模拟行为 - 保存后返回带ID的场馆
        Venue savedVenue = new Venue();
        savedVenue.setVenueID(2);
        savedVenue.setVenueName(null);
        savedVenue.setDescription("这是描述");
        savedVenue.setPrice(200);
        
        when(venueDao.save(any(Venue.class))).thenReturn(savedVenue);
        
        // 执行测试
        int result = venueService.create(newVenue);
        
        // 验证结果 - 服务层没有验证名称不能为null
        assertEquals(2, result);
        verify(venueDao, times(1)).save(newVenue);
    }
    
    @Test
    @DisplayName("测试创建场馆 - 价格为负数")
    void create_NegativePrice() {
        // 创建价格为负数的场馆
        Venue newVenue = new Venue();
        newVenue.setVenueName("负价格场馆");
        newVenue.setDescription("这是负价格场馆");
        newVenue.setPrice(-100);
        
        // 设置模拟行为 - 保存后返回带ID的场馆
        Venue savedVenue = new Venue();
        savedVenue.setVenueID(2);
        savedVenue.setVenueName("负价格场馆");
        savedVenue.setDescription("这是负价格场馆");
        savedVenue.setPrice(-100);
        
        when(venueDao.save(any(Venue.class))).thenReturn(savedVenue);
        
        // 执行测试
        int result = venueService.create(newVenue);
        
        // 验证结果 - 服务层没有验证价格不能为负数
        assertEquals(2, result);
        verify(venueDao, times(1)).save(newVenue);
    }
    
    @Test
    @DisplayName("测试创建场馆 - 名称超长")
    void create_NameTooLong() {
        // 创建名称超长的场馆（假设有限制，实际上代码中没有显式限制）
        StringBuilder longName = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longName.append("名称");
        }
        
        Venue newVenue = new Venue();
        newVenue.setVenueName(longName.toString());
        newVenue.setDescription("这是名称超长的场馆");
        newVenue.setPrice(200);
        
        // 设置模拟行为 - 假设DAO会拒绝过长的名称
        when(venueDao.save(newVenue)).thenThrow(new IllegalArgumentException("Venue name too long"));
        
        // 执行测试并验证异常
        assertThrows(IllegalArgumentException.class, () -> {
            venueService.create(newVenue);
        });
    }
    
    // 更新场馆测试
    
    @Test
    @DisplayName("测试更新场馆 - 正常情况")
    void update_Success() {
        // 更新测试场馆
        testVenue.setVenueName("更新后的场馆");
        testVenue.setDescription("更新后的描述");
        testVenue.setPrice(150);
        testVenue.setAddress("更新后的地址");
        
        // 执行测试
        venueService.update(testVenue);
        
        // 验证行为
        verify(venueDao, times(1)).save(testVenue);
    }
    
    @Test
    @DisplayName("测试更新场馆 - 场馆不存在")
    void update_VenueNotExists() {
        // 创建一个不存在的场馆
        Venue nonExistentVenue = new Venue();
        nonExistentVenue.setVenueID(999);
        nonExistentVenue.setVenueName("不存在的场馆");
        nonExistentVenue.setDescription("这是不存在的场馆描述");
        nonExistentVenue.setPrice(200);
        
        // 设置模拟行为 - 允许更新不存在的场馆（实际上会创建新场馆）
        when(venueDao.save(nonExistentVenue)).thenReturn(nonExistentVenue);
        
        // 执行测试
        venueService.update(nonExistentVenue);
        
        // 验证行为
        verify(venueDao, times(1)).save(nonExistentVenue);
    }
    
    @Test
    @DisplayName("测试更新场馆 - ID为null")
    void update_NullID() {
        // 创建一个ID为null的场馆
        Venue venueWithNullID = new Venue();
        // 不设置ID
        venueWithNullID.setVenueName("ID为null的场馆");
        venueWithNullID.setDescription("这是ID为null的场馆描述");
        venueWithNullID.setPrice(200);
        
        // 设置模拟行为 - 保存时会自动生成ID（实际上会创建新场馆）
        Venue savedVenue = new Venue();
        savedVenue.setVenueID(2);
        savedVenue.setVenueName("ID为null的场馆");
        savedVenue.setDescription("这是ID为null的场馆描述");
        savedVenue.setPrice(200);
        
        when(venueDao.save(venueWithNullID)).thenReturn(savedVenue);
        
        // 执行测试
        venueService.update(venueWithNullID);
        
        // 验证行为
        verify(venueDao, times(1)).save(venueWithNullID);
    }
    
    @Test
    @DisplayName("测试更新场馆 - 名称为null")
    void update_NullName() {
        // 更新测试场馆的名称为null
        testVenue.setVenueName(null);
        
        // 执行测试
        venueService.update(testVenue);
        
        // 验证行为 - 服务层没有验证名称不能为null
        verify(venueDao, times(1)).save(testVenue);
    }
    
    @Test
    @DisplayName("测试更新场馆 - 价格为负数")
    void update_NegativePrice() {
        // 更新测试场馆的价格为负数
        testVenue.setPrice(-100);
        
        // 执行测试
        venueService.update(testVenue);
        
        // 验证行为 - 服务层没有验证价格不能为负数
        verify(venueDao, times(1)).save(testVenue);
    }
    
    // 删除场馆测试
    
    @Test
    @DisplayName("测试删除场馆 - 正常情况")
    void delById_Success() {
        // 执行测试
        venueService.delById(1);
        
        // 验证行为
        verify(venueDao, times(1)).deleteById(1);
    }
    
    @Test
    @DisplayName("测试删除场馆 - 场馆不存在")
    void delById_VenueNotExists() {
        // 设置模拟行为 - 删除不存在的场馆时抛出异常
        doThrow(new IllegalArgumentException("Venue not found")).when(venueDao).deleteById(999);
        
        // 执行测试并验证异常
        assertThrows(IllegalArgumentException.class, () -> {
            venueService.delById(999);
        });
        
        // 验证行为
        verify(venueDao, times(1)).deleteById(999);
    }
    
    @Test
    @DisplayName("测试删除场馆 - ID为负数")
    void delById_NegativeID() {
        // 设置模拟行为 - 删除ID为负数的场馆时抛出异常
        doThrow(new IllegalArgumentException("ID must be positive")).when(venueDao).deleteById(-1);
        
        // 执行测试并验证异常
        assertThrows(IllegalArgumentException.class, () -> {
            venueService.delById(-1);
        });
        
        // 验证行为
        verify(venueDao, times(1)).deleteById(-1);
    }
    
    @Test
    @DisplayName("测试删除场馆 - ID为0")
    void delById_ZeroID() {
        // 设置模拟行为 - 删除ID为0的场馆时抛出异常
        doThrow(new IllegalArgumentException("ID must be positive")).when(venueDao).deleteById(0);
        
        // 执行测试并验证异常
        assertThrows(IllegalArgumentException.class, () -> {
            venueService.delById(0);
        });
        
        // 验证行为
        verify(venueDao, times(1)).deleteById(0);
    }
    
    // 统计场馆名称测试
    
    @Test
    @DisplayName("测试统计场馆名称 - 场馆存在")
    void countVenueName_VenueExists() {
        // 设置模拟行为
        when(venueDao.countByVenueName("测试场馆")).thenReturn(1);
        
        // 执行测试
        int result = venueService.countVenueName("测试场馆");
        
        // 验证结果
        assertEquals(1, result);
    }
    
    @Test
    @DisplayName("测试统计场馆名称 - 场馆不存在")
    void countVenueName_VenueNotExists() {
        // 设置模拟行为
        when(venueDao.countByVenueName("不存在的场馆")).thenReturn(0);
        
        // 执行测试
        int result = venueService.countVenueName("不存在的场馆");
        
        // 验证结果
        assertEquals(0, result);
    }
    
    @Test
    @DisplayName("测试统计场馆名称 - 名称为null")
    void countVenueName_NullName() {
        // 设置模拟行为
        when(venueDao.countByVenueName(null)).thenReturn(0);
        
        // 执行测试
        int result = venueService.countVenueName(null);
        
        // 验证结果
        assertEquals(0, result);
    }
    
    @Test
    @DisplayName("测试统计场馆名称 - 名称为空字符串")
    void countVenueName_EmptyName() {
        // 设置模拟行为
        when(venueDao.countByVenueName("")).thenReturn(0);
        
        // 执行测试
        int result = venueService.countVenueName("");
        
        // 验证结果
        assertEquals(0, result);
    }
    
    @Test
    @DisplayName("测试统计场馆名称 - 多个相同名称的场馆")
    void countVenueName_MultipleVenues() {
        // 设置模拟行为 - 理论上不应该有多个相同的场馆名称，但需要测试这种边界情况
        when(venueDao.countByVenueName("重复的场馆")).thenReturn(3);
        
        // 执行测试
        int result = venueService.countVenueName("重复的场馆");
        
        // 验证结果
        assertEquals(3, result);
    }
} 