package com.example.backend.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
public class RedisCacheConfig {

  // Tên cache thống nhất
  public static final String HOME_TOP6 = "homeTop6";
  public static final String POSTS = "posts";
  public static final String POST_BY_ID = "postById";
  public static final String MY_POSTS_BY_EMAIL = "myPostsByEmail";
  public static final String MY_REQUESTS_BY_EMAIL = "myRequestsByEmail";

  @Bean
  public CacheManager cacheManager(RedisConnectionFactory cf) {

    JdkSerializationRedisSerializer valueSerializer = new JdkSerializationRedisSerializer();

    RedisCacheConfiguration defaultCfg = RedisCacheConfiguration
        .defaultCacheConfig()
        .disableCachingNullValues()
        .serializeValuesWith(
            RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer)
        )
        .prefixCacheNameWith("vhub::")
        .entryTtl(Duration.ofSeconds(60)); // TTL mặc định

    Map<String, RedisCacheConfiguration> cfgMap = new HashMap<>();
    cfgMap.put(HOME_TOP6,           defaultCfg.entryTtl(Duration.ofSeconds(60)));
    cfgMap.put(POSTS,               defaultCfg.entryTtl(Duration.ofSeconds(60)));
    cfgMap.put(POST_BY_ID,          defaultCfg.entryTtl(Duration.ofMinutes(5)));
    cfgMap.put(MY_POSTS_BY_EMAIL,   defaultCfg.entryTtl(Duration.ofSeconds(45)));
    cfgMap.put(MY_REQUESTS_BY_EMAIL,defaultCfg.entryTtl(Duration.ofSeconds(45)));

    return RedisCacheManager.builder(cf)
        .cacheDefaults(defaultCfg)
        .withInitialCacheConfigurations(cfgMap)
        .build();
  }
}
