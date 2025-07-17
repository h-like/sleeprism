package com.example.sleeprism.repository;

import com.example.sleeprism.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PostRepositoryTests {

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PostRepository postRepository;

  @Test
  public void createPost(){
    Random random = new Random();

    for (int i = 1; i<=100; i++){

      User owner = User.builder()
          .username("c" + i)
          .nickname(i + "c")
          .password(passwordEncoder.encode("1"))
          .email(i + "c@" + i + "." + i)
          .role(UserRole.USER)
          .isDeleted(false)
          .status(UserStatus.ACTIVE)
          .build();

      userRepository.save(owner);

      Post post = Post.builder()
          .isDeleted(false)
          .isSellable(true)
          .isSold(false)
          .likeCount(0)
          .bookmarkCount(0)
          .currentOwner(owner)
          .originalAuthor(owner)
          .version(Long.valueOf(0))
          .viewCount(0L)
          .category(PostCategory.FREE_TALK)
          .title(i+"title")
          .content(i+"this is good content")
          .build();

      postRepository.save(post);
    }
  }
}