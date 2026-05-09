# 구현 및 문서화 메모

## 요구사항 대응표

| 요구사항 | 구현 위치 |
| --- | --- |
| 계정 추가 | `IssueService.addUser`, Swing `Add User` |
| 이슈 등록 | `IssueService.createIssue`, Swing `New Issue`, AWT `New` |
| 이슈 검색 | `IssueService.search` |
| 코멘트 추가 | `IssueService.addComment`, Swing `Comment` |
| 상세 정보 확인 | Swing/AWT detail panel |
| 이슈 배정 | `IssueService.assignIssue` |
| 상태 변경 | `IssueService.markFixed`, `IssueService.changeStatus` |
| 통계 분석 | `IssueService.statistics`, Swing `Stats` |
| assignee 추천 | `IssueService.recommendAssignees` |
| 영속 저장 | `FileIssueRepository`, `data/issues.store` |
| MVC 분리 | `model`, `service`, `controller`, `ui` 패키지 분리 |
| 두 UI | `SwingIssueApp`, `AwtIssueApp` |

## 추천 알고리즘 가정

현재 추천 기능은 resolved/closed 상태 이슈 중 fixer가 있는 이슈를 대상으로, 신규 이슈의 제목/설명과 기존 이슈의 제목/설명에서 3글자 이상 토큰을 추출한 뒤 공통 토큰 수를 점수로 사용한다.
점수가 높은 fixer 상위 3명을 추천한다. 프로젝트 문서에는 이 방법을 Information Retrieval 기반 유사도 추천의 단순 구현으로 설명하면 된다.

## 문서에 반드시 보강할 내용

- 팀원 학번/이름과 GitHub 주소
- 전체 유스케이스 다이어그램: include 2개 이상, extend 2개 이상
- 유스케이스 명세 6개
- 도메인 모델, SSD 2개, Operation Contract 2개 이상
- 클래스 다이어그램과 시퀀스 다이어그램
- MVC, Information Expert, Controller, Low Coupling, High Cohesion 적용 근거
- 테스트 케이스 목적과 실행 결과
- 두 UI가 같은 service/model/repository를 재사용한다는 설명과 실행 캡처
