// src/pages/RegisterPage.tsx
import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import '../../public/css/LoginPage.css';
import GoogleLogo from "../../public/assets/social-logo/google-logo.png";
import NaverLogo from "../../public/assets/social-logo/naver-logo.png";
import KakaoLogo from "../../public/assets/social-logo/kakao-logo.png";

// AuthResponseDTO 타입 정의 (LoginPage.tsx와 동일하게)
interface AuthResponseDTO {
  token?: string; // 백엔드 AuthResponseDTO의 필드명에 따라 변경 가능
  accessToken?: string; // 일반적인 JWT 필드명
  jwtToken?: string; // 또 다른 일반적인 JWT 필드명
  message?: string; // 에러 메시지 등을 받을 경우
}

function RegisterPage() {
  const [username, setUsername] = useState<string>('');
  const [email, setEmail] = useState<string>('');
  const [password, setPassword] = useState<string>('');
  const [confirmPassword, setConfirmPassword] = useState<string>('');
  const [nickname, setNickname] = useState<string>('');
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(false);

  const navigate = useNavigate();

  // 회원가입 성공 후 자동 로그인 처리 함수
  const autoSignIn = async (userEmail: string, userPassword: string) => {
    try {
      const response = await fetch('http://localhost:8080/api/users/signin', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email: userEmail, password: userPassword }),
      });

      const data: AuthResponseDTO = await response.json();

      if (!response.ok) {
        // 자동 로그인 실패 시 회원가입은 성공했으니 로그인 페이지로 리다이렉트
        throw new Error(data.message || '자동 로그인에 실패했습니다. 직접 로그인해주세요.');
      }

      // --- 토큰 필드명 확인 로직 (LoginPage와 동일하게) ---
      const jwtToken = data.accessToken || data.token || data.jwtToken;
      // --------------------------------------------------

      if (jwtToken) {
        localStorage.setItem('jwtToken', jwtToken);
        console.log('Auto-signin successful. JWT Token stored.');
        alert('회원가입 및 로그인 완료!'); // 회원가입+로그인 성공 알림
        navigate('/posts'); // 게시글 목록 페이지로 이동
      } else {
        console.warn('Auto-signin successful, but no JWT token received.');
        setError('회원가입 성공. 하지만 자동 로그인에 실패했습니다. 직접 로그인해주세요.');
        navigate('/login');
      }

    } catch (e: any) {
      console.error('자동 로그인 중 오류 발생:', e);
      setError(e.message || '회원가입은 성공했으나, 자동 로그인 중 알 수 없는 오류가 발생했습니다. 직접 로그인해주세요.');
      navigate('/login'); // 자동 로그인 실패 시 로그인 페이지로
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccessMessage(null);
    setLoading(true);

    if (password !== confirmPassword) {
      setError('비밀번호와 비밀번호 확인이 일치하지 않습니다.');
      setLoading(false);
      return;
    }

    try {
      const response = await fetch('http://localhost:8080/api/users/signup', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ username, email, password, nickname }),
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || '회원가입에 실패했습니다. 입력값을 확인해주세요.');
      }

      setSuccessMessage('회원가입이 성공적으로 완료되었습니다! 자동 로그인 중...');
      // 회원가입 성공 후 자동 로그인 시도
      await autoSignIn(email, password); // 회원가입 시 사용한 이메일과 비밀번호로 자동 로그인

    } catch (e: any) {
      console.error('회원가입 중 오류 발생:', e);
      setError(e.message || '알 수 없는 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
    } finally {
      setLoading(false);
    }
  };
  
  // 소셜 로그인 핸들러 (기능은 나중에 구현)
  const handleSocialLogin = (provider: 'google' | 'naver' | 'kakao') => {
    // TODO: 백엔드 소셜 로그인 URL로 리다이렉션하는 로직을 추가하세요.
    // 예: window.location.href = `http://localhost:8080/oauth2/authorization/${provider}`;
    alert(`${provider} 소셜 로그인 기능은 아직 구현 중입니다.`);
  };

  return (
    <div className="main-container"> {/* main-container -> login-container */}
      <div className="login-card">
        <h2 className="login-title">
          회원가입
        </h2>
        <form className="login-form" onSubmit={handleSubmit}>
          <div>
            <label htmlFor="username" className="sr-only">
              사용자 이름 (영문/숫자만, 1~50자)
            </label>
            <input
              id="username"
              name="username"
              type="text"
              required
              className="login-input" 
              placeholder="사용자 이름 (영문/숫자만)"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />
          </div>
          <div>
            <label htmlFor="nickname" className="sr-only">
              닉네임 (2~50자)
            </label>
            <input
              id="nickname"
              name="nickname"
              type="text"
              required
              className="login-input" 
              placeholder="닉네임"
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
            />
          </div>
          <div>
            <label htmlFor="email-address" className="sr-only">
              이메일 주소
            </label>
            <input
              id="email-address"
              name="email"
              type="email"
              autoComplete="email"
              required
              className="login-input"
              placeholder="이메일 주소"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>
          <div>
            <label htmlFor="password" className="sr-only">
              비밀번호 (8~20자)
            </label>
            <input
              id="password"
              name="password"
              type="password"
              autoComplete="new-password"
              required
              className="login-input" 
              placeholder="비밀번호 (8~20자)"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>
          <div>
            <label htmlFor="confirm-password" className="sr-only">
              비밀번호 확인
            </label>
            <input
              id="confirm-password"
              name="confirm-password"
              type="password"
              autoComplete="new-password"
              required
              className="login-input" 
              placeholder="비밀번호 확인"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
            />
          </div>

          {error && <p className="login-error-message">{error}</p>} {/* mt-2 text-center ... -> login-error-message */}
          {successMessage && <p className="login-success-message">{successMessage}</p>} {/* 새로운 success message 스타일 */}

          <div>
            <button
              type="submit"
              disabled={loading}
              className="login-button" 
            >
              {loading ? '가입 중...' : '회원가입'}
            </button>
          </div>
        </form>
        
        {/* 소셜 로그인 섹션 추가 */}
        <div className="social-login-divider">
          <span>또는</span>
        </div>
        <div className="social-login-buttons">
          <button onClick={() => handleSocialLogin('google')} className="social-button google">
            <img src={GoogleLogo} alt="Google" className="social-icon" />
            <span>Google로 회원가입</span>
          </button>
          <button onClick={() => handleSocialLogin('naver')} className="social-button naver">
            <img src={NaverLogo} alt="Naver" className="social-icon" />
            <span>네이버로 회원가입</span>
          </button>
          <button onClick={() => handleSocialLogin('kakao')} className="social-button kakao">
            <img src={KakaoLogo} alt="Kakao" className="social-icon" />
            <span>카카오로 회원가입</span>
          </button>
        </div>

        <div className="login-register-link"> {/* mt-6 text-center -> login-register-link */}
          <p className="text-1"> {/* text-sm text-gray-600 -> text-1 */}
            이미 계정이 있으신가요?{' '}
            <Link to="/login" className="register-link"> {/* font-medium ... -> register-link */}
              로그인
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}

export default RegisterPage;
