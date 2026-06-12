# PROJECT: Lightweight In-Memory + File-Based Database Engine

## OVERVIEW
This project is a custom database engine designed to support structured data storage, relational modeling, and efficient querying for ~100k+ records. The system uses a hybrid architecture:

- In-memory database for fast operations
- File-based storage (JSON) for persistence
- Controlled save/commit mechanism

---

## CORE IDEA
- Data is loaded from file into RAM at startup
- All operations happen in memory (fast crud + queries)
- Changes are tracked (dirty state)
- Data is written back to file only on explicit save/commit

---

## BASIC DATA MODEL
- Database
  - Tables
    - Columns (schema definition)
    - Rows (records)
    - Metadata (PK, FK rules, indexes)

### User-facing operations (crud only)
- Create
- Read
- Update
- Delete

---

## IN-MEMORY DESIGN
- Tables stored in RAM
- Rows stored as objects/maps
- Primary key lookup optimized using HashMap
- Foreign keys validated at runtime
- Dirty tracking system for pending changes

---

## PERSISTENCE LAYER
- JSON file acts as the source of truth for storage
- On startup: JSON → RAM reconstruction
- On save/commit: RAM → JSON serialization
- Optional confirmation layer before saving changes

---

## QUERY MODEL (FUTURE CORE)
- Filtering (WHERE conditions)
- Basic projections (select fields)
- Sorting (optional stage)
- Relationship traversal (FK-based access)
- ResultSet abstraction

---

## RELATIONSHIP MODEL
Supports relational structure like:

User
├─ Orders
│ ├─ Items
│ ├─ Payments
│ └─ Shipments
├─ Friends
├─ Messages
└─ Notifications


Implemented using:
- Foreign keys (FK)
- Reference validation
- Join-like operations (initially simple nested-loop joins)

---

## PERFORMANCE DESIGN
- Primary key lookup → HashMap (O(1))
- Relationship traversal → indexed lookups (later optimization)
- Target scale → ~100k records per database

---

## VERSION ROADMAP

### V1 — CORE DATABASE (FOUNDATION)
- Table system (columns + rows)
- crud operations
- JSON load/save
- Basic in-memory storage
- Dirty tracking + save confirmation layer

---

### V2 — RELATIONAL LAYER
- Primary Key enforcement
- Foreign Key validation
- Basic joins (nested-loop join initially)
- Multi-table relationships

---

### V3 — INDEXING LAYER
- HashMap-based indexes for fast lookup
- Query optimization for primary key access
- Faster filtering on indexed fields

---

### V4 — ADVANCED STRUCTURES
- B-Tree indexing (primary focus)
- Range queries (>, <, BETWEEN)
- Better sorting and scanning performance

---

## FINAL GOAL
A lightweight relational database engine capable of:

- Handling structured relational data
- Supporting 100k+ records efficiently
- Providing crud + query + join functionality
- Using memory-first execution with file persistence
