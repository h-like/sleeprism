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
    // 주의: username 매개변수가 실제로는 사용자 ID(Long 타입)라고 가정합니다.
    // JWT 토큰의 subject에 사용자 ID가 들어있는 경우 이 방식을 사용합니다.
    try {
      Long userId = Long.parseLong(username); // String으로 넘어온 username을 Long으로 변환
      return userRepository.findById(userId) // 사용자 ID로 User 엔티티 조회
          .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));
    } catch (NumberFormatException e) {
      // username이 숫자로 변환되지 않으면, 여전히 이메일로 시도하거나 예외 발생
      return userRepository.findByEmail(username)
          .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));
    }
  }
}
