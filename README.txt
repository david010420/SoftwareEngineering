SE 2026 Spring Term Project - Issue Management System

1. 산출물 요약
- Java 소스코드: src/main/java
- 모델 테스트 코드: src/test/java/its/IssueServiceTest.java
- 실행 스크립트: scripts/compile.ps1, scripts/test.ps1, scripts/run-swing.ps1, scripts/run-awt.ps1
- 영속 데이터: data/issues.store 파일이 실행 시 자동 생성됨

2. 실행 방법
- 컴파일: powershell -ExecutionPolicy Bypass -File scripts/compile.ps1
- 테스트: powershell -ExecutionPolicy Bypass -File scripts/test.ps1
- Swing UI 실행: powershell -ExecutionPolicy Bypass -File scripts/run-swing.ps1
- AWT UI 실행: powershell -ExecutionPolicy Bypass -File scripts/run-awt.ps1

3. 기본 계정 및 데모 데이터
- 최초 실행 시 project1과 admin, PL1, PL2, dev1~dev10, tester1~tester5가 자동 생성됨
- 일부 closed/resolved 이슈가 함께 생성되어 assignee 추천 기능을 바로 확인할 수 있음

4. 구현 기능
- 계정 추가: admin, PL, dev, tester 역할 지원
- 프로젝트 추가: 서비스 계층에서 지원, 현재 데모 프로젝트는 project1
- 이슈 등록: title, description 필수, reporter와 reported date 자동 저장
- 이슈 브라우즈/검색: query, reporter, assignee, status 기준 검색
- 이슈 상세 보기: 필드와 comments history 확인
- 코멘트 추가: 작성자, 작성 시간, 메시지를 누적 보관
- 이슈 배정 및 상태 변경: new, assigned, fixed, resolved, closed, reopened 흐름 지원
- 통계: 일별/월별 이슈 발생 수 표시
- 자동 추천: resolved/closed 이슈의 title/description 유사도 기반 fixer 상위 3명 추천
- 두 UI: Swing UI와 AWT UI가 같은 controller/service/model/repository를 재사용
- Swing UI 로그인: 앱 시작 시 아이디/비밀번호 직접 입력, 데모 비밀번호는 모두 1234, 로그인 성공 후 메인 화면 표시, 실행 중 Switch User 가능
- Swing UI 권한 표시: 로그인한 role이 사용할 수 있는 탭과 버튼만 화면에 표시

5. 설계 요약
- model: Issue, Comment, UserAccount, Project 등 순수 도메인 객체
- repository: FileIssueRepository가 Java 직렬화 파일로 영속 저장
- service: IssueService가 유스케이스와 상태 변경 규칙 담당
- controller: IssueController가 UI와 서비스 사이의 경계 역할
- ui: SwingIssueApp, AwtIssueApp는 화면 코드만 포함하며 비즈니스 로직을 직접 갖지 않음

6. GitHub 주소
- https://github.com/david010420/SoftwareEngineering

7. 보완 필요 사항
- 프로젝트 문서 PDF, 발표 슬라이드, 소개 동영상은 별도 작성 필요
- JUnit 제출 요구가 있으므로 빌드 도구 사용이 가능하면 IssueServiceTest를 JUnit 5 테스트로 이전 권장
- UML 다이어그램, 유스케이스 명세, SSD, Operation Contract, GRASP 적용 설명을 프로젝트 문서에 포함 필요
