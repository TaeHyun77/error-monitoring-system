### Error Monitoring Platform
---
각 애플리케이션에 경량 SDK를 적용하여 에러를 자동 수집하고, 중앙 서버에서 이를 저장·그룹핑·분석·알림까지 처리하는 에러 모니터링 플랫폼입니다.

즉, 분산된 서비스에서 발생하는 에러를 중앙에서 통합 관리하고, Slack 알림과 AI 분석을 통해 장애 대응을 자동화하는 것을 목표로 설계했습니다.

### Architecture 
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
        ├─ DB 저장 (Fingerprint 기반 그룹핑)
        ├─ Slack 알림
        └─ AI 분석 (Claude)
