# UI Screenshot Checklist

프로젝트 문서와 발표 슬라이드에 넣기 좋은 UI 캡처 목록이다.

## Login

- Swing 로그인 다이얼로그
- `tester1 / 1234`, `PL1 / 1234`, `dev1 / 1234`, `admin / 1234` 로그인 예시

## Role-Based UI

- `admin` 로그인 후 `Reports & Admin` 탭
  - Add User
  - Add Project
  - Stats
- `PL1` 로그인 후 `Workflow` 탭
  - Comment
  - Assign
  - Close
- `PL1` 로그인 후 `Reports & Admin` 탭
  - Recommend Assignee
  - Stats
- `dev1` 로그인 후 `Workflow` 탭
  - Comment
  - Fix
- `tester1` 로그인 후 `New Issue` 탭
  - Project 선택
  - Title
  - Priority
  - Description
- `tester1` 로그인 후 `Workflow` 탭
  - Comment
  - Resolve
  - Reopen

## Ticket Browse

- Browse 탭 전체 화면
- Ticket Query 필터
- Quick Filters
  - All
  - NEW
  - Assigned to Me
  - Reported by Me
  - FIXED
  - RESOLVED
- Ticket Detail
  - Properties
  - Description
  - Change History

## Demo Scenario

- Admin이 project 추가
- Tester가 새 이슈 생성
- Tester가 코멘트 추가
- PL이 NEW 필터로 이슈 검색
- PL이 dev에게 assign
- Dev가 Assigned to Me로 이슈 검색
- Dev가 Fix 처리
- Tester가 FIXED 필터로 이슈 검색
- Tester가 Resolve 처리
- PL이 RESOLVED 필터로 이슈 검색
- PL이 Close 처리

## Reports

- Recommendation Result 영역
  - 선택된 이슈
  - 추천 후보 목록
- Statistics Result 영역
  - Daily count
  - Monthly count

## Second UI Toolkit

- AWT UI 실행 화면
- AWT UI에서 같은 ticket 데이터가 표시되는 화면
- 문서에는 Swing/AWT가 같은 `IssueController`, `IssueService`, `FileIssueRepository`, model을 재사용한다고 설명한다.
