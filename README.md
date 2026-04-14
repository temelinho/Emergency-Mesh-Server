# EmergencyMeshServer

Backend server for the Emergency Communication Mobile Application. Built with Spring Boot and PostgreSQL.

## Tech Stack

* Java 17
* Spring Boot 3.2.0
* PostgreSQL
* Spring Data JPA
* WebSocket
* Docker

## Prerequisites

* Java 17+
* Maven
* Docker (optional)
* PostgreSQL (if running locally without Docker)

## Configuration

Update `src/main/resources/application.properties` if you are not using Docker or have different database credentials.

## Running with Docker

```bash
docker-compose up --build
```

The application will be available at `http://localhost:8080`.

## Running Locally

1. Ensure PostgreSQL is running and create a database named `emergencymesh`.
2. Run the application:

```bash
mvn spring-boot:run
```

## API Endpoints

### User

* `POST /api/users/register`: Register a new user
* `GET /api/users/{id}`: Get user details

### Messaging

* `POST /api/messages/send`: Send a message
* `GET /api/messages/user/{userId}`: Get message history for a user
* `GET /api/messages/conversation?user1={id}&user2={id}`: Get conversation between two users

### Location

* `POST /api/location/update`: Update user location
* `GET /api/location/{userId}`: Get user location

### Emergency Request

* `POST /api/emergency`: Create emergency request
* `GET /api/emergency`: List all requests
* `PUT /api/emergency/{id}/status`: Update request status

## WebSocket

* Endpoint: `/ws`
* Subscribe: `/user/queue/messages`
* Send: `/app/chat.sendMessage`

