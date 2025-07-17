// MyPageDashboard.tsx
import { Bookmark, Heart, PenSquare, SplinePointer } from "lucide-react";
import { useUser } from "../../contexts/UserContext";
import { ErrorDisplay, InfoCard } from "./MyPageUI";

// --- 페이지 컴포넌트 ---
const MyPageDashboard = () => {
  const { user, isLoading, error } = useUser();

  if (isLoading) return <SplinePointer />;
  if (error || !user) return <ErrorDisplay message={error || "사용자 정보를 찾을 수 없습니다."} />;

  return (
    <div className="page-content">
        <header className="page-header">
            <h1>대시보드</h1>
        </header>
        <div className="dashboard-grid">
            <InfoCard icon={<PenSquare size={20} color="white" />} title="내가 쓴 글" value={user.postCount} linkTo="my-posts" colorClass="icon-bg-purple" />
            <InfoCard icon={<Heart size={20} color="white" />} title="좋아요한 글" value={user.likedPostCount} linkTo="liked-posts" colorClass="icon-bg-pink" />
            <InfoCard icon={<Bookmark size={20} color="white" />} title="북마크" value={user.bookmarkedPostCount} linkTo="bookmarked-posts" colorClass="icon-bg-blue" />
        </div>
    </div>
  );
};

export default MyPageDashboard;