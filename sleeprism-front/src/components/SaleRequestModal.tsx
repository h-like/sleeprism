// src/components/SaleRequestModal.tsx
import React, { useState } from 'react';
import type { SaleRequestCreateRequestDTO } from '../type/Sale';
// import { SaleRequestCreateRequestDTO } from '../types/sale';

interface SaleRequestModalProps {
  postId: number;
  postTitle: string;
  onClose: () => void; // 모달 닫기 콜백
  onSuccess: () => void; // 판매 요청 성공 시 콜백 (댓글 새로고침 등)
}

function SaleRequestModal({ postId, postTitle, onClose, onSuccess }: SaleRequestModalProps) {
  const [proposedPrice, setProposedPrice] = useState<number | ''>(''); // 가격 입력 상태
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    if (proposedPrice === '' || proposedPrice < 100) { // 최소 가격 100원 (백엔드 DTO와 일치)
      setError('유효한 가격을 입력해주세요 (최소 100원).');
      setLoading(false);
      return;
    }

    const token = localStorage.getItem('jwtToken');
    if (!token) {
      alert('로그인이 필요합니다.');
      onClose(); // 모달 닫기
      // navigate('/login'); // 로그인 페이지로 리다이렉트 (필요시)
      return;
    }

    const requestBody: SaleRequestCreateRequestDTO = {
      postId: postId,
      proposedPrice: proposedPrice as number, // 타입 단언
    };

    try {
      const response = await fetch('http://localhost:8080/sleeprism/api/sale-requests', {
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
      onSuccess(); // 성공 콜백 호출
      onClose(); // 모달 닫기

    } catch (e: any) {
      console.error('판매 요청 중 오류 발생:', e);
      setError(e.message || '판매 요청 중 알 수 없는 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-gray-600 bg-opacity-75 flex items-center justify-center z-50 font-inter">
      <div className="bg-white p-8 rounded-lg shadow-xl w-full max-w-md border border-gray-300">
        <h2 className="text-2xl font-bold text-gray-800 mb-6 text-center">
          "{postTitle}" 게시글 구매 제안
        </h2>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="proposedPrice" className="block text-sm font-medium text-gray-700 mb-1">
              제안 가격 (원)
            </label>
            <input
              type="number"
              id="proposedPrice"
              name="proposedPrice"
              required
              min="100" // HTML5 유효성 검사
              className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
              placeholder="제시할 가격을 입력하세요 (최소 100원)"
              value={proposedPrice}
              onChange={(e) => setProposedPrice(parseInt(e.target.value) || '')}
            />
          </div>

          {error && <p className="text-red-500 text-sm mt-2 text-center">{error}</p>}

          <div className="flex justify-end space-x-4 mt-6">
            <button
              type="button"
              onClick={onClose}
              className="px-6 py-2 border border-gray-300 rounded-md text-gray-700 font-semibold hover:bg-gray-100 transition duration-200"
            >
              취소
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-6 py-2 bg-gradient-to-r from-green-500 to-teal-600 text-white font-semibold rounded-md shadow-md hover:from-green-600 hover:to-teal-700 transition duration-300 transform hover:scale-105 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? '제안 중...' : '제안하기'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default SaleRequestModal;
