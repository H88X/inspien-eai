# Inspien EAI 연계 프로젝트

## 프로젝트 개요
주문 데이터 처리 과정에서 발생할 수 있는 수작업 의존성과 
시스템 간 데이터 불일치 문제를 해결하기 위해 EAI 기반 연계 시스템을 설계 및 구현

## 아키텍처
<img width="1942" height="798" alt="EAI_아키텍처_서민기" src="https://github.com/user-attachments/assets/8cb9656c-c33a-4f36-967d-d3a79d54266c" />


## 기술 스택
- Language : Java
- Persistence : MyBatis
- Database : Oracle
- Integration : REST API, SFTP
- Scheduler : Spring Scheduler
- Logging : Logback
- Version Control : Git

- ## 주요 기능

### 주문 연계 
- 주문 데이터 수신
- HEADER / ITEM 매핑 처리
- ORDER_DB 저장
- SFTP 서버 파일 전송

### 출고 처리 (배치)
- Scheduler 기반 주기적 실행
- ORDER → SHIPMENT 데이터 변환
- 상태값 업데이트
