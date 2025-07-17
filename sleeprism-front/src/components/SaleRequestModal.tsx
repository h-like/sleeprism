// src/components/SaleRequestModal.tsx
// import React, 'react';
import React from 'react';
import type { SaleRequestCreateRequestDTO } from '../type/Sale';
import '../../public/css/DreamInterpretationModal.css'

interface SaleRequestModalProps {
  postId: number;
  postTitle: string;
  onClose: () => void;
  onSuccess: () => void;
}

// SaleRequestCreateRequestDTO 타입이 실제 프로젝트에 정의되어 있다고 가정합니다.
// 예시를 위해 임시로 정의합니다.
// interface SaleRequestCreateRequestDTO {
//   postId: number;
//   proposedPrice: number;
// }

function SaleRequestModal({ postId, postTitle, onClose, onSuccess }: SaleRequestModalProps) {
  const [proposedPrice, setProposedPrice] = React.useState<number | ''>('');
  const [error, setError] = React.useState<string | null>(null);
  const [loading, setLoading] = React.useState<boolean>(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (proposedPrice === '' || proposedPrice < 100) {
      setError('유효한 가격을 입력해주세요 (최소 100원).');
      return;
    }

    setLoading(true);

    const token = localStorage.getItem('jwtToken');
    if (!token) {
      alert('로그인이 필요합니다.');
      setLoading(false);
      onClose();
      return;
    }

    const requestBody: SaleRequestCreateRequestDTO = {
      postId: postId,
      proposedPrice: proposedPrice as number,
    };

    try {
      const response = await fetch('http://localhost:8080/api/sale-requests', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify(requestBody),
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || `판매 요청 실패: ${response.status}`);
      }

      alert('판매 요청이 성공적으로 전송되었습니다!');
      onSuccess();
      onClose();

    } catch (e: any) {
      console.error('판매 요청 중 오류 발생:', e);
      setError(e.message || '판매 요청 중 알 수 없는 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    // 전체 모달 배경: 어둡고 블러 처리된 배경
    <div className="modal-backdrop">
      {/* 모달 컨테이너: 흰색 배경, 둥근 모서리, 그림자 효과 */}
      <div className="modal-container"
      style={{height: 320}}
      >
        
        
        {/* 닫기 버튼 */}
        <button
          onClick={onClose}
          className="modal-close-button"
          aria-label="Close modal"
        >
          <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>

        {/* 모달 제목 */}
        <h2 className="modal-title">
          구매 제안하기
        </h2>
        <p className="content-text">
          &quot;{postTitle}&quot;
        </p>
        
        <form onSubmit={handleSubmit} className="modal-content-view" >
          <div>
            <label htmlFor="proposedPrice" className="content-text">
              제안 가격 (원)
            </label>
            <input
              type="number"
              id="proposedPrice"
              name="proposedPrice"
              required
              min="100"
              className="price-input"
              style={{ width: '50%' }}
              placeholder="최소 100원 이상 입력"
              value={proposedPrice}
              onChange={(e) => setProposedPrice(e.target.value === '' ? '' : parseInt(e.target.value, 10))}
            />
          </div>

          {/* 에러 메시지 */}
          {error && <p className="text-red-500 text-sm text-center font-medium">{error}</p>}

          {/* 버튼 그룹 */}
          <div className="button-group">
            <button
              type="submit"
              disabled={loading}
              className="modal-button primary"
            >
              {loading ? '제안 중...' : '제안하기'}
            </button>
            <button
              type="button"
              onClick={onClose}
              className="modal-button secondary" style={{margin: "3"}}
            >
              취소
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default SaleRequestModal;
