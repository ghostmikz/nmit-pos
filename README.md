# NMIT-POS

A point-of-sale desktop application built with Java Swing. Supports bilingual UI (English / Mongolian), role-based access, inventory management, sales reporting, and PDF export.

## Features

| Module | Description |
|---|---|
| POS Terminal | Product grid, cart, cash/card/QPay/Monpay checkout, receipt number generation |
| Inventory | Product and category CRUD, image upload, stock tracking, low-stock alerts |
| Dashboard | KPI cards (today's transactions, revenue, discounts), low-stock list, top products, weekly bar chart |
| Reports | Date-range filtered sales table, cashier/payment breakdown, PDF export |
| User Management | Add/edit staff accounts, role assignment (admin/manager/cashier), activate/deactivate |
| Settings | Language toggle (EN/MN), server host/port |

## Tech Stack

- **Language:** Java 21
- **UI:** Swing (custom-painted components, no LAF dependencies)
- **Database:** MySQL 8 (via stored procedures and views)
- **Networking:** Raw TCP sockets with JSON (Gson)
- **Libraries:** Gson 2.10, MySQL Connector/J 8, OpenPDF 1.3, jBCrypt 0.4, JFreeChart 1.5 (optional)

## Project Structure

```
NMIT-POS/
├── lib/                        # Shared JAR dependencies
├── pos-client/src/             # Client source
│   ├── controller/             # MVC controllers
│   ├── model/                  # Client-side models
│   ├── view/panels/            # Swing panels
│   ├── client/SocketClient     # TCP socket singleton
│   └── i18n/                   # EN + MN translations
├── pos-server/src/             # Server source
│   ├── handler/                # Request handlers per domain
│   ├── dao/                    # Database access objects
│   ├── model/                  # Server-side models
│   └── server/                 # Socket server + session manager
├── db/procedures/              # MySQL stored procedures
├── run-client.sh               # Compile + run client
└── run-server.sh               # Compile + run server
```

## Prerequisites

- Java 21+ (JetBrains Runtime recommended on Linux/Wayland for correct HiDPI scaling)
- MySQL 8 running on `localhost:3306` with database `pos_db`
- All JARs present in `lib/`

## Setup

### 1. Database

Create the `pos_db` database and run the stored procedures in `db/procedures/`. The default connection in `DatabaseConnection.java` uses:

```
URL:      jdbc:mysql://localhost:3306/pos_db
User:     root
Password: 0312
```

Change these in `pos-server/src/dao/DatabaseConnection.java` if needed.

### 2. Run the server

```bash
./run-server.sh
```

The server listens on port `9090` by default. Change `settings.properties` to use a different port.

### 3. Run the client

```bash
./run-client.sh
```

The script compiles all sources, copies resources, and launches the app. On first run, log in with the default admin account.

## Default Credentials

| Username | Password | Role |
|---|---|---|
| admin | admin | Admin |

## User Roles

| Role | Access |
|---|---|
| Admin | Everything including user management |
| Manager | Inventory, Dashboard, Reports, POS |
| Cashier | POS Terminal only |

## Settings

`settings.properties` in the project root controls:

```properties
language=en          # en or mn
server.host=localhost
server.port=9090
```

The language can also be changed from the Settings panel inside the app; it persists to this file.
