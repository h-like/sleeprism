import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import '../../public/css/LoginPage.css'; // 새로 생성할 CSS 파일을 임포트합니다.
import GoogleLogo  from "../../public/assets/social-logo/google-logo.png";
import NaverLogo  from "../../public/assets/social-logo/naver-logo.png";
import KakaoLogo  from "../../public/assets/social-logo/kakao-logo.png";

function LoginPage() {
  const [email, setEmail] = useState<string>('');
  const [password, setPassword] = useState<string>('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(false);

  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      const response = await fetch('http://localhost:8080/api/users/signin', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email, password }),
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || '로그인에 실패했습니다. 이메일 또는 비밀번호를 확인해주세요.');
      }

      // --- 토큰 필드명 확인 로직 수정 ---
      // 백엔드 AuthResponseDTO의 실제 토큰 필드명을 확인하고 우선순위를 조정하세요.
      // 예를 들어, AuthResponseDTO에 accessToken 필드가 있다면 data.accessToken을 먼저 사용합니다.
      const jwtToken = data.accessToken || data.token || data.jwtToken; 
      // -------------------------------

      if (jwtToken) {
        localStorage.setItem('jwtToken', jwtToken); // 로컬 스토리지에 토큰 저장
        console.log('JWT Token stored:', jwtToken);
        alert('로그인 완료!'); // 사용자 알림
        navigate('/posts'); 
      } else {
        console.warn('Login successful, but no JWT token received in response:', data); // 응답 데이터 로깅
        setError('로그인 성공. 하지만 토큰을 받지 못했습니다. 관리자에게 문의하세요.');
      }
      
    } catch (e: any) {
      console.error('로그인 중 오류 발생:', e);
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
    <div className="main-container">
      <div className="login-card">
        <h2 className="login-title">
          로그인
        </h2>
        <form className="login-form" onSubmit={handleSubmit}>
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
              비밀번호
            </label>
            <input
              id="password"
              name="password"
              type="password"
              autoComplete="current-password"
              required
              className="login-input"
              placeholder="비밀번호"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>

          {error && <p className="login-error-message">{error}</p>}

          <div>
            <button
              type="submit"
              disabled={loading}
              className="login-button"
            >
              {loading ? '로그인 중...' : '로그인'}
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
            <span>Google로 로그인</span>
          </button>
          <button onClick={() => handleSocialLogin('naver')} className="social-button naver">
            <img src={NaverLogo} alt="Naver" className="social-icon" />
            <span>네이버로 로그인</span>
          </button>
          <button onClick={() => handleSocialLogin('kakao')} className="social-button kakao">
            <img src={KakaoLogo} alt="Kakao" className="social-icon" />
            <span>카카오로 로그인</span>
          </button>
        </div>

        <div className="login-register-link">
          <p className="text-1">
            계정이 없으신가요?{' '}
            <Link to="/register" className="register-link">
              회원가입
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}

export default LoginPage;