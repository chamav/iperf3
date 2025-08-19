# iperf3 Client для Android

<div align="center">

![Android](https://img.shields.io/badge/Android-34+-green?logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.10-blue?logo=kotlin)
![Compose](https://img.shields.io/badge/Compose-1.5.4-blue?logo=jetpackcompose)
![API](https://img.shields.io/badge/API-24+-brightgreen)
![Architecture](https://img.shields.io/badge/Architecture-ARM64-red)

**Современное Android приложение для тестирования сетевой производительности с помощью iperf3**

</div>

## 📱 Описание

Android клиент для проведения тестов сетевой производительности с использованием протокола iperf3. Приложение специально оптимизировано для Samsung Galaxy Fold 6 с поддержкой складных устройств и современным Material 3 дизайном.

## ✨ Основные возможности

### 🏠 Главный экран
- Конфигурация TCP/UDP тестов с гибкими параметрами
- Live отображение прогресса с реальными метриками
- Мгновенный запуск и остановка тестов
- Детальные результаты по завершении

### 🌐 Управление серверами
- Добавление и редактирование iperf3 серверов
- Поддержка пользовательских портов и протоколов
- Установка сервера по умолчанию
- Заметки для серверов

### 📊 История результатов
- Просмотр всех проведенных тестов
- Фильтрация по протоколу (TCP/UDP) и хосту
- Экспорт результатов в CSV/JSON
- Детальная статистика по каждому тесту

### ⚙️ Настройки
- Настройки по умолчанию для новых тестов
- Конфигурация поведения приложения
- Управление размером истории
- Локализация на русском и английском языках

### 🔄 Фоновые тесты
- Foreground Service для продолжения тестов в фоне
- Push уведомления о прогрессе
- Сохранение результатов при сворачивании приложения

## 🏗️ Архитектура

Приложение построено на основе **Clean Architecture** с четким разделением слоев:

### 📁 Структура проекта
```
app/src/main/kotlin/com/iperf3client/
├── 📱 presentation/          # UI слой
│   ├── screen/              # Экраны приложения
│   │   ├── home/           # Главный экран с тестами
│   │   ├── servers/        # Управление серверами
│   │   ├── history/        # История результатов
│   │   └── settings/       # Настройки
│   ├── component/          # Переиспользуемые UI компоненты
│   ├── navigation/         # Навигация и роутинг
│   └── theme/             # Material 3 тема
├── 🔧 domain/               # Бизнес-логика
│   ├── model/              # Доменные модели
│   ├── repository/         # Интерфейсы репозиториев
│   └── usecase/           # Use Cases
├── 💾 data/                 # Данные
│   ├── database/           # Room база данных
│   ├── repository/         # Реализации репозиториев
│   └── engine/            # iperf3 engine
└── 🔔 service/              # Android Services
    └── IperfTestService.kt  # Foreground Service
```

### 🏛️ Технические решения
- **UI Framework:** Jetpack Compose + Material 3
- **База данных:** Room + DataStore Preferences
- **Архитектура:** MVVM + Clean Architecture
- **DI:** Manual Dependency Injection
- **Concurrency:** Kotlin Coroutines + Flow
- **Navigation:** Navigation Compose
- **Локализация:** Android Resources (RU/EN)

## 🎯 Целевые устройства

**Основная цель:** Samsung Galaxy Fold 6 (SM-F956B)
- **Архитектура:** ARM64 (arm64-v8a)
- **Android версия:** 7.0+ (API 24+)
- **Целевая версия:** Android 14 (API 34)

**Адаптивный дизайн:**
- Поддержка складных устройств
- Responsive навигация (Bottom Navigation / Navigation Drawer)
- Оптимизация для больших экранов

## 🚀 Установка и сборка

### Требования
- **JDK:** Java 17+
- **Android SDK:** Platform 34, Build Tools 34.0.0
- **Android NDK:** 26.3.11579264
- **Gradle:** 8.2+

### Сборка
```bash
# Клонирование репозитория
git clone https://github.com/chamav/iperf3.git
cd iperf3

# Переключение на ветку разработки
git checkout cl_android

# Сборка debug APK
./gradlew assembleDebug

# APK будет доступен в:
# app/build/outputs/apk/debug/app-debug.apk
```

### Интеграция iperf3 binary
Для полной функциональности требуется iperf3 binary для ARM64:

1. Скачайте или скомпилируйте iperf3 для ARM64
2. Поместите binary в `app/src/main/jniLibs/arm64-v8a/libiperf3.so`
3. Обновите `IperfEngineImpl.kt` для использования нативного binary

## 📲 Использование

### Первый запуск
1. Добавьте iperf3 сервер в разделе "Серверы"
2. Настройте параметры теста на главном экране
3. Нажмите "Запустить тест" для начала измерений

### Параметры тестов
- **Протокол:** TCP или UDP
- **Продолжительность:** 1-3600 секунд
- **Параллельные потоки:** 1-16
- **Обратное направление:** сервер → клиент
- **UDP битрейт:** настраиваемый для UDP тестов
- **Только Wi-Fi:** ограничение на Wi-Fi соединения

### Результаты
- Средняя, максимальная и минимальная скорость
- TCP: Retransmits, RTT
- UDP: Jitter, Packet Loss
- Объем переданных данных
- Timeline с поминутной статистикой

## 🌐 Локализация

Приложение поддерживает следующие языки:
- 🇷🇺 **Русский** (основной)
- 🇬🇧 **English** (дополнительный)

Автоматическое переключение в зависимости от настроек системы.

## 🛠️ Разработка

### Статус проекта
- ✅ **MVP готов** (83% завершения)
- ✅ Все основные экраны реализованы
- ✅ Clean Architecture настроена
- ✅ Mock engine для тестирования
- ⏳ Интеграция реального iperf3 binary
- ⏳ Детальные графики результатов

### Contribute
1. Fork репозитория
2. Создайте feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit изменения (`git commit -m 'Add some AmazingFeature'`)
4. Push в branch (`git push origin feature/AmazingFeature`)
5. Откройте Pull Request

## 📄 Лицензия

Этот проект является частной разработкой. Все права защищены.

## 🤝 Поддержка

При возникновении вопросов или проблем:
1. Проверьте [Issues](https://github.com/chamav/iperf3/issues)
2. Создайте новый Issue с подробным описанием
3. Приложите логи и скриншоты при необходимости

---

<div align="center">

**Сделано с ❤️ для тестирования сетевой производительности**

</div>