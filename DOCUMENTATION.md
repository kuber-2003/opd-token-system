# OPD Token Allocation Engine - Technical Documentation

## Table of Contents
1. [Overview](#overview)
2. [Algorithm Design](#algorithm-design)
3. [Prioritization Logic](#prioritization-logic)
4. [Edge Cases & Solutions](#edge-cases--solutions)
5. [Failure Handling](#failure-handling)
6. [API Reference](#api-reference)
7. [Production Recommendations](#production-recommendations)

---

## Overview

The OPD Token Allocation Engine is a dynamic, priority-based system designed for hospital outpatient departments. It manages patient queues across multiple doctors and time slots with elastic capacity management.

### Key Features
- **Multi-source token allocation** with configurable priorities
- **Dynamic reallocation** when capacity constraints change
- **Real-time queue management** with automatic priority updates
- **Comprehensive state management** tracking token lifecycle
- **RESTful API** with validation and error handling

### Technology Stack
- **Framework**: Spring Boot 3.2.1
- **Language**: Java 17
- **API Documentation**: SpringDoc OpenAPI 3 (Swagger)
- **Build Tool**: Maven
- **Architecture**: Service-oriented with in-memory storage

---

## Algorithm Design

### 1. Token Allocation Algorithm

The core allocation algorithm follows these steps:

```
FUNCTION allocateToken(patientId, doctorId, source, preferredTime):
    1. Find best available slot:
       - Filter slots by doctorId
       - Filter slots that are active and future/ongoing
       - For non-emergency: only consider slots with capacity
       - Calculate score for each slot based on:
         * Time proximity to preferred time
         * Current capacity utilization
       - Select slot with minimum score
    
    2. Create token with:
       - Generated unique ID
       - Sequential token number
       - Source-based priority
       - ALLOCATED status
    
    3. Increment slot occupancy
    
    4. Return allocated token
```

**Time Complexity**: O(n log n) where n is the number of slots
**Space Complexity**: O(1) for allocation operation

### 2. Emergency Token Allocation

Emergency tokens follow a special path:

```
FUNCTION allocateEmergencyToken(patientId, doctorId, notes):
    1. Find current or next immediate slot for doctor
    
    2. Create emergency token (priority 1000+)
    
    3. Add to slot (can exceed capacity)
    
    4. If slot now overcapacity:
       CALL reallocateOverflowTokens(slot)
    
    5. Return emergency token
```

### 3. Overflow Reallocation Algorithm

When a slot exceeds capacity (typically after emergency admission):

```
FUNCTION reallocateOverflowTokens(overflowSlot):
    1. Calculate overflow count:
       overflow = currentOccupancy - maxCapacity
    
    2. Find candidates for reallocation:
       - Filter tokens in overflow slot
       - Filter only reallocatable tokens (non-emergency, active status)
       - Sort by dynamic priority (lowest first)
       - Take top 'overflow' count tokens
    
    3. For each candidate token:
       a. Find next available slot after current slot
       b. If found:
          - Decrement source slot occupancy
          - Increment target slot occupancy
          - Update token's slotId
          - Mark as REALLOCATED
          - Increment reallocation counter
       c. If not found:
          - Log error for manual intervention
    
    4. Return list of reallocated tokens
```

**Reallocation Strategy**: Minimizes patient disruption by:
- Only moving lowest priority patients
- Never moving emergency cases
- Preferring nearest future slot
- Limiting reallocations per token (tracked)

---

## Prioritization Logic

### Base Priority System

| Token Source | Base Priority | Description | Use Case |
|--------------|---------------|-------------|----------|
| EMERGENCY | 1000+ | Highest priority, can exceed capacity | Life-threatening cases |
| PAID_PRIORITY | 500 | Premium service patients | VIP/Corporate packages |
| ONLINE_BOOKING | 300 | Pre-scheduled appointments | Advance bookings |
| FOLLOW_UP | 200 | Return visits | Post-treatment checkups |
| WALK_IN | 100 | Same-day registrations | Regular OPD patients |

### Dynamic Priority Calculation

Once a patient checks in, their priority increases over time:

```java
dynamicPriority = basePriority + (waitTimeMinutes × 0.5)
```

**Example Scenario:**

```
Time 9:00 AM:
- Token #1 (WALK_IN, priority 100)
- Token #2 (ONLINE, priority 300)
- Token #3 (PAID_PRIORITY, priority 500)

Token #1 checks in at 9:00 AM
Token #2 checks in at 9:15 AM
Token #3 checks in at 9:25 AM

Time 9:30 AM priorities:
- Token #1: 100 + (30 × 0.5) = 115
- Token #2: 300 + (15 × 0.5) = 307.5
- Token #3: 500 + (5 × 0.5) = 502.5

Queue order: #3, #2, #1
```

This ensures:
1. Paid patients get priority initially
2. Long-waiting patients eventually gain priority
3. System balances revenue and fairness

### Slot Selection Scoring

When allocating tokens, slots are scored based on:

```java
slotScore = timeProximityMinutes + capacityUtilizationPercentage
```

Lower score = better match. This means:
- Prefer slots close to preferred time
- Prefer less crowded slots
- Balance time convenience and queue load

---

## Edge Cases & Solutions

### 1. Slot Completely Full + Emergency Arrives

**Scenario**: All slots at capacity, emergency patient arrives

**Solution**:
```
1. Allocate emergency token to current/next slot (exceeds capacity)
2. Identify overflow count
3. Find lowest priority non-emergency tokens in that slot
4. Reallocate them to next available slots
5. Notify affected patients of new time
```

**Implementation**: Automatic via `reallocateOverflowTokens()`

### 2. Multiple Simultaneous Emergencies

**Scenario**: Multiple emergency patients arrive in quick succession

**Solution**:
```
1. Each emergency gets allocated to current/next slot
2. Each triggers separate reallocation
3. Reallocation is synchronized to prevent race conditions
4. Lower priority patients may be reallocated multiple times
5. System tracks reallocation count per token
```

**Safeguard**: Limit maximum reallocations per token (configurable)

### 3. Mass Cancellations Create Empty Slots

**Scenario**: Multiple patients cancel, leaving slots underutilized

**Solution**:
```
1. Each cancellation decrements slot occupancy
2. System maintains accurate capacity tracking
3. Empty slots become available for new allocations
4. No active reallocation needed (patients stay in chosen slots)
```

**Optimization**: Could implement "compact queue" feature to consolidate patients into earlier slots

### 4. Doctor Running Late

**Scenario**: Doctor delayed, current slot capacity must be reduced

**Solution**:
```
1. Call adjustSlotCapacity(slotId, reducedCapacity)
2. If new capacity < current occupancy:
   - Automatically trigger reallocateOverflowTokens()
3. Affected patients notified of new slot
```

**Implementation**: Via `PUT /api/slots/{slotId}/capacity` endpoint

### 5. No Available Slots Found

**Scenario**: All future slots for doctor are full, new booking arrives

**Solution**:
```
1. Allocation attempt returns null for bestSlot
2. Throw SlotCapacityExceededException
3. API returns 409 CONFLICT with message
4. Frontend can:
   - Suggest alternative doctor
   - Offer waitlist registration
   - Propose different date
```

### 6. Invalid State Transitions

**Scenario**: Attempt to start consultation on cancelled token

**Solution**:
```
1. Each state change validates current status
2. Throw InvalidTokenStateException with clear message
3. API returns 400 BAD REQUEST
4. Valid transitions defined:
   ALLOCATED → CHECKED_IN → IN_CONSULTATION → COMPLETED
   ALLOCATED → CANCELLED
   ALLOCATED/CHECKED_IN → NO_SHOW
```

**Enforcement**: State machine logic in Token model methods

### 7. Duplicate Patient Bookings

**Scenario**: Patient attempts to book multiple tokens for same doctor/day

**Solution** (Production Enhancement):
```
1. Before allocation, check existing tokens
2. Filter by: patientId, doctorId, date, active status
3. If found: reject with "existing booking" error
4. Allow if previous booking is cancelled/completed
```

**Current**: Not enforced in demo (would add in production)

### 8. Token Allocated but Slot Deleted

**Scenario**: Admin deletes slot that has active tokens

**Solution** (Production Enhancement):
```
1. Before slot deletion, check for active tokens
2. If found: prevent deletion, return error
3. Or: offer to reallocate all tokens first
4. Mark slot as inactive instead of deleting
```

**Current**: Soft deletion via `isActive` flag recommended

---

## Failure Handling

### 1. System Failures

| Failure Type | Detection | Response | Recovery |
|--------------|-----------|----------|----------|
| Slot not found | Token allocation/retrieval | SlotNotFoundException → 404 | Provide valid slot ID |
| Token not found | Any token operation | TokenNotFoundException → 404 | Verify token ID |
| Capacity exceeded | Normal allocation | SlotCapacityExceededException → 409 | Choose different time/doctor |
| Invalid state | State transitions | InvalidTokenStateException → 400 | Review valid transitions |
| Validation errors | Request body | MethodArgumentNotValidException → 400 | Fix request payload |

### 2. Concurrency Handling

**Thread Safety Measures**:
```java
// Synchronized methods for capacity changes
public synchronized void incrementOccupancy()
public synchronized void decrementOccupancy()

// ConcurrentHashMap for storage
private final Map<String, TimeSlot> slots = new ConcurrentHashMap<>();
private final Map<String, Token> tokens = new ConcurrentHashMap<>();

// Atomic counter for token numbers
private final AtomicInteger tokenCounter = new AtomicInteger(1);
```

### 3. Data Consistency

**Consistency Guarantees**:
1. **Slot occupancy always accurate**: Every allocation/cancellation updates count
2. **No orphaned tokens**: Token always references valid slot (unless slot deleted)
3. **Priority calculations current**: Recalculated on-demand, not cached
4. **State transitions valid**: Enforced by model methods

**Potential Issues in Production**:
- Lost updates if using plain database without transactions
- Race conditions if multiple servers access same data
- Stale reads if using caching without invalidation

**Solutions**:
- Use database transactions for multi-step operations
- Implement optimistic locking with version fields
- Use distributed locks (Redis) for critical sections
- Event sourcing for complete audit trail

### 4. Notification Failures

**Scenario**: Token reallocated but patient not notified

**Detection**:
```
- Log all reallocations with WARN level
- Track notification status per token
- Monitor failed notification queue
```

**Recovery**:
```
1. Retry notification with exponential backoff
2. Flag token as "notification pending"
3. Provide admin dashboard to see unnotified reallocations
4. Allow manual resend of notifications
```

### 5. Database Failures

**Current (In-Memory)**:
- All data lost on restart
- No persistence across sessions
- Acceptable for demo/testing

**Production**:
```
1. Primary database: PostgreSQL
   - Atomic transactions
   - ACID guarantees
   - Foreign key constraints

2. Caching layer: Redis
   - Queue state caching
   - Real-time statistics
   - Session management

3. Backup strategy:
   - Continuous replication
   - Point-in-time recovery
   - Daily snapshots
```

---

## API Reference

### Base URL
```
http://localhost:8080/api
```

### Slot Management Endpoints

#### 1. Create Slot
```http
POST /slots
Content-Type: application/json

{
  "doctorId": "DR001",
  "doctorName": "Dr. Rajesh Sharma",
  "department": "Cardiology",
  "startTime": "2024-02-01T09:00:00",
  "endTime": "2024-02-01T10:00:00",
  "maxCapacity": 10
}

Response: 201 CREATED
{
  "slotId": "uuid",
  "doctorId": "DR001",
  "doctorName": "Dr. Rajesh Sharma",
  "department": "Cardiology",
  "startTime": "2024-02-01T09:00:00",
  "endTime": "2024-02-01T10:00:00",
  "maxCapacity": 10,
  "currentOccupancy": 0,
  "isActive": true
}
```

#### 2. Get Slot
```http
GET /slots/{slotId}

Response: 200 OK
{slot object}
```

#### 3. Get All Slots
```http
GET /slots

Response: 200 OK
[{slot}, {slot}, ...]
```

#### 4. Get Doctor's Slots
```http
GET /slots/doctor/{doctorId}

Response: 200 OK
[{slot}, {slot}, ...]
```

#### 5. Adjust Slot Capacity
```http
PUT /slots/{slotId}/capacity
Content-Type: application/json

{
  "newCapacity": 8
}

Response: 200 OK
{updated slot}
```

### Token Management Endpoints

#### 1. Allocate Token
```http
POST /tokens
Content-Type: application/json

{
  "patientId": "PAT001",
  "patientName": "Ramesh Gupta",
  "doctorId": "DR001",
  "source": "ONLINE_BOOKING",
  "preferredTime": "2024-02-01T09:30:00",
  "notes": "Regular checkup"
}

Response: 201 CREATED
{
  "tokenId": "uuid",
  "patientId": "PAT001",
  "patientName": "Ramesh Gupta",
  "slotId": "uuid",
  "doctorId": "DR001",
  "source": "ONLINE_BOOKING",
  "status": "ALLOCATED",
  "tokenNumber": 1,
  "createdAt": "2024-02-01T08:00:00",
  "notes": "Regular checkup"
}
```

#### 2. Allocate Emergency Token
```http
POST /tokens/emergency
Content-Type: application/json

{
  "patientId": "PAT_EMG_001",
  "patientName": "Emergency Patient",
  "doctorId": "DR001",
  "notes": "Chest pain"
}

Response: 201 CREATED
{token object with EMERGENCY source}
```

#### 3. Get Token
```http
GET /tokens/{tokenId}

Response: 200 OK
{token object}
```

#### 4. Check-in Patient
```http
POST /tokens/{tokenId}/check-in

Response: 200 OK
{token with CHECKED_IN status and checkedInAt timestamp}
```

#### 5. Start Consultation
```http
POST /tokens/{tokenId}/start-consultation

Response: 200 OK
{token with IN_CONSULTATION status}
```

#### 6. Complete Consultation
```http
POST /tokens/{tokenId}/complete-consultation

Response: 200 OK
{token with COMPLETED status}
```

#### 7. Cancel Token
```http
DELETE /tokens/{tokenId}

Response: 200 OK
{token with CANCELLED status}
```

#### 8. Mark No-Show
```http
POST /tokens/{tokenId}/no-show

Response: 200 OK
{token with NO_SHOW status}
```

#### 9. Get Doctor Queue
```http
GET /tokens/queue/{doctorId}

Response: 200 OK
[
  {token sorted by priority, highest first},
  ...
]
```

#### 10. Get Statistics
```http
GET /tokens/statistics?doctorId=DR001

Response: 200 OK
{
  "totalTokens": 25,
  "activeTokens": 10,
  "completedTokens": 12,
  "cancelledTokens": 2,
  "noShowTokens": 1,
  "emergencyTokens": 1,
  "totalSlots": 3,
  "averageUtilization": 83.3
}
```

### Error Responses

All errors follow this format:
```json
{
  "status": 404,
  "message": "Token not found with ID: xyz",
  "timestamp": "2024-02-01T10:30:00"
}
```

**Status Codes**:
- `200 OK`: Successful operation
- `201 CREATED`: Resource created
- `400 BAD REQUEST`: Invalid input or state
- `404 NOT FOUND`: Resource not found
- `409 CONFLICT`: Capacity or business rule violation
- `500 INTERNAL SERVER ERROR`: Unexpected error

---

## Production Recommendations

### 1. Database Integration

**Replace in-memory storage with PostgreSQL**:

```sql
-- Slots table
CREATE TABLE slots (
    slot_id UUID PRIMARY KEY,
    doctor_id VARCHAR(50) NOT NULL,
    doctor_name VARCHAR(100) NOT NULL,
    department VARCHAR(100),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    max_capacity INT NOT NULL,
    current_occupancy INT DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_doctor_time (doctor_id, start_time)
);

-- Tokens table
CREATE TABLE tokens (
    token_id UUID PRIMARY KEY,
    patient_id VARCHAR(50) NOT NULL,
    patient_name VARCHAR(100) NOT NULL,
    slot_id UUID REFERENCES slots(slot_id),
    doctor_id VARCHAR(50) NOT NULL,
    source VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    token_number INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    checked_in_at TIMESTAMP,
    consultation_started_at TIMESTAMP,
    consultation_completed_at TIMESTAMP,
    reallocation_count INT DEFAULT 0,
    notes TEXT,
    INDEX idx_patient (patient_id),
    INDEX idx_doctor_status (doctor_id, status),
    INDEX idx_slot (slot_id)
);
```

**Use Spring Data JPA**:
```java
@Entity
@Table(name = "slots")
public class TimeSlot {
    @Id
    @GeneratedValue
    private UUID slotId;
    
    @OneToMany(mappedBy = "slot")
    private List<Token> tokens;
    
    // ... other fields
}

public interface SlotRepository extends JpaRepository<TimeSlot, UUID> {
    List<TimeSlot> findByDoctorIdAndStartTimeAfter(String doctorId, LocalDateTime time);
}
```

### 2. Caching Strategy

**Use Redis for real-time data**:
```java
@Cacheable(value = "doctorQueue", key = "#doctorId")
public List<Token> getDoctorQueue(String doctorId) {
    // ...
}

@CacheEvict(value = "doctorQueue", key = "#token.doctorId")
public Token allocateToken(Token token) {
    // ...
}
```

### 3. Async Operations

**Background reallocation**:
```java
@Async
public CompletableFuture<List<Token>> reallocateOverflowTokens(TimeSlot slot) {
    // Perform reallocation asynchronously
    // Send notifications in parallel
}
```

### 4. WebSocket for Real-Time Updates

**Push queue updates to frontend**:
```java
@Service
public class QueueUpdateService {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    public void notifyQueueUpdate(String doctorId) {
        messagingTemplate.convertAndSend(
            "/topic/queue/" + doctorId,
            getDoctorQueue(doctorId)
        );
    }
}
```

### 5. Authentication & Authorization

**Secure endpoints with JWT**:
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/slots/**").hasRole("ADMIN")
                .requestMatchers("/api/tokens/**").hasAnyRole("DOCTOR", "RECEPTIONIST")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer().jwt();
        return http.build();
    }
}
```

### 6. Monitoring & Logging

**Structured logging**:
```java
@Slf4j
public class OPDTokenEngine {
    public Token allocateToken(...) {
        MDC.put("patientId", patientId);
        MDC.put("doctorId", doctorId);
        log.info("Allocating token for patient");
        // ...
    }
}
```

**Metrics with Micrometer**:
```java
@Component
public class TokenMetrics {
    private final Counter tokensAllocated;
    private final Timer allocationTime;
    
    public TokenMetrics(MeterRegistry registry) {
        this.tokensAllocated = registry.counter("tokens.allocated");
        this.allocationTime = registry.timer("tokens.allocation.time");
    }
}
```

### 7. Load Balancing

**Distribute across multiple instances**:
- Use sticky sessions for WebSocket connections
- Share session state via Redis
- Use database-backed queues for job processing

### 8. Testing Strategy

```java
// Unit tests
@Test
public void testEmergencyAllocation() {
    // Test emergency token gets highest priority
}

// Integration tests
@SpringBootTest
@AutoConfigureMockMvc
public class TokenControllerTest {
    @Test
    public void testFullAllocationFlow() {
        // Create slot -> Allocate token -> Check-in -> Complete
    }
}

// Load tests
// Use JMeter or Gatling to simulate:
// - 100 concurrent bookings
// - Multiple simultaneous emergencies
// - High cancellation rates
```

---

## Conclusion

This system demonstrates:
- ✅ Sophisticated algorithm design with priority-based allocation
- ✅ Comprehensive edge case handling
- ✅ Robust failure management
- ✅ Production-ready architecture (with recommended enhancements)
- ✅ Clear API design with validation
- ✅ Real-world simulation showing all features

**Ready for**: Demo, testing, and as foundation for production system with database integration.
