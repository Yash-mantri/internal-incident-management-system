# IIMP Backend — Internal Incident Management Portal

**Stack:** Spring Boot 3.2 · Spring Security 6 · JWT (JJWT) · JPA · H2 (dev) / PostgreSQL (prod)

---

## Quick Start

```bash
# H2 dev mode (runs out of the box)
mvn spring-boot:run

# PostgreSQL mode
mvn spring-boot:run -Dspring.profiles.active=postgres
```

- App: http://localhost:8080
- H2 Console: http://localhost:8080/h2-console (JDBC: `jdbc:h2:mem:iimp_db`)

---

## Seeded Users (password: `password123`)

| Email              | Role          | Department |
|--------------------|---------------|------------|
| alice@iimp.com     | ADMIN         | IT         |
| bob@iimp.com       | MANAGER       | IT         |
| carol@iimp.com     | SUPPORT_STAFF | IT         |
| dave@iimp.com      | EMPLOYEE      | Finance    |
| eve@iimp.com       | EMPLOYEE      | HR         |
| frank@iimp.com     | MANAGER       | HR         |

---

## API Reference

### Auth
| Method | URL                         | Access  | Description                    |
|--------|-----------------------------|---------|--------------------------------|
| POST   | /api/auth/login             | Public  | Login → returns JWT + role     |
| POST   | /api/auth/refresh           | Public  | Refresh access token           |
| POST   | /api/auth/change-password   | Any     | Change password (first login)  |

**Login request:**
```json
{ "email": "alice@iimp.com", "password": "password123" }
```
**Response includes:** `accessToken`, `refreshToken`, `role`, `mustChangePassword`

---

### Incidents
| Method | URL                             | Access             | Description              |
|--------|---------------------------------|--------------------|--------------------------|
| POST   | /api/incidents                  | Any auth           | Create new incident      |
| GET    | /api/incidents                  | Any auth           | List (role-scoped)       |
| GET    | /api/incidents/{id}             | Any auth           | Get ticket detail        |
| PUT    | /api/incidents/{id}/assign      | MANAGER, ADMIN     | Assign to support staff  |
| PUT    | /api/incidents/{id}/status      | Role-restricted    | Update status            |
| POST   | /api/incidents/{id}/comments    | Any auth           | Add comment              |
| POST   | /api/incidents/{id}/attachments | Any auth           | Upload file (multipart)  |

**Filter params for GET /api/incidents:**
`?status=OPEN&priority=HIGH&category=IT&from=2025-01-01T00:00:00&to=2025-12-31T23:59:59&page=0&size=20`

**Status transitions by role:**
- `SUPPORT_STAFF`: IN_PROGRESS → RESOLVED
- `MANAGER`: RESOLVED → CLOSED | RESOLVED → IN_PROGRESS
- `ADMIN`: any → any (except CLOSED → any)

---

### Dashboards
| Method | URL                     | Access             | Description                           |
|--------|-------------------------|--------------------|---------------------------------------|
| GET    | /api/dashboard/employee | EMPLOYEE, ADMIN    | Own ticket counts + recent + notifs   |
| GET    | /api/dashboard/support  | SUPPORT_STAFF, ADMIN | Assigned queue + SLA countdown      |
| GET    | /api/dashboard/manager  | MANAGER, ADMIN     | Dept KPIs + team workload + requesters|
| GET    | /api/dashboard/admin    | ADMIN              | System-wide KPIs + all users + SLA % |

**Manager dashboard includes:**
- `deptTotalTickets`, `deptOpenCount`, `deptInProgressCount`, `deptResolvedCount`, `deptSlaBreachedCount`
- `teamWorkload[]` — each support staff member + their assigned/resolved count
- `topRequesters[]` — each employee + their total/open/resolved request count

**Admin dashboard includes:**
- System-wide ticket counts by status, category, priority
- SLA compliance percentage
- All users across all departments
- Recent 10 tickets

---

### Admin — User Management
| Method | URL                                | Description                |
|--------|------------------------------------|----------------------------|
| GET    | /api/admin/users                   | All users (all departments)|
| GET    | /api/admin/users/{id}              | Single user                |
| POST   | /api/admin/users                   | Create user (temp password)|
| PUT    | /api/admin/users/{id}              | Update role/dept           |
| PATCH  | /api/admin/users/{id}/deactivate   | Soft-deactivate user       |
| PATCH  | /api/admin/users/{id}/reactivate   | Reactivate user            |
| GET    | /api/admin/sla                     | List SLA configs           |
| PUT    | /api/admin/sla                     | Update SLA hours           |

---

### Manager
| Method | URL                         | Description                        |
|--------|-----------------------------|------------------------------------|
| GET    | /api/manager/support-staff  | Active support staff for dropdown  |

---

### Notifications
| Method | URL                             | Description                  |
|--------|---------------------------------|------------------------------|
| GET    | /api/notifications              | All notifications             |
| GET    | /api/notifications/unread       | Unread only                  |
| GET    | /api/notifications/count        | Unread count badge           |
| PATCH  | /api/notifications/{id}/read    | Mark one as read             |
| PATCH  | /api/notifications/read-all     | Mark all as read             |

---

## SLA Scheduler

Runs automatically every 5 minutes:
1. Flags breached incidents (`slaDueAt < now`) and notifies managers + admins
2. Escalates critical unassigned tickets older than 1 hour to all admins

---

## Project Structure

```
src/main/java/com/iimp/
├── IimpApplication.java
├── config/
│   ├── SecurityConfig.java
│   └── GlobalExceptionHandler.java
├── controller/
│   ├── AuthController.java
│   ├── IncidentController.java
│   ├── DashboardController.java
│   ├── AdminController.java
│   ├── ManagerController.java
│   └── NotificationController.java
├── dto/
│   ├── AuthDtos.java
│   ├── IncidentDtos.java
│   ├── UserDtos.java
│   ├── CommentDtos.java
│   ├── AttachmentDtos.java
│   ├── AuditDtos.java
│   ├── NotificationDtos.java
│   └── DashboardDtos.java
├── entity/
│   ├── User.java
│   ├── Category.java
│   ├── SlaConfig.java
│   ├── Incident.java
│   ├── IncidentComment.java
│   ├── Attachment.java
│   ├── IncidentAudit.java
│   └── Notification.java
├── enums/
│   ├── Role.java
│   ├── Priority.java
│   └── IncidentStatus.java
├── exception/
│   ├── ResourceNotFoundException.java
│   ├── BadRequestException.java
│   └── AccessDeniedException.java
├── repository/  (7 JPA repositories)
├── scheduler/
│   └── SlaScheduler.java
├── security/
│   ├── JwtUtils.java
│   ├── JwtAuthenticationFilter.java
│   └── UserDetailsServiceImpl.java
└── service/
    ├── AuthService.java
    ├── IncidentService.java
    ├── UserService.java
    ├── DashboardService.java
    ├── NotificationService.java
    └── AuditService.java
```

---

## Switch to PostgreSQL

1. Create DB: `CREATE DATABASE iimp_db;`
2. Run: `mvn spring-boot:run -Dspring.profiles.active=postgres`
3. Set env vars: `DB_USER`, `DB_PASS`

---

## Security Notes for Production

- Replace `app.jwt.secret` with a strong 256-bit random key via environment variable
- Set `spring.jpa.hibernate.ddl-auto=validate` after first run
- Configure real SMTP: `MAIL_USER`, `MAIL_PASS`
- Enable HTTPS
