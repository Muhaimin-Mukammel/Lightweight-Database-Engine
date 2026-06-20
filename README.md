# Lightweight Database Engine (V3)

A **custom file-based paged database engine** written in Java. This project implements its own binary storage format using fixed-size pages, primary key hashing, and a clean interactive CLI.

**Current Version:** V3 (Completed) — Binary paging system + Hash indexing + Functional CRUD via Dashboard.

---

## Features

- **Custom Binary File Format** — Uses `RandomAccessFile` with fixed 128-byte slots
- **SuperBlock** — Professional database file header with metadata
- **HashIndex** — Fast in-memory primary key lookups (`O(1)`) with `Location` (page + slot)
- **Page & Slot Management** — Efficient space allocation and reuse
- **CRUD Operations** — Create tables, Insert, Select, Delete (Update coming soon)
- **Interactive Dashboard** — User-friendly command-line interface
- **Catalog Management** — Persistent table schemas and metadata
- **Hybrid Design** — Persistent binary storage + in-memory indexing

---

## Architecture Highlights

The project follows a clean layered architecture:

```
Dashboard → CrudService → StorageEngine + HashIndex → PageManager + SuperBlock
```

**Components I'm most proud of:**
- **SuperBlock** — Clean, fixed-layout header
- **HashIndex** — Simple yet powerful indexing
- **Dashboard + CRUD** — Excellent abstraction that hides all the low-level complexity

---

## How to Build & Run

### Prerequisites
- Java 25+
- Maven

### Build
```bash
mvn clean compile
```

### Run
```bash
mvn exec:java -Dexec.mainClass="Main"
```

Or manually:
```bash
java -cp target/classes Main
```

The database file `data.db` will be created in the project root.

---

## CLI Usage (Dashboard)

### Global Mode Commands
- `CREATE TABLE <name>` — Launch table creation wizard
- `LIST TABLES` — Show all tables
- `USE <table>` — Enter table mode
- `EXIT` — Quit the program

### Table Mode Commands
- `INSERT` — Add a new record (guided input)
- `SELECT` — Display all records in the table
- `DELETE <pk_value>` — Delete record by primary key
- `BACK` — Return to global mode
- `EXIT`

---

## Project Structure

```
src/main/java/
├── Main.java
├── DashBoard/
│   └── Dashboard.java
├── CRUD/
│   └── CrudService.java
└── db_engine/
    ├── SuperBlock.java
    ├── PageManager.java
    ├── StorageEngine.java
    ├── HashIndex.java
    ├── Page.java
    ├── Slot.java
    ├── RowSerializer.java
    ├── RowDeserializer.java
    ├── TableMeta.java
    ├── Column.java
    ├── CatalogManager.java
    ├── Constants.java
    └── ...
```

---

## Version Roadmap

### Completed
- **V1** — Basic model + persistence
- **V2** — Relational concepts
- **V3** — Binary paged storage + Hash indexing + CLI (Current)

### Upcoming (V4)
- B-Tree indexing for range queries
- `UPDATE` operation
- Basic `WHERE` filtering
- Free list for better space management
- Improved durability and error handling

---

## Current Limitations (V3)

- Fixed slot size (128 bytes)
- No `UPDATE` yet
- Limited data type support
- Full scans for non-primary key operations
- Index is rebuilt on every startup
- No transaction support or concurrency

---

## Learning Objectives

This project was built to deeply understand how real databases work under the hood — file formats, paging, indexing, and clean abstraction layers.

**Key Achievement**: All the low-level complexity is successfully hidden behind a simple and pleasant user interface.

---

## Future Enhancements

- SQL-like query parser
- Secondary indexes
- Variable-length records + overflow pages
- Basic transaction support
- Import/Export utilities

---

**Built with ❤️ for learning systems and database internals.**

Feedback and suggestions are always welcome!
