# Lightweight Database Engine

A small, single-file relational database engine written from scratch in Java, with a CRUD service layer and an interactive shell for creating tables and running inserts/selects/deletes. Built as a learning project to explore how databases work under the hood: pages, slots, a catalog, a primary-key index, and a binary row format.

> ⚠️ This is a learning/portfolio project, not production software. See [Known Limitations](#known-limitations) below before relying on it for anything real.

## Features

- **Single-file storage** — an entire database (schema + data) lives in one binary file on disk, accessed via `RandomAccessFile`.
- **Page-based storage** — data is stored in fixed-size 4 KB pages, each split into fixed-size slots.
- **Schema catalog** — table definitions (columns, types, primary key) are persisted as JSON (via Jackson) inside the same file.
- **Primary-key hash index** — every table keeps an in-memory `HashMap<String, Location>` mapping primary keys to `(pageId, slotId)`, rebuilt from disk on startup.
- **Binary row (de)serialization** — rows are packed/unpacked into raw bytes according to the table's column types (`INT`, `STRING`).
- **CRUD service** — `create table`, `insert`, `get`, `delete`, `getAll`, layered over the storage engine.
- **Interactive console dashboard** — a `Scanner`-based REPL for creating tables and running basic commands without writing Java.

## Architecture

```
DashBoard.Dashboard        (console REPL)
        │
        ▼
  CRUD.CrudService          (table/row operations)
        │
        ▼
db_engine.StorageEngine     (page I/O, index rebuild, orchestration)
   │         │         │
   ▼         ▼         ▼
PageManager  CatalogManager  SuperBlock
   │              │
   ▼              ▼
  Page         TableMeta ──> Column, HashIndex
```

### Package layout

```
src/main/java/
├── db_engine/
│   ├── Column.java                  Column definition (name, type, isPrimaryKey)
│   ├── Constants.java                PAGE_SIZE, SUPERBLOCK_SIZE, MAX_SLOTS, etc.
│   ├── HashIndex.java                In-memory pk -> (pageId, slotId) index
│   ├── Json.java                     Thin Jackson ObjectMapper wrapper
│   ├── Page.java                     A fixed-size (4096-byte) page buffer
│   ├── PageManager.java              Slot allocation/read/delete within a page
│   ├── Row.java                      In-memory row as a column-name -> value map
│   ├── RowSerializer.java            Row -> bytes (and primary-key extraction)
│   ├── RowDeserializer.java          Bytes -> Row
│   ├── Slot.java                     Slot metadata (offset, size, deleted flag)
│   ├── StorageEngine.java            Page I/O + catalog + index rebuild
│   ├── SuperBlock.java               File header: catalog offset/size, next page id
│   ├── CatalogManager.java           Persists table schemas as JSON in the file
│   ├── TableMeta.java                Table schema + its HashIndex
│   ├── DuplicateKeyException.java
│   ├── RowNotFoundException.java
│   ├── SchemaException.java
│   └── TableNotFoundException.java
├── CRUD/
│   └── CrudService.java              createTable / insert / get / delete / getAll
└── DashBoard/
    └── Dashboard.java                 Console REPL (CREATE, USE, INSERT, SELECT, DELETE)
```

## How it works

### On-disk layout

The database file is divided into three logical regions, tracked by the `SuperBlock` (written at offset 0):

| Field             | Type   | Description                                  |
|-------------------|--------|-----------------------------------------------|
| `catalogOffset`   | long   | Byte offset where the JSON table catalog lives |
| `catalogSize`     | long   | Size in bytes of the catalog JSON              |
| `pageStartOffset` | long   | Byte offset where the page region begins       |
| `nextPageId`      | int    | Next page id to allocate                       |

On startup, `StorageEngine`:
1. Opens the file with `RandomAccessFile`. If it's empty, writes a fresh `SuperBlock`; otherwise reads the existing one.
2. Loads the catalog (table schemas) via `CatalogManager`, deserializing the JSON blob into `Map<String, TableMeta>`.
3. Calls `rebuildIndex()`, which scans every page/slot on disk and repopulates each table's `HashIndex` by extracting the primary key from each stored record.

### Pages and slots

Each `Page` is a flat `byte[4096]` array. `PageManager` carves it into fixed-size **128-byte slots**:

- `insert(page, record)` scans for the first all-zero slot and copies the record in.
- `read(page, slotId)` copies the 128 bytes back out.
- `delete(page, slotId)` zeroes the slot out.

### Rows and serialization

A `Row` is just a `Map<String, Object>`. `RowSerializer` writes it to bytes following the table's column order: an `INT` column writes 4 bytes, a `STRING` column writes a length-prefixed UTF-8-ish byte sequence. `RowDeserializer` reads it back the same way. `RowSerializer.extractPrimaryKey` can pull just the primary-key value out of a raw record without fully deserializing it, which is what `rebuildIndex()` uses.

### Catalog

`CatalogManager` serializes the entire `Map<String, TableMeta>` to JSON (using Jackson, via the `Json` helper class) and writes it at `catalogOffset` every time a table is added. `TableMeta`'s `HashIndex` field is marked `@JsonIgnore` so the index itself is never persisted — it's always rebuilt from the page data on load.

### CRUD layer

`CrudService` wraps the storage engine with table-and-row-level operations:

- `createTable(TableMeta)` — registers a new table in the catalog.
- `insert(tableName, pk, row)` — serializes the row and inserts it into a page, then records its location in the table's index.
- `get(tableName, pk)` — looks up the index, reads the slot, deserializes.
- `delete(tableName, pk)` — zeroes the slot and removes the index entry.
- `getAll(tableName)` — iterates every key in the table's index and fetches each row.

### Dashboard (console REPL)

`Dashboard` is a simple `Scanner`-driven loop with two modes:

- **Global mode** — `CREATE` (interactively define a new table's columns/types/primary key), `USE <table>`, `EXIT`.
- **Table mode** (after `USE`) — `INSERT`, `SELECT <pk>`, `SELECT *`, `DELETE <pk>`, `BACK`.

## Getting started

### Prerequisites

- Java 17+ (uses switch expressions / pattern matching syntax in `Dashboard.java`)
- Maven (project uses a standard `src/main/java` layout)
- [Jackson](https://github.com/FasterXML/jackson) (`jackson-databind`) for catalog (de)serialization

### Build

```bash
mvn clean package
```

### Run

The `Dashboard` class expects a `CrudService` wired up to a `StorageEngine` pointed at a database file, e.g.:

```java
StorageEngine engine = new StorageEngine("mydb.db");
CrudService crud = new CrudService(engine);
new Dashboard(crud).run();
```

Once running, a typical session looks like:

```
================ DB SHELL ================
Tables: []
Mode: GLOBAL
Commands: CREATE, USE <table>, EXIT
> CREATE
Table name: USERS
Number of columns: 2

Column 1
Name: id
Type (INT/STRING): INT
Primary Key? (y/n): y

Column 2
Name: name
Type (INT/STRING): STRING
Primary Key? (y/n): n
✔ Table created: USERS

> USE USERS
✔ Using table: USERS

Mode: TABLE -> USERS
Commands: INSERT, SELECT <pk>, SELECT *, DELETE <pk>, BACK
> INSERT
id (INT): 1
name (STRING): Ada
Primary Key value: 1
✔ Inserted

> SELECT 1
{id=1, name=Ada}

> SELECT *
{id=1, name=Ada}
```

## Known limitations

This is a from-scratch educational implementation, so it cuts corners that a real database engine wouldn't:

- **Single-page storage** — `CrudService.insert` always writes to page 0 (flagged in the source as a temporary fix), so total capacity is bounded by `4096 / 128 = 32` records regardless of `nextPageId`.
- **Fixed 128-byte slots** — a serialized row larger than 128 bytes will silently truncate (`PageManager.insert` uses `Math.min(record.length, SLOT_SIZE)`), and rows are not allowed to span multiple slots.
- **No real "page full" recovery** — `PageManager.insert` throws once a page's slots are exhausted rather than allocating a new page.
- **No type validation beyond `INT`/`STRING`** — `RowSerializer`/`RowDeserializer` only understand two types.
- **No concurrency control** — no locking, no transactions, no crash recovery/WAL.
- **No query language** — there's no filtering, joins, or scanning by anything other than primary key; `getAll` just iterates the index.
- **No duplicate-key checking on insert** — `CrudService.insert` will silently overwrite an existing primary key's index entry rather than raising `DuplicateKeyException` (the exception class exists but isn't wired in yet).
- **Defined but unused exceptions** — `RowNotFoundException`, `DuplicateKeyException`, `SchemaException`, and `TableNotFoundException` exist as types but most code paths currently return `null` or silently no-op instead of throwing them.

## Roadmap ideas

- Multi-page storage with free-space tracking
- Variable-length records (instead of fixed 128-byte slots)
- Wiring up the existing exception types for proper error handling
- Basic `WHERE`-style filtering on `getAll`
- Secondary indexes beyond the primary key
- Write-ahead logging / crash recovery

## License

No license file is currently included in the repository — add one (MIT/Apache-2.0 are common choices for projects like this) if you intend for others to reuse the code.