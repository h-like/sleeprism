// EditProfilePage.tsx
import { Camera, SplinePointer, X } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { updateUserProfile, useUser } from "../../contexts/UserContext";
import '../../../public/css/MyPage.css'

const EditProfilePage = () => {
    const { user, updateUser } = useUser();
    const [nickname, setNickname] = useState(user?.nickname || '');
    const [email, setEmail] = useState(user?.email || '');
    const [profileImageFile, setProfileImageFile] = useState<File | null>(null);
    const [preview, setPreview] = useState<string | null>(user?.profileImageUrl || null);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [message, setMessage] = useState<{type: 'success' | 'error', text: string} | null>(null);
    const fileInputRef = useRef<HTMLInputElement>(null);

    useEffect(() => {
        if (user) {
            setNickname(user.nickname);
            setEmail(user.email);
            setPreview(user.profileImageUrl);
        }
    }, [user]);

    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (file) {
            setProfileImageFile(file);
            setPreview(URL.createObjectURL(file));
        }
    };

    const handleRemoveImage = () => {
        setProfileImageFile(null);
        setPreview('https://placehold.co/128x128/E5E7EB/4B5563?text=:)');
        if (fileInputRef.current) {
            fileInputRef.current.value = "";
        }
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setIsSubmitting(true);
        setMessage(null);

        const formData = new FormData();
        const requestDto: { nickname?: string, isRemoveProfileImage?: boolean } = {};
        
        if (nickname !== user?.nickname) requestDto.nickname = nickname;
        if (preview !== user?.profileImageUrl && !profileImageFile) requestDto.isRemoveProfileImage = true;

        formData.append('request', new Blob([JSON.stringify(requestDto)], { type: 'application/json' }));
        if (profileImageFile) {
            formData.append('profileImageFile', profileImageFile);
        }

        try {
            const updatedData = await updateUserProfile(formData);
            updateUser(updatedData);
            setMessage({ type: 'success', text: '프로필이 성공적으로 업데이트되었습니다.' });
        } catch (error) {
            setMessage({ type: 'error', text: '프로필 업데이트에 실패했습니다.' });
        } finally {
            setIsSubmitting(false);
        }
    };

    if (!user) return <SplinePointer />;

    return (
        <div className="page-content">
            <header className="page-header"><h1>개인 정보 수정</h1></header>
            <form onSubmit={handleSubmit}>
                {message && <div className={`message ${message.type}`}>{message.text}</div>}
                <div className="form-group profile-image-group">
                    <img src={preview || ''} alt="프로필 미리보기" className="profile-preview" />
                    <div className="profile-image-buttons">
                        <input type="file" accept="image/*" onChange={handleFileChange} ref={fileInputRef} style={{ display: 'none' }} id="file-upload" />
                        <button type="button" className="btn btn-secondary" onClick={() => fileInputRef.current?.click()}>
                            <Camera size={16} /> 이미지 변경
                        </button>
                        <button type="button" className="btn btn-tertiary" onClick={handleRemoveImage}>
                            <X size={16} /> 이미지 제거
                        </button>
                    </div>
                </div>
                <div className="form-group">
                    <label htmlFor="nickname">닉네임</label>
                    <input id="nickname" type="text" value={nickname} onChange={e => setNickname(e.target.value)} />
                </div>
                <div className="form-group">
                    <label htmlFor="email">이메일</label>
                    <input id="email" type="email" value={email} disabled />
                </div>
                <div className="form-actions">
                    <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
                        {isSubmitting ? '저장 중...' : '변경사항 저장'}
                    </button>
                </div>
            </form>
        </div>
    );
};
export default EditProfilePage;