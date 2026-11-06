# 💸 Personal Finance Manager

Personal Finance Manager is a full-featured Spring Boot–based web application developed by me as a solo project to demonstrate backend development skills in Java and Spring.

It enables users to manage their personal finances by securely tracking incomes, expenses, setting financial goals, and generating detailed reports.

---

## 👨‍💻 About the Developer

Hi, I’m Vishnu Urugonda — an aspiring backend developer passionate about building efficient, secure, and scalable systems.  
This project was built completely from scratch, including:

- Database design (H2 + JPA)
- Session-based security (Spring Security)
- Clean RESTful API structure
- Unit testing with JUnit and Mockito
- YAML-based config and profiles

---

## 🧩 Features

### ✅ User Authentication  
- Register, Login (session-based), Logout  
- Passwords stored securely using BCrypt  
- User isolation: data is fully scoped to the logged-in user

### ✅ Transactions  
- Add, view, edit, delete income and expense entries  
- Tag transactions with category and description  
- Prevent future-dated transactions  
- Filter by category/date

### ✅ Categories  
- Built-in default categories (not deletable)  
- Create/delete personal categories  
- Prevent deletion if the category is in use

### ✅ Savings Goals  
- Create financial goals with target amount and date  
- Track progress (% complete) based on transaction history  
- Update or delete goals anytime

### ✅ Reports  
- Monthly and yearly financial summaries  
- Expense/income breakdown by category  
- Net savings automatically calculated

### ✅ Technical Stack  
- Spring Boot 3.2 (Java 17)  
- Spring Security (session-based auth)  
- Spring Data JPA (H2 database)  
- Validation (Hibernate Validator)  
- Unit Testing (JUnit 5, Mockito)  
- Maven for dependency management  
- Config via `application.yml`

---

## ⚙️ Installation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/urugondavishnu/personal-finance-manager.git
   cd personal-finance-manager
2. **Build the project**:
   ```bash
   ./mvnw clean install

## ▶️ Running the App Locally

1. **Start the server**:
   ```bash
   ./mvnw spring-boot:run
2. **Access the app in your browser**:
   ```bash
   http://localhost:8080

## ✅ Running Unit Tests
   ```bash
   ./mvnw test
   ```
   Expected output:
   ```yaml
   Tests run: 6, Failures: 0, Errors: 0
   BUILD SUCCESS
   ```

## 🌐 Deployment
The application is live and deployed using Render via a custom Dockerfile.

🔗 Live URL: https://personal-finance-manager-ix14.onrender.com

The project includes a Dockerfile that builds and runs the Spring Boot JAR using multi-stage Docker builds. This ensures portability and ease of deployment.

## 🔐 User Credentials
By default, users need to register with a unique username and password to access the application.

**Register**: Create your own username and password via the registration page.

**Login**: Use the registered username and password to log in.

Passwords are securely hashed with BCrypt for safety.

For Testing or Demo Purposes
You can use the following default test user credentials (if you have seeded any) or create your own:
| Username   | Password    |
| ---------- | ----------- |
| `testuser@example.com` | `Test@1234` |


## 📂 Folder Structure
   ```bash
   src/
├── main/
│   ├── java/com/vishnu/financemanager/
│   └── resources/application.yml
└── test/
    └── java/com/vishnu/financemanager/service/
   ```

## 🤝 Contributing
This is a solo-built project, but PRs are welcome for improvements.
To contribute:
   ```bash
   fork → clone → branch → commit → pull request
   ```

## 📬 Contact
For questions or feedback, please contact  [urugondavishnu](https://github.com/urugondavishnu).

## 📌 Project Status
- ✅ 100% features complete (as per spec)
- ✅ All APIs unit-tested
- ✅ Deployment is live
