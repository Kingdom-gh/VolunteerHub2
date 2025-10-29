package com.example.backend.repo;


import com.example.backend.entity.VolunteerPost;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class VolunteerPostRepository {

  private final JdbcTemplate jdbc;

  public VolunteerPostRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  private final RowMapper<VolunteerPost> POST_MAPPER = (rs, i) -> {
    VolunteerPost p = new VolunteerPost();
    p.setId(rs.getLong("id"));
    p.setPostTitle(rs.getString("postTitle"));
    p.setCategory(rs.getString("category"));
    Date d = rs.getDate("deadline");
    p.setDeadline(d != null ? d.toLocalDate() : null);
    p.setLocation(rs.getString("location"));
    p.setDescription(rs.getString("description"));
    p.setThumbnail(rs.getString("thumbnail"));
    int nov = rs.getInt("noOfVolunteer");
    p.setNoOfVolunteer(rs.wasNull() ? null : nov);
    p.setOrgName(rs.getString("orgName"));
    p.setOrgEmail(rs.getString("orgEmail"));
    return p;
  };

  // Lấy 6 bài sắp hết hạn (deadline ASC)
  public List<VolunteerPost> findTop6ByOrderByDeadlineAsc() {
    String sql = "SELECT id, postTitle, category, deadline, location, description, thumbnail, noOfVolunteer, orgName, orgEmail " +
                 "FROM volunteer_post ORDER BY deadline ASC LIMIT 6";
    return jdbc.query(sql, POST_MAPPER);
  }

  // Tìm kiếm bằng postTitle (LIKE)
  public List<VolunteerPost> findByPostTitleContainingIgnoreCase(String postTitle) {
    String like = "%" + postTitle.toLowerCase() + "%";
    String sql = "SELECT id, postTitle, category, deadline, location, description, thumbnail, noOfVolunteer, orgName, orgEmail " +
                 "FROM volunteer_post WHERE LOWER(postTitle) LIKE ?";
    return jdbc.query(sql, POST_MAPPER, like);
  }

  // Lấy bài đăng theo email của tổ chức (API bị comment)
  public List<VolunteerPost> findByOrgEmail(String orgEmail) {
    String sql = "SELECT id, postTitle, category, deadline, location, description, thumbnail, noOfVolunteer, orgName, orgEmail " +
                 "FROM volunteer_post WHERE orgEmail = ? ORDER BY id DESC";
    return jdbc.query(sql, POST_MAPPER, orgEmail);
  }

  public List<VolunteerPost> findAll() {
    String sql = "SELECT id, postTitle, category, deadline, location, description, thumbnail, noOfVolunteer, orgName, orgEmail " +
                 "FROM volunteer_post ORDER BY id DESC";
    return jdbc.query(sql, POST_MAPPER);
  }

  public Optional<VolunteerPost> findById(Long id) {
    String sql = "SELECT id, postTitle, category, deadline, location, description, thumbnail, noOfVolunteer, orgName, orgEmail " +
                 "FROM volunteer_post WHERE id = ?";
    List<VolunteerPost> list = jdbc.query(sql, POST_MAPPER, id);
    return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
  }

  public VolunteerPost save(VolunteerPost post) {
    if (post.getId() == null) {
      String sql = "INSERT INTO volunteer_post (postTitle, category, deadline, location, description, thumbnail, noOfVolunteer, orgName, orgEmail) " +
                   "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
      KeyHolder kh = new GeneratedKeyHolder();
      jdbc.update(con -> {
        PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, post.getPostTitle());
        ps.setString(2, post.getCategory());
        if (post.getDeadline() != null) {
          ps.setDate(3, Date.valueOf(post.getDeadline()));
        } else {
          ps.setNull(3, java.sql.Types.DATE);
        }
        ps.setString(4, post.getLocation());
        ps.setString(5, post.getDescription());
        ps.setString(6, post.getThumbnail());
        if (post.getNoOfVolunteer() != null) {
          ps.setInt(7, post.getNoOfVolunteer());
        } else {
          ps.setNull(7, java.sql.Types.INTEGER);
        }
        ps.setString(8, post.getOrgName());
        ps.setString(9, post.getOrgEmail());
        return ps;
      }, kh);
      Number key = kh.getKey();
      if (key != null) post.setId(key.longValue());
      return post;
    } else {
      String sql = "UPDATE volunteer_post SET postTitle=?, category=?, deadline=?, location=?, description=?, thumbnail=?, noOfVolunteer=?, orgName=?, orgEmail=? WHERE id=?";
      jdbc.update(sql,
        post.getPostTitle(),
        post.getCategory(),
        post.getDeadline() != null ? Date.valueOf(post.getDeadline()) : null,
        post.getLocation(),
        post.getDescription(),
        post.getThumbnail(),
        post.getNoOfVolunteer(),
        post.getOrgName(),
        post.getOrgEmail(),
        post.getId()
      );
      return post;
    }
  }

  // API: update-volunteer-count/:id
  public int decrementVolunteerCount(Long id) {
    String sql = "UPDATE volunteer_post SET noOfVolunteer = noOfVolunteer - 1 WHERE id = ? AND noOfVolunteer > 0";
    return jdbc.update(sql, id);
  }

  public boolean existsById(Long id) {
    Integer cnt = jdbc.queryForObject("SELECT COUNT(1) FROM volunteer_post WHERE id = ?", Integer.class, id);
    return cnt != null && cnt > 0;
  }

  public void deleteById(Long id) {
    jdbc.update("DELETE FROM volunteer_post WHERE id = ?", id);
  }
}