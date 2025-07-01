import React from 'react';

// StarBackground 컴포넌트 (별 배경)
const StarBackground: React.FC = () => {
    // 미리 계산된 box-shadow 값들 (Sass @function을 대체)
    // 별의 밀도와 크기를 조절하기 위해 임의의 값을 생성합니다.
    const generateFixedShadows = (count: number, maxCoord: number) => {
        let shadows = [];
        for (let i = 0; i < count; i++) {
            const x = Math.floor(Math.random() * maxCoord);
            const y = Math.floor(Math.random() * maxCoord);
            shadows.push(`${x}px ${y}px #FFF`);
        }
        return shadows.join(', ');
    };

    const shadowsSmall = generateFixedShadows(700, 2000); // 작은 별 700개, 2000px 범위
    const shadowsMedium = generateFixedShadows(200, 2000); // 중간 별 200개, 2000px 범위
    const shadowsBig = generateFixedShadows(100, 2000);   // 큰 별 100개, 2000px 범위

    return (
        <div className="star-background-container">
            <div id="stars" style={{ boxShadow: shadowsSmall }}></div>
            <div id="stars2" style={{ boxShadow: shadowsMedium }}></div>
            <div id="stars3" style={{ boxShadow: shadowsBig }}></div>
        </div>
    );
};

export default StarBackground;
