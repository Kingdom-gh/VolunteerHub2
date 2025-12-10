package com.example.backend.cache;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

import static com.example.backend.config.RedisCacheConfig.*;

@Component
public class PostCacheEvictHelper {

  // Xóa cache chi tiết một bài theo id
  @CacheEvict(cacheNames = POST_BY_ID, key = "#id")
  public void evictPostById(Long id) {
  }

  // Xóa cache “bài của tôi” theo email
  @CacheEvict(cacheNames = MY_POSTS_BY_EMAIL, key = "#email.toLowerCase()")
  public void evictMyPostsByEmail(String email) {
    // no-op
  }
}
