REST-приложение для управления банковскими картами
Это REST-приложение на Spring Boot для управления банковскими картами, использующее PostgreSQL в качестве базы данных и Liquibase для миграций базы данных. Приложение контейнеризировано с помощью Docker и управляется через Docker Compose. API документирован с использованием Springdoc OpenAPI и доступен через Swagger UI.
Требования
Перед запуском приложения убедитесь, что у вас установлены:

Docker: Необходим для сборки и запуска контейнеров приложения.
Docker Compose: Используется для управления несколькими контейнерами.
Maven: Требуется для сборки JAR-файла приложения.
Java 22: Приложение собрано с использованием Java 22.
pgAdmin (опционально): Для проверки базы данных PostgreSQL.

Структура проекта

src/main/resources/application.yml: Конфигурация Spring Boot, включая настройки подключения к базе данных, Liquibase, JWT и Springdoc.
src/main/resources/db/migration/changelog-master.yml: Основной файл Liquibase для управления миграциями базы данных.
Dockerfile: Описывает Docker-образ для приложения Spring Boot.
docker-compose.yml: Определяет сервисы (приложение и база данных) и их конфигурацию.
pom.xml: Конфигурация Maven для зависимостей и сборки.

Установка и запуск приложения
Следуйте этим шагам для настройки и запуска приложения:

Клонируйте репозиторий:
git clone https://github.com/timkatima/BankREST
cd BankREST


Соберите приложение:Используйте Maven для компиляции и упаковки приложения в JAR-файл:
mvn clean package

Это создаст файл target/bank-rest-0.0.1-SNAPSHOT.jar.

Запустите приложение с помощью Docker Compose:Запустите приложение и базу данных PostgreSQL:
docker-compose up --build


Флаг --build обеспечивает пересборку Docker-образа приложения.
Команда запускает два контейнера:
bank_rest-app-1: Приложение Spring Boot на порту 8081.
bank_rest-db-1: База данных PostgreSQL на порту 5432.

Проверьте, что приложение работает:

Проверьте логи, чтобы убедиться, что приложение и база данных запустились успешно:docker logs bank_rest-app-1
docker logs bank_rest-db-1

Ищите сообщения вроде:app-1 | Started BankCardsApplication in X seconds
db-1  | database system is ready to accept connections


Проверьте API, отправив запрос на endpoint аутентификации (например, /api/auth/login):curl -X POST http://localhost:8081/api/auth/login -H "Content-Type: application/json" -d '{"username":"regular_user","password":"password"}'

Используйте полученный JWT-токен для аутентифицированных запросов:curl -H "Authorization: Bearer ваш_jwt_токен" http://localhost:8081/api/some_endpoint


Доступ к Swagger UI для просмотра документации API:http://localhost:8081/swagger-ui/index.html

Документация OpenAPI доступна по:http://localhost:8081/api-docs




Доступ к базе данных (опционально):

Для проверки базы данных подключитесь к ней через pgAdmin или команду psql:docker exec -it bank_rest-db-1 psql -U postgres -d bankdb

Выполните SQL-команды для проверки таблиц и данных:\dt
SELECT * FROM users;
SELECT * FROM cards;


Либо настройте pgAdmin для подключения к localhost:5432 с параметрами:
Имя пользователя: postgres
Пароль: root
База данных: bankdb


Остановка приложения:Чтобы остановить и удалить контейнеры и тома:
docker-compose down -v


Детали конфигурации

База данных:

База данных bankdb автоматически создается при запуске сервиса db.
Liquibase применяет миграции, определенные в src/main/resources/db/migration/changelog-master.yml, для создания таблиц (users, cards) и вставки начальных данных.
Настройки подключения указаны в application.yml:spring:
datasource:
url: jdbc:postgresql://db:5432/bankdb
username: postgres
password: root
driver-class-name: org.postgresql.Driver

Приложение:

Приложение Spring Boot работает на порту 8081.
Для аутентификации используется JWT с секретом и сроком действия 1 час:jwt:
secret: K7mN9pQ2vL8jR4tY5uI6oP1wQ3eA8xZ9oX7kP9mQ2vL8jR4tY
expiration: 3600000


Swagger UI и OpenAPI включены для документации API:springdoc:
api-docs:
path: /api-docs
swagger-ui:
path: /swagger-ui
enabled: true

Docker Compose:

Файл docker-compose.yml определяет два сервиса: app (Spring Boot) и db (PostgreSQL).
Сервис db использует постоянный том (pgdata) для хранения данных базы.
Сервис app зависит от состояния healthy сервиса db перед запуском.

Устранение неполадок

База данных не отображается в pgAdmin:

Убедитесь, что pgAdmin настроен на подключение к localhost:5432 с учетными данными postgres/root.
Проверьте, не занят ли порт 5432 другим процессом. При конфликте измените проброс порта в docker-compose.yml (например, "5433:5432") и обновите настройки pgAdmin.


Ошибки с истекшими JWT-токенами:

Если вы видите ошибки вроде JWT expired at ..., сгенерируйте новый токен через endpoint аутентификации:curl -X POST http://localhost:8081/api/auth/login -H "Content-Type: application/json" -d '{"username":"ваш_логин","password":"ваш_пароль"}'


Для увеличения срока действия токена измените jwt.expiration в application.yml, например:jwt:
expiration: 86400000 # 24 часа


Предупреждения Maven:

Если возникают предупреждения о org.jetbrains:annotations, обновите pom.xml, указав версию:<dependency>
<groupId>org.jetbrains</groupId>
<artifactId>annotations</artifactId>
<version>24.0.1</version>
</dependency>


Приложение не подключается к базе данных:

Убедитесь, что сервис db запущен и находится в состоянии healthy:docker ps


Проверьте настройки spring.datasource в application.yml. Обратите внимание, что для Docker используется jdbc:postgresql://db:5432/bankdb, а для локального запуска — jdbc:postgresql://localhost:5432/bankdb.


Миграции Liquibase:Схема базы данных и начальные данные управляются Liquibase через changelog-master.yml в src/main/resources/db/migration/.

Шифрование:Приложение использует ключ шифрования, указанный в application.yml:
encryption:
key: Kj8pLm9nQ2vX4yZ8aB5cD6eF7gH9iJ0k