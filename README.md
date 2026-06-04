# Issue Tracking System

소프트웨어공학 텀 프로젝트 — Java 기반 이슈 관리 시스템

## 요구 환경

- Java 17 이상
- Gradle (gradlew 스크립트 포함)



## 기본 계정

최초 실행 시 아래 데모 계정이 자동 생성된다. 공통 비밀번호는 `1234`.

| 계정 | 역할 |
|---|---|
| `admin` | 관리자 |
| `PL1`, `PL2` | Project Leader |
| `dev1` ~ `dev10` | 개발자 |
| `tester1` ~ `tester5` | 테스터 |

## 주요 기능

- **계정 관리**: admin이 계정 추가 (admin / PL / dev / tester 역할)
- **프로젝트 관리**: admin이 프로젝트 추가
- **이슈 등록**: tester가 제목·설명 입력, reporter/등록일시 자동 저장
- **이슈 검색**: 검색어, reporter, assignee, status 조건 조합 필터
- **이슈 배정**: PL이 dev에게 배정 → 상태 `ASSIGNED` 자동 전환
- **상태 변경**: `NEW → ASSIGNED → FIXED → RESOLVED → CLOSED / REOPENED`
- **코멘트**: 작성자·시각 포함 누적 이력 보관
- **통계**: 일별/월별 이슈 발생 수 집계 (Reports & Admin 탭)
- **담당자 추천**: resolved/closed 이슈 제목·설명 유사도 기반 상위 3명 추천

## 아키텍처

MVC 구조로 UI와 비즈니스 로직을 분리한다.

```
ui (Swing / AWT)
    ↓
controller (IssueController / UserController / ProjectController)
    ↓
service (IssueService / UserServiceImpl / TFRecommendService / ...)
    ↓
repository (SqliteIssueRepository / SqliteUserRepository / ...)
    ↓
data/its.db  ← SQLite 영속 저장
```



두 UI(`SwingIssueApp`, `AwtIssueApp`)는 동일한 controller/service/model/repository를 재사용한다.

## 테스트

`src/test/java/service/` 아래에 JUnit 5 테스트가 있다.

- `IssueServiceTest` — 이슈 생성·상태 전이·검색·통계
- `UserServiceImplTest` — 로그인·계정 등록
- `ProjectServiceTest` — 프로젝트 생성·조회
- `TFRecommendServiceTest` — 담당자 추천 알고리즘

## 영속 저장

SQLite에 이슈·계정·프로젝트 데이터를 저장한다. 앱 재시작 후에도 데이터가 유지된다.

## GitHub

https://github.com/david010420/SoftwareEngineering
