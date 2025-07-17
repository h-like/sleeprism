import React, { useState, useEffect } from 'react';
import '../../public/css/DreamInterpretationModal.css'; // 카드 뒤집기 애니메이션을 위해 그대로 사용

// 타입 정의
interface InterpretationOption {
  optionIndex: number;
  title: string;
  content: string;
  tarotCardId: number;
  tarotCardName: string;
  tarotCardImageUrl: string;
}

interface InterpretationResponse {
  id: number;
  interpretationOptions: InterpretationOption[];
  selectedOptionIndex: number | null;
}

interface DreamInterpretationModalProps {
  postId: number;
  onClose: () => void;
}

const CARD_BACK_IMAGE_URL = '/images/tarot_back.png';

const DreamInterpretationModal: React.FC<DreamInterpretationModalProps> = ({ postId, onClose }) => {
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [step, setStep] = useState<'loading' | 'selection' | 'result'>('loading');
  const [interpretationData, setInterpretationData] = useState<InterpretationResponse | null>(null);
  const [finalResult, setFinalResult] = useState<InterpretationOption | null>(null);
  const [flippedIndex, setFlippedIndex] = useState<number | null>(null);

  const BACKEND_BASE_URL = 'http://localhost:8080';

  useEffect(() => {
    const getInterpretation = async () => {
      setLoading(true);
      const token = localStorage.getItem('jwtToken');
      if (!token) {
        setError('해몽 기능을 사용하려면 로그인이 필요합니다.');
        setLoading(false);
        return;
      }
      try {
        const response = await fetch(`${BACKEND_BASE_URL}/api/dream-interpretations/interpret`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
          body: JSON.stringify({ postId }),
        });

        if (!response.ok) {
          const errorData = await response.json();
          throw new Error(errorData.errorMessage || '꿈 해몽 정보를 가져오는 데 실패했습니다.');
        }
        
        const data: InterpretationResponse = await response.json();
        setInterpretationData(data);

        if (data.selectedOptionIndex !== null) {
          const previouslySelected = data.interpretationOptions.find(
            opt => opt.optionIndex === data.selectedOptionIndex
          );
          if (previouslySelected) {
            setFinalResult(previouslySelected);
            setStep('result');
          } else {
            setStep('selection');
          }
        } else {
          setStep('selection');
        }

      } catch (e: any) {
        setError(e.message);
      } finally {
        setLoading(false);
      }
    };
    getInterpretation();
  }, [postId]);

  const handleSelectOption = async (option: InterpretationOption, index: number) => {
    if (flippedIndex !== null || !interpretationData) return;

    setFlippedIndex(index);
    setFinalResult(option);

    setTimeout(() => {
      setStep('result');
    }, 1000);

    const token = localStorage.getItem('jwtToken');
    if (token) {
      try {
        await fetch(`${BACKEND_BASE_URL}/api/dream-interpretations/${interpretationData.id}/select`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
          body: JSON.stringify({
            selectedOptionIndex: option.optionIndex,
            selectedTarotCardId: option.tarotCardId,
          }),
        });
      } catch (e) {
        console.error("Failed to save selection:", e);
      }
    }
  };

  const handleReset = () => {
    setStep('selection');
    setFinalResult(null);
    setFlippedIndex(null);
  };

  const renderContent = () => {
    if (loading) {
      return (
        <div className="modal-content-view">
          <p className="modal-title" style={{ animation: 'pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite' }}>
            AI가 당신의 꿈을 분석하고 있습니다...
          </p>
        </div>
      );
    }

    if (error) {
      return (
        <div className="modal-content-view">
          <h3 className="modal-title" style={{ color: '#dc2626' }}>오류 발생</h3>
          <p className="modal-subtitle">{error}</p>
          <div className="button-group">
            <button onClick={onClose} className="modal-button primary">닫기</button>
          </div>
        </div>
      );
    }

    if (step === 'selection' && interpretationData) {
      return (
        <div className="modal-content-view">
          <h3 className="modal-title">마음이 이끄는 카드를 선택하세요</h3>
          <p className="modal-subtitle">AI가 당신의 꿈을 분석하여 3개의 카드를 준비했습니다.</p>
          <div className="card-selection-area">
            {interpretationData.interpretationOptions.map((option, index) => (
              <div
                key={option.optionIndex}
                className="card-scene"
                style={{ transform: `rotate(${(index - 1) * 8}deg)` }}
                onClick={() => handleSelectOption(option, index)}
              >
                <div className={`card ${flippedIndex === index ? 'is-flipped' : ''}`}>
                  <div className="card-face card-face-back"><img src={CARD_BACK_IMAGE_URL} alt="타로카드 뒷면" /></div>
                  <div className="card-face card-face-front"><img src={option.tarotCardImageUrl ? `${BACKEND_BASE_URL}${option.tarotCardImageUrl}` : ''} alt={option.tarotCardName} /></div>
                </div>
              </div>
            ))}
          </div>
        </div>
      );
    }

    if (step === 'result' && finalResult) {
      return (
        <div className="modal-content-view result-view">
          <img src={finalResult.tarotCardImageUrl ? `${BACKEND_BASE_URL}${finalResult.tarotCardImageUrl}` : ''} alt={finalResult.tarotCardName} />
          <div className="scroll-wrapper">
            <h3 className="modal-title">{finalResult.title}</h3>
            <div className="content-text">
              <p>{finalResult.content}</p>
            </div>
          </div>
          <div className="button-group">
            <button onClick={handleReset} className="modal-button secondary">다시 선택</button>
            <button onClick={onClose} className="modal-button primary">확인</button>
          </div>
        </div>
      );
    }

    return null;
  };

  return (
    <div className="modal-backdrop">
      <div className="modal-container">
        {renderContent()}
        <button onClick={onClose} className="modal-close-button" aria-label="닫기">
          <svg viewBox="0 0 24 24"><path stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path></svg>
        </button>
      </div>
    </div>
  );
};

export default DreamInterpretationModal;