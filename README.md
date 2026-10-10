# RuleWeaver - Decision Engine & Persistence Layer

RuleWeaver is a Java-based platform designed for managing policy rules, applicant evaluations, and historic policy versions using MongoDB Atlas. It enforces strict JSON schema validation, unique compound indexing, and clean repository-based data access.

---

## 🛠️ Tech Stack & Architecture

- **Language:** Java 17
- **Database:** MongoDB Atlas (MongoDB Sync Driver `5.12.0`)
- **Build & Dependency Management:** Apache Maven
- **Testing:** JUnit 5

---

## 📁 Repository Structure & Data Contracts

### Database Collections & Fields

1. **`applicants`**
   - `_id`: String (e.g., `"APP001"`)
   - `name`: String
   - `gpa`: Double

2. **`policies`**
   - `policyId`: String (e.g., `"POLICY001"`)
   - `name`: String

3. **`policy_versions`**
   - `policyId`: String
   - `version`: Integer *(Unique Compound Index enforced on `policyId` + `version`)*
   - `minScore`: Integer / Rule conditions

4. **`evaluation_results`**
   - `applicantId`: String
   - `policyId`: String
   - `policyVersion`: Integer
   - `decision`: String (e.g., `"ELIGIBLE"`, `"INELIGIBLE"`)
   - `reasons`: List of Strings

---

## 🚀 Getting Started

### Prerequisites
- JDK 17 or higher
- Apache Maven
- MongoDB Atlas cluster connection string

### Environment Setup
Set your MongoDB connection string in your environment variables before running:

```bash
# On Linux/macOS
export MONGODB_URI="your_mongodb_atlas_connection_string"

# On Windows (PowerShell)
$env:MONGODB_URI="your_mongodb_atlas_connection_string"# DBMSOOPS-PROJECT
