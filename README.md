# JavaBank — Full-Stack Banking Application

A complete full-stack banking demo: a Java REST API backend and a
vanilla HTML/CSS/JS frontend, containerized with Docker.

## Stack

- **Backend:** Plain Java (JDK 21), using `com.sun.net.httpserver.HttpServer`
  for the REST API — **zero external dependencies**, so the Docker build
  never needs to download a Maven/Gradle dependency tree.
- **Frontend:** Static HTML/CSS/JS single-page app, served by nginx.
- **Persistence:** Java object serialization to a file (`data/bank.dat`),
  stored in a Docker named volume so data survives container restarts.
- **Orchestration:** `docker-compose.yml` wires backend + frontend together.

## Project structure

```
JavaBankFullStack/
├── backend/
│   ├── src/
│   │   ├── Account.java              (abstract base class)
│   │   ├── SavingsAccount.java       (min balance, monthly interest)
│   │   ├── CurrentAccount.java       (overdraft, monthly fee)
│   │   ├── Transaction.java          (ledger entry)
│   │   ├── Bank.java                 (account registry + persistence)
│   │   ├── ApiServer.java            (REST API + main method)
│   │   ├── Json.java                 (minimal JSON parse/serialize helper)
│   │   ├── InsufficientFundsException.java
│   │   └── NoSuchAccountException.java
│   └── Dockerfile
├── frontend/
│   ├── index.html
│   ├── style.css
│   ├── app.js
│   └── Dockerfile
├── docker-compose.yml
└── README.md
```

## Running with Docker (recommended)

From the project root:

```bash
docker compose up --build
```

- Frontend: **http://localhost:3000**
- Backend API: **http://localhost:8080**

Data persists in the `bank-data` Docker volume between restarts. To wipe it:

```bash
docker compose down -v
```

## Running without Docker

**Backend** (requires JDK 21+):

```bash
cd backend
javac -d out src/*.java
java -cp out ApiServer
```

The API listens on port `8080` by default (override with the `PORT`
environment variable).

**Frontend:** just open `frontend/index.html` in a browser, or serve the
folder with any static file server, e.g.:

```bash
cd frontend
python3 -m http.server 3000
```

Then visit `http://localhost:3000`. The frontend calls the backend at
`http://localhost:8080` by default — edit `API_BASE` at the top of
`app.js` if your backend runs elsewhere.

## API reference

| Method | Path                                | Body / Query                                        | Description                            |
|--------|--------------------------------------|-------------------------------------------------------|------------------------------------------|
| GET    | `/api/health`                        | —                                                       | Health check                              |
| GET    | `/api/accounts`                      | —                                                       | List all accounts                         |
| POST   | `/api/accounts`                      | `{type, holderName, pin, openingBalance}`               | Open a new account                        |
| GET    | `/api/accounts/{accNum}/balance`     | `?pin=`                                                 | Get balance                               |
| GET    | `/api/accounts/{accNum}/statement`   | `?pin=`                                                 | Full transaction history                  |
| POST   | `/api/accounts/{accNum}/deposit`     | `{pin, amount}`                                         | Deposit funds                             |
| POST   | `/api/accounts/{accNum}/withdraw`    | `{pin, amount}`                                         | Withdraw funds                            |
| POST   | `/api/transfer`                      | `{fromAccountNumber, toAccountNumber, pin, amount}`      | Transfer between accounts                 |
| POST   | `/api/monthend`                      | —                                                       | Apply interest/fees to all accounts       |
| DELETE | `/api/accounts/{accNum}`             | `?pin=`                                                 | Close an account (must be zero balance)   |

`type` is `SAVINGS` or `CURRENT`. All error responses are
`{"error": "message"}` with an appropriate HTTP status code (400/401/404/409).

### Example: open an account and deposit

```bash
curl -X POST http://localhost:8080/api/accounts \
  -H "Content-Type: application/json" \
  -d '{"type":"SAVINGS","holderName":"Jane Doe","pin":"1234","openingBalance":1000}'

# -> {"accountNumber":"ACC100000","holderName":"Jane Doe","type":"SAVINGS","balance":1000.0}

curl -X POST http://localhost:8080/api/accounts/ACC100000/deposit \
  -H "Content-Type: application/json" \
  -d '{"pin":"1234","amount":250}'
```

## Design notes

- `Account` is abstract; `SavingsAccount` and `CurrentAccount` override
  `canWithdraw()`, `accountType()`, and `applyMonthlySchedule()` — a
  polymorphism example carried over from the original console version.
- `Bank.transfer()` always withdraws from the source first and throws
  before crediting the destination, so a failed transfer never leaves
  money "created" on the receiving side.
- PIN checks are done server-side on every mutating and read
  operation. This is a demo app — PINs are not hashed and there's no
  session/token auth, so don't reuse this for anything real.
- The backend intentionally avoids frameworks (Spring, etc.) and
  external JSON libraries so the whole thing builds offline once the
  base Docker images are pulled — useful in restricted network
  environments.
