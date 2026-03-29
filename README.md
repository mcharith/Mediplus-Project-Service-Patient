# Patient-Service

---

A dedicated microservice responsible for managing **patient records**, including profile information and profile pictures.  
It exposes a **RESTful API** consumed by the API Gateway, enabling other services to access, create, update, or delete patient data efficiently.  
The service ensures data consistency, validation, and secure handling of patient profile images.

---

## 📖 About

Patient-Service is part of a microservice architecture designed for a healthcare or channeling system.  
It provides endpoints for CRUD operations on patient records, including profile picture uploads, and integrates seamlessly with the API Gateway for service discovery.  
The service is built using **Spring Boot** and follows best practices for scalability, maintainability, and performance.  
Data is persisted in **PostgreSQL**, and DTO ↔ Entity mapping is handled efficiently with **MapStruct**.  
It also supports validation using **Spring Validation** to ensure data integrity, while **Lombok** is used to reduce boilerplate code.

---

## 🛠️ Tech Stack

| Technology                      | Details                                           |
|---------------------------------|--------------------------------------------------|
| Java                             | 25                                               |
| Spring Boot                      | 4.0.3                                            |
| Spring Cloud                     | 2025.1.0                                         |
| Spring Data JPA                  | ORM / persistence layer                           |
| PostgreSQL                        | Relational database (port 12500)                |
| MapStruct                        | DTO ↔ Entity mapping                              |
| Lombok                           | Boilerplate reduction                             |
| Spring Validation                | Bean validation                                   |
| Spring Cloud Netflix Eureka Client| Service registration & discovery                |
| Spring Cloud Config Client       | Fetches config from Config-Server               |
| Spring Boot Actuator             | Health & management endpoints                     |

---

## ⚙️ Service Details

| Property        | Value                                          |
|-----------------|-----------------------------------------------|
| Port            | 8001                                          |
| Artifact ID     | patient-service                                |
| Group ID        | lk.ijse.eca                                   |
| Database        | PostgreSQL — jdbc:postgresql://localhost:12500/eca |
| Picture Storage | `~/.ijse/eca/patient/`                        |

---

## 📡 API Endpoints

**Base path:** `/api/v1/patient`

| Method | Path                        | Description                   | Content-Type         |
|--------|----------------------------|-------------------------------|--------------------|
| POST   | /api/v1/patient            | Create a new patient          | multipart/form-data |
| GET    | /api/v1/patient            | Get all patients              | —                  |
| GET    | /api/v1/patient/{nic}      | Get a patient by NIC          | —                  |
| PUT    | /api/v1/patient/{nic}      | Update a patient              | multipart/form-data |
| DELETE | /api/v1/patient/{nic}      | Delete a patient              | —                  |
| GET    | /api/v1/patient/{nic}/picture | Get a patient's profile picture | —                  |

---

## 📝 Sample Request Body

> Requests must use `Content-Type: multipart/form-data`.

**POST / PUT /api/v1/patient**

| Field   | Type   | Required        | Validation                     |
|---------|--------|----------------|--------------------------------|
| nic     | string | Yes (on create)| `^\d{9}[vV]$`                  |
| name    | string | Yes            | Letters and spaces only         |
| address | string | Yes            | —                              |
| mobile  | string | Yes            | —                              |
| email   | string | No             | Valid email format             |
| picture | file   | Yes (on create)| Max size 5 MB                  |

> **NIC format:** 9 digits followed by V or v (e.g., 123456789V). NIC is the primary key.

---

## 🖼️ Sample Response

```json
{
  "nic": "123456789V",
  "name": "Charith Siriwardana",
  "address": "123 Main Street, Matale",
  "mobile": "0778986962",
  "email": "charith@example.com",
  "picture": "/api/v1/patient/123456789V/picture"
}
```

---

## 🚀 Getting Started

### Prerequisites

- Config-Server, Service-Registry, and API Gateway must be running.
- A PostgreSQL instance must be accessible on port `12500` with a database named `eca`.

### Startup Order

1. Config-Server (9100)
2. Service-Registry (9001)
3. API Gateway (7001)
4. Patient-Service (8001)

### Run the Service

```bash
./mvnw spring-boot:run
```

The service will start on **port 8001** and expose all endpoints listed above.

---