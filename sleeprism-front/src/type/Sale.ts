// src/types/sale.ts

export interface SaleRequestCreateRequestDTO {
  postId: number;
  proposedPrice: number;
}

export interface SaleRequestResponseDTO {
  id: number;
  postId: number;
  postTitle: string;
  buyerId: number;
  buyerNickname: string;
  sellerId: number;
  sellerNickname: string;
  proposedPrice: number;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'CANCELED'; // 백엔드 SaleRequestStatus와 일치
  requestedAt: string; // ISO 8601 string
  respondedAt: string | null; // ISO 8601 string 또는 null
}
