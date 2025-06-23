// src/pages/RegisterPage.tsx
import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';

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
      const response = await fetch('http://localhost:8080/sleeprism/api/users/signin', {
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
      const response = await fetch('http://localhost:8080/sleeprism/api/users/signup', {
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

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-100 to-purple-100 flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8 font-inter">
      <div className="max-w-md w-full bg-white p-8 rounded-xl shadow-2xl border border-gray-200">
        <h2 className="text-center text-4xl font-extrabold text-gray-900 mb-8">
          회원가입
        </h2>
        <form className="space-y-6" onSubmit={handleSubmit}>
          <div>
            <label htmlFor="username" className="sr-only">
              사용자 이름 (영문/숫자만, 1~50자)
            </label>
            <input
              id="username"
              name="username"
              type="text"
              required
              className="appearance-none rounded-md relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 focus:z-10 sm:text-sm"
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
              className="appearance-none rounded-md relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 focus:z-10 sm:text-sm"
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
              className="appearance-none rounded-md relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 focus:z-10 sm:text-sm"
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
              className="appearance-none rounded-md relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 focus:z-10 sm:text-sm"
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
              className="appearance-none rounded-md relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 focus:z-10 sm:text-sm"
              placeholder="비밀번호 확인"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
            />
          </div>

          {error && <p className="mt-2 text-center text-sm text-red-600">{error}</p>}
          {successMessage && <p className="mt-2 text-center text-sm text-green-600">{successMessage}</p>}

          <div>
            <button
              type="submit"
              disabled={loading}
              className="group relative w-full flex justify-center py-2 px-4 border border-transparent text-sm font-medium rounded-md text-white bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-700 hover:to-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 transition duration-300 transform hover:scale-105"
            >
              {loading ? '가입 중...' : '회원가입'}
            </button>
          </div>
        </form>
        <div className="mt-6 text-center">
          <p className="text-sm text-gray-600">
            이미 계정이 있으신가요?{' '}
            <Link to="/login" className="font-medium text-indigo-600 hover:text-indigo-500">
              로그인
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}

export default RegisterPage;
