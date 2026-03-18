
# Grade Journal

Веб-приложение для электронного журнала успеваемости с разделением на frontend и backend.

## Кратко о проекте

Проект представляет собой электронный журнал успеваемости, в котором реализованы:

- авторизация пользователей с JWT
- роли: `admin`, `teacher`, `student`
- просмотр учебных групп
- просмотр расписания
- выставление оценок и посещаемости
- Swagger-документация API
- интеграция с внешними REST API
- OAuth2-интеграция с GitHub
- кэширование через Redis
- контейнеризация через Docker Compose

## Технологический стек

### Backend
- Java 21
- Spring Boot 3
- Spring Web
- Spring Security
- Spring Data JPA
- JdbcTemplate
- PostgreSQL
- Flyway
- Redis
- Swagger / SpringDoc OpenAPI
- JUnit 5
- Mockito

### Frontend
- React
- TypeScript
- Vite
- Zustand
- TanStack Query
- Axios
- Tailwind CSS

### Infrastructure
- Docker
- Docker Compose

---

# Структура проекта

```text
grade_journal/
├── grade_journal_back/   # backend на Spring Boot
├── grade_journal_front/  # frontend на React + TypeScript
└── docker-compose.yml
````

---

# Функциональность

## Для администратора

* управление пользователями
* заполнение профилей преподавателей и студентов
* управление расписанием

## Для преподавателя

* просмотр групп
* просмотр расписания
* выставление оценок
* учет посещаемости

## Для студента

* просмотр профиля
* просмотр оценок
* просмотр посещаемости
* просмотр расписания

---

# Внешние API

В проекте используются минимум 2 внешних REST API:

1. **GitHub OAuth / GitHub API**

   * авторизация через GitHub
   * получение данных пользователя GitHub

2. **Public Holidays API**

   * получение списка официальных праздников по стране и году

---

# Требования для запуска

Перед запуском должны быть установлены:

* Docker Desktop
* Docker Compose
* Git
* Java 21
* Maven 3.9+
  (нужны только для локальной разработки без Docker)

---

# Запуск проекта с нуля через Docker

## 1. Перейти в корневую папку проекта

```powershell
cd D:\бгуир\Курсач РИС\grade_journal
```

## 2. Собрать и поднять контейнеры

```powershell
docker compose up -d --build
```

## 3. Проверить, что контейнеры запущены

```powershell
docker compose ps
```

Должны быть подняты сервисы:

* `postgres`
* `redis`
* `backend`
* `frontend`

---

# Адреса после запуска

## Frontend

```text
http://localhost:5173
```

## Backend

```text
http://localhost:8080
```

## Swagger

```text
http://localhost:8080/swagger-ui.html
```

---

# Тестовый вход

Пример учетной записи преподавателя:

* логин: `teacher10`
* пароль: `teach10`

---

# Остановка проекта

## Остановить контейнеры

```powershell
docker compose down
```

## Остановить контейнеры и удалить volumes

```powershell
docker compose down -v
```

---

# Полезные команды

## Просмотр логов всех сервисов

```powershell
docker compose logs
```

## Просмотр логов backend

```powershell
docker compose logs backend --tail=200
```

## Просмотр логов frontend

```powershell
docker compose logs frontend --tail=200
```

## Просмотр логов Redis

```powershell
docker compose logs redis --tail=100
```

## Проверка Redis

```powershell
docker compose exec redis redis-cli ping
```

Ожидаемый ответ:

```text
PONG
```

## Проверка ключей Redis

```powershell
docker compose exec redis redis-cli keys "*"
```

---

# Локальная разработка без Docker

## Backend

Перейти в папку backend:

```powershell
cd D:\бгуир\Курсач РИС\grade_journal\grade_journal_back
```

Собрать проект:

```powershell
mvn clean package -DskipTests
```

Запустить backend:

```powershell
java -jar .\target\grade_journal_back-0.0.1-SNAPSHOT.jar
```

## Frontend

Перейти в папку frontend:

```powershell
cd D:\бгуир\Курсач РИС\grade_journal\grade_journal_front
```

Установить зависимости:

```powershell
npm install
```

Запустить dev-сервер:

```powershell
npm run dev
```

---

# Тестирование

Для запуска тестов backend:

```powershell
cd D:\бгуир\Курсач РИС\grade_journal\grade_journal_back
mvn test
```

Для запуска конкретных тестов:

```powershell
mvn clean "-Dtest=AuthControllerTest,ProfileControllerTest,AuthServiceTest" test
```

---

# Что уже реализовано

* разделение на frontend и backend
* работа с PostgreSQL
* JWT-аутентификация
* role-based access
* refresh token
* Swagger
* Redis-кэширование
* Docker Compose
* GitHub OAuth
* внешний REST API праздников
* базовые unit/web tests

---

# Примечание

Проект рассчитан на локальный запуск в учебных целях.
Для production-использования необходимо:

* вынести секреты в переменные окружения
* настроить полноценный production-конфиг
* добавить централизованное логирование и мониторинг
* усилить обработку ошибок и валидацию

---

# Я Автор

Курсовой проект по разработке web-приложения: **электронный журнал успеваемости**.

