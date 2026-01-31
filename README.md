# OPD Token Allocation Engine - Java Spring Boot

> **Backend Intern Assignment** - Medoc Health  
> A production-grade token allocation system for hospital OPD management

[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.1-brightgreen)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.8+-blue)](https://maven.apache.org/)

---

## 📋 Assignment Overview

This project implements a **complete token allocation system** for hospital OPD that supports:

- ✅ **Per-slot hard limits** with strict capacity enforcement
- ✅ **Dynamic token reallocation** when emergencies occur
- ✅ **Multi-source prioritization** (Emergency > Paid > Online > Follow-up > Walk-in)
- ✅ **Edge case handling** (cancellations, no-shows, emergency insertions)
- ✅ **REST API** with Swagger documentation
- ✅ **Real-world simulation** of full OPD day with 3 doctors

---

## 🎯 Deliverables Checklist

### 1. ✅ API Design (endpoints + data schema)
- **15+ REST endpoints** for complete slot and token management
- **Swagger UI** for interactive API documentation
- **Request/Response validation** using Bean Validation
- **Proper HTTP status codes** and error handling

### 2. ✅ Token Allocation Algorithm Implementation
- **Priority-based allocation** with dynamic scoring
- **Smart slot selection** (time proximity + capacity utilization)
- **Automatic overflow reallocation** when capacity exceeded
- **Thread-safe operations** with synchronized methods

### 3. ✅ Documentation
- **DOCUMENTATION.md** with:
  - Prioritization logic with examples
  - 8+ edge cases with detailed solutions
  - Failure handling strategies
  - Complete API reference
- **Inline code comments** and JavaDoc
- **README.md** (this file) with setup instructions

### 4. ✅ OPD Day Simulation (3 Doctors)
- **OPDSimulation.java** demonstrates:
  - 3 doctors across different departments
  - 9 time slots with varying capacities
  - All token sources (online, walk-in, priority, follow-up, emergency)
  - Cancellations, no-shows, check-ins
  - Dynamic capacity adjustment
  - Real-time queue and statistics

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────┐
│         REST Controllers                │
│  - SlotController (6 endpoints)         │
│  - TokenController (10 endpoints)       │
│  - Request validation                   │
│  - Error handling                       │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│      OPDTokenEngine (Service)           │
│  - Token allocation algorithm           │
│  - Priority calculation                 │
│  - Automatic reallocation               │
│  - Queue management                     │
│  - Statistics generation                │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│         Domain Models                   │
│  - TimeSlot                             │
│  - Token                                │
│  - TokenSource (enum with priorities)   │
│  - TokenStatus (lifecycle states)       │
└─────────────────────────────────────────┘
```

---

## 🚀 Quick Start

### Prerequisites

- **Java 17** or higher
- **Maven 3.8** or higher
- **IDE** (IntelliJ IDEA recommended, VS Code with Java extensions works too)

### Installation & Running

```bash
# 1. Navigate to project directory
cd opd-token-system

# 2. Build the project
mvn clean install

# 3. Run the Spring Boot application
mvn spring-boot:run

# Alternative: Run directly from compiled JAR
java -jar target/opd-token-system-1.0.0.jar
```

**The application will start on:** `http://localhost:8080`

### Access Points

| Service | URL | Description |
|---------|-----|-------------|
| **Swagger UI** | http://localhost:8080/swagger-ui.html | Interactive API documentation |
| **API Docs** | http://localhost:8080/api-docs | OpenAPI JSON specification |
| **Base API** | http://localhost:8080/api | REST API base path |

---

## 🧪 Running the Simulation

The simulation demonstrates a complete OPD day with 3 doctors and all system features.

### Option 1: Run from IDE

1. Open `OPDSimulation.java` in your IDE
2. Right-click → Run 'OPDSimulation.main()'

### Option 2: Run from Command Line

```bash
# Compile and run
mvn compile exec:java -Dexec.mainClass="com.medoc.opd.OPDSimulation"
```

### Simulation Phases

The simulation executes 13 phases:

1. **Slot Creation** - Creates 9 slots for 3 doctors
2. **Online Bookings** - Pre-scheduled appointments
3. **Walk-in Registrations** - Same-day patients
4. **Priority Patients** - VIP/paid service
5. **Emergency Admission** - Triggers automatic reallocation
6. **Cancellations** - Frees up capacity
7. **Check-ins** - Activates dynamic priority
8. **No-shows** - Capacity recovery
9. **Consultations** - Start and complete
10. **Capacity Adjustment** - Doctor running late
11. **Follow-ups** - Return visits
12. **Queue Status** - Current state per doctor
13. **Statistics** - Final metrics and analysis

---

## 📊 API Examples

### 1. Create a Time Slot

```bash
curl -X POST "http://localhost:8080/api/slots" \
  -H "Content-Type: application/json" \
  -d '{
    "doctorId": "DR001",
    "doctorName": "Dr. Rajesh Sharma",
    "department": "Cardiology",
    "startTime": "2024-02-01T09:00:00",
    "endTime": "2024-02-01T10:00:00",
    "maxCapacity": 10
  }'
```

### 2. Allocate a Token

```bash
curl -X POST "http://localhost:8080/api/tokens" \
  -H "Content-Type: application/json" \
  -d '{
    "patientId": "PAT001",
    "patientName": "Ramesh Gupta",
    "doctorId": "DR001",
    "source": "ONLINE_BOOKING",
    "preferredTime": "2024-02-01T09:30:00",
    "notes": "Regular checkup"
  }'
```

### 3. Emergency Token (Triggers Reallocation)

```bash
curl -X POST "http://localhost:8080/api/tokens/emergency" \
  -H "Content-Type: application/json" \
  -d '{
    "patientId": "PAT_EMG_001",
    "patientName": "Emergency Patient",
    "doctorId": "DR001",
    "notes": "Severe chest pain"
  }'
```

### 4. Get Doctor's Queue (Sorted by Priority)

```bash
curl "http://localhost:8080/api/tokens/queue/DR001"
```

### 5. Check-in Patient

```bash
curl -X POST "http://localhost:8080/api/tokens/{tokenId}/check-in"
```

### 6. Get Statistics

```bash
curl "http://localhost:8080/api/tokens/statistics?doctorId=DR001"
```

---

## 🎯 Key Features Explained

### 1. Priority System

| Token Source | Base Priority | Description |
|--------------|---------------|-------------|
| EMERGENCY | 1000+ | Can exceed capacity, never reallocated |
| PAID_PRIORITY | 500 | Premium service, high priority |
| ONLINE_BOOKING | 300 | Pre-scheduled appointments |
| FOLLOW_UP | 200 | Return visits |
| WALK_IN | 100 | Same-day registrations |

**Dynamic Priority**: After check-in, priority increases by 0.5 per minute waiting.

### 2. Smart Slot Selection

When allocating a token, the system:
1. Filters slots by doctor and availability
2. Calculates score for each slot:
   ```
   score = timeProximityMinutes + capacityUtilizationPercentage
   ```
3. Selects slot with lowest score (best match)

### 3. Automatic Reallocation

When emergency exceeds capacity:
1. Identify overflow tokens (lowest priority non-emergency)
2. Find next available slots
3. Move tokens to new slots
4. Update status to REALLOCATED
5. Log for patient notification

### 4. Edge Cases Handled

- ✅ Slot full + emergency arrives
- ✅ Multiple simultaneous emergencies
- ✅ Mass cancellations
- ✅ No-show detection
- ✅ Doctor running late (capacity reduction)
- ✅ Invalid state transitions
- ✅ Duplicate bookings (validation ready)
- ✅ Concurrent operations (thread-safe)

See `DOCUMENTATION.md` for detailed explanations.

---

## 📁 Project Structure

```
opd-token-system/
├── src/main/java/com/medoc/opd/
│   ├── OPDTokenSystemApplication.java    # Main Spring Boot app
│   ├── OPDSimulation.java                # Simulation runner
│   ├── model/
│   │   ├── TimeSlot.java                 # Slot entity
│   │   ├── Token.java                    # Token entity
│   │   ├── TokenSource.java              # Priority enum
│   │   └── TokenStatus.java              # Lifecycle enum
│   ├── service/
│   │   └── OPDTokenEngine.java           # Core allocation engine
│   ├── controller/
│   │   ├── SlotController.java           # Slot REST API
│   │   └── TokenController.java          # Token REST API
│   ├── config/
│   │   ├── OpenAPIConfig.java            # Swagger config
│   │   └── GlobalExceptionHandler.java   # Error handling
│   └── exception/
│       ├── SlotNotFoundException.java
│       ├── TokenNotFoundException.java
│       ├── SlotCapacityExceededException.java
│       └── InvalidTokenStateException.java
├── src/main/resources/
│   └── application.properties            # App configuration
├── pom.xml                                # Maven dependencies
├── DOCUMENTATION.md                       # Technical documentation
└── README.md                              # This file
```

---

## 🔧 Technical Decisions

### Why Java Spring Boot?

1. **Enterprise Standard** - Widely used in healthcare systems
2. **Type Safety** - Compile-time error detection
3. **Rich Ecosystem** - Extensive libraries and tools
4. **Production Ready** - Built-in features for monitoring, security
5. **Scalability** - Easy to scale horizontally

### Why In-Memory Storage?

- **Demo Simplicity** - Easy to run without database setup
- **Fast Performance** - Instant operations
- **Clear Logic** - Focus on algorithm, not infrastructure

**Production**: Replace with PostgreSQL + Redis (see DOCUMENTATION.md)

### Why This Priority System?

- **Fairness** - Clear rules prevent favoritism
- **Revenue** - Incentivizes premium service
- **Safety** - Emergencies always prioritized
- **Efficiency** - Dynamic scoring adapts to wait times

---

## 📈 Evaluation Criteria Coverage

### ✅ Quality of Algorithm Design

- Priority-based allocation with dynamic scoring
- Efficient slot selection (O(n log n))
- Automatic reallocation minimizing disruption
- Thread-safe concurrent operations

### ✅ Handling of Real-World Edge Cases

- 8+ edge cases documented with solutions
- Emergency overflow handling
- Graceful degradation when resources exhausted
- Invalid state transition prevention

### ✅ Code Structure and Clarity

- Clean separation of concerns (MVC pattern)
- Comprehensive JavaDoc and comments
- Type-safe with generic collections
- Follows Spring Boot best practices

### ✅ Practical Reasoning and Trade-offs

- In-memory vs database tradeoffs discussed
- Priority system balances revenue and fairness
- Reallocation strategy minimizes patient disruption
- Production recommendations provided

---

## 🚀 Production Enhancements

For production deployment, implement:

1. **Database** - PostgreSQL with proper indexing
2. **Caching** - Redis for queue state
3. **Authentication** - JWT tokens or OAuth2
4. **WebSocket** - Real-time queue updates
5. **Monitoring** - Prometheus + Grafana
6. **Logging** - ELK stack for structured logs
7. **Testing** - Unit, integration, and load tests
8. **CI/CD** - Automated deployment pipeline

See `DOCUMENTATION.md` section "Production Recommendations" for details.

---

## 📝 API Documentation

### Interactive Documentation

Visit **http://localhost:8080/swagger-ui.html** after starting the application.

The Swagger UI provides:
- Complete list of all endpoints
- Request/response schemas
- Interactive "Try it out" feature
- Example requests
- Error response formats

### Key Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/slots | Create time slot |
| GET | /api/slots/{id} | Get slot details |
| PUT | /api/slots/{id}/capacity | Adjust capacity |
| POST | /api/tokens | Allocate token |
| POST | /api/tokens/emergency | Emergency allocation |
| POST | /api/tokens/{id}/check-in | Check-in patient |
| DELETE | /api/tokens/{id} | Cancel token |
| GET | /api/tokens/queue/{doctorId} | Get queue |
| GET | /api/tokens/statistics | Get statistics |

---

## 🧪 Testing the System

### Manual Testing via Swagger UI

1. Start the application: `mvn spring-boot:run`
2. Open browser: http://localhost:8080/swagger-ui.html
3. Use the interactive interface to:
   - Create slots for doctors
   - Allocate various token types
   - Simulate check-ins and consultations
   - Test emergency scenarios
   - View queues and statistics

### Automated Testing via Simulation

```bash
mvn compile exec:java -Dexec.mainClass="com.medoc.opd.OPDSimulation"
```

This runs through all 13 phases showing:
- Complete day workflow
- All token types
- Edge case handling
- Real-time statistics

---

## 📧 Submission

This project is ready for submission with:

- ✅ Complete source code
- ✅ Comprehensive documentation
- ✅ Working simulation
- ✅ Interactive API (Swagger)
- ✅ All deliverables met

**Submitted for**: Backend Intern Position - Medoc Health

---

## 📄 License

This project is submitted as part of the Backend Intern assignment for Medoc Health.

---

## 👤 Author

**Backend Intern Candidate**  
Assignment: OPD Token Allocation Engine  
Date: January 2026

---

## 🙏 Acknowledgments

- Spring Boot team for excellent framework
- Swagger/OpenAPI for API documentation
- Lombok for reducing boilerplate code
- Medoc Health for the assignment opportunity

---

**Need Help?**

- Check `DOCUMENTATION.md` for technical details
- Use Swagger UI for API reference
- Run simulation for working example
- Review code comments for implementation details
