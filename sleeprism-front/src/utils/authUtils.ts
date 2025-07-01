// JWT 토큰에서 사용자 ID를 디코딩하는 헬퍼 함수
interface DecodedToken {
  userId?: number;
  id?: number;
  sub?: string; // subject (주로 사용자 ID)
  // 다른 JWT 페이로드 필드들
}

export const getUserIdFromToken = (): number | null => {
  const token = localStorage.getItem('jwtToken');
  if (!token) {
    return null;
  }
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
      return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
    }).join(''));

    const decodedToken: DecodedToken = JSON.parse(jsonPayload);
    // 백엔드 JWT 구현에 따라 'userId', 'id', 'sub' 중 하나를 사용자 ID로 사용할 수 있습니다.
    const userIdRaw = decodedToken.userId || decodedToken.id || decodedToken.sub;
    const userId = typeof userIdRaw === 'number' ? userIdRaw : parseInt(userIdRaw as string, 10);
    return isNaN(userId) ? null : userId;
  } catch (e) {
    console.error("JWT 토큰 디코딩 중 오류 발생:", e);
    return null;
  }
};
