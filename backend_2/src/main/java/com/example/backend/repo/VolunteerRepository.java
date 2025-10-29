package com.example.backend.repo;

import com.example.backend.entity.Volunteer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class VolunteerRepository {

  private final JdbcTemplate jdbc;

  public VolunteerRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  private final RowMapper<Volunteer> VOL_MAPPER = (rs, i) -> {
    Volunteer v = new Volunteer();
    v.setVolunteerEmail(rs.getString("volunteerEmail"));
    return v;
  };

  public Optional<Volunteer> findByVolunteerEmail(String volunteerEmail) {
    String sql = "SELECT volunteerEmail FROM volunteer WHERE volunteerEmail = ?";
    List<Volunteer> list = jdbc.query(sql, VOL_MAPPER, volunteerEmail);
    return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
  }

  public void save(Volunteer volunteer) {
    jdbc.update("INSERT INTO volunteer (volunteerEmail) VALUES (?)", volunteer.getVolunteerEmail());
  }
}