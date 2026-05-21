# 구현 완료 내용 및 남은 작업 정리

## 1. 구현 완료 내용

### 1.1 프로젝트 구조

현재 프로젝트는 Java 표준 라이브러리만 사용하여 `javac`로 컴파일 가능한 구조로 구성되어 있다.

```text
src/main/java/its
├─ model        도메인 모델
├─ repository   파일 기반 영속 저장소
├─ service      핵심 비즈니스 로직
├─ controller   UI와 서비스 사이의 경계
└─ ui
   ├─ swing      Swing UI
   └─ awt        AWT UI
```

빌드 도구가 없는 환경에서도 실행할 수 있도록 PowerShell 스크립트를 제공한다.

```text
scripts/compile.ps1
scripts/test.ps1
scripts/run-swing.ps1
scripts/run-awt.ps1
```

### 1.2 MVC 구조

요구사항의 UI와 응용 로직 분리 조건을 만족하도록 계층을 분리했다.

- `model`: `Issue`, `Comment`, `UserAccount`, `Project`, `Role`, `Priority`, `IssueStatus`
- `repository`: `FileIssueRepository`, `IssueStore`
- `service`: `IssueService`, `IssueSearchCriteria`, `IssueStatistics`
- `controller`: `IssueController`
- `ui`: `SwingIssueApp`, `AwtIssueApp`

두 UI는 비즈니스 로직을 직접 갖지 않고 동일한 `IssueController`를 호출한다. 따라서 UI를 바꾸더라도 model/service/repository 계층은 재사용 가능하다.

### 1.3 영속 저장

이슈, 계정, 프로젝트 데이터는 실행 중 `data/issues.store` 파일에 저장된다.

- 저장 방식: Java object serialization
- 구현 위치: `src/main/java/its/repository/FileIssueRepository.java`
- 최초 실행 시 데모 데이터 자동 생성

### 1.4 계정 및 데모 데이터

최초 실행 시 아래 데모 데이터가 자동 생성된다.

- 프로젝트: `project1`
- 계정:
  - `admin`
  - `PL1`, `PL2`
  - `dev1` ~ `dev10`
  - `tester1` ~ `tester5`
- 추천 기능 검증용 `resolved`/`closed` 이슈 일부

### 1.5 이슈 관리 기능

구현된 주요 기능은 다음과 같다.

- 계정 추가
- 프로젝트 추가
  - admin 계정의 `Reports & Admin` 탭에서 `Add Project` 가능
- 이슈 등록
  - tester 계정의 `New Issue` 탭에서 프로젝트 선택 가능
  - `title`, `description` 필수
  - `reporter` 저장
  - `reported date` 자동 저장
  - 기본 우선순위 `MAJOR`
  - 기본 상태 `NEW`
- 이슈 브라우즈
- 이슈 검색
  - 검색어
  - reporter
  - assignee
  - status
  - 데모용 quick filter: All, NEW, Assigned to Me, Reported by Me, FIXED, RESOLVED
- 이슈 상세 정보 확인
  - 제목, 설명, reporter, reported date, priority, status, assignee, fixer, comments
- 코멘트 추가
  - 작성자
  - 작성 시각
  - 메시지
  - 누적 history 보존
- 이슈 배정
  - PL이 dev 계정에 배정
  - 상태가 `ASSIGNED`로 변경
- 이슈 수정 완료 처리
  - dev 계정이 `FIXED`로 변경
  - fixer 자동 저장
- 상태 변경
  - `NEW`
  - `ASSIGNED`
  - `FIXED`
  - `RESOLVED`
  - `CLOSED`
  - `REOPENED`

### 1.6 통계 기능

`IssueService.statistics()`에서 일별/월별 이슈 발생 수를 집계한다.

Swing UI의 `Stats` 버튼으로 확인할 수 있다.

### 1.7 Assignee 자동 추천 기능

`resolved` 또는 `closed` 상태의 기존 이슈 이력을 활용하여 담당자를 추천한다.

현재 구현 방식:

1. 대상 이슈의 title/description에서 3글자 이상 토큰 추출
2. resolved/closed 이슈의 title/description과 공통 토큰 수 계산
3. 점수가 높은 기존 이슈의 fixer를 상위 3명까지 추천

구현 위치:

```text
src/main/java/its/service/IssueService.java
```

문서에는 Information Retrieval 기반 유사도 추천 방식으로 설명하면 된다.

### 1.8 두 가지 UI

요구사항의 “두 개 이상의 UIToolKit” 조건을 맞추기 위해 Swing과 AWT UI를 구현했다.

Swing UI:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/run-swing.ps1
```

AWT UI:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/run-awt.ps1
```

두 UI 모두 동일한 controller/service/model/repository를 재사용한다.

Swing UI는 Trac의 ticket browser/detail 화면을 참고하여 다음 구조로 개선했다.

- 왼쪽: ticket query 필터와 ticket 목록
- 오른쪽: ticket 상세 화면
- 상세 화면: ticket 제목, properties, description, change history 영역 분리
- 하단: ticket actions 버튼 그룹

### 1.9 로그인 기능

Swing UI 시작 시 아이디/비밀번호 입력 기반 로그인 다이얼로그가 먼저 표시되도록 구현했다.

- 앱 시작 시 username/password 직접 입력 후 로그인
- 데모 계정의 공통 비밀번호는 `1234`
- 로그인 취소 시 앱 종료
- 로그인 성공 전에는 메인 화면을 표시하지 않음
- 실행 중 `Switch User` 버튼으로 다른 계정 로그인
- 로그인 상태 라벨 표시
- 로그인 전에는 Browse 탭만 표시
- 로그인 후에는 role에 따라 사용할 수 있는 탭과 버튼만 표시
- 이슈 생성 시 reporter는 로그인한 계정으로 자동 저장
- 댓글 작성자는 로그인한 계정으로 자동 저장
- assign/fix/status 변경 actor도 로그인한 계정으로 자동 처리
- `Add User`, `Add Project`는 admin 계정만 가능
- `Assign`, `Close`는 PL 계정만 가능
- `Fix`는 dev 계정만 가능
- `New Issue`, `Resolve`, `Reopen`은 tester 계정에서 표시
- `Recommend Assignee`는 PL 계정에서 표시
- `Stats`는 admin/PL 계정에서 표시
- 추천 결과와 통계 결과는 `Reports & Admin` 탭 안의 결과 영역에 표시

### 1.10 테스트

모델/서비스 흐름 검증용 테스트 하네스를 작성했다.

```text
src/test/java/its/IssueServiceTest.java
```

실행:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/test.ps1
```

검증된 내용:

- 프로젝트/계정 생성
- 이슈 생성
- reporter 자동 저장
- 상태 전이
- 댓글 누적
- fixer 저장
- 검색
- 통계 집계

현재 실행 결과:

```text
IssueServiceTest passed
```

## 2. 실행 방법

### 2.1 컴파일

```powershell
powershell -ExecutionPolicy Bypass -File scripts/compile.ps1
```

### 2.2 테스트

```powershell
powershell -ExecutionPolicy Bypass -File scripts/test.ps1
```

### 2.3 Swing UI 실행

```powershell
powershell -ExecutionPolicy Bypass -File scripts/run-swing.ps1
```

### 2.4 AWT UI 실행

```powershell
powershell -ExecutionPolicy Bypass -File scripts/run-awt.ps1
```

## 3. 내가 해야 하는 작업

### 3.1 GitHub 관련

- GitHub 저장소 주소를 README에 최종 반영해야 한다.
- 팀원별 commit history가 평가 대상이므로, 이후 작업은 팀원별 계정으로 나누어 커밋하는 것이 좋다.
- GitHub 프로젝트 progress history 화면을 캡처해 프로젝트 문서에 넣어야 한다.

### 3.2 프로젝트 문서 PDF 작성

최종 제출용 프로젝트 문서는 60 page 이내 PDF 1개로 작성해야 한다.

반드시 포함할 내용:

- 표지
  - 팀원 학번
  - 팀원 이름
- 프로젝트 내용 요약
  - 구현 완료 기능
  - 누락되거나 제한된 기능이 있으면 명시
- 요구 정의 및 분석
  - 전체 유스케이스 다이어그램
  - include 관계 2개 이상
  - extend 관계 2개 이상
  - 유스케이스 명세 6개
  - 도메인 모델
  - SSD 2개
  - Operation Contract 2개 이상
- 설계
  - 클래스 다이어그램
  - 시퀀스 다이어그램
  - MVC 구조 설명
  - GRASP 패턴 적용 설명
  - OO 설계 원칙 적용 이유
- 구현 결과
  - 주요 화면 캡처
  - 기능별 실행 설명
  - 두 UI가 같은 로직을 재사용한다는 설명
- 테스트 수행 내역
  - 테스트 목적
  - 테스트 코드 설명
  - 테스트 실행 결과 캡처
- GitHub 프로젝트 활용 요약
  - GitHub 주소
  - commit history
  - 팀원별 기여 내용

### 3.3 UML 작성

문서에 들어갈 UML을 별도로 작성해야 한다.

권장 다이어그램:

- Use Case Diagram
- Domain Model
- System Sequence Diagram 2개
- Operation Contract 2개 이상
- Class Diagram
- Sequence Diagram

클래스 다이어그램에는 최소한 아래 계층 관계를 보여주면 좋다.

- UI: `SwingIssueApp`, `AwtIssueApp`
- Controller: `IssueController`
- Service: `IssueService`
- Repository: `IssueRepository`, `FileIssueRepository`
- Model: `Issue`, `Comment`, `UserAccount`, `Project`

### 3.4 발표 슬라이드 작성

발표 슬라이드에는 다음 흐름을 추천한다.

1. 프로젝트 개요
2. 요구사항 요약
3. 아키텍처
4. 주요 도메인 모델
5. 핵심 유스케이스
6. 두 UI 구현 방식
7. 영속 저장 방식
8. assignee 추천 알고리즘
9. 테스트 전략
10. 데모 시나리오
11. 팀원별 기여

### 3.5 데모 영상 제작

30분 이내 영상에 포함해야 할 내용:

- 설계 설명
- 구현 설명
- 테스트 설명
- 실제 실행 데모

데모 시나리오는 과제 PDF의 예제 흐름을 그대로 따라가는 것이 좋다.

1. `tester1`이 이슈 생성
2. `tester1`이 코멘트 추가
3. `PL1`이 new 이슈 검색
4. `PL1`이 `dev1`에게 assign
5. `dev1`이 assigned 이슈 검색
6. `dev1`이 코멘트 추가 후 fixed 처리
7. `tester1`이 fixed 이슈를 resolved 처리
8. `PL1`이 resolved 이슈를 closed 처리
9. `PL2`가 new 이슈에서 추천 기능 확인

### 3.6 JUnit 보완 권장

현재 테스트는 외부 빌드 도구가 없는 환경을 고려해 `main` 메서드 기반 테스트 하네스로 작성했다.

과제 제출물에는 JUnit 테스트 코드가 명시되어 있으므로, 제출 전 시간이 있으면 아래 중 하나를 권장한다.

- Maven 또는 Gradle 추가
- JUnit 5 의존성 추가
- `IssueServiceTest`를 JUnit 형식으로 변환

### 3.7 UI 보완 권장

현재 기능 데모 중심의 UI이므로, 최종 발표 전에 아래를 보완하면 좋다.

- 상태 전이 권한 검증 강화
- priority 선택 UI
- 통계 그래프 시각화
- AWT UI의 입력 기능 확대

## 4. 제출 전 체크리스트

- [ ] README의 GitHub 주소 갱신
- [ ] 팀원 정보 입력
- [ ] 프로젝트 문서 PDF 작성
- [ ] 발표 슬라이드 작성
- [ ] 소개/데모 영상 제작
- [ ] JUnit 테스트 코드 보완
- [ ] Swing UI 로그인 화면 및 실행 화면 캡처
- [ ] Recommendation Result 및 Statistics Result 화면 캡처
- [ ] AWT UI 실행 캡처
- [ ] 테스트 실행 결과 캡처
- [ ] GitHub commit history 캡처
- [ ] 제출 zip 파일명 확인

제출 zip 파일명 형식:

```text
팀번호_팀원이름1_팀원이름2_팀원이름3_팀원이름4.zip
```
