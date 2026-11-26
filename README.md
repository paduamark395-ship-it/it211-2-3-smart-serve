# Smart Serve

A Java application that makes the fast-food / restaurant experience easier: customers order from a digital menu without lining up, track their order, and reserve or check available tables.

**Course:** IT211 – Section 2-3  
**Team:** Mark Padua (Project Manager / QA), Hazel Ann S. Gunao (Documentation), Aizie Botalon (Backend), Janna Adelhene Mejia (Lead Developer), Amy Gerero (Frontend), Charly Datur (UI/UX Designer)

## Features
- Digital menu with categories
- Place an order without lining up
- Order tracking: Received → Preparing → Ready → Completed
- Table reservation and live table availability

## Project structure
```
src/        Java source code
test/       JUnit tests
diagrams/   Use-case and class diagrams
docs/       Requirements, CRC cards, class assignments
```

## How to run
1. Install JDK 17 or newer.
2. Compile: `javac -d out src/*.java`
3. Run: `java -cp out Main` (once the console menu exists)
4. Tests: run the files in `test/` with JUnit 5 from your IDE.

## Team workflow
**Branches**
- `main` always works. Never commit directly to it.
- Work on `feature/<name>-<class>`, e.g. `feature/aizie-order`.
- Merge into `main` through a pull request that at least one teammate reviews.

**Commit messages:** `type: short description`
- `feat:` new feature or class
- `fix:` bug fix
- `docs:` documentation or diagrams
- `test:` tests

Example: `feat: add Order class with validation`

## Class assignments
See [docs/class-assignments.md](docs/class-assignments.md).
