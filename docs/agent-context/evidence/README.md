# 검증 증거

`<task_id>/` 디렉토리마다 다음 파일이 저장된다.

- `<검사이름>.log`: 검사 출력 (저장 전에 비밀정보를 가림)
- `summary.json`: 검사별 결과
- `gate.json`: 위험도별 게이트 판정 결과

quality-gate Skill의 스크립트만 이 디렉토리에 쓴다. 증거를 커밋할지는 프로젝트가 정한다. 커밋하지 않으려면 `.gitignore`에 `docs/agent-context/evidence/*/`를 추가한다.
