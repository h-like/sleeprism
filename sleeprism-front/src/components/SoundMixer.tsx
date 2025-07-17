import React, { useState, useEffect, useRef } from 'react';
import { Volume2, VolumeX, Shuffle, Clock, Search, Plus, XCircle, Loader2 } from 'lucide-react';
import '../../public/css/StarBackground.css'; // SoundMixer.css 파일 임포트
import '../../public/css/SoundMixer.css'
import StarBackground from './StarBackground'; // StarBackground 컴포넌트 임포트 (StarBackground.tsx 파일)

// SoundInfo 인터페이스 정의
interface SoundInfo {
    id: string;
    name: string;
    url: string;
    relativeVolume: number;
}

// Freesound API 응답에서 사운드 객체의 타입 정의
interface FreesoundSound {
    id: number;
    name: string;
    previews: {
        'preview-hq-mp3': string;
        'preview-lq-mp3': string;
    };
}

// 간단한 알림 모달 컴포넌트 (CSS 클래스 사용)
const AlertModal: React.FC<{ message: string; onClose: () => void }> = ({ message, onClose }) => {
    return (
        <div className="alert-modal-overlay">
            <div className="alert-modal-content">
                <p>{message}</p>
                <button onClick={onClose}>
                    확인
                </button>
            </div>
        </div>
    );
};

// 타이머 모달 컴포넌트
interface TimerModalProps {
    show: boolean;
    onClose: () => void;
    timerMinutes: number;
    setTimerMinutes: (minutes: number) => void;
    remainingTime: number;
    startTimer: () => void;
    stopTimer: () => void;
    formatTime: (seconds: number) => string;
}

const TimerModal: React.FC<TimerModalProps> = ({
    show,
    onClose,
    timerMinutes,
    setTimerMinutes,
    remainingTime,
    startTimer,
    stopTimer,
    formatTime
}) => {
    if (!show) return null;

    return (
        <div className="timer-modal-overlay">
            <div className="timer-modal-content">
                <button className="timer-modal-close-button" onClick={onClose}>
                    <XCircle size={24} />
                </button>
                <h2 className="section-header">타이머 설정</h2>
                <div className="timer-input-group">
                    <input
                        type="number"
                        min="1"
                        value={timerMinutes > 0 ? timerMinutes : ''}
                        onChange={(e) => setTimerMinutes(parseInt(e.target.value) || 0)}
                        placeholder="분"
                        className="timer-input"
                    />
                    <button
                        onClick={startTimer}
                        disabled={remainingTime > 0 || timerMinutes <= 0}
                        className="timer-start-button"
                    >
                        <Clock size={20} /> <span>타이머 시작</span>
                    </button>
                    <button
                        onClick={stopTimer}
                        disabled={remainingTime === 0}
                        className="timer-cancel-button"
                    >
                        <XCircle size={20} /> <span>타이머 취소</span>
                    </button>
                </div>
                {remainingTime > 0 && (
                    <p className="remaining-time-display">
                        남은 시간: <span className="time-value">{formatTime(remainingTime)}</span>
                    </p>
                )}
            </div>
        </div>
    );
};


function SoundMixer() {
    const [availableSounds, setAvailableSounds] = useState<SoundInfo[]>([]);
    const [activeSounds, setActiveSounds] = useState<SoundInfo[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const audioRefs = useRef<{ [key: string]: HTMLAudioElement }>({});

    const [masterVolume, setMasterVolume] = useState(0.5);
    const [isMuted, setIsMuted] = useState(false);
    const prevMasterVolumeRef = useRef(0.5);

    const [timerMinutes, setTimerMinutes] = useState(0);
    const [remainingTime, setRemainingTime] = useState(0);
    const timerIntervalRef = useRef<NodeJS.Timeout | null>(null);
    const [showTimerModal, setShowTimerModal] = useState(false); // 타이머 모달 상태

    const [freesoundSearchQuery, setFreesoundSearchQuery] = useState('');
    const [freesoundSearchResults, setFreesoundSearchResults] = useState<SoundInfo[]>([]);
    const [isFreesoundSearching, setIsFreesoundSearching] = useState(false);

    const [alertMessage, setAlertMessage] = useState<string | null>(null);
    const showAlert = (message: string) => setAlertMessage(message);
    const closeAlert = () => setAlertMessage(null);

    const [activeTab, setActiveTab] = useState<'default' | 'freesound'>('default'); // 사운드 선택 탭 상태

    // 컴포넌트 마운트 시 초기 사운드 목록 로드
    useEffect(() => {
        setIsLoading(true);
        const BACKEND_INTERNAL_SOUNDS_API_URL = 'http://localhost:8080/api/sounds/internal';

        fetch(BACKEND_INTERNAL_SOUNDS_API_URL)
            .then(response => {
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                return response.json();
            })
            .then((data: SoundInfo[]) => {
                const predefinedSounds = [
                    { id: "preset-bar", name: "카페 소리", url: "/api/sounds/bar.mp3", relativeVolume: 0.5 },
                    { id: "preset-underwater", name: "물속의 소리", url: "/api/sounds/underwater.mp3", relativeVolume: 0.5 },
                    { id: "preset-nature", name: "여름 밤의 소리", url: "/api/sounds/nature.mp3", relativeVolume: 0.5 }
                ];

                // 백엔드 데이터와 프리셋 사운드를 합치면서 중복 ID 제거
                const combinedSoundsMap = new Map<string, SoundInfo>();
                data.forEach(sound => combinedSoundsMap.set(sound.id, sound));
                predefinedSounds.forEach(sound => {
                    if (!combinedSoundsMap.has(sound.id)) { // 백엔드 데이터에 없는 경우만 추가
                        combinedSoundsMap.set(sound.id, sound);
                    }
                });

                const soundsWithVolume = Array.from(combinedSoundsMap.values()).map(sound => ({
                    ...sound,
                    relativeVolume: sound.relativeVolume ?? 0.5
                }));
                setAvailableSounds(soundsWithVolume);
                setIsLoading(false);
                console.log("백엔드 API에서 가져온 기본 사운드 목록:", soundsWithVolume);
            })
            .catch(error => {
                console.error("기본 사운드 목록을 가져오는 데 실패했습니다:", error);
                showAlert("기본 사운드 목록을 불러오는 데 실패했습니다. 백엔드 서버를 확인해주세요.");
                setIsLoading(false);
            });

        return () => {
            stopAllSounds();
            if (timerIntervalRef.current) {
                clearInterval(timerIntervalRef.current);
            }
        };
    }, []);

    useEffect(() => {
        activeSounds.forEach(sound => {
            const audio = audioRefs.current[sound.id];
            if (audio) {
                audio.volume = sound.relativeVolume * masterVolume;
            }
        });
    }, [masterVolume, activeSounds]);

    const stopAllSounds = () => {
        Object.values(audioRefs.current).forEach(audio => {
            if (audio) {
                audio.pause();
                audio.currentTime = 0;
            }
        });
        audioRefs.current = {};
        setActiveSounds([]);
    };

    const addSound = (soundInfo: { id: string; name: string; url: string }) => {
        if (activeSounds.some(s => s.id === soundInfo.id)) {
            showAlert(`${soundInfo.name}은(는) 이미 추가되었습니다.`);
            return;
        }

        const initialRelativeVolume = 0.5;
        const audio = new Audio(soundInfo.url);
        audio.loop = true;
        audio.volume = initialRelativeVolume * masterVolume;

        audio.play().catch(e => {
            console.error(`오디오 재생 실패 (${soundInfo.name}, URL: ${soundInfo.url}):`, e);
            showAlert(`'${soundInfo.name}' 사운드 재생에 실패했습니다. (오디오 파일 문제 또는 브라우저 정책으로 인한 자동 재생 차단)`);
        });

        audioRefs.current[soundInfo.id] = audio;

        setActiveSounds(prevSounds => [
            ...prevSounds,
            { ...soundInfo, relativeVolume: initialRelativeVolume }
        ]);
    };

    const handleIndividualVolumeChange = (id: string, newRelativeVolume: string) => {
        const parsedRelativeVolume = parseFloat(newRelativeVolume);
        setActiveSounds(prevSounds =>
            prevSounds.map(s =>
                s.id === id ? { ...s, relativeVolume: parsedRelativeVolume } : s
            )
        );
    };

    const removeSound = (id: string) => {
        if (audioRefs.current[id]) {
            audioRefs.current[id].pause();
            audioRefs.current[id].currentTime = 0;
            delete audioRefs.current[id];
        }
        setActiveSounds(prevSounds => prevSounds.filter(s => s.id !== id));
    };

    const handleMasterVolumeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const newVolume = parseFloat(e.target.value);
        setMasterVolume(newVolume);
        setIsMuted(newVolume === 0);
    };

    const toggleMute = () => {
        if (isMuted) {
            setMasterVolume(prevMasterVolumeRef.current > 0 ? prevMasterVolumeRef.current : 0.5);
            setIsMuted(false);
        } else {
            prevMasterVolumeRef.current = masterVolume;
            setMasterVolume(0);
            setIsMuted(true);
        }
    };

    const generateRandomMix = () => {
        stopAllSounds();

        const numSoundsToMix = Math.floor(Math.random() * (Math.min(availableSounds.length, 6) - 1)) + 2;
        
        const selectedSoundIds = new Set<string>();
        const selectedSounds: SoundInfo[] = [];

        while (selectedSoundIds.size < numSoundsToMix && availableSounds.length > 0) {
            const randomIndex = Math.floor(Math.random() * availableSounds.length);
            const sound = availableSounds[randomIndex];
            if (!selectedSoundIds.has(sound.id)) {
                selectedSoundIds.add(sound.id);
                selectedSounds.push(sound);
            }
            if (selectedSoundIds.size === availableSounds.length) break;
        }

        if (selectedSounds.length === 0) {
            showAlert("랜덤 믹스를 생성할 사운드가 없습니다.");
            return;
        }

        selectedSounds.forEach(soundInfo => {
            const randomRelativeVolume = parseFloat((Math.random()).toFixed(2));
            const audio = new Audio(soundInfo.url);
            audio.loop = true;
            audio.volume = randomRelativeVolume * masterVolume;
            audio.play().catch(e => console.error(`오디오 재생 실패 (${soundInfo.name}):`, e));

            audioRefs.current[soundInfo.id] = audio;

            setActiveSounds(prevSounds => [
                ...prevSounds,
                { ...soundInfo, relativeVolume: randomRelativeVolume }
            ]);
        });
    };

    const startTimer = () => {
        if (timerMinutes <= 0) {
            showAlert('타이머 시간을 1분 이상으로 설정해주세요.');
            return;
        }

        stopTimer();

        const totalSeconds = timerMinutes * 60;
        setRemainingTime(totalSeconds);

        timerIntervalRef.current = setInterval(() => {
            setRemainingTime(prevTime => {
                if (prevTime <= 1) {
                    clearInterval(timerIntervalRef.current!);
                    timerIntervalRef.current = null;
                    stopAllSounds();
                    showAlert('타이머 시간이 종료되었습니다.');
                    setShowTimerModal(false); // 타이머 종료 시 모달 닫기
                    return 0;
                }
                return prevTime - 1;
            });
        }, 1000);
    };

    const stopTimer = () => {
        if (timerIntervalRef.current) {
            clearInterval(timerIntervalRef.current);
            timerIntervalRef.current = null;
            setRemainingTime(0);
        }
    };

    const formatTime = (seconds: number) => {
        const minutes = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return `${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    };

    const handleFreesoundSearch = async () => {
        if (!freesoundSearchQuery.trim()) {
            showAlert('검색어를 입력해주세요.');
            return;
        }

        setIsFreesoundSearching(true);
        setFreesoundSearchResults([]);

        const proxyUrl = `http://localhost:8080/api/freesound-search?query=${encodeURIComponent(freesoundSearchQuery)}`;

        try {
            const response = await fetch(proxyUrl);
            if (!response.ok) {
                const errorData = await response.json().catch(() => ({ error: '알 수 없는 백엔드 오류' }));
                throw new Error(`백엔드 프록시 오류! 상태: ${response.status}, 메시지: ${errorData.error || '알 수 없는 오류'}`);
            }
            const data = await response.json();
            console.log("백엔드 프록시를 통한 Freesound API 검색 결과:", data);

            const mappedResults: SoundInfo[] = data.results.map((sound: FreesoundSound) => ({
                id: `fs-${sound.id}`,
                name: sound.name,
                url: sound.previews['preview-hq-mp3'] || sound.previews['preview-lq-mp3'],
                relativeVolume: 0.5
            }));
            setFreesoundSearchResults(mappedResults);

            if (mappedResults.length === 0) {
                showAlert('Freesound에서 검색 결과가 없습니다. 다른 검색어를 시도해보세요.');
            }

        } catch (error: any) {
            console.error("Freesound 검색 실패 (백엔드 프록시 경유):", error);
            showAlert(`사운드 검색 중 오류가 발생했습니다: ${error.message || '알 수 없는 오류'}`);
        } finally {
            setIsFreesoundSearching(false);
        }
    };

    return (
        <div className="sound-mixer-container">
            {/* StarBackground 컴포넌트를 배경으로 추가 */}
            <StarBackground />
            
            <div className="mixer-card">
                <div className="content-wrapper">
                    <h1 className="title">
                        나만의 몽환 사운드 믹서
                    </h1>

                    {/* 마스터 볼륨 및 뮤트 */}
                    <div className="master-volume-control">
                        <div className="volume-slider-group">
                            <span>전체 볼륨</span>
                            <input
                                type="range"
                                min="0"
                                max="1"
                                step="0.01"
                                value={masterVolume}
                                onChange={handleMasterVolumeChange}
                            />
                        </div>
                        <button
                            onClick={toggleMute}
                            className="mute-button"
                        >
                            {isMuted ? <VolumeX size={20} /> : <Volume2 size={20} />}
                            {isMuted ? '음소거 해제' : '음소거'}
                        </button>
                    </div>

                    <div className="main-content-layout">
                        {/* 사운드 선택 섹션 (탭 포함) */}
                        <div className="sound-selection-section">
                            <h2 className="section-header">사운드 선택</h2>
                            <div className="tab-buttons">
                                <button
                                    className={`tab-button ${activeTab === 'default' ? 'active' : ''}`}
                                    onClick={() => setActiveTab('default')}
                                >
                                    기본 사운드
                                </button>
                                <button
                                    className={`tab-button ${activeTab === 'freesound' ? 'active' : ''}`}
                                    onClick={() => setActiveTab('freesound')}
                                >
                                    Freesound 검색
                                </button>
                            </div>

                            <div className="tab-content">
                                {activeTab === 'default' && (
                                    <div className="default-sounds-panel">
                                        {isLoading ? (
                                            <p className="loading-message">
                                                <Loader2 size={20} className="icon-spin" /> 사운드 목록을 불러오는 중...
                                            </p>
                                        ) : (
                                            <div className="default-sounds-list">
                                                {availableSounds.map(sound => (
                                                    <button
                                                        key={sound.id}
                                                        onClick={() => addSound(sound)}
                                                        className="sound-button"
                                                        disabled={activeSounds.some(s => s.id === sound.id)}
                                                    >
                                                        <Plus size={16} />
                                                        <span>{sound.name}</span>
                                                    </button>
                                                ))}
                                            </div>
                                        )}
                                    </div>
                                )}

                                {activeTab === 'freesound' && (
                                    <div className="freesound-search-panel">
                                        <div className="freesound-search-input-group">
                                            <input
                                                type="text"
                                                value={freesoundSearchQuery}
                                                onChange={(e) => setFreesoundSearchQuery(e.target.value)}
                                                onKeyPress={(e) => { if (e.key === 'Enter') handleFreesoundSearch(); }}
                                                placeholder="검색할 사운드 이름 입력 (예: forest, rain)"
                                                className="search-input"
                                            />
                                            <button
                                                onClick={handleFreesoundSearch}
                                                disabled={isFreesoundSearching || !freesoundSearchQuery.trim()}
                                                className="search-button"
                                            >
                                                {isFreesoundSearching ? <Loader2 size={20} className="icon-spin" /> : <Search size={20} />}
                                                <span>{isFreesoundSearching ? '검색 중...' : '검색'}</span>
                                            </button>
                                        </div>
                                        <div className="search-results">
                                            {isFreesoundSearching ? (
                                                <p className="loading-message">
                                                    <Loader2 size={20} className="icon-spin" /> Freesound에서 사운드를 검색 중...
                                                </p>
                                            ) : freesoundSearchResults.length > 0 ? (
                                                <div className="search-results-grid">
                                                    {freesoundSearchResults.map(sound => (
                                                        <div key={sound.id} className="search-result-item">
                                                            <span className="sound-name">{sound.name}</span>
                                                            <button
                                                                onClick={() => addSound(sound)}
                                                                className="add-sound-button"
                                                                disabled={activeSounds.some(s => s.id === sound.id)}
                                                            >
                                                                <Plus size={14} /> <span>추가</span>
                                                            </button>
                                                        </div>
                                                    ))}
                                                </div>
                                            ) : (
                                                <p className="no-results-message">검색 결과가 없습니다. 검색어를 입력하고 검색 버튼을 눌러보세요.</p>
                                            )}
                                        </div>
                                    </div>
                                )}
                            </div>
                        </div>

                        {/* 현재 활성화된 사운드 목록 (믹서 트랙) */}
                        <div id="active-sounds" className="active-sounds-section">
                            <h2 className="section-header">현재 믹스</h2>
                            {activeSounds.length === 0 ? (
                                <p className="no-active-sounds-message">재생 중인 사운드가 없습니다. 위에서 사운드를 추가하거나 랜덤 믹스를 추천받아 보세요.</p>
                            ) : (
                                <div className="active-sounds-list">
                                    {activeSounds.map(sound => (
                                        <div key={sound.id} className="active-sound-item">
                                            <span className="sound-name">{sound.name}</span>
                                            <input
                                                type="range"
                                                min="0"
                                                max="1"
                                                step="0.01"
                                                value={sound.relativeVolume}
                                                onChange={(e) => handleIndividualVolumeChange(sound.id, e.target.value)}
                                                disabled={isMuted}
                                                className="individual-volume-slider"
                                            />
                                            <button
                                                onClick={() => removeSound(sound.id)}
                                                className="remove-sound-button"
                                            >
                                                <XCircle size={18} /> <span>삭제</span>
                                            </button>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    </div>

                    {/* 하단 컨트롤 (랜덤 믹스, 타이머 버튼) */}
                    <div className="bottom-controls">
                        <div className="random-mix-section">
                            <button
                                onClick={generateRandomMix}
                                className="random-mix-button"
                            >
                                <Shuffle size={24} /> <span>랜덤 믹스 추천</span>
                            </button>
                        </div>
                        <div className="timer-button-section">
                            <button
                                onClick={() => setShowTimerModal(true)}
                                className="open-timer-modal-button"
                            >
                                <Clock size={24} /> <span>타이머 설정</span>
                            </button>
                        </div>
                    </div>

                    {/* Alert Modal 렌더링 */}
                    {alertMessage && <AlertModal message={alertMessage} onClose={closeAlert} />}

                    {/* Timer Modal 렌더링 */}
                    <TimerModal
                        show={showTimerModal}
                        onClose={() => setShowTimerModal(false)}
                        timerMinutes={timerMinutes}
                        setTimerMinutes={setTimerMinutes}
                        remainingTime={remainingTime}
                        startTimer={startTimer}
                        stopTimer={stopTimer}
                        formatTime={formatTime}
                    />
                </div>
            </div>
        </div>
    );
}

export default SoundMixer;
