### Error Monitoring Platform
---
각 애플리케이션에 경량 SDK를 적용하여 에러를 자동 수집하고, 중앙 서버에서 이를 저장·그룹핑·분석·알림까지 처리하는 에러 모니터링 플랫폼입니다.

즉, 분산된 서비스에서 발생하는 에러를 중앙에서 통합 관리하고, Slack 알림과 AI 분석을 통해 장애 대응을 자동화하는 것을 목표로 설계했습니다.<br><br>

### Architecture 
---
<pre>
```text
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
```
</pre><br><br>

### Processing Flow
---
<pre>
```text
에러 발생
   ↓
SDK가 예외 및 ERROR 로그 감지
   ↓
ErrorReport 생성 + fingerprint 생성
   ↓
중앙 서버로 비동기 전송
   ↓
DB 저장 및 에러 그룹핑
   ↓
알림 조건 판단
   ├─ 신규 에러 → Slack 알림
   ├─ 급증 → Slack 알림
   └─ 재발 → Slack 알림
   ↓
(배치)
AI 분석 수행 (Claude)
   ↓
분석 결과 저장 및 활용
```
</pre><br><br>

### MCP 기반 AI 분석 흐름
---
<pre>
```text
[사용자]
   ↓
AI (ChatGPT / Claude)
   ↓
MCP Tool 호출
   ↓
error-monitor-server
   ├─ 에러 조회
   ├─ 유사 에러 검색 (search_similar)
   ├─ 프로젝트 간 비교 (compare_errors)
   ↓
AI가 결과를 분석하여 응답
```
</pre><br><br>

### 핵심 원칙
---
1. SDK는 메인 애플리케이션을 방해하지 않아야 한다. (비동기 처리 + 실패 시 무시)
   
2. 같은 원인의 에러는 fingerprint 기반으로 그룹핑한다.
   
3. 민감 정보는 SDK 단계에서 반드시 마스킹한다.
   
4. 모든 에러가 아닌, 의미 있는 이벤트만 알림으로 전달한다.
   
5. MCP + search_similar를 통해 과거 해결 사례를 재사용한다.

에러 발생 → SDK 수집 → 서버 저장 → 그룹핑 → 알림 및 AI 분석 → AI 기반 조회
