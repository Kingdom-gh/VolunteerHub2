package com.example.backend.repo;


import com.example.backend.entity.VolunteerPost;
import com.example.backend.entity.VolunteerRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

@Repository
public class VolunteerRequestRepository {

  private final JdbcTemplate jdbc;

  public VolunteerRequestRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public VolunteerRequest save(VolunteerRequest request) {
    if (request.getVolunteerPost() == null || request.getVolunteerPost().getId() == null) {
      throw new IllegalArgumentException("volunteerPost.id is required");
    }
    if (request.getVolunteer() == null || request.getVolunteer().getVolunteerEmail() == null) {
      throw new IllegalArgumentException("volunteerEmail is required");
    }

    String sql = "INSERT INTO volunteer_request (postId, volunteerEmail, requestDate, suggestion, status) VALUES (?, ?, ?, ?, ?)";
    KeyHolder kh = new GeneratedKeyHolder();
    jdbc.update(con -> {
      PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      ps.setLong(1, request.getVolunteerPost().getId());
      ps.setString(2, request.getVolunteer().getVolunteerEmail());
      ps.setTimestamp(3, request.getRequestDate() != null ? Timestamp.valueOf(request.getRequestDate()) : new Timestamp(System.currentTimeMillis()));
      ps.setString(4, request.getSuggestion());
      ps.setString(5, request.getStatus());
      return ps;
    }, kh);
    if (kh.getKey() != null) {
      request.setId(kh.getKey().longValue());
    }
    return request;
  }

  public List<VolunteerRequest> findByVolunteerVolunteerEmail(String volunteerEmail) {
    String sql =
      "SELECT r.id AS r_id, r.requestDate AS r_requestDate, r.suggestion AS r_suggestion, r.status AS r_status, " +
      "p.id AS p_id, p.postTitle AS p_postTitle, p.category AS p_category, p.deadline AS p_deadline, " +
      "p.location AS p_location, p.orgEmail AS p_orgEmail " +
      "FROM volunteer_request r " +
      "JOIN volunteer_post p ON p.id = r.postId " +
      "WHERE r.volunteerEmail = ? " +
      "ORDER BY r.id DESC";

    RowMapper<VolunteerRequest> mapper = (rs, i) -> {
      VolunteerPost p = new VolunteerPost();
      p.setId(rs.getLong("p_id"));
      p.setPostTitle(rs.getString("p_postTitle"));
      p.setCategory(rs.getString("p_category"));
      java.sql.Date d = rs.getDate("p_deadline");
      p.setDeadline(d != null ? d.toLocalDate() : null);
      p.setLocation(rs.getString("p_location"));
      p.setOrgEmail(rs.getString("p_orgEmail"));

      VolunteerRequest r = new VolunteerRequest();
      r.setId(rs.getLong("r_id"));
      Timestamp ts = rs.getTimestamp("r_requestDate");
      r.setRequestDate(ts != null ? ts.toLocalDateTime() : null);
      r.setSuggestion(rs.getString("r_suggestion"));
      r.setStatus(rs.getString("r_status"));
      r.setVolunteerPost(p);
      return r;
    };

    return jdbc.query(sql, mapper, volunteerEmail);
  }

  public boolean existsById(Long id) {
    Integer cnt = jdbc.queryForObject("SELECT COUNT(1) FROM volunteer_request WHERE id = ?", Integer.class, id);
    return cnt != null && cnt > 0;
  }

  public void deleteById(Long id) {
    jdbc.update("DELETE FROM volunteer_request WHERE id = ?", id);
  }
}