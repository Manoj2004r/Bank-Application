# JavaBank — Full-Stack Banking Application

A full-stack banking demo built with a lightweight Java REST API and a vanilla HTML, CSS, and JavaScript frontend. The project demonstrates object-oriented programming, REST API development, Docker containerization, Kubernetes orchestration, and infrastructure observability with Prometheus and Grafana.

> **Disclaimer:** JavaBank is an educational demo, not a production banking system. Do not use real customer information, account credentials, or financial data.

## Architecture

The frontend is hosted on Amazon S3 in the deployed environment and communicates with the backend through its configured API endpoint. The backend can be containerized and orchestrated using Docker Compose or Kubernetes. Prometheus and Grafana provide monitoring for the Kubernetes environment.

```text
                         User / Browser
                               |
                               v
                      Amazon S3 Frontend
                       (HTML / CSS / JS)
                               |
                               | REST API requests
                               v
                         Java REST API
                         (JDK 21)
                               |
                               v
                     File-Based Persistence
                         bank.dat file
                               |
                               v
                    Docker Volume / Storage


                 Kubernetes Observability
                               |
                     +---------+---------+
                     |                   |
                     v                   v
                 Prometheus            Grafana
                 Metrics               Dashboards
```

The storage and deployment paths depend on the environment. The Docker Compose setup uses a named volume for persistence. Verify the Kubernetes storage configuration separately before assuming that data persists across pod replacements.

## Technology Stack

| Category | Technologies |
|---|---|
| Backend | Java, JDK 21, `HttpServer` |
| Frontend | HTML5, CSS3, JavaScript |
| Persistence | Java object serialization |
| Containerization | Docker |
| Local orchestration | Docker Compose |
| Container orchestration | Kubernetes |
| Monitoring | Prometheus |
| Dashboards | Grafana |
| Frontend hosting | Amazon S3 |

## Features

- Open savings and current accounts
- Deposit and withdraw funds
- Transfer funds between accounts
- Retrieve account balances
- View transaction statements
- Close eligible accounts
- Apply monthly interest and fees
- Check API health
- Persist account data to a file in the Docker Compose environment

## Application Design

The backend uses plain Java and the built-in `com.sun.net.httpserver.HttpServer` for HTTP handling, without external application-framework or JSON-library dependencies.

Key classes include:

- `Account` — abstract base class for account behavior
- `SavingsAccount` — savings-account rules and monthly interest
- `CurrentAccount` — current-account overdraft and fee behavior
- `Transaction` — transaction ledger entries
- `Bank` — account registry, business logic, and persistence
- `ApiServer` — REST API entry point
- `Json` — lightweight JSON parsing and serialization helper

The application demonstrates object-oriented principles, including abstraction, inheritance, polymorphism, and exception handling.

## Repository Structure

```text
Bank-Application/
├── backend/
│   ├── src/
│   └── Dockerfile
├── frontend/
│   ├── index.html
│   ├── style.css
│   ├── app.js
│   └── Dockerfile
├── k8s/
│   └── Kubernetes manifests
├── docker-compose.yml
├── .gitignore
└── README.md
```

## Run Locally with Docker Compose

**Prerequisites:** Docker and Docker Compose.

From the project root, run:

```bash
docker compose up --build
```

Default local endpoints:

- Frontend: `http://localhost:3000`
- Backend API: `http://localhost:8080`
- Health check: `http://localhost:8080/api/health`

The `bank-data` named Docker volume stores the serialized bank data across container restarts, provided the volume is retained.

To stop the services:

```bash
docker compose down
```

To remove the Compose-managed volume as well, use:

```bash
docker compose down -v
```

**Warning:** Removing the volume deletes the bank data stored in it.

## Run Without Docker

### Backend

Requires JDK 21 or later.

```bash
cd backend
javac -d out src/*.java
java -cp out ApiServer
```

The API listens on port `8080` by default. The port can be overridden using the `PORT` environment variable.

### Frontend

Open `frontend/index.html` in a browser or serve the frontend directory using a static file server:

```bash
cd frontend
python3 -m http.server 3000
```

Visit `http://localhost:3000`.

The frontend's `API_BASE` setting must point to the address where the backend is reachable from the browser.

## Kubernetes Deployment

Kubernetes manifests are maintained in the `k8s/` directory.

The Kubernetes deployment work demonstrates container orchestration and management of application workloads.

When deploying, verify the following:

- Deployment image names and tags
- Service types and port mappings
- Frontend-to-backend API connectivity
- Environment variables and configuration
- Health probes and resource requests or limits
- Persistent storage for `bank.dat`, if data must survive pod replacement

The Kubernetes deployment should be configured and tested against the actual manifests in the repository.

## Monitoring and Observability

Prometheus and Grafana are used to monitor Kubernetes resources and visualize metrics.

The Grafana dashboard includes views for:

- Cluster CPU utilization
- Cluster memory utilization
- CPU and memory usage by namespace
- Kubernetes resource counts
- Node and running pod counts

Prometheus collects metrics, while Grafana presents them in dashboards. These tools help demonstrate observability practices for containerized workloads.

## REST API Reference

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/health` | Health check |
| GET | `/api/accounts` | List accounts |
| POST | `/api/accounts` | Open an account |
| GET | `/api/accounts/{accNum}/balance?pin=` | Get account balance |
| GET | `/api/accounts/{accNum}/statement?pin=` | Retrieve transaction history |
| POST | `/api/accounts/{accNum}/deposit` | Deposit funds |
| POST | `/api/accounts/{accNum}/withdraw` | Withdraw funds |
| POST | `/api/transfer` | Transfer funds |
| POST | `/api/monthend` | Apply monthly interest and fees |
| DELETE | `/api/accounts/{accNum}?pin=` | Close an eligible account |

The account-opening request accepts `type`, `holderName`, `pin`, and `openingBalance`. Account types are `SAVINGS` and `CURRENT`.

## Security Considerations

This application is intended for learning and demonstration.

- Account PINs are not hashed in the current implementation.
- The application does not implement session-based or token-based authentication.
- Avoid exposing the API publicly without appropriate authentication and transport security.
- Do not commit credentials, real account data, or sensitive environment files.
- Review Kubernetes network exposure and permissions before deployment.
- Treat file-based persistence and serialized data as application data that requires appropriate protection.

## Future Improvements

- Build automated CI/CD pipelines with Jenkins.
- Add alerting for application errors and resource pressure.
- Improve authentication and secure PIN handling.
- Add automated tests for account operations and API endpoints.
- Implement robust persistent storage for Kubernetes deployments.
- Explore GitOps-based deployment workflows using Argo CD or Flux.

## Learning Outcomes

This project brings together Java programming, REST API design, Docker, Kubernetes, and monitoring. It provides a practical foundation for further work in DevOps, cloud engineering, deployment automation, and observability.
