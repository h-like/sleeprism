import { NavLink } from 'react-router-dom';
import { ChevronRight } from 'lucide-react';

// Spinner, ErrorDisplay, InfoCard를 이 파일로 옮기고 export 합니다.
export const Spinner = () => (
    <div className="spinner-container"><div className="spinner"></div></div>
);

export const ErrorDisplay = ({ message }: { message: string }) => (
    <div className="error-display">
        <p className="font-bold">오류</p>
        <p>{message}</p>
    </div>
);

export const InfoCard = ({ icon, title, value, linkTo, colorClass }: { icon: React.ReactNode, title: string, value: string | number, linkTo: string, colorClass: string }) => (
  <NavLink to={linkTo} className="info-card">
    <div>
      <div className="info-card-header">
        <div className={`info-card-icon ${colorClass}`}>{icon}</div>
        <ChevronRight className="icon-muted" />
      </div>
      <p className="info-card-title">{title}</p>
      <p className="info-card-value">{value}</p>
    </div>
  </NavLink>
);