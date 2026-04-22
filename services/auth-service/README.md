# auth-service

Spring Boot 3.3 service issuing HS256 JWTs (signed with the shared
`cbp.auth.secret` so any common-auth `JwtValidator` can verify them).
Supports login, refresh, introspect, and admin-only register. Users
are stored in H2 with BCrypt password hashes; a demo admin is seeded
at startup via `CommandLineRunner`.

Latest JaCoCo instruction coverage: **39.64%**.
