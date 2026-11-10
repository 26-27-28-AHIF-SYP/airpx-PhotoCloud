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
# AirPX PhotoCloud Project

## Project Idea

AirPX PhotoCloud is a cloud-based photo management and sharing platform designed to provide users with an intuitive, secure, and efficient way to store, organize, and share their photographs. The platform aims to offer seamless integration between local device storage and cloud-based services, allowing users to upload, tag, categorize, and share photos with minimal friction.

Key features include:
- Automated photo organization through AI-powered tagging and metadata extraction
- Granular permission settings (View/Save/Add/Delete) per album or per friend, replacing "all-or-nothing" sharing models
- A nested album system where permissions can be inherited from parent albums or overridden at the child level
- End-to-end encryption for privacy and security
- Cross-device synchronization between mobile and web interfaces

## File Structure

The project is organized into the following directories and files:

### 1. Core Components
- `mobileapp/`
  - `Readme.md` - Mobile application documentation
  - Contains implementation for Android app with offline access, push notifications, and camera roll auto-backup

- `webapp/`
  - `Readme.md` - Web application documentation
  - Includes browser-based notifications and login synchronization with mobile accounts

- `backend/`
  - `Readme.md` - Backend system documentation
  - Describes the API architecture and core services

- `database/`
  - `Readme.md` - Database design and schema
  - Contains details about storage structure and data models

### 2. Project Documentation
- `docs/`
  - `2026.10.08-projektantrag-pitch.pptx` - Presentation of the project idea
  - `OldReadMe` - Historical project documentation
  - `ProjecktAntrag.md` - Project proposal document
  - `Umfrage.md` - User survey document
  - `userstories.md` - User stories and requirements
  - `AirPxUCD.png` and `AirPxUCD.puml` - Use case diagrams
  - `AirPxDeploymentDiagram.png` and `AirPxDeploymentDiagram.puml` - Deployment architecture diagrams

### 3. Feature Plan

The project follows a prioritized implementation roadmap:

1. Album and photo management (upload, nested albums)
2. Permission system (View/Save/Add/Delete) per album or per friend
3. Development of user interface (mobile and web)
4. User account system (including mobile-web login synchronization)
5. Search and filtering functionality
6. AI-powered features:
   - Automatic sorting by date, location, event, and people
   - Search using metadata tags (date, location, camera information)
   - Object, scene, and face recognition in images
   - Text recognition in images (like signs or documents)

## Development Workflow

The team follows an agile methodology with two-week sprints. Key practices include:

- Continuous integration with automated testing
- Code reviews using PRs
- Feature flags for gradual feature rollout
- Monitoring and logging for operations

This structure ensures scalability, maintainability, and ease of onboarding for new developers.
