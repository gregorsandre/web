Just notes for now:

starting up the database and creating test/fake users:
1. make sure the docker engine is running then use following commands
```
docker compose up -d
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=reset"        # clears the database
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=reset,seed"   # clears the database and creates 120 test users
.\mvnw.cmd spring-boot:run                                           # for normal testing of register and login
```

to wipe the database completely:
```
docker compose down -v
```