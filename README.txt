소프트웨어공학 프로젝트 제출 산출물 README

1. 제출 산출물 목록 및 요약

1) SoftwareEngineering/
   - Java 기반 이슈 관리 시스템 소스코드와 Gradle 프로젝트 파일을 포함합니다.
   - 주요 구성:
     - src/main/java/model: Issue, UserAccount, Project, Comment 등 도메인 모델
     - src/main/java/repository: SQLite 기반 데이터 저장소
     - src/main/java/service: 이슈 생성/조회/상태 변경, 사용자 관리, 프로젝트 관리, 담당자 추천, 통계 기능
     - src/main/java/controller: UI와 서비스 계층을 연결하는 컨트롤러
     - src/main/java/ui/swing: Swing 기반 실행 UI
     - src/main/java/ui/awt: AWT 기반 실행 UI
     - src/test/java/service: JUnit 5 기반 서비스 테스트
     - docs: UI 확인용 체크리스트 문서
   - SQLite 데이터베이스는 실행 시 data/its.db 경로에 생성/사용됩니다.

2) 보고서 최종 희망 사항.docx
   - 프로젝트 최종 보고서입니다.
   - 요구사항, 설계, 구현 내용, 테스트 및 프로젝트 결과를 정리한 문서입니다.

3) 소공_발표_최종완성 (1).pptx
   - 프로젝트 최종 발표 자료입니다.
   - 시스템 개요, 핵심 기능, 설계 및 구현 결과를 발표용으로 요약한 자료입니다.

4) 소공 비디오.mp4
   - 프로젝트 설명 영상입니다.


5) README.txt
   - 제출 산출물 목록, 요약, GitHub 주소, 프로그램 실행 방법을 정리한 파일입니다.


2. GitHub 주소

https://github.com/david010420/SoftwareEngineering


3. 프로그램 개요

본 프로그램은 Java로 구현한 이슈 관리 시스템입니다. 사용자는 역할에 따라 로그인한 뒤 프로젝트와 이슈를 관리할 수 있습니다. 주요 기능은 다음과 같습니다.

- 사용자 로그인 및 역할별 기능 제한
- 관리자 계정의 사용자 및 프로젝트 관리
- Tester의 이슈 등록
- PL의 이슈 담당자 배정
- Developer의 이슈 처리 및 상태 변경
- 이슈 검색, 필터링, 상세 조회
- 댓글 및 변경 이력 관리
- 일/월 단위 이슈 통계
- 기존 해결 이슈 기반 담당자 추천
- Swing UI 및 AWT UI 제공
- SQLite 기반 데이터 영속 저장


4. 실행 환경

- 운영체제: Windows 기준
- Java: JDK 17 이상 권장
- 빌드 도구: Gradle Wrapper 포함
- 외부 라이브러리:
  - SQLite JDBC
  - JUnit 5
  - Mockito

별도로 Gradle을 설치하지 않아도 SoftwareEngineering 폴더에 포함된 gradlew.bat 파일로 빌드 및 실행할 수 있습니다.


5. 프로그램 실행 방법

1) 명령 프롬프트 또는 PowerShell을 실행합니다.

2) 프로젝트 코드 폴더로 이동합니다.

   cd "3_이수용_권영욱_차현준\SoftwareEngineering"

3) 프로젝트를 빌드합니다.

   .\gradlew.bat build

4) Swing UI로 실행합니다.

   .\gradlew.bat runSwing

5) AWT UI로 실행하려면 다음 명령을 사용합니다.

   .\gradlew.bat runAWT

6) 테스트만 실행하려면 다음 명령을 사용합니다.

   .\gradlew.bat test


6. 기본 계정

프로그램 최초 실행 시 기본 계정이 생성됩니다. 모든 기본 계정의 비밀번호는 1234입니다.

- admin: 관리자 계정
- PL1, PL2: 프로젝트 리더 계정
- dev1 ~ dev10: 개발자 계정
- tester1 ~ tester5: 테스터 계정
