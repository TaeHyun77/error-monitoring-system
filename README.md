
각 애플리케이션에 가벼운 SDK를 붙여 에러를 자동 수집하고, 중앙 서버가 이를 저장·그룹핑·분석·알림까지 처리하는 에러 모니터링 플랫폼

즉, 각 서비스에서 발생한 에러를 SDK로 수집하여 중앙 서버에서 저장·그룹핑하고, Slack 알림 및 AI 분석을 통해 장애 대응을 자동화하는 구조<br><br>

### 핵심 설계 포인트
---
1. SDK는 메인 애플리케이션을 방해하지 않아야 한다.
   
3. 같은 원인의 에러는 fingerprint로 그룹핑해야 한다.
   
5. 민감정보는 SDK 단계에서 반드시 마스킹 한다
   
7. 알림은 모든 에러가 아니라 의미 있는 이벤트에만 보내야 한다.
   
9. MCP와 search_similar를 통해 과거 해결 사례를 재사용할 수 있어야 한다.<br><br>

### 간단한 흐름도
---
[각 서비스 (Spring Boot)]
        │
        │  (SDK가 에러 감지)
        ▼
[error-monitor-sdk]
        │
        │  ErrorReport (JSON)
        ▼
[error-monitor-server]
        │
        ├─ DB 저장 (grouping)
        ├─ Slack 알림
        └─ AI 분석 (Claude)<br><br>

### 동작 흐름
[사용자]
   ↓
AI (ChatGPT / Claude)
   ↓
MCP Tool 호출
   ↓
error-monitor-server
   ├─ 에러 조회
   ├─ 유사 에러 검색
   ├─ 프로젝트 간 비교
   ↓
AI가 결과 분석 후 응답

∴ 에러 발생 → SDK 수집 → 서버 저장 → 그룹핑 → 알림/분석 → AI 조회
