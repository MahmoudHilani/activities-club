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

## Backend-only startup

The backend also starts directly from the `backend` folder, but you must provide a `JWT_SECRET` because the app no longer falls back to an insecure signing key.

```sh
cd backend
export JWT_SECRET=replace-with-a-local-secret-at-least-32-bytes
./mvnw spring-boot:run
```

It will connect to Postgres on `localhost:5433` unless you override the `DB_*`, `SERVER_PORT`, or `JWT_*` environment variables.
