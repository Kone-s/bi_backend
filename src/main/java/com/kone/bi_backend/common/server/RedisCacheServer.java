package com.kone.bi_backend.common.server;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kone.bi_backend.model.entity.Chart;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;

/**
 * redis缓存服务
 */
@Service
public class RedisCacheServer {
    @Resource
    private RedissonClient redissonClient;

    public Page<Chart> getCachedResult(String cacheKey) {
        // 从缓存中获取数据
        RMap<String, Page<Chart>> cache = redissonClient.getMap(cacheKey);
        return cache.get(cacheKey);
    }

    public void putCachedResult(String cacheKey, Page<Chart> chartPage) {
        // 放入缓存
        RMap<String, Page<Chart>> cache = redissonClient.getMap(cacheKey);
        cache.put(cacheKey, chartPage);
        // 设置缓存过期时间为60秒
        cache.expire(Duration.ofSeconds(60));
    }

    @Async
    public void asyncPutCachedResult(String cacheKey, Page<Chart> chartPage) {
        putCachedResult(cacheKey, chartPage);
    }
}

