
// --- src/types/data.ts ---
// 프로젝트 전반에서 사용될 데이터의 타입을 정의합니다.
// 이렇게 타입을 분리하면 데이터 구조를 한눈에 파악하고 재사용하기 좋습니다.

export interface Feature {
    icon: 'moon' | 'prism' | 'lounge' | 'library';
    color: 'cyan' | 'purple' | 'pink' | 'yellow';
    title: string;
    description: string;
}

export interface DreamTag {
    text: string;
    color: 'cyan' | 'purple' | 'pink' | 'yellow' | 'gray';
}

export interface Dream {
    title: string;
    content: string;
    tags: DreamTag[];
}

