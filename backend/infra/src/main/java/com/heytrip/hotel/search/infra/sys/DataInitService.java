package com.heytrip.hotel.search.infra.sys;

/**
 * 数据初始化服务
 */
public interface DataInitService {
    
    /**
     * 初始化默认应用
     */
    void initDefaultApp();
    
    /**
     * 初始化默认用户
     */
    void initDefaultUser();
    
    /**
     * 初始化所有数据
     */
    void initAllData();
}
