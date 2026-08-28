# run-replication-test.ps1
$ErrorActionPreference = 'Stop'

Write-Host "Starting Docker Compose for Replication E2E Test..."
cd d:\Docs\CodeProject\DOAN\code\erp-backend\src\test\resources\replication-e2e
docker-compose down -v
docker-compose up -d

Write-Host "Waiting for databases to be healthy (10 seconds)..."
Start-Sleep -Seconds 10

Write-Host "Initializing schema and replication..."
# Install psql client locally if not available? It's better to execute inside the container.
docker exec pg-master-hq psql -U erp_user -d erp_db -c "CREATE TABLE IF NOT EXISTS category (id serial PRIMARY KEY, code varchar(50) UNIQUE NOT NULL, name varchar(200) NOT NULL, is_active boolean NOT NULL);"
docker exec pg-replica-branch psql -U erp_user -d erp_db -c "CREATE TABLE IF NOT EXISTS category (id serial PRIMARY KEY, code varchar(50) UNIQUE NOT NULL, name varchar(200) NOT NULL, is_active boolean NOT NULL);"

Write-Host "Setting up Logical Replication..."
docker exec pg-master-hq psql -U erp_user -d erp_db -c "DROP PUBLICATION IF EXISTS erp_pub;"
docker exec pg-master-hq psql -U erp_user -d erp_db -c "CREATE PUBLICATION erp_pub FOR ALL TABLES;"
# Need to give replica container a bit of time
Start-Sleep -Seconds 2
docker exec pg-replica-branch psql -U erp_user -d erp_db -c "DROP SUBSCRIPTION IF EXISTS erp_sub;"
docker exec pg-replica-branch psql -U erp_user -d erp_db -c "CREATE SUBSCRIPTION erp_sub CONNECTION 'host=pg-master port=5432 user=erp_user password=erp_password dbname=erp_db' PUBLICATION erp_pub;"

Write-Host "Running ReplicationE2ETest..."
cd d:\Docs\CodeProject\DOAN\code\erp-backend
# Remove @Disabled temporarily to run it
(Get-Content src\test\java\com\storename\erp\catalog\ReplicationE2ETest.java) -replace '@Disabled\("Manual E2E Test requiring running Docker containers"\)', '// @Disabled' | Set-Content src\test\java\com\storename\erp\catalog\ReplicationE2ETest.java

try {
    .\mvnw.cmd test -Dtest=ReplicationE2ETest
} finally {
    # Put @Disabled back
    (Get-Content src\test\java\com\storename\erp\catalog\ReplicationE2ETest.java) -replace '// @Disabled', '@Disabled("Manual E2E Test requiring running Docker containers")' | Set-Content src\test\java\com\storename\erp\catalog\ReplicationE2ETest.java
    
    Write-Host "Tearing down Docker containers..."
    cd d:\Docs\CodeProject\DOAN\code\erp-backend\src\test\resources\replication-e2e
    docker-compose down -v
}

Write-Host "Done!"
