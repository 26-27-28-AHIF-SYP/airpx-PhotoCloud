# AirPX PhotoCloud

AirPX PhotoCloud is a proposed photo-management and sharing platform. Its goals include organizing and sharing photos across mobile and web, with fine-grained album permissions and privacy-conscious handling of personal data. Treat these as project goals, not as implemented features unless the code or current documentation confirms them.

## Repository layout

- `androidApp/` — Android app area; `Readme.md` is currently empty.
- `iosApp/` — iOS research and proposed development/synchronization approach in `Readme.md`.
- `webapp/` — Web app area; `Readme.md` is currently empty.
- `backend/` — Backend area; `Readme.md` is currently empty.
- `database/` — Database area; `Readme.md` is currently empty.
- `docs/` — Project proposal, user stories, research, diagrams, and other project documentation.
- `README.md` — Current project proposal and scope.

The repository currently contains project documentation, not application source code. Before implementing a feature, inspect the relevant documentation and the current repository state; do not assume that a planned feature or architecture already exists.

## Product direction

The project proposal and user stories describe these intended capabilities:

- Photo upload, organization, search, and nested albums.
- Sharing with granular permissions and support for inherited or overridden album access.
- Access from mobile and web, including proposed offline and notification features.
- Photo metadata search and possible AI-assisted organization or recognition.
- Privacy and security for personal photos and account data.

These are proposed requirements. Do not present them as completed functionality without evidence in the codebase.

## Working guidance

- Keep changes scoped to the requested task and follow the conventions of the affected platform or documentation.
- Check existing project documents before introducing architectural or product decisions. If requirements conflict or leave a consequential choice open, ask for clarification.
- Preserve unrelated user changes; inspect the working tree before editing and avoid reverting changes outside the task.
- Validate code changes with the relevant existing checks. For documentation-only changes, verify links, paths, and factual claims against the repository.
- Update directly related documentation when behavior or project structure changes.
