# 결정 기록

새 결정은 맨 위에 추가한다. 오케스트레이터는 작업을 마칠 때 주요 결정을 이 형식으로 기록한다.

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
