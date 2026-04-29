# Error Monitoring and Analysis System

에러 모니터링 및 분석 시스템


## 응답 언어 지침

- 모든 결과값, 설명, 주석, 커밋 메시지, PR 설명 등은 반드시 한글로 작성한다.
- 코드 내 변수명, 함수명, 클래스명 등 식별자는 영문을 유지한다.


## 빌드 및 실행 방법

프로젝트 전체를 빌드하려면 ./gradlew build 명령어를 사용
SDK만 빌드하려면 ./gradlew :error-monitor-sdk:build를 사용합니다.

Docker로 실행할 경우, 먼저 docker network create airport-chat-net으로 네트워크를 생성한 뒤 docker-compose up을 실행하면 됩니다.


## 아키텍처 구조

이 프로젝트는 Gradle 멀티 모듈 구조로 구성된 모노레포이며, 두 개의 모듈이 HTTP를 통해서만 통신하도록 설계되어 있습니다.

error-monitor-sdk는 각 Spring Boot 애플리케이션에서 의존성으로 추가하는 라이브러리로, 애플리케이션에서 발생하는 예외를 자동으로 감지하고 중앙 서버로 전송하는 역할을 합니다.

error-monitor-server는 Spring Boot 3.4.3 기반의 애플리케이션으로, SDK로부터 전달받은 에러 데이터를 수신하여 fingerprint 기준으로 그룹핑하고 PostgreSQL에 저장한 뒤, Thymeleaf 기반 대시보드로 시각화하는 역할을 합니다.



## 에러 처리 흐름

클라이언트 애플리케이션에서 예외가 발생하면, SDK가 이를 감지합니다. 감지는 MVC 인터셉터 또는 Logback의 ERROR 로그를 통해 이루어집니다.

감지된 예외는 ErrorCaptor에서 처리되며, 예외 정보와 함께 SHA-256 기반 fingerprint를 생성하여 ErrorEvent 객체로 변환됩니다.

이 데이터는 별도의 비동기 스레드 풀을 통해 HTTP 요청으로 중앙 서버에 전송되며, 요청 시 X-API-Key 헤더를 포함합니다.

만약 전송에 실패할 경우, 데이터는 로컬의 error-backup/ 디렉토리에 JSON 파일 형태로 백업됩니다.

서버에서는 /api/errors 엔드포인트로 요청을 받아 ApiKeyAuthFilter를 통해 API Key를 검증합니다.

이후 ErrorIngestService에서 (projectId, fingerprint) 기준으로 에러 그룹을 조회하거나 생성하고, 에러 발생 횟수를 증가시키거나 새로운 이벤트로 저장합니다.

에러 상태는 UNRESOLVED에서 시작하여 해결 시 RESOLVED로 변경되며, 이후 동일 에러가 다시 발생하면 REGRESSED 상태로 전환됩니다. 필요에 따라 IGNORED 상태로도 관리할 수 있습니다.


## SDK 주요 구성 요소

SDK는 Spring Boot Auto Configuration 기반으로 동작하며, 애플리케이션에 별도의 설정 없이도 자동으로 에러를 수집하도록 설계되어 있습니다.

ErrorMonitorAutoConfiguration은 SDK의 모든 Bean을 구성하며, error-monitor.enabled 설정이 true일 경우 자동으로 활성화됩니다.

ErrorMonitorProperties는 서버 URL, 프로젝트 ID, API Key, 환경 정보 등 설정 값을 관리합니다.

ErrorCaptor는 에러를 실제로 수집하고 전송하는 핵심 컴포넌트로, 중복 방지와 비동기 처리를 담당합니다.

ExceptionInterceptor는 Spring MVC 요청 처리 이후 발생한 예외를 감지하며, 기존 예외 처리 흐름을 방해하지 않고 관찰만 수행합니다.

LogbackErrorAppender는 ERROR 레벨 로그 중 예외가 포함된 로그를 감지하여 추가적인 에러 수집을 수행합니다.

FingerprintGenerator는 예외 타입과 최초 애플리케이션 스택 프레임을 기반으로 SHA-256 해시를 생성하여 동일한 에러를 그룹화할 수 있도록 합니다.

SensitiveDataFilter는 Authorization 헤더, 토큰, 비밀번호 등 민감 정보를 [FILTERED] 형태로 마스킹하여 전송합니다.

에러 전송은 HttpErrorTransport를 통해 이루어지며, 실패 시 FileBackupTransport가 로컬 파일로 데이터를 저장합니다.



## 서버 주요 구성 요소

서버는 인증, 프로젝트 관리, 에러 수집, 그룹핑, 이벤트 저장, 대시보드 표시 기능으로 구성되어 있습니다.

ApiKeyAuthFilter는 /api/errors 요청에 대해 X-API-Key를 검증하여 인증을 수행합니다.

프로젝트 관리 기능에서는 프로젝트 생성 시 proj_ 접두어와 UUID를 결합한 API Key가 자동으로 발급됩니다.

에러 수집은 ErrorIngestController를 통해 이루어지며, SDK로부터 전달받은 데이터를 처리합니다.

에러 그룹은 ErrorGroup 엔티티로 관리되며, fingerprint를 기준으로 동일한 에러를 하나의 그룹으로 묶고 상태를 관리합니다.

각 에러 발생은 ErrorEvent로 저장되며, 특정 그룹과 연관되어 이력을 구성합니다.

대시보드는 Thymeleaf와 Tailwind CSS를 사용하여 구현되어 있으며, 에러 목록과 상세 정보를 시각적으로 제공합니다.

예외 처리 전반은 MonitorException, ErrorCode, 그리고 전역 예외 핸들러를 통해 일관되게 관리됩니다.



## 설계 원칙

SDK는 절대로 호스트 애플리케이션의 동작을 방해하거나 장애를 유발해서는 안 되며, 모든 처리는 비동기로 수행되고 실패 시에도 조용히 무시됩니다.

에러는 fingerprint를 기준으로 그룹핑되어 동일한 원인의 에러를 하나로 묶어 관리합니다.

민감 정보는 반드시 SDK 단계에서 마스킹된 후 전송됩니다.

SDK와 서버는 컴파일 타임 의존성이 없으며, 오직 HTTP/JSON 기반 계약을 통해서만 통신하도록 설계되었습니다.