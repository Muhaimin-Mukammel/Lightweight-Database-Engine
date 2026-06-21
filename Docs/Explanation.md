# Lightweight Database Engine — Full Class-by-Class Explanation
### (Ordered by actual runtime execution sequence)

This document explains every class in the project — what it does, how it does it, and why it exists — in the order the program actually touches them when it runs, starting from boot (`StorageEngine` construction) through to a user typing commands in the `Dashboard` shell.

---

## Execution Order Overview

When you start the program, this is the real sequence of events:

1. **`Constants`** — static config values are referenced immediately (PAGE_SIZE, slot counts, etc.)
2. **`SuperBlock`** — file header is created or read first
3. **`Page`** — the raw 4KB buffer abstraction (used by everything below)
4. **`PageManager`** — slot-level read/write/delete logic inside a page
5. **`Slot`** — (unused leftover class, explained for completeness)
6. **`Json`** — JSON helper, used immediately by the catalog
7. **`Column`** — schema building block, needed before `TableMeta` can load
8. **`TableMeta`** — table schema definition (uses `Column`, `HashIndex`)
9. **`HashIndex`** — in-memory primary-key index per table
10. **`CatalogManager`** — loads/saves the table schema catalog (uses `Json`, `TableMeta`, `SuperBlock`)
11. **`StorageEngine`** — the orchestrator; wires together SuperBlock + CatalogManager + PageManager, then rebuilds indexes
12. **`RowSerializer`** — converts a `Row` to bytes (used once real CRUD operations begin)
13. **`RowDeserializer`** — converts bytes back to a `Row`
14. **`Row`** — the in-memory row representation used by serializer/deserializer/CRUD/dashboard
15. **`RowNotFoundException`**, **`DuplicateKeyException`**, **`SchemaException`**, **`TableNotFoundException`** — typed error vocabulary (defined early, used — or not used — later)
16. **`CrudService`** — the application-facing API layer built on top of `StorageEngine`
17. **`Dashboard`** — the interactive console shell that drives `CrudService` and is what the user actually types into

---

## 1. `Constants.java`

**What it does:** Centralizes magic numbers used throughout the engine:
- `PAGE_SIZE = 4096` — size of one page on disk (mirrors real OS/database page sizes).
- `SUPERBLOCK_SIZE = 32` — intended size of the header block.
- `CATALOG_RESERVED_SIZE = 10240` — reserved space for the JSON catalog.
- `MAX_SLOTS = 128` — number of row slots per page.

**Why:** Avoids hardcoding the same numbers in multiple files. Notably, `Page.java` *also* defines its own `PAGE_SIZE = 4096` independently rather than referencing `Constants.PAGE_SIZE` — a small duplication that's easy to miss if one value is ever changed without the other.

**Bug to flag:** `128 slots × 128 bytes/slot = 16,384 bytes`, but a page is only `4096` bytes. Only ~32 slots of 128 bytes actually fit in a 4096-byte page, yet `StorageEngine.rebuildIndex()` loops up to `MAX_SLOTS = 128`, which will read out of bounds past slot 31. This is the most serious structural inconsistency in the codebase.

---

## 2. `SuperBlock.java`

**What it does:** The file's header / "boot record" — the very first bytes of the database file, holding global metadata needed to interpret everything else.

**How:** Four fields, written/read in a fixed binary layout via `RandomAccessFile`:
- `catalogOffset` (long) — where in the file the JSON catalog starts.
- `catalogSize` (long) — how many bytes the catalog currently occupies.
- `pageStartOffset` (long) — where page data starts (right after the reserved catalog region).
- `nextPageId` (int) — the next page ID to allocate.

`write()` seeks to offset 0 and writes these four fields in order; `read()` does the mirror-image read.

**Why:** Every database file format needs a fixed, predictable place to start so the engine can bootstrap itself without first knowing the schema — directly analogous to a filesystem's superblock or SQLite's database header.

**Gap worth noting:** `nextPageId` is declared but never actually incremented anywhere in the codebase (`CrudService.insert` always writes to page 0). So multi-page growth isn't wired up yet, even though the field exists for it.

---

## 3. `Page.java`

**What it does:** Represents one fixed-size 4096-byte block of the database file in memory.

**How:** Holds an immutable `pageId` and a `byte[] data` of length `PAGE_SIZE`. No higher-level structure (no header, no slot directory) — it's a raw buffer.

**Why:** Mirrors how real storage engines treat pages as "dumb" fixed-size containers: the page itself doesn't know what's inside it; that's the `PageManager`'s job. Keeping `Page` minimal makes it trivial to read/write to/from disk.

---

## 4. `PageManager.java`

**What it does:** Implements low-level slotted storage *within* a single page: `insert(page, record)`, `read(page, slotId)`, `delete(page, slotId)`.

**How:**
- Defines `SLOT_SIZE = 128` (a local constant, separate from `Constants.MAX_SLOTS` — another duplication).
- `insert`: scans the page's byte array in 128-byte strides; a slot is considered "empty" if every byte in that 128-byte window is `0`. On the first empty slot found, it copies the record's bytes into that slot (truncating if the record is larger than 128 bytes) and returns the slot index (`slot / SLOT_SIZE`).
- `read`: given a `slotId`, computes `start = slotId * SLOT_SIZE` and copies out exactly 128 bytes.
- `delete`: zeroes out the 128-byte window for that slot.

**Why & key design facts:**
- A simplified slotted-page layout, but unlike real databases it has **no slot directory or free-list** — "emptiness" is inferred by scanning for all-zero bytes, which is fragile: a legitimately all-zero row could theoretically be misread as empty.
- Fixed-size 128-byte slots cap how large a serialized row can be. `RowSerializer` doesn't enforce this, so a row with a long `STRING` field will silently get truncated by `System.arraycopy`'s `Math.min(record.length, SLOT_SIZE)` — a real data-corruption risk for variable-length data.
- `read()` never returns `null` — it always returns a 128-byte array (possibly all zeroes for an empty slot). This matters later in `StorageEngine.rebuildIndex()`.

---

## 5. `Slot.java`

**What it does:** Defined as a small struct: `offset`, `size`, `deleted` — looks like it was meant to formalize slot metadata for a more proper slotted-page design (a real slot directory pointing at variable offsets/sizes).

**Why:** This class is **dead code** — nothing in the codebase instantiates or references `Slot`. `PageManager` computes slot offsets purely arithmetically instead. Strongly suggests an earlier design iteration that was abandoned in favor of a simpler fixed-stride scheme, but the leftover class was never deleted.

---

## 6. `Json.java`

**What it does:** A thin static wrapper around Jackson's `ObjectMapper`, providing `serialize(Object) → String` and `deserialize(String, TypeReference<T>) → T`.

**How:** A single shared `ObjectMapper` configured with `FAIL_ON_UNKNOWN_PROPERTIES = false` (so deserializing JSON with extra/missing fields won't throw). Both methods wrap Jackson's checked `JsonProcessingException` into an unchecked `RuntimeException`.

**Why:** This is the persistence mechanism for the *catalog* (table schemas), not for row data (rows use a custom binary format via `RowSerializer`/`RowDeserializer`). Using JSON for the catalog is pragmatic: schemas are small and don't need the packing efficiency rows need.

---

## 7. `Column.java`

**What it does:** A plain data holder describing one column of a table — its `name`, `type` (a string like `"INT"` or `"STRING"`), and whether it's `primaryKey`.

**How:** Three private fields with a no-arg constructor (needed for Jackson JSON deserialization) and a full constructor, plus getters/setters.

**Why:** Every other piece of the engine — serialization, deserialization, schema storage — needs to know "what columns exist and what type are they" to convert between Java objects and bytes. `Column` is the schema's atomic unit. Making `type` a raw `String` instead of an enum avoids defining a type system, but means typos are only caught via scattered `.equalsIgnoreCase` calls.

---

## 8. `TableMeta.java`

**What it does:** The schema definition for one table: its `name`, ordered list of `Column`s, the name of the `primaryKey` column, and (transient, not persisted) its `HashIndex`.

**How:** Two constructors — a no-arg one (for Jackson) and a full one. The `index` field is annotated `@JsonIgnore` so Jackson never tries to serialize/deserialize it as part of the catalog JSON (it's runtime-only state, rebuilt via `StorageEngine.rebuildIndex()`, not persisted data).

**Why:** This is the schema "contract" that every serializer/deserializer/CRUD operation consults to know column order and types. Storing it as JSON means table definitions survive restarts, while the index — purely derived, fast-to-rebuild state — deliberately isn't persisted.

---

## 9. `HashIndex.java`

**What it does:** An in-memory index from primary key (`String`) → `Location` (a `pageId` + `slotId` pair), used so that looking up a row by primary key doesn't require scanning the whole table.

**How:**
- Inner static class `Location` is an immutable pair `(pageId, slotId)`.
- Wraps a `java.util.HashMap<String, Location>` with `put`, `get`, `remove`, `clear`.
- A `raw()` accessor exposes the underlying map directly (added to support `CrudService.getAll()`, which needs to iterate all keys).

**Why:** This is the engine's only index structure — a simplified version of what a real DB's primary-key B-tree or hash index does. Because it's purely in-memory and not persisted, it must be rebuilt every time the engine starts up, which is the classic trade-off of "fast runtime lookups, but pay a full-scan cost once at boot."

---

## 10. `CatalogManager.java`

**What it does:** Owns the table-schema catalog: loading it from the database file at startup, saving it back, and providing CRUD-level access to add/list tables.

**How:**
- Holds a reference to the same `RandomAccessFile` and `SuperBlock` used by `StorageEngine` (shared, not duplicated).
- `load()`: if `sb.catalogSize <= 0`, there's nothing to load (fresh file). Otherwise computes how many bytes are actually available to read, reads them, trims, and deserializes into a `Map<String, TableMeta>` via Jackson's generic `TypeReference`.
- `save()`: serializes the whole `tables` map to JSON, writes it at `catalogOffset`, updates `sb.catalogSize`, and persists the updated `SuperBlock`.
- `addTable(t)`: inserts into the in-memory map, then immediately calls `save()`.

**Why:** The single writer/reader of the JSON-encoded catalog, keeping schema metadata physically separate from row data. Always re-serializing the *entire* catalog on every `save()` is simple and safe for a small number of tables, though it doesn't scale gracefully with hundreds of tables.

**Subtle gap:** `load()` guards against reading past end-of-file, but there's no corresponding guard in `save()` against writing past `Constants.CATALOG_RESERVED_SIZE` (10 KB) into territory meant for page data — if the catalog JSON ever exceeds that reserved size, it would silently corrupt the start of the page region.

---

## 11. `StorageEngine.java`

**What it does:** The central orchestrator tying together the physical file, the superblock header, the catalog, and page I/O. The only class that talks to the actual file on disk via `RandomAccessFile`.

**How, step by step:**
- **Constructor:** Opens (or creates) the file in `"rw"` mode. If the file is brand new (`length() == 0`), creates a fresh `SuperBlock` and writes it; otherwise reads the existing `SuperBlock`. Creates a `PageManager`, a `CatalogManager`, loads the catalog, and calls `rebuildIndex()`.
- **`readPage(pageId)` / `writePage(page)`:** Translates a logical `pageId` into a file offset: `superBlock.pageStartOffset + pageId * PAGE_SIZE`. `readPage` reads `PAGE_SIZE` bytes if the offset is within file length; otherwise returns a fresh all-zero page.
- **`rebuildIndex()`:** For every known table, clears its `HashIndex`, then scans every page from `0` to `superBlock.nextPageId`, and within each page scans every slot up to `Constants.MAX_SLOTS`, extracts each record's primary key via `RowSerializer.extractPrimaryKey`, and if non-empty, records `(pk → pageId, slotId)`.

**Why:** Embodies "the index is a cache, the file is the truth." Because `HashIndex` lives only in memory, every restart loses all knowledge of "where is row X" — `rebuildIndex()` reconstructs it by brute-force scanning the whole file.

**Bugs worth knowing:**
1. `rebuildIndex()` checks `if (record == null) continue;` — but `PageManager.read()` never returns `null`. This null-check is dead code; empty slots instead produce a primary key extracted from all-zero bytes, which the next check `pk.isEmpty()` only partially guards against (catches empty strings, not a numeric `0`).
2. The loop bound `Constants.MAX_SLOTS = 128` combined with `SLOT_SIZE = 128` means it iterates far beyond the ~32 slots that actually fit in a 4096-byte page, causing `PageManager.read` to throw `ArrayIndexOutOfBoundsException` once `start` exceeds 4096.
3. `initFile()` is dead/unused — the constructor calls `superBlock.write(raf)` directly instead.

---

## 12. `RowSerializer.java`

**What it does:** Two responsibilities: (a) convert a `Row` into a `byte[]` for storage, and (b) extract just the primary key value from an already-serialized `byte[]` record without fully deserializing it.

**How:**
- `serialize`: for each column in schema order, writes either a 4-byte `int` or a 4-byte length + raw string bytes.
- `extractPrimaryKey`: re-parses the byte array the same way, but only returns the value once it hits the column marked `isPrimaryKey()` — still reads (and discards) every preceding column's bytes correctly to keep the cursor aligned, since the format is positional.

**Why:** `extractPrimaryKey` exists specifically to support `StorageEngine.rebuildIndex()`: rebuilding the index at startup needs the primary key of every stored record without paying the cost of fully deserializing every row into a `Row` object. A "scan only what you need" shortcut.

---

## 13. `RowDeserializer.java`

**What it does:** Converts a raw `byte[]` slot back into a `Row`, using a table's schema (`TableMeta`) to know how to interpret the bytes.

**How:** Wraps the byte array in a `DataInputStream`, then iterates `meta.getColumns()` in order:
- `"INT"` columns: reads 4 bytes as a Java `int`.
- `"STRING"` columns: reads a 4-byte length prefix, then that many raw bytes, converted to a `String` (platform default charset — no charset specified).

**Why:** A fixed-format binary decoder relying entirely on column order matching the order bytes were written in (`RowSerializer`), with no field tags or self-describing structure. Fast and compact, but brittle: if the schema changes after data was written, old rows will deserialize garbage with no error, since there's no versioning or validation.

---

## 14. `Row.java`

**What it does:** An in-memory representation of one table row as a column-name → value map.

**How:** Wraps a `HashMap<String, Object>` with `put`, `get`, and `getValues()` (exposes the raw map, used by `Dashboard` to print rows).

**Why:** Decouples the "logical" row (used by application code, CRUD layer, Dashboard) from the "physical" row (raw bytes on disk). `RowSerializer`/`RowDeserializer` are the bridge. Using `Object` as the value type lets the same `Row` class hold both `Integer` and `String` values, at the cost of needing runtime casts elsewhere.

---

## 15. Exception Classes — `RowNotFoundException`, `DuplicateKeyException`, `SchemaException`, `TableNotFoundException`

These four are grouped together because they form the engine's intended error vocabulary, even though they're inconsistently used.

### `RowNotFoundException.java`
**What:** `RuntimeException` with message `"No row with key '<pk>' in table <tableName>"`.
**Why:** Meant to distinguish "row genuinely doesn't exist" from any other failure. **Not actually thrown anywhere** — `CrudService.get()` just returns `null` on a missing key instead.

### `DuplicateKeyException.java`
**What:** `RuntimeException` with message `"Primary key '<pk>' already exists in table <tableName>"`.
**Why:** Intended to enforce primary-key uniqueness. **Not actually thrown** — `CrudService.insert()` calls `table.getIndex().put(...)` directly without checking for an existing key first, so a duplicate insert silently overwrites the old index entry (orphaning the old slot's bytes on disk).

### `SchemaException.java`
**What:** A generic `RuntimeException` taking just a message — a catch-all for schema-related problems.
**Why:** Provides a typed "this is specifically a schema problem" signal. **Not currently thrown anywhere** — e.g. `Dashboard.parse()` throws a plain `RuntimeException("Unknown type")` instead of this.

### `TableNotFoundException.java`
**What:** `RuntimeException` with message `"Table not found: <tableName>"`.
**Why:** Same typed-exception pattern. Partially exercised in spirit only: `CrudService.insert()` throws a plain `new RuntimeException("Table not found: " + tableName)` instead of using this dedicated class — functionally similar but inconsistent.

---

## 16. `CrudService.java` (package `CRUD`)

**What it does:** The application-facing API that turns "table name + primary key + row" operations into the lower-level page/slot/index operations. This is the layer `Dashboard` actually talks to.

**How, per method:**
- **Constructor:** Takes a `StorageEngine`, caches its `PageManager` (`engine.pages()`).
- **`getTable(name)` / `listTables()`:** Thin pass-throughs to `engine.catalog().getTables()`.
- **`createTable(table)`:** Adds the table to the catalog and explicitly calls `save()` again — redundant, since `CatalogManager.addTable()` already calls `save()` internally; the catalog gets serialized to disk twice on every table creation.
- **`insert(tableName, pk, row)`:**
  1. Looks up the table's schema; throws a plain `RuntimeException` if missing.
  2. Serializes the `Row` to bytes via `RowSerializer`.
  3. **Always reads page 0** — the code comment `// ⚠️ TEMP FIX: still using page 0 (your system limitation)` is an honest admission that multi-page support isn't implemented; every row in every table gets crammed into the same one page.
  4. Calls `pageManager.insert(page, record)` to find a free 128-byte slot and write the bytes.
  5. Writes the page back to disk.
  6. Updates the table's `HashIndex` with `pk → (pageId=0, slotId)`.
- **`get(tableName, pk)`:** Looks up the `Location` in the table's `HashIndex`; if found, reads that page, reads the slot's bytes, deserializes into a `Row`. Returns `null` if table or key isn't found.
- **`delete(tableName, pk)`:** Same lookup pattern, but calls `pageManager.delete()` (zeroing the slot) and removes the key from the index.
- **`getAll(tableName)`:** Iterates every key in the table's index (via `HashIndex.raw().keySet()`) and calls `get()` for each — built entirely on top of the single-row `get()` rather than a more efficient page-scan.

**Why this design, and what it reveals:** A textbook "service layer" pattern — hides pages/slots/byte arrays behind a clean `insert/get/delete/getAll` API keyed by primary key. The "page 0 only" admission is the single most important limitation in the entire codebase: **all tables share one 4096-byte page**, capacity is roughly `4096 / 128 ≈ 32 rows total across the entire database`, and once that page fills, every insert throws `PageManager`'s `"Page full"` exception — regardless of which table you're inserting into. Consistent with `SuperBlock.nextPageId` never being incremented: the multi-page allocation logic it was designed to support simply hasn't been built yet.

---

## 17. `Dashboard.java` (package `DashBoard`)

**What it does:** A command-line REPL (read-eval-print loop) that's the human-facing entry point to the whole engine — lets a user create tables, switch into a table's context, and insert/select/delete rows by typing commands into a console.

**How, in detail:**
- Holds a `CrudService` (the API it drives) and a `Scanner` for reading console input, plus mutable state `currentTable` tracking which table (if any) is "selected" — giving the shell two modes: **global mode** (no table selected) and **table mode** (a table is selected).
- `run()`: an infinite loop. Each iteration prints the current mode, the list of known tables, and the commands valid in that mode, then reads one line and dispatches it to `handle()`.
- `handle(input)`:
  - `"EXIT"` → `System.exit(0)` (hard process exit — no flush/cleanup beyond what's already happened, since every write call already persists immediately).
  - `"BACK"` → clears `currentTable`, returning to global mode.
  - In global mode: `"CREATE"` → `createTable()`; `"USE <name>"` → validates the table exists (uppercased lookup) and switches `currentTable`.
  - Anything else dispatches to `handleTable(input)` (table mode).
- `createTable()`: interactively prompts for table name, column count, and for each column: name, type, and whether it's the primary key — builds a `List<Column>`, constructs a `TableMeta`, calls `crud.createTable(table)`.
- `handleTable(input)`: re-fetches the `TableMeta` for `currentTable` (defensive — resets to global mode if it's somehow gone); dispatches `INSERT`, `SELECT *`, `SELECT <pk>`, `DELETE <pk>`.
- `insert(table)`: for every column in the schema, prompts for a value, parses it according to declared type (`parse()`), builds a `Row`, then separately prompts for the primary key value and calls `crud.insert(...)`.
- `select(pk)` / `selectAll()` / `delete(pk)`: thin wrappers around the corresponding `CrudService` calls, printing results or error messages.
- `parse(val, type)`: converts a typed-in string into an `Integer` or leaves it as a `String`, based on the column's declared type; throws a plain `RuntimeException` for unknown types.

**Why:** The "psql"/"sqlite3 shell" equivalent for this toy engine — a way to manually exercise every CRUD operation without writing Java test code. The two-mode design mirrors how real DB shells have a notion of "current database/schema context" so commands don't need to repeat the table name every time.

**Leftover noted:** `handleGlobal()` exists but is dead code — `handle()` inlines the same logic itself rather than calling it, another small remnant from refactoring.

---

## End-to-End Flow Recap

1. **Startup:** `StorageEngine` opens/creates the file, reads or writes the `SuperBlock`, loads the JSON catalog via `CatalogManager`, and rebuilds every table's `HashIndex` by scanning pages. `Dashboard` is constructed wrapping a `CrudService` wrapping that `StorageEngine`.
2. **Creating a table:** User types `CREATE` → `Dashboard` collects column definitions → builds a `TableMeta` → `CrudService.createTable()` → `CatalogManager.addTable()` persists the updated schema map as JSON at the catalog offset.
3. **Inserting a row:** User types `INSERT` in table mode → `Dashboard` builds a `Row` from typed input → `CrudService.insert()` serializes it with `RowSerializer`, finds a free slot in page 0 via `PageManager`, writes the page back to disk, records `pk → (0, slotId)` in the table's `HashIndex`.
4. **Selecting a row:** `CrudService.get()` looks up `(pageId, slotId)` in the index, reads that 128-byte slot, reconstructs a `Row` via `RowDeserializer` using the table's schema for field order/types.
5. **Deleting a row:** Zeroes the slot bytes and removes the index entry — bytes remain physically zeroed on disk (no compaction/garbage collection of freed space).

---

## Known Issues Summary (for quick reference)

| # | Issue | Where | Impact |
|---|---|---|---|
| 1 | Slot count constant mismatch | `Constants.MAX_SLOTS` (128) vs actual capacity (~32 slots/page) — surfaces in `StorageEngine.rebuildIndex()` | Likely `ArrayIndexOutOfBoundsException` on index rebuild |
| 2 | Unused class | `Slot.java` — never instantiated or referenced anywhere | Dead code, no functional impact |
| 3 | Dead null-check | `PageManager.read()` never returns `null`, but `StorageEngine.rebuildIndex()` checks for it | Null-check is dead code; empty slots may produce false index entries |
| 4 | Single-page bottleneck | All tables write to page 0 only — `CrudService.insert()` | Hard ~32-row capacity ceiling across the *entire* database |
| 5 | Page allocation not implemented | `SuperBlock.nextPageId` declared but never incremented | Multi-page growth doesn't actually work |
| 6 | No catalog size enforcement | `CatalogManager.save()` doesn't check against `Constants.CATALOG_RESERVED_SIZE` | Large catalogs could silently corrupt the page region |
| 7 | Unused exception types | `RowNotFoundException`, `DuplicateKeyException`, `SchemaException` defined but never thrown | Typed error vocabulary exists but isn't wired in |
| 8 | Inconsistent exception usage | `TableNotFoundException` defined, but `CrudService.insert()` throws a plain `RuntimeException` instead | Minor inconsistency, no functional bug |
| 9 | Silent row truncation | `PageManager.insert()` truncates records larger than 128 bytes via `Math.min()` | Potential silent data corruption for long strings |
| 10 | Redundant catalog save | `CrudService.createTable()` calls `save()`, but `CatalogManager.addTable()` already saves internally | Harmless but wasteful double write |