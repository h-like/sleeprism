import React from "react";

const features = [
  {
    title: "빠르고 간편한 사용",
    description: "설치 없이 웹에서 바로 시작할 수 있어요.",
  },
  {
    title: "모바일 최적화",
    description: "모든 기기에서 최상의 경험을 제공합니다.",
  },
  {
    title: "무료로 사용 가능",
    description: "가입만 하면 누구나 무료로 이용할 수 있어요.",
  },
];

const LandingPage = () => {
  return (
    <div className="font-sans text-gray-800">
      {/* Hero Section */}
      <section className="bg-gradient-to-r from-indigo-500 to-purple-600 text-white py-20 px-4 text-center">
        <h1 className="text-4xl md:text-5xl font-bold mb-6">
          누구나 쉽게 시작하는 나만의 서비스
        </h1>
        <p className="text-lg md:text-xl mb-8">
          지금 가입하고 특별한 기능들을 무료로 경험해보세요!
        </p>
        <button className="bg-white text-indigo-600 font-semibold px-6 py-3 rounded-full hover:bg-gray-100 transition">
          지금 시작하기
        </button>
      </section>

      {/* Features Section */}
      <section className="py-16 px-4 max-w-5xl mx-auto">
        <h2 className="text-3xl font-bold text-center mb-10">주요 기능</h2>
        <div className="grid md:grid-cols-3 gap-8">
          {features.map((feature, idx) => (
            <div
              key={idx}
              className="bg-white rounded-2xl shadow-md p-6 hover:shadow-lg transition"
            >
              <h3 className="text-xl font-semibold mb-2">{feature.title}</h3>
              <p className="text-gray-600">{feature.description}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Footer */}
      <footer className="bg-gray-100 text-center py-6 text-sm text-gray-500">
        © 2025 MyLanding. All rights reserved.
      </footer>
    </div>
  );
};

export default LandingPage;
