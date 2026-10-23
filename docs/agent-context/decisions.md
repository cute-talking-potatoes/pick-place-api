# 결정 기록

새 결정은 맨 위에 추가한다. 오케스트레이터는 작업을 마칠 때 주요 결정을 이 형식으로 기록한다.

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
