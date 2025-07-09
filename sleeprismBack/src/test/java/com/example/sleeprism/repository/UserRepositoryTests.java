package com.example.sleeprism.repository;

import com.example.sleeprism.entity.User;
import com.example.sleeprism.entity.UserRole;
import com.example.sleeprism.entity.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserRepositoryTests {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Test
  public void insertUser() {
    Random random = new Random();

    IntStream.rangeClosed(1, 100).forEach(i -> {

      User user = User.builder()
          .username("a" + i)
          .password(passwordEncoder.encode("1"))
          .email(i + "@" + i + "." + i)
          .nickname(i + "a")
          .role(UserRole.USER)
          .isDeleted(false)
          .status(UserStatus.ACTIVE)

          .build();


      userRepository.save(user);
    });
  }

}