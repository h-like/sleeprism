package com.example.sleeprism.security;

import com.example.sleeprism.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
  private final UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    // 데이터베이스에서 사용자 이름(username), 즉 이메일로 사용자 정보를 조회하고
    // UserDetails를 구현한 User 엔티티를 바로 반환합니다.
    return userRepository.findByEmail(username) // findByEmail로 User 엔티티를 가져옴
        .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));
  }
}
