Just notes for now:

starting up the database and creating test/fake users:
```
docker compose up -d
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=reset"        # clears the database
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=reset,seed"   # clears the database and creates 120 test users
.\mvnw.cmd spring-boot:run                                           # for normal testing of register and login
```

starting up the frontend (needs the backend running on port 8080):
```
cd frontend
npm install
npm run dev        # http://localhost:5173, API calls are proxied to the backend under /api
```
