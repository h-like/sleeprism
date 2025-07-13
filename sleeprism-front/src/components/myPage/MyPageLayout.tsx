// MyPageLayout.tsx
import { Bookmark, Heart, Home, PenSquare, User } from "lucide-react";
import { NavLink, Outlet, useLocation } from "react-router-dom";
import { useUser } from "../../contexts/UserContext";

// --- 레이아웃 컴포넌트 ---
const MyPageLayout = () => {
  const { user, isLoading, error } = useUser();
  const location = useLocation();

  return (
    <div className="mypage-container">
      <aside className="mypage-sidebar">
        {isLoading ? (
          <div className="profile-header-skeleton">
            <div className="avatar-skeleton"></div>
            <div className="text-skeleton"></div>
            <div className="text-skeleton short"></div>
          </div>
        ) : error || !user ? (
          <div className="profile-header-error">정보 로딩 실패</div>
        ) : (
          <div className="profile-header">
            <img src={user.profileImageUrl} alt="프로필" className="profile-avatar" />
            <h2 className="profile-name">{user.nickname}</h2>
            <p className="profile-email">{user.email}</p>
          </div>
        )}
        <nav>
          {/* 경로를 상대 경로로 수정하고, 대시보드는 end prop을 추가합니다. */}
          <NavLink to="" end><Home size={18} /> 대시보드</NavLink>
          <NavLink to="edit-profile"><User size={18} /> 개인 정보 수정</NavLink>
          <NavLink to="my-posts"><PenSquare size={18} /> 내가 쓴 글</NavLink>
          <NavLink to="liked-posts"><Heart size={18} /> 좋아요한 글</NavLink>
          <NavLink to="bookmarked-posts"><Bookmark size={18} /> 북마크</NavLink>
        </nav>
      </aside>
      <main className="mypage-main-content">
        <Outlet />
      </main>
    </div>
  );
};
