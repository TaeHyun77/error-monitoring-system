# 아키텍처

## SDK 전송 계층 (transport)

### 전송 흐름
`ErrorCaptor` → `ThreadPoolExecutor`에 `ErrorSendTask` 제출 → `HttpErrorTransport.send()` → 성공/실패 분기

- 전송 성공 시 완료
- 전송 실패 시(`CLIENT_ERROR`, `SERVER_ERROR`) → `FileBackupTransport.backup()` → JSON 파일로 로컬 저장
- 큐 포화로 reject 시 → `BackupOnRejectHandler`가 즉시 백업

### 재전송 흐름
`BackupReplayScheduler` → `scheduleWithFixedDelay`로 주기적 실행 → 백업 디렉토리의 JSON 파일을 배치 단위로 처리

1. `FileBackupTransport.listBackupFiles()`로 백업 파일 목록 조회 (시간순 정렬)
2. `FileBackupTransport.readBackupFile()`로 역직렬화
3. `HttpErrorTransport.trySend()`로 재전송 시도
4. `SendResult`에 따라 분기 처리:
   - `SUCCESS` → 백업 파일 삭제
   - `CLIENT_ERROR` → dead-letter 디렉토리로 이동 (재시도 무의미)
   - `SERVER_ERROR` → 배치 중단, 다음 주기에 재시도

### SendResult 분류
- `SUCCESS` — 전송 성공 (2xx)
- `CLIENT_ERROR` — 서버가 이벤트를 거부 (4xx, 재시도 무의미)
- `SERVER_ERROR` — 서버 다운 또는 네트워크 오류 (5xx, 재시도 의미 있음)
