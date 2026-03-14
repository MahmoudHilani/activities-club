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

The backend also starts directly from the `backend` folder without preloading shell variables because `application.yaml` now has local development defaults.

```sh
cd backend
./mvnw spring-boot:run
```

It will connect to Postgres on `localhost:5433` unless you override the `DB_*`, `SERVER_PORT`, or `JWT_*` environment variables.
