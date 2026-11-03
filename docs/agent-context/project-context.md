# 프로젝트 컨텍스트

> 오케스트레이터와 모든 에이전트가 작업 시작 전에 읽는 문서다. 사실만 짧게 유지하고, 바뀌면 바로 고친다.

## 개요

- 서비스 목적: 출사 장소 추천 서비스(Pick Place)의 백엔드 API
- 주요 사용자:

## 기술 스택

- 백엔드: Spring Boot 4.0.3 (Java 버전: 17, toolchain), 빌드 도구: Gradle (wrapper `./gradlew`)
- 주요 의존성: Spring Data JPA, Spring Security + OAuth2 Client, Web MVC, Validation, Lombok
- 프론트엔드: 이 저장소에는 없음
- DB: MySQL (`mysql-connector-j`). PostgreSQL이 아니므로 postgresql Skill의 MySQL과 다른 부분은 적용하지 않는다.
- 실행 환경: Docker 설정 없음

## 디렉토리 구조

- 백엔드: `src/main/java/talkingpotatoes/pickplaceapi/<도메인>/{controller,service,repository,domain,dto}` (도메인: user, place, post, photo, file, chat, meeting, alert), 공통은 `global/{config,security,api,domain,exception}`
- 설정: `src/main/resources/application.yml`, `src/main/resources/prop/`
- 테스트: `src/test/java/talkingpotatoes/pickplaceapi/`
- DB 마이그레이션: 없음(Flyway/Liquibase 미사용)

## 명령

에이전트는 아래 명령을 그대로 실행한다.

- Gradle은 항상 `--no-daemon`으로 실행한다. 샌드박스 안에서 시작된 Gradle 데몬이 샌드박스 제약을 유지한 채 재사용되어 `.gradle/.../fileHashes.lock (Operation not permitted)`로 빌드가 시작되지 않는 문제가 있었다(2026-11-03).

- 백엔드 단위 테스트: `./gradlew --no-daemon test`
- 백엔드 통합 테스트: `./gradlew --no-daemon test` (`@SpringBootTest`, 그리고 H2(MySQL 호환 모드, `src/test/resources/place-delete-h2.properties`)를 쓰는 `*IntegrationTest`가 단위 테스트와 같은 태스크에서 실행됨. MySQL Testcontainers는 없음)
- 백엔드 린트: `./gradlew --no-daemon checkstyleMain checkstyleTest` (네이버 컨벤션 v1.2 + 프로젝트 커스텀, `config/checkstyle/`. 경고 1건이라도 있으면 실패하며 `./gradlew --no-daemon build`에도 포함됨)
- 백엔드 빌드: `./gradlew --no-daemon build`
- API 계약 테스트: 없음
- 의존성 스캔: 없음
- SAST: 없음
- 프론트엔드 테스트: 해당 없음
- 프론트엔드 린트: 해당 없음
- 프론트엔드 빌드: 해당 없음

## 위험 경로

위험도 HIGH로 다룰 경로를 적는다.

- 인증·인가: `global/config/SecurityConfig.java`, `global/security/**`, `user/**`
- 결제·정산: 없음
- 마이그레이션: 없음(JPA 엔티티 변경 `**/domain/**`은 스키마에 영향)
- 기타: 파일 업로드 `file/**`
