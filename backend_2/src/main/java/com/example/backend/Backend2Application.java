package com.example.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class Backend2Application {

  public static void main(String[] args) {
    SpringApplication.run(Backend2Application.class, args);
  }

}
