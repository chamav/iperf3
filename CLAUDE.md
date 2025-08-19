# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Проект Android iperf3 Client MVP

Это Android-приложение для измерения пропускной способности сети с использованием протокола iperf3. Приложение разработано специально для Samsung Galaxy Fold 6 с поддержкой складных устройств.

## Команды разработки

### Сборка приложения
```bash
# Сборка debug APK
./gradlew assembleDebug

# Сборка release APK  
./gradlew assembleRelease

# Очистка проекта
./gradlew clean
```

### Тестирование
```bash
# Запуск юнит-тестов
./gradlew testDebugUnitTest

# Запуск инструментальных тестов
./gradlew connectedAndroidTest

# Запуск всех тестов
./gradlew test
```

### Инструменты разработки
```bash
# Проверка синтаксиса Kotlin (используется встроенный компилятор)
./gradlew compileDebugKotlin

# Линтинг Android
./gradlew lintDebug
```

## Архитектура приложения

### Clean Architecture
Приложение построено на принципах Clean Architecture с тремя основными слоями:

#### Domain слой (`domain/`)
- **модели**: `TestParams`, `TestResult`, `ServerItem`, `LiveMetricsTick`, `IperfEngine`
- **репозитории**: интерфейсы `HistoryRepository`, `ServersRepository`, `SettingsRepository`
- **use cases**: `RunTestUseCase`, `ManageServersUseCase`, `ExportHistoryUseCase`

#### Data слой (`data/`)
- **database**: Room база данных с DAO и Entity классами
- **engine**: `IperfEngineImpl` - mock-реализация iperf3 engine (готова к замене на реальную)
- **repository**: реализации интерфейсов репозиториев

#### Presentation слой (`presentation/`)
- **экраны**: Home, Servers, History, Settings с соответствующими ViewModel
- **компоненты**: переиспользуемые UI компоненты
- **навигация**: Navigation Compose с поддержкой адаптивных макетов
- **тема**: Material 3 с поддержкой темной темы

### Ключевые технологии
- **UI**: Jetpack Compose + Material 3
- **База данных**: Room + DataStore для настроек
- **Архитектура**: MVVM + Clean Architecture
- **Навигация**: Navigation Compose
- **Асинхронность**: Kotlin Coroutines + Flow
- **Фоновые задачи**: Foreground Service

## Структура проекта

### Основные пакеты
```
com.iperf3client/
├── data/                    # Data слой
│   ├── database/           # Room БД, DAO, Entity
│   ├── engine/             # IperfEngine реализация
│   └── repository/         # Реализации репозиториев
├── domain/                 # Domain слой
│   ├── model/              # Доменные модели
│   ├── repository/         # Интерфейсы репозиториев
│   └── usecase/           # Бизнес-логика
├── presentation/           # UI слой
│   ├── component/          # UI компоненты
│   ├── navigation/         # Навигация
│   ├── screen/            # Экраны приложения
│   └── theme/             # Material 3 тема
└── service/               # Android Services
```

### Основные экраны
- **HomeScreen**: параметры тестов, запуск/остановка, прогресс
- **ServersScreen**: управление списком серверов
- **HistoryScreen**: история результатов с фильтрацией
- **SettingsScreen**: настройки приложения

## Особенности разработки

### Поддержка складных устройств
- Приложение адаптируется под различные размеры экрана
- Использует `WindowSizeClass` для определения layout strategy
- Поддерживает состояния "сложен/разложен" без потери данных

### Mock Engine
- Текущая реализация использует mock iperf3 engine (`IperfEngineImpl`)
- Готова к замене на реальную интеграцию с iperf3 binary
- Интерфейс `IperfEngine` определяет контракт для реальной реализации

### Локализация
- Поддержка русского и английского языков
- Строковые ресурсы в `values/strings.xml` и `values-ru/strings.xml`

### Целевая платформа
- Минимальная версия: Android 7.0 (API 24)
- Целевая версия: Android 14 (API 34)
- Поддерживаемая архитектура: ARM64 (arm64-v8a)

## Конфигурация сборки

### Gradle конфигурация
- Kotlin 1.9.10
- Compose 1.5.4
- Поддержка только ARM64 архитектуры
- Room 2.6.0 для базы данных
- Navigation Compose для навигации

### Зависимости
Проект использует современный Android tech stack:
- Jetpack Compose для UI
- Room для локального хранения
- Navigation Compose для навигации
- Material 3 для дизайна
- Coroutines + Flow для асинхронности

## Отладка и диагностика

### Логирование
Приложение использует централизованную систему логирования через `Logger` класс:

```bash
# Просмотр логов в режиме реального времени
adb logcat -s "IperfClient:*"

# Просмотр только ошибок
adb logcat -s "IperfClient:*" "*:E"

# Очистка логов
adb logcat -c
```

### Файлы логов
- **Основные логи**: `/data/data/com.iperf3client/files/logs/iperf_client.log`
- **Краш-репорты**: `/data/data/com.iperf3client/files/crash_report.txt`
- **Логи тестов**: `/data/data/com.iperf3client/files/logs/iperf3_[sessionId].log`

### Извлечение логов с устройства
```bash
# Извлечь основной лог-файл
adb pull /data/data/com.iperf3client/files/logs/iperf_client.log ./

# Извлечь краш-репорт
adb pull /data/data/com.iperf3client/files/crash_report.txt ./

# Извлечь все логи
adb pull /data/data/com.iperf3client/files/logs/ ./logs/
```

### Диагностика падений
1. **Проверить logcat**: `adb logcat -s "IperfClient:*" "*:E" "*:W"`
2. **Проверить файл краша**: `/data/data/com.iperf3client/files/crash_report.txt`
3. **Проверить состояние сервиса**: поиск по тегу "IperfTestService"

### Основные теги логирования
- `IperfEngine` - работа с iperf3 движком
- `HomeViewModel` - логика главного экрана
- `IperfTestService` - фоновый сервис тестирования
- `CrashHandler` - обработчик критических ошибок
- `Logger` - система логирования

## Sentry интеграция

### Настройка
1. Скопируйте `local.properties.example` в `local.properties`
2. Заполните ваш Sentry DSN в поле `sentry.dsn`
3. Файл `local.properties` уже добавлен в `.gitignore` и не попадет в репозиторий

### Конфигурация
- **DSN хранится**: в `local.properties` (локально, не коммитится)
- **Передается**: через `BuildConfig.SENTRY_DSN`  
- **Логи по умолчанию**: включены для тестирования
- **Отправка**: info/warning/error/crash в Sentry + breadcrumbs

### Изменение настроек по умолчанию
Для production измените в `SettingsRepositoryImpl.kt`:
```kotlin
private const val DEFAULT_SENTRY_ENABLED = false
```

## Статус MVP
✅ **MVP готов к сборке APK**
- Все основные экраны реализованы
- Архитектура масштабируема
- Mock engine готов к замене
- Поддержка складных устройств
- Локализация настроена
- Система логирования и обработки ошибок настроена
- Sentry интеграция для централизованного мониторинга