<div align="center">

# 🧠 AI Prompt Engineering Studio

### A full-stack platform for generating, optimizing, analyzing, and battle-testing AI prompts

Craft expert-level prompts for Large Language Models — no prompt engineering experience required.

<br/>

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![MongoDB](https://img.shields.io/badge/MongoDB-6+-47A248?style=for-the-badge&logo=mongodb&logoColor=white)](https://www.mongodb.com/)
[![Gemini](https://img.shields.io/badge/Google%20Gemini-API-8E75B2?style=for-the-badge&logo=googlegemini&logoColor=white)](https://ai.google.dev/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)

[![GitHub Repo stars](https://img.shields.io/github/stars/yourusername/ai-prompt-engineering-studio?style=for-the-badge&color=yellow)](https://github.com/yourusername/ai-prompt-engineering-studio/stargazers)
[![GitHub forks](https://img.shields.io/github/forks/yourusername/ai-prompt-engineering-studio?style=for-the-badge&color=blue)](https://github.com/yourusername/ai-prompt-engineering-studio/network/members)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge)](LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=for-the-badge)](CONTRIBUTING.md)

<br/>

[Live Demo](#) · [Report Bug](https://github.com/yourusername/ai-prompt-engineering-studio/issues) · [Request Feature](https://github.com/yourusername/ai-prompt-engineering-studio/issues)

</div>

---

## 📑 Table of Contents

- [About the Project](#-about-the-project)
- [Features](#-features)
- [Prompt Engineering Concepts](#-prompt-engineering-concepts)
- [Architecture](#-architecture)
- [System Workflow](#-system-workflow)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [Installation](#-installation)
- [Environment Variables](#-environment-variables)
- [API Overview](#-api-overview)
- [Database Design](#-database-design)
- [Semantic Search with ChromaDB](#-semantic-search-with-chromadb)
- [Security Features](#-security-features)
- [Future Enhancements](#-future-enhancements)
- [Contributing](#-contributing)
- [License](#-license)
- [Contact](#-contact)
- [Acknowledgements](#-acknowledgements)

---

## 💡 About the Project

### The Problem

Most people interacting with AI models write prompts the same way they'd type a search query — short, vague, and unstructured. The result is inconsistent, low-quality output, followed by frustrated trial-and-error rewriting. Prompt engineering — the practice of deliberately structuring instructions to get reliable, high-quality output from an LLM — is a real skill, but there's no accessible tool that teaches or automates it.

### The Solution

**AI Prompt Engineering Studio** is not another chatbot wrapper. It's a dedicated workspace for the craft of prompt engineering itself:

- **Generate** a prompt from a plain task description, in any of six proven prompting styles
- **Optimize** an existing prompt for clarity and structure
- **Analyze** a prompt's quality across five measurable dimensions
- **Battle-test** multiple prompting styles head-to-head on the same task and see which one actually performs best
- **Organize** everything into a searchable, taggable personal library

### Objectives

- Demonstrate practical, hands-on prompt engineering techniques rather than just describing them
- Provide a reusable library so good prompts are never rewritten from scratch
- Give quantifiable feedback (scores, comparisons) instead of guesswork
- Serve as both a defensible final-year engineering project and a genuine SaaS-shaped foundation

### Real-World Applications

- Content teams standardizing prompts for marketing copy generation
- Developers building prompt libraries for internal AI tooling
- Students and researchers learning prompt engineering through direct comparison
- Any team that wants prompt quality to be measured, not guessed at

---

## ✨ Features

| Feature | Description |
|---|---|
| 🔐 **User Authentication** | Secure registration and login with JWT-based stateless sessions |
| 📊 **Dashboard** | At-a-glance usage stats, per-feature activity breakdown, and quick actions |
| ⚡ **Prompt Generator** | Generates a full prompt from a task description in 6 engineering styles |
| 🛠️ **Prompt Optimizer** | Rewrites an existing prompt for clarity, structure, and specificity |
| 📈 **Prompt Analyzer** | Scores a prompt on grammar, clarity, context, hallucination risk, and complexity |
| ⚔️ **Prompt Battle Arena** | Runs multiple prompt styles against the same task and recommends the best performer |
| 📚 **Prompt Library** | Save, search, tag, favorite, edit, and delete your best prompts |
| 🗂️ **Collections** | Group related prompts into custom, color-coded collections |
| 🕓 **Prompt History** | A running, filterable log of every prompt you've ever generated |
| 💬 **AI Chat** | Multi-session conversational AI with full message history |
| 🔍 **Semantic Search** | Optional vector-based similarity search over your saved prompts (ChromaDB) |
| 📤 **Export** | Download any saved prompt as PDF, Markdown, or plain text |
| 🛡️ **Admin Dashboard** | Platform-wide user management and usage statistics for administrators |
| 🌗 **Dark Mode** | Full light/dark theming across every page |
| 📱 **Responsive UI** | A glassmorphism-styled SaaS interface that works on desktop and mobile |

---

## 🎯 Prompt Engineering Concepts

The Prompt Generator and Battle Arena are built around six core prompting techniques:

- **Zero-Shot Prompting** — a direct instruction with no examples, relying purely on the model's pretrained knowledge
- **Few-Shot Prompting** — includes one or more input/output examples to steer the model's response pattern
- **Chain-of-Thought Prompting** — explicitly asks the model to reason step-by-step before producing a final answer
- **Role Prompting** — assigns the model a specific persona or expert identity to shape tone and depth
- **Step-by-Step Prompting** — breaks a task into an explicit, ordered sequence of instructions
- **Instruction Prompting** — a clear, structured directive with defined constraints and expected output format

On top of these, the platform supports:

- **Prompt Refinement** — the Optimizer rewrites a prompt while preserving its original intent
- **Prompt Evaluation** — the Analyzer scores objective quality dimensions with actionable suggestions
- **Prompt Comparison** — the Battle Arena runs styles side-by-side against real model output
- **Output Formatting** — generated content is rendered with proper structure for readability

### Technique Comparison

| Technique | Best For | Example Use Case | Complexity |
|---|---|---|---|
| Zero-Shot | Simple, well-defined tasks | "Summarize this article" | ⭐ |
| Few-Shot | Tasks needing a specific format | Classifying support tickets by category | ⭐⭐ |
| Chain-of-Thought | Multi-step reasoning problems | Solving a word problem or logical puzzle | ⭐⭐⭐ |
| Role-Based | Tasks needing domain expertise/tone | "As a senior copywriter, write..." | ⭐⭐ |
| Step-by-Step | Procedural or sequential tasks | Writing a recipe or setup guide | ⭐⭐ |
| Instruction | Tasks with strict output constraints | Generating structured JSON or a table | ⭐⭐⭐ |

---

## 🏗️ Architecture

<div align="center">

![Architecture Diagram](https://claude.ai/chat/docs/images/architecture.png)

</div>

The platform follows a classic four-layer architecture. A vanilla JS frontend communicates over HTTPS/REST with a layered Spring Boot backend (controllers → services → repositories), secured end-to-end with JWT authentication. The backend persists all application data in MongoDB, optionally indexes prompt embeddings in ChromaDB for semantic search, and calls the Google Gemini API for all text generation, scoring, and embedding tasks.

---

## 🔄 System Workflow

<div align="center">

![Workflow Diagram](https://claude.ai/chat/docs/images/workflow.png)

</div>

A typical request — for example, clicking **Generate Prompt** — flows as follows:

1. The user logs in or registers; the backend issues a signed JWT.
2. From the Dashboard, the user selects a tool (Generator, Optimizer, Analyzer, or Battle Arena).
3. The frontend sends an authenticated REST request with the JWT in the `Authorization` header.
4. The relevant controller delegates to its service, which builds the appropriate prompt template and calls the Google Gemini API.
5. The result is persisted to MongoDB (as a history record, or directly to the Library) and returned to the frontend for display.

---

## 🧰 Tech Stack

| Layer | Technology |
|---|---|
| Frontend | HTML5, CSS3, Vanilla JavaScript (ES6) |
| Backend | Java 21, Spring Boot 3.3.x, Spring Security |
| Authentication | JWT (JJWT), BCrypt password hashing |
| Database | MongoDB (Spring Data MongoDB) |
| Vector Database | ChromaDB *(optional, for semantic search)* |
| AI Integration | Google Gemini API (text generation + embeddings) |
| API Documentation | Springdoc OpenAPI / Swagger UI |
| PDF Generation | iText |
| Deployment | Docker, Docker Compose |
| Version Control | Git, GitHub |

---

## 📁 Project Structure

```
ai-prompt-engineering-studio/
│
├── backend/
│   ├── pom.xml
│   ├── Dockerfile
│   ├── .env
│   └── src/main/
│       ├── resources/
│       │   └── application.yml
│       └── java/com/promptstudio/
│           ├── PromptStudioApplication.java
│           ├── enums/            → PromptType, UserRole, ExportFormat, PromptAction
│           ├── entity/           → User, Prompt, PromptCollection, ChatSession
│           ├── dto/              → AuthDTOs, UserDTOs, PromptDTOs, ChatDTOs, CommonDTOs
│           ├── repository/       → UserRepository, PromptRepository, PromptCollectionRepository, ChatSessionRepository
│           ├── service/          → AuthService, UserService, AiService, PromptService, ChatService, ExportService, AdminService
│           ├── controller/       → AuthController, UserController, PromptController, ChatController, ExportController, AdminController
│           ├── security/         → JwtUtil, JwtAuthenticationFilter, CustomUserDetailsService
│           ├── config/           → SecurityConfig, AppConfig
│           └── exception/        → ApiException, GlobalExceptionHandler
│
├── frontend/
│   ├── index.html                → Landing page
│   ├── login.html
│   ├── register.html
│   ├── dashboard.html            → Single-page app shell for all authenticated modules
│   ├── css/
│   │   ├── style.css             → Global theme, components, landing & auth pages
│   │   └── dashboard.css         → Sidebar, modules, tables, chat UI
│   └── js/
│       ├── api.js                → Fetch wrapper, session management, toasts
│       ├── auth.js                → Login / register logic
│       ├── app.js                → Dashboard shell, navigation, theming
│       ├── modules.js            → Generator, Optimizer, Analyzer, Battle Arena
│       └── workspace.js          → Library, Collections, History, Chat, Profile, Admin
│
├── docker-compose.yml
└── README.md
```

---

## ⚙️ Installation

### Prerequisites

- Java 21+
- Maven 3.9+
- MongoDB 6+ (running locally or via Docker)
- A Google Gemini API key ([Google AI Studio](https://aistudio.google.com/))
- A modern browser + a static file server (e.g. VS Code Live Server)
- *(Optional)* Docker, for running ChromaDB

### 1. Clone the repository

```bash
git clone https://github.com/yourusername/ai-prompt-engineering-studio.git
cd ai-prompt-engineering-studio
```

### 2. Backend setup

```bash
cd backend
cp .env.example .env
# Edit .env and fill in your MongoDB URI, JWT secret, and Gemini API key
mvn clean install
```

### 3. MongoDB

Make sure a MongoDB instance is running locally on the default port, or update `MONGODB_URI` in `.env` to point to your instance (local, Docker, or Atlas).

```bash
# Example: run MongoDB via Docker
docker run -d -p 27017:27017 --name prompt-studio-mongo mongo:6
```

### 4. ChromaDB (optional)

Semantic search is disabled by default (`CHROMADB_ENABLED=false`). To enable it:

```bash
docker run -d -p 8000:8000 --name prompt-studio-chroma chromadb/chroma
```

Then set `CHROMADB_ENABLED=true` in your `.env`.

### 5. Gemini API configuration

Get an API key from [Google AI Studio](https://aistudio.google.com/) and set it as `GEMINI_API_KEY` in your `.env`.

### 6. Run the backend

```bash
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`. Swagger UI is available at `http://localhost:8080/swagger-ui/index.html`.

### 7. Run the frontend

Serve the `frontend/` folder with any static file server, for example VS Code's Live Server extension. Then open `index.html` in your browser.

> ⚠️ Make sure the port your static server runs on doesn't require any manual CORS configuration — the backend accepts any `localhost`/`127.0.0.1` origin during local development.

---

## 🔑 Environment Variables

Create a `.env` file inside `backend/` based on the template below. **Never commit real secrets.**

```env
# MongoDB
MONGODB_URI=your_mongodb_uri

# JWT
JWT_SECRET=your_jwt_secret_min_256_bits
JWT_ACCESS_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=604800000

# Google Gemini
GEMINI_API_KEY=your_gemini_api_key
GEMINI_MODEL=gemini-1.5-flash

# ChromaDB (optional)
CHROMADB_ENABLED=false
CHROMADB_URL=http://localhost:8000

# CORS
CORS_ALLOWED_ORIGINS=http://127.0.0.1:5500,http://localhost:5500

# Default Admin Account
ADMIN_EMAIL=admin@promptstudio.com
ADMIN_PASSWORD=your_admin_password
ADMIN_NAME=Studio Admin

# Server
SERVER_PORT=8080
```

---

## 🌐 API Overview

All endpoints are documented interactively via Swagger UI at `/swagger-ui/index.html`. High-level summary:

<details>
<summary><strong>🔐 Authentication</strong> — <code>/api/auth</code></summary>

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Register a new account |
| POST | `/api/auth/login` | Authenticate and receive a JWT |

</details>

<details>
<summary><strong>👤 User Profile</strong> — <code>/api/users</code></summary>

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/users/me` | Get the authenticated user's profile |
| PUT | `/api/users/me` | Update profile information |
| PUT | `/api/users/me/password` | Change password |
| GET | `/api/users/me/dashboard-stats` | Get dashboard statistics |

</details>

<details>
<summary><strong>⚡ Prompt Generation, Optimization & Analysis</strong> — <code>/api/prompts</code></summary>

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/prompts/generate` | Generate a new prompt in a chosen style |
| POST | `/api/prompts/optimize` | Optimize an existing prompt |
| POST | `/api/prompts/analyze` | Analyze and score a prompt |
| POST | `/api/prompts/battle` | Run a Battle Arena comparison |

</details>

<details>
<summary><strong>📚 Library & History</strong> — <code>/api/prompts</code></summary>

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/prompts/library` | Save a new prompt to the library |
| GET | `/api/prompts/library` | List saved prompts (paginated) |
| GET | `/api/prompts/library/search` | Search saved prompts |
| PUT | `/api/prompts/{id}` | Update a saved prompt |
| DELETE | `/api/prompts/{id}` | Delete a prompt |
| GET | `/api/prompts/history` | List full prompt history |
| DELETE | `/api/prompts/history` | Clear unsaved history |

</details>

<details>
<summary><strong>🗂️ Collections</strong> — <code>/api/prompts/collections</code></summary>

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/prompts/collections` | Create a collection |
| GET | `/api/prompts/collections` | List collections |
| PUT | `/api/prompts/collections/{id}` | Update a collection |
| DELETE | `/api/prompts/collections/{id}` | Delete a collection |

</details>

<details>
<summary><strong>💬 AI Chat</strong> — <code>/api/chat</code></summary>

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/chat/sessions` | Create a chat session |
| GET | `/api/chat/sessions` | List chat sessions |
| POST | `/api/chat/sessions/{id}/messages` | Send a message |

</details>

<details>
<summary><strong>🛡️ Admin</strong> — <code>/api/admin</code></summary>

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/admin/users` | List all users |
| PATCH | `/api/admin/users/{id}/status` | Activate/deactivate a user |
| GET | `/api/admin/stats` | Platform-wide statistics |

</details>

---

## 🗄️ Database Design

MongoDB collections:

| Collection | Purpose |
|---|---|
| `users` | Account credentials, profile info, and role |
| `prompts` | Unified record for every Generator/Optimizer/Analyzer/Battle/Library entry, including embedded analysis scores and battle results |
| `prompt_collections` | User-defined groupings of saved prompts |
| `chat_sessions` | AI Chat sessions, with messages embedded as a nested list |

---

## 🔎 Semantic Search with ChromaDB

When enabled, every prompt saved to the Library is embedded using Gemini's text embedding model and stored in a ChromaDB collection alongside its metadata. Library search then performs a vector similarity query, returning prompts that are *conceptually* similar to a search query — not just ones containing matching keywords. This is a fully optional layer: with it disabled, the Library falls back to standard MongoDB keyword search with no loss of core functionality.

---

## 🛡️ Security Features

- **JWT Authentication** — stateless, signed tokens for every protected request
- **BCrypt** — industry-standard password hashing, never storing plaintext
- **Spring Security** — role-based access control (`USER` / `ADMIN`) enforced at the filter-chain level
- **Explicit 401 vs 403 handling** — clear separation between "not authenticated" and "not authorized"
- **Input Validation** — Jakarta Bean Validation on every request DTO
- **Global Exception Handling** — consistent, structured JSON error responses across the entire API
- **CORS** — restricted to explicitly trusted origins

---

## 🚀 Future Enhancements

- 🔗 Multi-LLM support (OpenAI, Claude, Mistral)
- 🎙️ Voice-to-Prompt input
- 🛒 Prompt Marketplace for sharing and monetizing prompts
- 👥 Team collaboration and shared workspaces
- 🧩 Browser extension for in-context prompt generation
- 🤖 AI-driven prompt recommendations based on usage patterns
- 🕰️ Version control / diffing for prompt library entries

---

## 🤝 Contributing

Contributions are welcome and appreciated!

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

Please ensure your code follows the existing style and includes relevant tests where applicable.

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

---

Project Link: [https://github.com/yourusername/ai-prompt-engineering-studio](https://github.com/yourusername/ai-prompt-engineering-studio)

---


</div>
