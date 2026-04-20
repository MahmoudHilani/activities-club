# Activities Club

## Local startup

Use Docker Compose for the full stack:

```sh
cp .env.example .env
docker compose up --build
```

Services:

- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`
- PgAdmin: `http://localhost:5050`
- Postgres: `localhost:5433`

The root `.env` file controls the exposed ports, database credentials, and JWT settings used by Compose. Start from `.env.example` and keep `.env` local.

## Dev startup

For day-to-day development, use the PowerShell launcher instead of rebuilding the whole app stack in Docker:

```powershell
.\dev.ps1
```

From Command Prompt, use:

```bat
dev.cmd
```

That command:

- starts Postgres in Docker
- runs the backend locally with `spring-boot:run`
- runs the frontend locally with Vite
- stores uploaded activity images in `public/uploads` at the repository root by default

This is the intended fast feedback loop for development because frontend changes hot-reload immediately and backend changes do not require rebuilding a Docker image first.

Optional:

```powershell
.\dev.ps1 -WithPgAdmin
```

Command Prompt equivalent:

```bat
dev.cmd -WithPgAdmin
```

Stop the local dev processes and Docker database with:

```powershell
.\dev-stop.ps1
```

Or from Command Prompt:

```bat
dev-stop.cmd
```

## Backend-only startup

The backend also starts directly from the `backend` folder, but you must provide a `JWT_SECRET` because the app no longer falls back to an insecure signing key.

```sh
cd backend
export JWT_SECRET=replace-with-a-local-secret-at-least-32-bytes
./mvnw spring-boot:run
```

It will connect to Postgres on `localhost:5433` unless you override the `DB_*`, `SERVER_PORT`, or `JWT_*` environment variables.

Uploaded activity images are stored in `public/uploads` by default. Override `UPLOADS_DIR` if you want a different folder.
