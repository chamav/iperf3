# Руководство по диагностике iperf3 Client

## Быстрый старт

### 1. Установка APK на устройство
```bash
# Подключите устройство через USB и включите отладку
adb install app/build/outputs/apk/debug/app-debug.apk

# Или переустановка (если уже установлено)
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 2. Запуск диагностики
```bash
# Сделать скрипт исполняемым (только первый раз)
chmod +x debug_logs.sh

# Запустить мониторинг логов
./debug_logs.sh live

# Или показать только ошибки
./debug_logs.sh errors
```

## Диагностика падений приложения

### Шаг 1: Воспроизведение проблемы
1. Очистите логи: `./debug_logs.sh clear`
2. Запустите мониторинг: `./debug_logs.sh live`
3. В другом терминале запустите приложение и воспроизведите падение
4. Наблюдайте за логами в реальном времени

### Шаг 2: Анализ ошибок
```bash
# Анализ краша
./debug_logs.sh crash

# Статус приложения
./debug_logs.sh status

# Извлечь все логи
./debug_logs.sh pull
```

### Шаг 3: Изучение файлов логов
После извлечения логов в папку `./device_logs/`:

- `iperf_client.log` - основной лог приложения с метками времени
- `crash_report.txt` - детальный отчет о падении (если есть)
- `test_logs/` - логи отдельных тестов iperf3

## Типичные проблемы и решения

### 1. Падение при завершении теста

**Симптомы:**
- Приложение крашится после "Test completed"
- В логах видно "IperfTestService" ошибки

**Диагностика:**
```bash
./debug_logs.sh live | grep -E "(TestService|Completed|Error)"
```

**Возможные причины:**
- Ошибка в cleanup коде сервиса
- Проблема с освобождением Wake Lock
- Ошибка в сохранении результатов

### 2. Memory Leak / OutOfMemoryError

**Симптомы:**
- Приложение падает с "OutOfMemoryError"
- Медленная работа после нескольких тестов

**Диагностика:**
```bash
adb shell dumpsys meminfo com.iperf3client
```

### 3. Проблемы с сервисом

**Симптомы:**
- Тест не запускается
- Уведомления не появляются

**Диагностика:**
```bash
# Проверить сервисы
adb shell dumpsys activity services | grep iperf3client

# Проверить уведомления
adb shell dumpsys notification | grep iperf3client
```

## Полезные команды ADB

### Логирование
```bash
# Мониторинг в реальном времени
adb logcat -s "IperfClient:*"

# Только ошибки и предупреждения
adb logcat -s "IperfClient:*" "*:E" "*:W"

# Сохранить логи в файл
adb logcat -d > logcat_dump.txt
```

### Управление приложением
```bash
# Запустить приложение
adb shell am start -n com.iperf3client/.MainActivity

# Остановить приложение
adb shell am force-stop com.iperf3client

# Очистить данные приложения
adb shell pm clear com.iperf3client
```

### Файлы на устройстве
```bash
# Просмотр файлов приложения (требует root)
adb shell ls -la /data/data/com.iperf3client/files/

# Извлечение конкретного файла
adb pull /data/data/com.iperf3client/files/logs/iperf_client.log
```

## Анализ логов

### Ключевые маркеры в логах
- `[I] Application: IperfApplication started` - приложение запущено
- `[I] HomeViewModel: Starting test` - тест начинается  
- `[I] IperfEngine: Test completed successfully` - тест завершен успешно
- `[E] CrashHandler: Uncaught exception` - критическая ошибка
- `[W] IperfTestService: Test already running` - попытка запуска второго теста

### Поиск проблем
```bash
# В логах на устройстве
grep -i "error\|exception\|crash\|failed" device_logs/iperf_client.log

# В logcat
adb logcat -d | grep -i "iperf.*error\|iperf.*exception"
```

## Создание отчета об ошибке

Для создания полного отчета об ошибке выполните:

```bash
# 1. Извлечь логи
./debug_logs.sh pull

# 2. Создать системный дамп
adb shell dumpsys > device_logs/system_dump.txt

# 3. Сохранить информацию об устройстве
adb shell getprop > device_logs/device_properties.txt

# 4. Создать архив
tar -czf iperf3_debug_$(date +%Y%m%d_%H%M%S).tar.gz device_logs/
```

Отправьте созданный архив разработчикам вместе с описанием проблемы.

## Режим отладки

Для более детального логирования в debug-сборке все логи пишутся как в logcat, так и в файл. В release-сборке можно отключить файловое логирование для повышения производительности.

Уровни логирования:
- `D` (Debug) - детальная информация
- `I` (Info) - общая информация о работе
- `W` (Warning) - предупреждения о потенциальных проблемах
- `E` (Error) - ошибки, которые не приводят к крашу
- `CRASH` - критические ошибки, приводящие к падению

## Полезные ссылки

- [Android Debugging Guide](https://developer.android.com/studio/debug)
- [ADB Commands](https://developer.android.com/studio/command-line/adb)
- [Logcat Command-line Tool](https://developer.android.com/studio/command-line/logcat)