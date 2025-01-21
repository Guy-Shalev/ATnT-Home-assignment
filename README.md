# Movie Ticket Booking System
[![Java](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://www.oracle.com/java/technologies/javase-jdk17-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)


## Description
A Spring Boot-based RESTful API for managing movie ticket bookings, including features for movie management, theater management, showtime scheduling, and ticket reservations.
## ✨Features

* Movie Management: Add, update, delete, and search movies
* Theater Management: Manage theaters and their seating capacities
* Showtime Scheduling: Schedule movie showtimes with conflict prevention
* Booking System: Secure ticket booking with seat selection
* User Management: User registration and role-based access control

## 📋 Prerequisites

* Java 17 or higher
* Maven 3.6 or higher
* An IDE (IntelliJ IDEA recommended)
* Git

## 🛠 Technology Stack

* Framework: Spring Boot 3.x
* Security: Spring Security with Basic Auth
* Database: H2 Database (in-memory)
* Documentation: ReadMe.md file
* Testing: JUnit 5, Mockito

## 🚀 Getting Started
### Setup and Installation

1. Clone the repository:

```bash 
git clone <repository-url>
cd ATnT-Home-assignment
```

2. Build the project:

```bash
mvn clean install
```
3. Run the application:

```bash
mvn spring-boot:run
```
The application will start on ```http://localhost:8080```

### Database Configuration
The application uses H2 in-memory database by default. The configuration can be found in ```application.properties```:
```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.h2.console.enabled=true
```
Access the H2 console at: ```http://localhost:8080/h2-console```

## 📡 API Endpoints
### User Management

* `POST /api/users/register` - Register new user
* `GET /api/users/current` - Get current user details

### Movie Management

* `POST /api/movies` - Create new movie (Admin)
* `GET /api/movies` - Get all movies
* `GET /api/movies/{id}` - Get movie by ID
* `PUT /api/movies/{id}` - Update movie (Admin)
* `DELETE /api/movies/{id}` - Delete movie (Admin)
* `GET /api/movies/search` - Search movies

### Theater Management

* `POST /api/theaters` - Create new theater (Admin)
* `GET /api/theaters` - Get all theaters
* `GET /api/theaters/{id}` - Get theater by ID
* `PUT /api/theaters/{id}` - Update theater (Admin)
* `DELETE /api/theaters/{id}` - Delete theater (Admin)

### Showtime Management

* `POST /api/showtimes` - Create new showtime (Admin)
* `GET /api/showtimes/{id}` - Get showtime by ID
* `GET /api/showtimes/movie/{movieId}` - Get showtimes by movie
* `GET /api/showtimes/theater/{theaterId}` - Get showtimes by theater
* `PUT /api/showtimes/{id}` - Update showtime (Admin)
* `DELETE /api/showtimes/{id}` - Delete showtime (Admin)

### Booking Management

* `POST /api/bookings` - Create new booking
* `GET /api/bookings/{id}` - Get booking by ID
* `GET /api/bookings/user` - Get user's bookings
* `GET /api/bookings/seat-available` - Check seat availability
  
## 🔒 Security
### Authentication
The API uses Basic Authentication. Include the following header in your requests:
`Authorization: Basic base64(username:password)`

Default admin credentials:
* Username: admin
* Password: adminPass123

## 🧪 Testing
Run the tests using:
```bash
mvn test
```
The project includes:

* Unit tests
* Controller integration tests
* Service integration tests

## ⚠️ Error Handling
The API uses standard HTTP status codes and returns error responses in the following format:
```json
{
"status": 400,
"message": "Error message",
"timestamp": "2024-01-21T10:00:00",
"path": "/api/endpoint"
}
```