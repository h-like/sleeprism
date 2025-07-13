import '../../../public/css/MyPage.css'

const PlaceholderPage = ({ title }: { title: string }) => (
    <div className="page-content">
        <header className="page-header"><h1>{title}</h1></header>
        <p>이 페이지는 준비 중입니다.</p>
    </div>
);
export default PlaceholderPage;