# 선착순 쿠폰 발급 시스템

대용량 트래픽 환경의 선착순 쿠폰 발급 시스템을 밑바닥부터 만들며,
**동시성 문제를 직접 재현하고 → 해결책을 단계적으로 적용하고 → 수치로 검증**하는 학습 프로젝트입니다.

모든 단계는 브랜치로 남아 있어, 시스템이 "왜 이 아키텍처에 도달했는지"를 커밋 히스토리로 따라갈 수 있습니다.

## 기술 스택

| 분류 | 기술 |
|---|---|
| Language / Framework | Java 17, Spring Boot 3.2.5, Spring Data JPA |
| Database | MySQL 8.0 |
| In-Memory Store | Redis 7.2 |
| Message Broker | Apache Kafka 3.7 (KRaft mode) |
| Auth | JWT (jjwt), BCrypt |
| Infra | Docker Compose |
| Test | JUnit 5, Mockito, AssertJ, Awaitility |

## 최종 아키텍처

```
                   ┌─────────────────────── API 서버 ───────────────────────┐
사용자 ── POST /issue ─→  Redis 원자 연산 판정        Kafka 발행      202 응답
                   │   (SADD 중복체크 + INCR 재고)  (발급 확정 기록)          │
                   └────────────────────────────────┬───────────────────────┘
                                                    │ coupon-issue topic (3 partitions)
                                                    ▼
                                              Kafka Consumer
                                                    │ DB INSERT (유니크 제약 = 최후 방어선)
                                                    ▼
                                                  MySQL
```

- **요청 경로에서 DB 제거**: 중복/재고 판정은 Redis 단일 스레드의 원자 명령으로 처리
- **발급 확정은 Kafka에 먼저 기록**: 서버가 죽어도 메시지가 남아 컨슈머가 이어서 처리 (최종적 일관성)
- **DB 유니크 제약 `(coupon_id, user_id)`**: 메시지 중복 소비에도 안전한 멱등성의 최후 방어선

## 동시성 문제 해결 여정

재고 100장 쿠폰에 동시 요청 1,000건(스레드 풀 32)을 꽂는 테스트로 각 단계를 검증했습니다.

| 단계 | 브랜치 | 방식 | 결과 |
|---|---|---|---|
| 문제 재현 | `3-concurrency` | 무방비 (check-then-act) | ❌ 129장 발급, 카운터 63 (lost update), 데드락 871건 |
| 해결 1 | `4-synchronized` | `synchronized` 메서드 | ❌ 정확히 2배(200장) — 락 해제와 커밋 사이 틈 |
| 해결 1' | `4-synchronized` | `synchronized` 파사드 (커밋까지 잠금) | ✅ 정합성 확보 — 단, 단일 JVM 한정 + 전역 직렬화 |
| 해결 2 | `5-pessimistic-lock` | DB 비관적 락 (`SELECT FOR UPDATE`) | ✅ **8,544ms** — 탈락자까지 전원 행 락에 직렬화 |
| 해결 3 | `6-optimistic-lock` | DB 낙관적 락 (`@Version` + 재시도) | ✅ **7,257ms** (재시도 927회) — 소진 후 거절은 병렬 통과 |
| 해결 4 | `7-redis` | Redis 원자 연산 (SADD + INCR) | ✅ **2,408ms** — 락/재시도 없음, 도착순 공정성 |
| 해결 5 | `8-kafka` | Redis 판정 + Kafka 비동기 저장 | ✅ API 응답과 DB 반영 분리 (측정값은 docs 참고) |

### 단계별로 배운 것

- **무방비**: "읽고 → 판단하고 → 쓰는" 구간이 원자적이지 않으면 초과 발급, lost update,
  FK 공유락(S) ↔ 배타락(X) 데드락이 동시에 터진다.
- **synchronized**: 자물쇠 범위가 트랜잭션 커밋보다 짧으면 뚫린다. 커밋까지 감싸도
  서버 2대가 되는 순간 JVM마다 자물쇠가 따로 생겨 무용지물.
- **비관적 락**: 읽는 순간부터 X락. 락 해제 시점 = 커밋 시점이라 안전하지만,
  락 대기가 DB 커넥션을 점유한 채 이뤄져 커넥션 풀 고갈 → 장애 전파 위험.
- **낙관적 락**: 충돌이 드물면 최고, 상시면 재시도 폭풍. 실측에서는 "소진 후 거절"이
  병렬로 빠져나가는 워크로드 특성 덕에 비관적 락보다 빨랐다 — 락 선택은 워크로드 모양에 달렸다.
- **Redis**: 경쟁 지점을 단일 스레드 원자 명령으로 옮기면 락 자체가 필요 없어진다.
  INCR 반환값이 도착순 순번이라 재시도 복권으로 인한 순서 역전(공정성 문제)도 해소.
- **Kafka**: Redis와 DB는 한 트랜잭션으로 묶을 수 없다. "발급 확정"을 유실되지 않는
  로그에 먼저 기록하고 DB 반영은 컨슈머가 재시도하는 구조로 정합성 공백을 메운다.

상세 실험 기록: [docs/03-concurrency-notes.md](docs/03-concurrency-notes.md) ·
[docs/06-lock-comparison.md](docs/06-lock-comparison.md) ·
[docs/concurrency-problems.html](docs/concurrency-problems.html)

## 브랜치 가이드

| 브랜치 | 내용 |
|---|---|
| `main` | 프로젝트 기본 설정 + 문서 |
| `1-user` | 회원가입 / 로그인(JWT) / 인증 인터셉터 + `@LoginUser` |
| `2-coupon` | 쿠폰 도메인 (생성/발급/조회/사용), 전역 예외 처리 |
| `3-concurrency` ~ `8-kafka` | 위 여정 표 참고 |

## API

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| POST | `/api/users/signup` | 회원가입 | - |
| POST | `/api/auth/login` | 로그인 → AccessToken 발급 | - |
| GET | `/api/users/me` | 내 정보 | ✅ |
| POST | `/api/coupons` | 쿠폰 생성 | ✅ |
| POST | `/api/coupons/{id}/issue` | 쿠폰 발급 (202 Accepted, 비동기) | ✅ |
| GET | `/api/coupons/my` | 내 쿠폰 목록 | ✅ |
| POST | `/api/coupons/issued/{id}/use` | 쿠폰 사용 | ✅ |

요청 예시는 [http/](http) 디렉토리의 IntelliJ HTTP Client 파일 참고.

## 실행 방법

```bash
# 1. 인프라 기동 (MySQL, Redis, Kafka)
docker compose up -d

# 2. 애플리케이션 실행
./gradlew bootRun

# 3. 테스트
./gradlew test              # 단위/슬라이스 테스트
./gradlew concurrencyTest   # 동시성 검증 (인프라 기동 필요)
```

## 앞으로 할 것

- [ ] 컨슈머 장애 처리: 재시도 전략, DLQ(Dead Letter Queue), 수동 ack
- [ ] 컨슈머 애플리케이션 분리 및 Scale-out (파티션-컨슈머 리밸런싱 관찰)
- [ ] 모니터링: Prometheus + Grafana (TPS, P95, Consumer Lag, HikariCP)
- [ ] k6 부하 테스트 및 병목 분석 → 튜닝 사이클
- [ ] 멀티모듈 분리 (api / consumer / domain / infra)
