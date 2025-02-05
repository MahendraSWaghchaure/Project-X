# Project-X
ProjectX is a smart school bus tracking system for parents, school admins, and drivers, ensuring student safety, real-time GPS tracking, and automated alerts. Built with Spring Boot, .NET, React.js, and MongoDB, it offers live tracking, check-in/out logging, driver payment management, and emergency alerts via web &amp; mobile apps. 🚀



# 🚍 ProjectX - Smart School Bus Tracking System

## 📌 Overview
ProjectX is a **full-stack web and mobile application** designed to enhance **student safety and school transportation management**. It enables **parents, school administrators, and drivers** to efficiently **track school buses, receive real-time notifications, and manage driver payments**.

## 🔹 Key Features
- ✅ **Live Bus Tracking** – Track real-time GPS locations of school buses.
- ✅ **Arrival & Departure Alerts** – Get notified when the bus reaches school, home, or designated stops.
- ✅ **Student Check-In/Out System** – Log student boarding & deboarding securely.
- ✅ **Driver Management** – Track driver records, work hours, payments, and performance.
- ✅ **Emergency Alerts** – Quick response notifications for delays, route changes, or incidents.
- ✅ **Admin Dashboard** – Manage routes, student records, driver assignments, and payments.
- ✅ **Mobile & Web Support** – Works on both smartphones and browsers for easy access.

## 🛠 Tech Stack
### **Frontend:**
- React.js (UI Development)
- Bootstrap (Styling & Layout)
- JavaScript, HTML5, CSS3
- React Router (Navigation)

### **Backend:**
- Spring Boot (Java) – RESTful API Development
- ASP.NET Core (C#) – Additional Services & API Integration

### **Database:**
- MongoDB (NoSQL Database for scalable storage) & MySQL for testing purpose

### **Authentication & Security:**
- JSON Web Token (JWT) for secure API access
- OAuth2 for authentication & authorization

### **Real-Time Features:**
- Google Maps API for location tracking
- WebSockets (Socket.io) for live updates

### **Cloud & DevOps:**
- AWS (Hosting & Deployment)
- Docker & Kubernetes (Containerization & Orchestration)
- Jenkins (CI/CD Automation)

## 🚀 Getting Started
### Prerequisites
Ensure you have the following installed:
- **Node.js** & **npm** (for frontend development)
- **Java JDK 11+** (for Spring Boot backend)
- **MongoDB** (for database)
- **Docker** (for containerization, optional)

### Installation
#### **1. Clone the Repository**
```sh
 git clone https://github.com/MahendraSWaghchaure/Project-X.git
 cd Project-X
```

#### **2. Set Up Backend (Spring Boot & .NET Core)**
```sh
 cd backend
 mvn clean install  # For Spring Boot
 dotnet restore      # For .NET Core (if applicable)
```

#### **3. Set Up Frontend (React.js)**
```sh
 cd frontend
 npm install
 npm start
```

#### **4. Run MongoDB Locally (Optional)**
```sh
 mongod --dbpath /your/db/path
```

### Running the Application
- **Start Backend:**
```sh
 mvn spring-boot:run  # Spring Boot
 dotnet run           # .NET Core
```

- **Start Frontend:**
```sh
 npm start
```

## Temporary login
## 📖 API Endpoints
### **Authentication**
| Method | Endpoint        | Description |
|--------|---------------|-------------|
| POST   | /api/auth/login  | User login |
| POST   | /api/auth/signup | User registration |
| GET    | /api/auth/me     | Get current user details |

### **Bus Tracking & Management**
| Method | Endpoint          | Description |
|--------|------------------|-------------|
| GET    | /api/buses        | Get list of all buses |
| GET    | /api/buses/:id    | Get details of a specific bus |
| POST   | /api/buses        | Add a new bus |
| PUT    | /api/buses/:id    | Update bus details |
| DELETE | /api/buses/:id    | Remove a bus |

### **Driver & Payment Management**
| Method | Endpoint          | Description |
|--------|------------------|-------------|
| GET    | /api/drivers      | Get all drivers |
| GET    | /api/drivers/:id  | Get driver details |
| POST   | /api/payments     | Process driver payment |

## 📸 Screenshots
*(Add screenshots of UI here for better visualization)*

## 🏗 Future Enhancements
- 📌 AI-powered route optimization for efficiency.
- 📌 Integration with **SMS & Email notifications**.
- 📌 Multi-language support for a broader user base.
- 📌 Blockchain-based **secure attendance tracking**.

## 🤝 Contributing
We welcome contributions! If you would like to enhance ProjectX:
1. Fork the repository
2. Create a feature branch (`git checkout -b feature-name`)
3. Commit your changes (`git commit -m 'Added new feature'`)
4. Push to the branch (`git push origin feature-name`)
5. Create a pull request

## 📜 License
This project is licensed under the **MIT License**.

## 📬 Contact
For any queries, feel free to reach out:
📧 Email: mahendra.s.waghchaure@gmail.com  
🔗 LinkedIn: [Mahendra Waghchaure](https://www.linkedin.com/in/mahendra-waghchaure/)  
💻 GitHub: [MahendraSWaghchaure](https://github.com/MahendraSWaghchaure)

---
🚀 **ProjectX is built with performance, security, and scalability in mind, making school transportation smarter & safer!**
