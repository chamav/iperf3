#!/bin/bash

# Скрипт для диагностики iperf3 приложения

echo "=== Iperf3 Client Debug Script ==="
echo

# Проверяем подключение устройства
if ! adb devices | grep -q "device$"; then
    echo "❌ Нет подключенного устройства Android"
    echo "Подключите устройство и включите USB отладку"
    exit 1
fi

echo "✅ Устройство подключено"

# Функция для показа логов в режиме реального времени
show_live_logs() {
    echo "📱 Показ логов в реальном времени (Ctrl+C для остановки):"
    echo "   Фильтр: IperfClient, ошибки и предупреждения"
    echo
    adb logcat -s "IperfClient:*" "*:E" "*:W"
}

# Функция для показа только ошибок
show_errors() {
    echo "🚨 Показ только ошибок за последние 100 строк:"
    adb logcat -d -t 100 "*:E" | grep -i "iperf\|exception\|error\|crash"
}

# Функция для очистки логов
clear_logs() {
    echo "🧹 Очистка логов..."
    adb logcat -c
    echo "Логи очищены"
}

# Функция для извлечения логов с устройства
pull_logs() {
    echo "📥 Извлечение файлов логов с устройства..."
    
    # Создаем папку для логов
    mkdir -p ./device_logs
    
    # Извлекаем основной лог-файл
    adb pull /data/data/com.iperf3client/files/logs/iperf_client.log ./device_logs/ 2>/dev/null && \
        echo "✅ Основной лог скачан: ./device_logs/iperf_client.log" || \
        echo "❌ Не удалось скачать основной лог (возможно, файл не существует)"
    
    # Извлекаем краш-репорт
    adb pull /data/data/com.iperf3client/files/crash_report.txt ./device_logs/ 2>/dev/null && \
        echo "✅ Краш-репорт скачан: ./device_logs/crash_report.txt" || \
        echo "❌ Краш-репорт не найден (это хорошо!)"
    
    # Извлекаем логи тестов
    adb pull /data/data/com.iperf3client/files/logs/ ./device_logs/test_logs/ 2>/dev/null && \
        echo "✅ Логи тестов скачаны в: ./device_logs/test_logs/" || \
        echo "❌ Логи тестов не найдены"
    
    echo "📂 Все доступные логи сохранены в папку ./device_logs/"
}

# Функция для анализа падений
analyze_crashes() {
    echo "🔍 Анализ падений приложения..."
    echo "Поиск ключевых ошибок в logcat:"
    adb logcat -d | grep -i -A5 -B5 "iperf.*exception\|iperf.*error\|iperf.*crash\|uncaught.*exception"
}

# Функция для показа состояния приложения
app_status() {
    echo "📊 Статус приложения:"
    echo "Процессы:"
    adb shell ps | grep com.iperf3client || echo "   Приложение не запущено"
    echo
    echo "Сервисы:"
    adb shell dumpsys activity services | grep -A10 -B5 iperf3client || echo "   Сервисы не активны"
}

# Главное меню
case "$1" in
    "live"|"l")
        show_live_logs
        ;;
    "errors"|"e")
        show_errors
        ;;
    "clear"|"c")
        clear_logs
        ;;
    "pull"|"p")
        pull_logs
        ;;
    "crash"|"cr")
        analyze_crashes
        ;;
    "status"|"s")
        app_status
        ;;
    "help"|"h"|"")
        echo "Использование: $0 [команда]"
        echo
        echo "Команды:"
        echo "  live, l     - Показать логи в реальном времени"
        echo "  errors, e   - Показать только ошибки"
        echo "  clear, c    - Очистить логи"
        echo "  pull, p     - Извлечь файлы логов с устройства"
        echo "  crash, cr   - Анализ падений"
        echo "  status, s   - Статус приложения"
        echo "  help, h     - Показать эту справку"
        echo
        echo "Примеры:"
        echo "  $0 live     # Мониторинг в реальном времени"
        echo "  $0 errors   # Только ошибки"
        echo "  $0 pull     # Скачать все логи"
        ;;
    *)
        echo "❌ Неизвестная команда: $1"
        echo "Используйте '$0 help' для справки"
        exit 1
        ;;
esac