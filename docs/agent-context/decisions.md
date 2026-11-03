# 결정 기록

새 결정은 맨 위에 추가한다. 오케스트레이터는 작업을 마칠 때 주요 결정을 이 형식으로 기록한다.

## 2026-11-03 — 장소·파일 보안·정합성 결함 수정 (place-file-hardening)

- 배경: 알려진 이슈 #1(세션 쿠키 설정 미적용), #3(장소 삭제 시 사진 미정리), #4(오류 응답에 서버 경로 노출), #5(사진 업로드 검증 부족).
- 결정:
  - 세션 설정을 최상위 `server.servlet.session.*`로 옮겼다.
  - FileException 메시지에서 경로·IOException을 빼고 로그로만 남긴다.
  - 장소 사진은 `file.imageTypes`(.jpg .jpeg .png .gif .webp, 대소문자 무시)와 매직 바이트로 검증한다.
  - 장소 삭제 시 사진·파일 행을 bulk 삭제하고 디스크 파일은 커밋 후에만 지운다.
  - 장소 수정·삭제는 장소 행을 PESSIMISTIC_WRITE로 잠근다.
  - 실DB 검증용으로 `testRuntimeOnly 'com.h2database:h2'`(2.4.240)를 추가했다.
- 고려한 대안: 동시성은 DB FK에 의존하기(DDL 미확인으로 기각), 통합 테스트는 Testcontainers(Docker 미기동, 사용자가 H2 선택).
- 영향과 후속 작업:
  - 사진 업로드에 새 오류 코드 `ERR_FILE_INVALID_IMAGE`가 생겼다.
  - 운영에서는 `SESSION_COOKIE_SECURE=true`가 필요하다.
  - 남은 minor 이슈:
    - 락 타임아웃 힌트가 없다(InnoDB 기본 50초 대기).
    - 수정 중 큰 업로드가 락을 오래 잡는다.
    - H2와 InnoDB는 동작이 다르다. MySQL Testcontainers는 후속 과제다.
    - Bookmark·Photo·PlaceReview가 장소를 참조하면 삭제가 실패할 수 있다.
    - polyglot 이미지를 막지 못한다.
- 관련 task_id와 증거 경로: `place-file-hardening` — `docs/agent-context/evidence/place-file-hardening/`
  - HIGH, 재시도 1회
  - 1회차: FAILED. concurrency-review가 failed였고 important 2건(동시성, 실DB 미검증)이 나왔다.
  - 2회차: GATE PASSED. 테스트 116건이다.
  - waive(사용자 승인): api-contract-tests, dependency-scan, sast. 도구가 없어서다.

## 2026-11-03 — 이슈 #14 테스트 자산 정리

- 배경: `feature/#14`에 수동 테스트용 자산(Q16)이 남아 있어 `main`에 PR을 올리기 전에 정리가 필요했다.
- 결정(사용자 승인): `PlacePageController`·`static/test/place.html` 삭제, 미추적 `templates/file-test.html` 삭제, `FileController`의 주석 처리 테스트 코드와 `SecurityConfig`의 `/test/place` 허용은 커밋 상태로 되돌림, `/files/` 업로드 디렉토리를 `.gitignore`에 추가(디스크 파일은 유지).
- 고려한 대안: 프론트 연동 전까지 테스트 페이지 유지(사용자가 삭제 선택).
- 영향과 후속 작업: 지도 수동 테스트 페이지가 없어진다. 샌드박스에서 시작된 Gradle 데몬이 `.gradle` 잠금 파일 쓰기를 막아 검사가 환경 문제로 실패했고, `--no-daemon`으로 다시 실행해 통과했다.
- 관련 task_id와 증거 경로: `issue14-cleanup` — `docs/agent-context/evidence/issue14-cleanup/` (LOW, unit-tests·lint·build passed, GATE: PASSED)

## 2026-11-03 — devhub를 hub 최신(e307d5f)으로 업데이트

- 배경: `devhub status`가 `external_skills must be <plugin>:<skill>` 오류로 실패했다. 설치된 devhub CLI는 hub `3ae5e1a`부터 생긴 `superpowers:<이름>` 형식을 요구하는데, `hub.lock`은 그 이전 `ed3e189`에 고정되어 예전 형식(`test-driven-development`)의 에이전트 정의를 읽고 있었다.
- 결정: `devhub update --latest` → `devhub sync`로 `e307d5f`에 맞췄다. 생성 파일(CLAUDE.md, AGENTS.md, `.claude/`, `.codex/`, `.agents/`)을 바꿨고 overlay는 비어 있어 덮어쓴 수정은 없다.
- 고려한 대안: hub 저장소를 직접 고치기(원격 main에 이미 수정이 있어 불필요).
- 영향과 후속 작업: 플러그인 Skill이 선택적 보조로 표시되고, `docs/agent-context/gate-policy.json`으로 `lint`·`integration-tests`·`api-contract-tests`를 해당 없음으로 선언할 수 있게 됐다(사용자만 작성). `.devhub/hub.lock` 변경은 커밋 대상이다. 로컬 hub 클론(`~/.devhub/hub`)은 원격보다 4커밋 뒤처져 있다(devhub는 스냅샷을 쓰므로 동작에는 영향 없음).
- 관련 task_id와 증거 경로: 없음(설정 작업, `devhub status` → `clean`)

## 2026-11-02 — 린트 도입: Checkstyle(네이버 컨벤션 v1.2 + 커스텀)

- 배경: 린트 도구가 없어 모든 작업의 품질 게이트가 `lint` UNVERIFIED에서 멈췄다(Q20). IntelliJ에는 네이버 코딩 컨벤션 v1.2 스킴(들여쓰기 공백 4칸)이 설정되어 있었지만 터미널·에이전트에서는 검사할 수 없었다.
- 결정: Gradle `checkstyle` 플러그인(10.21.2)과 네이버 공식 규칙(`naver/hackday-conventions-java`)을 `config/checkstyle/`에 두고 `maxWarnings = 0`으로 강제한다. 커스텀: 탭 대신 공백 4칸, enum의 닫는 `;` 단독 줄 허용, `UUID` 약어 허용, test 코드의 한글 메서드명 허용. 기존 코드는 import를 네이버 순서로 재정렬하고 `static final` 순서·120자 초과·파일 끝 줄바꿈을 고쳤다(로직 변경 없음).
- 고려한 대안: gate-policy.json으로 lint를 n/a 선언(사용자가 보류), 현재 import 순서에 규칙을 맞추기(IntelliJ 스킴과 어긋나 기각), 한글 테스트명을 영문으로 변경(86곳 수정 부담으로 기각).
- 영향과 후속 작업: `./gradlew build`가 린트 위반에서 실패한다. IntelliJ CheckStyle-IDEA 플러그인의 규칙도 같은 파일로 바꿔야 IDE와 검사 기준이 일치한다.
- 관련 task_id와 증거 경로: 없음(설정 작업, `./gradlew checkstyleMain checkstyleTest` 경고 0건, `./gradlew build` 통과·테스트 72/72)

## 2026-10-23 — 프로젝트 문서화(docs/project/) 및 devhub 연결 점검

- 배경: 사용자 요청으로 커밋·코드 기반 개발 현황, API 명세 문서화와 devhub 에이전트 동작 점검을 수행.
- 결정: 사람용 문서는 `docs/project/`(01~08)에 두고, 에이전트 운영 기록은 이 디렉토리에 유지한다. 코드는 수정하지 않았다(문서만 작성, LOW·직접 처리).
- 고려한 대안: Claude Docs 등 외부 문서 — 사용자가 저장소 `docs/` 하위를 지정해 채택하지 않음.
- 영향과 후속 작업: 발견한 이슈는 `docs/project/06-known-issues-and-todos.md`, 미결 질문은 `07-open-questions.md`. 린트 도구 부재로 모든 작업의 게이트가 `lint` UNVERIFIED가 됨(Q20).
- 관련 task_id와 증거 경로: `devhub-smoke-baseline` — `docs/agent-context/evidence/devhub-smoke-baseline/` (unit-tests 72/72 passed, build passed, GATE: UNVERIFIED — lint missing, waive 안 함)

## YYYY-MM-DD — <결정 제목>

- 배경:
- 결정:
- 고려한 대안:
- 영향과 후속 작업:
- 관련 task_id와 증거 경로:
