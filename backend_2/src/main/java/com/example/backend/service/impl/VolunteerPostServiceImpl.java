package com.example.backend.service.impl;

import com.example.backend.entity.VolunteerPost;
import com.example.backend.repo.VolunteerPostRepository;
import com.example.backend.service.VolunteerPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import com.example.backend.cache.PostCacheEvictHelper;
import static com.example.backend.config.RedisCacheConfig.*;
import com.example.backend.dto.VolunteerPostDto;
import java.util.stream.Collectors;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VolunteerPostServiceImpl implements VolunteerPostService {

    private final VolunteerPostRepository postRepository;
    private final PostCacheEvictHelper postCacheEvictHelper;

    private VolunteerPostDto toDto(VolunteerPost post) {
        if (post == null) {
            return null;
        }
        return new VolunteerPostDto(
            post.getId(),
            post.getPostTitle(),
            post.getCategory(),
            post.getDeadline(),
            post.getLocation(),
            post.getDescription(),
            post.getThumbnail(),
            post.getNoOfVolunteer(),
            post.getOrgName(),
            post.getOrgEmail()
        );
    }

    @Override
    @Cacheable(cacheNames = HOME_TOP6, key = "'top6'",
        unless = "#result == null || #result.isEmpty()")
    public List<VolunteerPostDto> getLatestVolunteers() {
        List<VolunteerPost> posts = postRepository.findTop6ByOrderByDeadlineAsc();
        return posts.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    @Override
    @Cacheable(cacheNames = POSTS,
        key = "'q:' + (#search == null ? '' : #search.trim().toLowerCase())",
        unless = "#result == null || #result.isEmpty()")
    public List<VolunteerPostDto> getAllVolunteers(String search) {
        List<VolunteerPost> posts;
        if (search != null && !search.isEmpty()) {
            posts = postRepository.findByPostTitleContainingIgnoreCase(search);
        } else {
            posts = postRepository.findAll();
        }

        return posts.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    @Override
    @Cacheable(cacheNames = POST_BY_ID, key = "#id", unless = "#result == null")
    public VolunteerPostDto getVolunteerPostDetails(Long id) {
        return postRepository.findById(id)
            .map(this::toDto)
            .orElse(null);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {POSTS, HOME_TOP6}, allEntries = true) // danh sách & top6 bẩn → xóa hết
    public Long addVolunteerPost(VolunteerPost post) {
        post.setId(null); // ✅ Ép insert

        if (post.getPostTitle() == null || post.getPostTitle().isBlank()) {
            throw new IllegalArgumentException("postTitle is required");
        }

        // Nếu chọn Cách A (cho phép client gửi orgEmail), giữ check này:
        // Nếu chọn Cách B (lấy từ principal ở controller), bỏ check này đi.
        if (post.getOrgEmail() == null || post.getOrgEmail().isBlank()) {
          throw new IllegalArgumentException("orgEmail is required");
        } else {
           post.setOrgEmail(post.getOrgEmail().trim());
        }

    // ✅ Bù mặc định để tránh NOT NULL
        if (post.getNoOfVolunteer() == null) post.setNoOfVolunteer(0);
        var saved = postRepository.saveAndFlush(post);
        return saved.getId();
    }


    @Override
    @Transactional
    @CacheEvict(cacheNames = {POSTS, HOME_TOP6}, allEntries = true)
    public int decrementVolunteerCount(Long id) {
        int n = postRepository.decrementVolunteerCount(id);
        // XÓA cache chi tiết đúng id (annotation-based qua helper):
        postCacheEvictHelper.evictPostById(id);
        return n;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {POSTS, HOME_TOP6}, allEntries = true)
    public void updateVolunteerPost(Long id, VolunteerPost updatedData) {
        var existing = postRepository.findById(id).orElse(null);
        if (existing == null) return;
        // copy các field được phép sửa trong controller
        existing.setPostTitle(updatedData.getPostTitle());
        existing.setDeadline(updatedData.getDeadline());
        existing.setLocation(updatedData.getLocation());
        existing.setCategory(updatedData.getCategory());
        existing.setThumbnail(updatedData.getThumbnail());
        existing.setDescription(updatedData.getDescription());
        // Lưu ý: orgEmail/orgName/noOfVolunteer KHÔNG cập nhật ở API này
        postRepository.save(existing);
        postCacheEvictHelper.evictPostById(id);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {POSTS, HOME_TOP6}, allEntries = true)
    public void deleteVolunteerPost(Long id) {
        if (postRepository.existsById(id)) {
            postRepository.deleteById(id);
            postCacheEvictHelper.evictPostById(id);
        }
    }

    @Override
    @Cacheable(cacheNames = MY_POSTS_BY_EMAIL, key = "#email",
        unless = "#result == null || #result.isEmpty()")
    public List<VolunteerPostDto> getMyVolunteerPosts(String email) {
        List<VolunteerPost> posts = postRepository.findByOrgEmail(email);
        return posts.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
}
