# 💰 BillSplit

<div align="center">

![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen?style=for-the-badge&logo=spring)
![React](https://img.shields.io/badge/React-18-blue?style=for-the-badge&logo=react)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?style=for-the-badge&logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Ready-blue?style=for-the-badge&logo=docker)

**A modern, full-stack expense sharing application for groups and friends**

[Features](#-features) • [Quick Start](#-quick-start) • [Documentation](#-documentation) • [Contributing](#-contributing)

</div>

---

## 📖 Overview

BillSplit is a comprehensive expense management application that simplifies splitting bills among groups. Whether you're planning a trip, sharing household expenses, or managing group activities, BillSplit helps you track who paid what and automatically calculates the optimal way to settle debts.

### Key Highlights

- ✨ **Smart Debt Calculation**: Automatically calculates minimal transactions to settle all debts
- 👥 **Group Management**: Create groups, invite members, and manage roles
- 💳 **Flexible Splitting**: Equal or custom expense splits
- 📧 **Email Notifications**: Automated invitations and settlement notifications
- 🔐 **Secure Authentication**: JWT-based authentication with Spring Security
- 📱 **Responsive Design**: Modern UI built with Tailwind CSS
- 🐳 **Docker Ready**: One-command deployment with Docker Compose

---

## ✨ Features

### 🔐 Authentication & User Management
- User registration and login
- JWT-based secure authentication
- User profile management
- Password update functionality

### 👥 Group Management
- Create and manage expense groups
- Invite members via email (works for both registered and non-registered users)
- Admin and member role management
- Pending invitation system
- Group deletion (admin only)

### 💰 Expense Tracking
- Add expenses with detailed descriptions
- **Equal Split**: Automatically divide expenses equally among participants
- **Custom Split**: Specify exact amounts for each member
- Support for pending members (users without accounts)
- Expense deletion with soft-delete functionality
- Visual expense history with activity feed

### 📊 Balance Management
- Real-time balance calculations
- View who owes whom
- Track total paid, owed, and net balances
- Settlement history tracking
- "All settled up!" status when balances are zero

### 💸 Settlement System
- Optimal debt settlement algorithm
- Calculate minimal transactions to settle all debts
- Process settlements with proof (images and messages)
- Settlement history tracking
- Admin can delete settlement records

### 📧 Email Notifications
- Group invitation emails
- Settlement notifications
- Invitation acceptance/rejection notifications

### 🎨 User Interface
- Modern, responsive design
- Intuitive navigation
- Real-time updates
- Toast notifications for user feedback
- Profile dropdown with settings
- Dark/light theme support (via Tailwind)

---

## 🛠️ Tech Stack

### Backend
- **Java 17** - Modern Java features
- **Spring Boot 3.2.0** - Rapid application development
- **Spring Security** - JWT authentication and authorization
- **Spring Data JPA** - Database abstraction layer
- **Spring Mail** - Email service integration
- **PostgreSQL 15** - Relational database
- **Flyway** - Database migration tool
- **Swagger/OpenAPI** - API documentation
- **Maven** - Dependency management

### Frontend
- **React 18** - Modern UI library
- **React Router** - Client-side routing
- **Axios** - HTTP client
- **Tailwind CSS** - Utility-first CSS framework
- **React Toastify** - Toast notifications

### DevOps
- **Docker** - Containerization
- **Docker Compose** - Multi-container orchestration
- **Nginx** - Web server for frontend

---

## 🚀 Quick Start

### Prerequisites

- [Docker](https://www.docker.com/get-started) and Docker Compose installed
- (Optional) Java 17+ and Node.js 18+ for local development

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/BillSplitApplication.git
   cd BillSplitApplication
   ```

2. **Configure environment variables (Optional)**
   
   Create a `.env` file in the root directory for email configuration:
   ```bash
   MAIL_USERNAME=your-email@gmail.com
   MAIL_PASSWORD=your-app-password
   ```
   
   > **Note**: Email configuration is optional. The application works without it, but email notifications won't be sent.

3. **Start the application**
   ```bash
   docker-compose up --build
   ```

4. **Access the application**
   - 🌐 **Frontend**: http://localhost:3000
   - 🔌 **Backend API**: http://localhost:8080/api
   - 📚 **API Documentation**: http://localhost:8080/swagger-ui.html
   - 🗄️ **Database**: localhost:5432

### First Steps

1. Register a new account at http://localhost:3000/register
2. Create your first group
3. Invite members by email
4. Start adding expenses!

---

## 📚 Documentation

### API Endpoints

The complete API documentation is available via Swagger UI when the application is running:
- **Swagger UI**: http://localhost:8080/swagger-ui.html

#### Authentication
```
POST   /api/auth/register     - Register a new user
POST   /api/auth/login        - Login user
GET    /api/auth/me           - Get current user profile
PUT    /api/auth/profile      - Update user profile
PUT    /api/auth/password     - Update password
```

#### Groups
```
GET    /api/groups                    - Get user's groups
POST   /api/groups                    - Create a new group
GET    /api/groups/{id}               - Get group details
PUT    /api/groups/{id}               - Update group
DELETE /api/groups/{id}               - Delete group (admin only)
GET    /api/groups/{id}/members       - Get group members
POST   /api/groups/{id}/members       - Add member to group
DELETE /api/groups/{id}/members/{userId} - Remove member
POST   /api/groups/{id}/leave         - Leave group
GET    /api/groups/invitations        - Get pending invitations
POST   /api/groups/invitations/{id}/accept - Accept invitation
POST   /api/groups/invitations/{id}/reject - Reject invitation
```

#### Expenses
```
GET    /api/expenses/group/{groupId}  - Get group expenses
POST   /api/expenses                  - Create a new expense
DELETE /api/expenses/{id}             - Delete expense
GET    /api/expenses/{id}/shares      - Get expense shares
```

#### Settlements
```
GET    /api/settlements/group/{groupId}        - Calculate settlements
POST   /api/settlements/group/{groupId}/process - Process settlement
GET    /api/settlements/group/{groupId}/history - Get settlement history
DELETE /api/settlements/group/{groupId}/{id}  - Delete settlement (admin)
```

### Database Schema

The application uses PostgreSQL with the following main entities:

- **users** - User accounts and authentication
- **groups** - Expense groups
- **group_members** - Group membership and roles
- **pending_group_members** - Pending invitations
- **expenses** - Expense records
- **expense_shares** - Individual expense shares
- **settlements** - Settlement transactions

Database migrations are managed by Flyway and located in `backend/src/main/resources/db/migration/`.

---

## 🏗️ Project Structure

```
BillSplitApplication/
├── backend/                          # Spring Boot backend
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/billsplit/
│   │   │   │   ├── config/          # Configuration classes
│   │   │   │   ├── controller/       # REST controllers
│   │   │   │   ├── dto/             # Data Transfer Objects
│   │   │   │   ├── entity/          # JPA entities
│   │   │   │   ├── exception/       # Exception handlers
│   │   │   │   ├── repository/       # JPA repositories
│   │   │   │   ├── security/        # Security configuration
│   │   │   │   ├── service/         # Business logic
│   │   │   │   └── util/            # Utility classes
│   │   │   └── resources/
│   │   │       ├── application.yml   # Application config
│   │   │       └── db/migration/     # Flyway migrations
│   │   └── test/                     # Test files
│   ├── pom.xml                       # Maven dependencies
│   └── Dockerfile                    # Backend Docker image
│
├── frontend/                         # React frontend
│   ├── src/
│   │   ├── components/               # Reusable components
│   │   ├── contexts/                 # React contexts
│   │   ├── pages/                    # Page components
│   │   ├── services/                 # API services
│   │   └── index.js                  # Entry point
│   ├── public/                       # Static files
│   ├── package.json                  # Node dependencies
│   ├── tailwind.config.js            # Tailwind config
│   └── Dockerfile                    # Frontend Docker image
│
├── docker-compose.yml                # Docker Compose config
├── .env.example                      # Environment variables template
└── README.md                         # This file
```

---

## 🔧 Local Development

### Backend Setup

```bash
cd backend

# Run with Maven
./mvnw spring-boot:run

# Or build and run
./mvnw clean package
java -jar target/bill-split-backend-0.0.1-SNAPSHOT.jar
```

### Frontend Setup

```bash
cd frontend

# Install dependencies
npm install

# Start development server
npm start

# Build for production
npm run build
```

### Database Setup

```bash
# Start PostgreSQL with Docker
docker run --name billsplit-db \
  -e POSTGRES_DB=billsplit \
  -e POSTGRES_USER=billsplit_user \
  -e POSTGRES_PASSWORD=billsplit_password \
  -p 5432:5432 \
  -d postgres:15-alpine
```

---

## 🧪 Testing

### Backend Tests
```bash
cd backend
./mvnw test
```

### Frontend Tests
```bash
cd frontend
npm test
```

---

## 📝 Configuration

### Environment Variables

See `env.example` for all available environment variables.

#### Required for Production
- `JWT_SECRET` - Strong secret key for JWT token generation (generate with: `openssl rand -base64 32`)
- `POSTGRES_PASSWORD` - Strong database password
- `APP_URL` - Production application URL
- `CORS_ALLOWED_ORIGINS` - Allowed CORS origins (comma-separated)
- `REACT_APP_API_URL` - Backend API URL

#### Optional
- `MAIL_USERNAME` - Email username for notifications
- `MAIL_PASSWORD` - Email password/app password
- `LOG_LEVEL` - Logging level (INFO, WARN, ERROR)
- `RATE_LIMIT_ENABLED` - Enable rate limiting (default: true)
- `RATE_LIMIT_REQUESTS` - Max requests per window (default: 100)
- `RATE_LIMIT_WINDOW_MINUTES` - Time window in minutes (default: 1)

---

## 🚀 Deployment

### Quick Start

1. **Copy environment file:**
   ```bash
   cp env.example .env
   ```

2. **Configure production values in `.env`:**
   - Set strong `JWT_SECRET` (generate with `openssl rand -base64 32`)
   - Set production `APP_URL` and `CORS_ALLOWED_ORIGINS`
   - Configure database credentials
   - Set email credentials (optional)

3. **Deploy with Docker:**
   ```bash
   docker-compose up -d --build
   ```

### Production Deployment

For detailed deployment instructions, see [DEPLOYMENT.md](DEPLOYMENT.md).

**Key Steps:**
1. Set all required environment variables
2. Generate strong JWT secret
3. Configure CORS for your domain
4. Set up SSL/HTTPS (via reverse proxy or hosting platform)
5. Deploy to your chosen platform (Railway, Render, etc.)

### Platform Options

- **Railway** - Easy deployment with PostgreSQL
- **Render** - Free tier available
- **DigitalOcean App Platform** - Simple deployment
- **AWS/GCP/Azure** - Enterprise solutions

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. **Fork the repository**
2. **Create a feature branch**
   ```bash
   git checkout -b feature/amazing-feature
   ```
3. **Make your changes**
4. **Commit your changes**
   ```bash
   git commit -m 'Add some amazing feature'
   ```
5. **Push to the branch**
   ```bash
   git push origin feature/amazing-feature
   ```
6. **Open a Pull Request**

### Development Guidelines

- Follow existing code style and conventions
- Add comments for complex logic
- Update documentation for new features
- Test your changes thoroughly
- Ensure all tests pass before submitting

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👤 Author

**Tarush Batra**

- GitHub: [@TarushBatra](https://github.com/TarushBatra)
- Email: tarushbatra11318@gmail.com

---

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- React team for the amazing UI library
- All contributors and users of this project

---

<div align="center">

**Made with ❤️ using Spring Boot and React**

⭐ Star this repo if you find it helpful!

</div>
