---
name: pr-qa
description: PR 분석 및 회귀 테스트 전체 QA 워크플로우를 실행합니다
argument-hint: "[PR번호 또는 PR URL]"
---

# PR QA 워크플로우

PR $ARGUMENTS 에 대해 다음 5단계를 **순서대로** 실행합니다.
각 단계는 반드시 이전 단계가 완료된 후에 시작합니다.

## 워크플로우 개요

| Phase | 에이전트 | 역할 |
|-------|---------|------|
| 1 | `pr-analyzer` | PR 변경사항 분석 |
| 2 | `playwright-regression-test-generator` | 회귀 테스트 생성 |
| 3 | `regression-test-runner` | 테스트 실행 및 결과 문서화 |
| 4 | `test-report-commenter` | PR에 테스트 결과 코멘트 게시 |
| 5 | `test-result-analyzer` | 실패 분석 및 코드 수정 (실패 시에만) |

---

## Phase 1: PR 분석

Task tool을 사용하여 `pr-analyzer` 에이전트를 호출합니다.

**프롬프트에 포함할 내용:**
- PR 번호 또는 URL: `$ARGUMENTS`
- 분석 결과를 `regression-tests/pr-results/PR-{번호}/analysis-report.md`에 저장하도록 지시

**완료 후:**
- 분석 보고서 파일 경로를 확인
- 결과를 사용자에게 간단히 보고

---

## Phase 2: 회귀 테스트 생성

Task tool을 사용하여 `playwright-regression-test-generator` 에이전트를 호출합니다.

**프롬프트에 포함할 내용:**
- Phase 1에서 생성된 분석 보고서의 경로와 핵심 내용
- 테스트 파일을 `regression-tests/playwright-tests/tests/{domain}/` 에 저장하도록 지시
- 기존 테스트가 있으면 추가/수정하도록 안내

**완료 후:**
- 생성된 테스트 파일 목록 확인
- 결과를 사용자에게 보고

---

## Phase 3: 테스트 실행

Task tool을 사용하여 `regression-test-runner` 에이전트를 호출합니다.

**프롬프트에 포함할 내용:**
- Phase 2에서 생성된 테스트 파일 경로
- Git Worktree 사용 지침:
  ```
  git fetch origin pull/{PR번호}/head:pr-{PR번호}
  git worktree add ../store-pr-test pr-{PR번호}
  ```
- 서버는 worktree 경로(`../store-pr-test`)에서 실행
- 테스트는 현재 디렉토리에서 실행
- 결과를 `regression-tests/pr-results/PR-{번호}/regression-test-report-{날짜}.md`에 저장
- 완료 후 worktree 정리:
  ```
  git worktree remove ../store-pr-test --force
  git branch -D pr-{PR번호}
  ```

**완료 후:**
- 테스트 결과 보고서 경로 확인
- 성공/실패 요약을 사용자에게 보고

---

## Phase 4: PR 코멘트 게시

Task tool을 사용하여 `test-report-commenter` 에이전트를 호출합니다.

**프롬프트에 포함할 내용:**
- Phase 3에서 생성된 테스트 결과 보고서 경로
- PR 번호
- `gh pr comment` 명령어로 결과 요약을 PR에 게시하도록 지시

**완료 후:**
- 코멘트 게시 성공 여부 확인
- 결과를 사용자에게 보고

---

## Phase 5: 실패 분석 (조건부)

**Phase 3에서 실패한 테스트가 있을 경우에만** 실행합니다.

Task tool을 사용하여 `test-result-analyzer` 에이전트를 호출합니다.

**프롬프트에 포함할 내용:**
- Phase 3의 테스트 결과 보고서 경로
- 실패한 테스트 목록
- PR 번호 (fix 브랜치 생성 시 참조)

**완료 후:**
- 분석 결과와 수정 사항을 사용자에게 보고
- 코드 수정이 이루어졌다면 fix 브랜치 정보 공유

---

## 진행 상황 보고 (필수)

각 Phase 시작 전과 완료 후에 반드시 사용자에게 진행 상황을 보고합니다:

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[Phase N/5] {Phase 이름}
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
에이전트: {agent_name}
작업: {설명}
상태: 진행 중...
```

완료 후:
```
[Phase N/5] 완료
결과: {간단한 결과 요약}
생성된 파일: {파일 경로}
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

## 최종 요약

모든 Phase 완료 후 전체 워크플로우 결과를 요약합니다:

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
PR QA 워크플로우 완료
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
PR: #{번호}
분석 보고서: {경로}
테스트 결과: 성공 {N}개 / 실패 {N}개
PR 코멘트: 게시 완료
수정 사항: {있음/없음}
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

## 에러 처리

- Phase 1 (PR 분석) 실패 시: 전체 워크플로우 중단, 사용자에게 PR 정보 확인 요청
- Phase 2 (테스트 생성) 실패 시: 전체 워크플로우 중단, 분석 보고서 검토 요청
- Phase 3 (테스트 실행) 실패 시: Phase 4로 진행 (실패 결과도 PR에 보고), 이후 Phase 5 실행
- Phase 4 (코멘트 게시) 실패 시: 사용자에게 수동 게시 안내, Phase 5는 계속 진행
- Phase 5 (실패 분석) 실패 시: 사용자에게 수동 분석 안내
