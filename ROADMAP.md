# 나만의 쿠폰 시스템 학습 로드맵

> 참고: [devFancy/springboot-coupon-system](https://github.com/devFancy/springboot-coupon-system) · [성능 개선기 블로그](https://devfancy.github.io/spring-boot-coupon-system-performance-improvement/)
>
> 스택: Java 17 · Spring Boot 3.2.x · Gradle · MySQL · Redis · Kafka · Docker
> 방식: 단일 모듈로 시작 → 필요해지는 시점에 분리 (원본 프로젝트도 이렇게 진화함)

각 단계마다 "이 단계의 질문"에 스스로 답할 수 있으면 다음으로 넘어가세요.
원본 레포의 브랜치를 참고 답안처럼 활용할 수 있게 매핑해뒀습니다.

---

## Phase 0. 프로젝트 뼈대 (반나절)

- [ ] `F:\coupon-system`에서 `git init`, `.gitignore` 작성
- [ ] Spring Initializr로 프로젝트 생성
  - Java 17, Spring Boot 3.2.x, Gradle
  - 의존성: Web, Spring Data JPA, MySQL Driver, Validation, Lombok, Actuator
- [ ] `docker-compose.yml` 작성 — MySQL 8.0 하나만 (Redis/Kafka는 필요해질 때 추가)
- [ ] `GET /health` 헬스체크 API + DB 연결 확인 (`application.yml`)
- [ ] 첫 커밋

**이 단계의 질문**: 애플리케이션이 뜨고, DB에 붙고, 커밋이 남았는가?

---

## Phase 1. 도메인 기본기 — User / Auth / Coupon (1~2주)

원본 브랜치: `feat/1-user-signup` → `feat/6-user-login-with-password-hashing` → `feat/9-auth-login-accessToken` → `feat/issue-11-coupon-creation-api` → `feat/issue-13-coupon-issuance-api` → `feat/issue-17-coupon-usage-api`

- [ ] User 회원가입 API (비밀번호는 BCrypt 해싱)
- [ ] 로그인 API → JWT AccessToken 발급
- [ ] 인터셉터(또는 Spring Security 필터) + ArgumentResolver로 토큰에서 userId 추출
- [ ] Coupon 생성 API (관리자용: 이름, 총수량, 유효기간)
- [ ] 쿠폰 발급 API — **일단 동기 방식**: JPA로 재고 확인 → 감소 → IssuedCoupon 저장
- [ ] 내 쿠폰 조회 / 쿠폰 사용 API
- [ ] 각 API 테스트 코드 (단위 + `@SpringBootTest`)

**이 단계의 질문**: API Gateway 없이 인증이 어떻게 완결되는가? 발급 로직의 트랜잭션 경계는 어디인가?

---

## Phase 2. 동시성 문제를 직접 터뜨리기 (1~2주) ★ 핵심

원본 브랜치: `refactor/coupon-issue-concurrency` → `refactor/issue-30-coupon-issue-HA-distributed-lock-for-redis` → `refator/coupon-issue-redis-set-incr` → `refactor/redis-duplicate-check-and-count`

- [ ] **문제 재현 테스트 먼저**: 재고 100개 쿠폰에 `ExecutorService`로 동시에 1,000건 발급 요청
  → 초과 발급(재고 음수), 중복 발급이 실제로 일어나는지 확인
- [ ] 해결책을 순서대로 적용하고 각각의 한계를 기록:
  1. `synchronized` — 왜 서버 2대면 무용지물인가?
  2. DB 비관적 락 (`SELECT ... FOR UPDATE`) — 처리량 병목은?
  3. DB 낙관적 락 (`@Version`) — 실패 재시도 비용은?
  4. Redis 분산락 (Redisson) — 락 자체가 병목이 되는 지점은?
  5. Redis INCR + SET 자료구조 — 락 없이 재고/중복을 해결하는 방식
- [ ] 각 방식의 처리 시간을 같은 테스트로 측정해서 표로 기록 (블로그 소재로 최고)

**이 단계의 질문**: 원본 프로젝트가 나중에 분산락을 **제거**한 이유를 내 언어로 설명할 수 있는가?

---

## Phase 3. Kafka 비동기 전환 (2주)

원본 브랜치: `feat/issue-15-async-coupon-issue-kafka` → `refactor/kafka-producer-and-consumer-*` → `refactor/issue-20-coupon-kafka-consumer-dlq` → `refactor/consumer-retry-strategy` → `hotfix/consumer-modify-ack-menual-process`

- [ ] docker-compose에 Kafka 추가 (KRaft 모드, 브로커 1개로 시작)
- [ ] 발급 API를 분리: Redis에서 재고/중복 검증 → Kafka 발행 → 즉시 응답
- [ ] Consumer가 메시지를 받아 DB에 IssuedCoupon 저장
- [ ] DB에 `(coupon_id, user_id)` **유니크 제약** 추가 — 멱등성의 최후 방어선
- [ ] Consumer를 별도 스프링부트 애플리케이션으로 분리 (이 시점에 멀티모듈 전환 고려)
- [ ] 실패 처리: 재시도 전략 → DLQ(Dead Letter Queue) → 수동 ack
- [ ] Consumer를 2개 띄우고 파티션 분배(리밸런싱)를 로그로 관찰

**이 단계의 질문**: Consumer가 같은 메시지를 두 번 받아도 안전한 이유는? 파티션 수와 Consumer 수의 관계는?

---

## Phase 4. 모니터링 (1주)

원본 브랜치: `infra/2-add-monitoring` → `feature/kafka-prometheus-dashboard` → `feat/logging-add-globalTraceId`

- [ ] docker-compose에 Prometheus + Grafana 추가
- [ ] Actuator + Micrometer로 메트릭 노출, Prometheus 수집 설정
- [ ] Grafana 대시보드: TPS, 응답시간(P95), JVM 힙, HikariCP 활성 커넥션, Kafka Consumer Lag
- [ ] 요청별 traceId 로깅 (MDC) — API → Kafka → Consumer까지 이어지게
- [ ] (선택) Loki + Promtail로 로그 수집

**이 단계의 질문**: 부하를 걸었을 때 어디가 병목인지 대시보드만 보고 말할 수 있는가?

---

## Phase 5. 부하 테스트 & 성능 개선 (2~3주) ★ 블로그 본편

원본 브랜치: `infra/k6-test-coupon-issue` → `perf/issue-62-load-test-and-optimization` → `perf/issue-64-optimize-system-throughput-with-scale-up-and-out`

- [ ] k6 설치, 발급 API 시나리오 작성 (VU 500부터 시작해 단계적으로 증가)
- [ ] 목표 설정: 예) "VU 5,000에서 P95 3초 이내, 에러율 0%"
- [ ] docker-compose에 `cpus: 2`, `mem_limit: 4g`로 컨테이너별 리소스 제한 (t3.medium 모사)
- [ ] 병목 찾기 → 개선 → 재측정 사이클:
  - Resilience4j Rate Limiter 도입
  - Tomcat 스레드 풀 / HikariCP 풀 크기 조정 (늘리는 게 항상 답이 아님을 확인)
  - Kafka 파티션 수 / Consumer 스레드 조정
  - Consumer 애플리케이션 2개로 Scale-out
- [ ] 개선 전후 수치를 모두 기록 — 절대값이 아니라 **변화량**이 핵심
- [ ] (선택) Kafka 브로커 2개 이중화 — replication, ISR, 리더 선출 실험

**이 단계의 질문**: "스레드를 늘렸는데 왜 느려졌는가?"에 답할 수 있는가?

---

## Phase 6. (선택) 확장

- [ ] 멀티모듈 정리: api / consumer / domain / infra + support(logging, monitoring)
- [ ] Flyway로 DB 마이그레이션 관리
- [ ] AWS에서 최종 검증 (스팟 인스턴스, 테스트 후 즉시 종료 — 몇천 원이면 충분)
- [ ] 각 Phase의 기록을 블로그 글로 정리

---

## 진행 규칙

1. **브랜치 전략**: 원본처럼 `feat/`, `refactor/`, `perf/` 프리픽스로 기능 단위 브랜치 → main 머지
2. **문제를 먼저 터뜨리고 해결한다**: 동시성이든 성능이든, 재현 테스트 없이 해결책부터 넣지 않기
3. **수치로 기록한다**: 개선 전/후를 항상 같은 조건에서 측정
4. **막히면 원본 브랜치를 컨닝한다**: 단, 보고 베끼지 말고 이해한 뒤 내 코드로 다시 작성
