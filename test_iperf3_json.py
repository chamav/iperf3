#!/usr/bin/env python3
"""
Скрипт для анализа JSON вывода iperf3 при разных режимах тестирования
"""

import json
import subprocess
import sys

def run_iperf3_test(streams=1, reverse=False, duration=3):
    """Запуск iperf3 теста и парсинг JSON"""
    cmd = [
        "iperf3",
        "-c", "iperf.he.net",
        "-p", "5201", 
        "-t", str(duration),
        "-P", str(streams),
        "-J"  # JSON output
    ]
    
    if reverse:
        cmd.append("-R")
    
    print(f"\n{'='*60}")
    print(f"Тест: {streams} поток(ов), {'Download' if reverse else 'Upload'}")
    print(f"Команда: {' '.join(cmd)}")
    print(f"{'='*60}")
    
    try:
        result = subprocess.run(cmd, capture_output=True, text=True, timeout=duration+5)
        
        if result.returncode != 0:
            print(f"Ошибка: {result.stderr}")
            return
            
        data = json.loads(result.stdout)
        
        # Анализируем финальные результаты
        if 'end' in data:
            end = data['end']
            print("\n📊 Финальные результаты:")
            
            if 'sum' in end:
                sum_data = end['sum']
                print(f"  sum.bits_per_second: {sum_data.get('bits_per_second', 0):,} bps")
                print(f"  sum.bits_per_second (Mbps): {sum_data.get('bits_per_second', 0) / 1_000_000:.2f} Mbps")
                
            if 'sum_sent' in end:
                sum_sent = end['sum_sent']
                print(f"  sum_sent.bits_per_second: {sum_sent.get('bits_per_second', 0):,} bps")
                print(f"  sum_sent.bits_per_second (Mbps): {sum_sent.get('bits_per_second', 0) / 1_000_000:.2f} Mbps")
                
            if 'sum_received' in end:
                sum_received = end['sum_received']
                print(f"  sum_received.bits_per_second: {sum_received.get('bits_per_second', 0):,} bps")
                print(f"  sum_received.bits_per_second (Mbps): {sum_received.get('bits_per_second', 0) / 1_000_000:.2f} Mbps")
            
            # Проверяем streams
            if 'streams' in end:
                print(f"\n  Количество потоков: {len(end['streams'])}")
                total_bps = 0
                for i, stream in enumerate(end['streams']):
                    stream_bps = stream.get('sender', {}).get('bits_per_second', 0) or stream.get('receiver', {}).get('bits_per_second', 0) or stream.get('bits_per_second', 0)
                    total_bps += stream_bps
                    print(f"    Поток {i+1}: {stream_bps / 1_000_000:.2f} Mbps")
                print(f"  📈 Сумма всех потоков: {total_bps / 1_000_000:.2f} Mbps")
                
        # Проверяем интервалы
        if 'intervals' in data and len(data['intervals']) > 0:
            interval = data['intervals'][0]  # Берем первый интервал
            print("\n📉 Первый интервал:")
            
            if 'sum' in interval:
                sum_data = interval['sum']
                print(f"  sum.bits_per_second: {sum_data.get('bits_per_second', 0) / 1_000_000:.2f} Mbps")
                
            if 'streams' in interval:
                print(f"  Потоков в интервале: {len(interval['streams'])}")
                
    except subprocess.TimeoutExpired:
        print("Тайм-аут при выполнении iperf3")
    except json.JSONDecodeError as e:
        print(f"Ошибка парсинга JSON: {e}")
        print(f"Вывод: {result.stdout[:500]}")
    except FileNotFoundError:
        print("iperf3 не найден. Установите: sudo apt install iperf3")
    except Exception as e:
        print(f"Неожиданная ошибка: {e}")

if __name__ == "__main__":
    # Проверяем наличие iperf3
    try:
        subprocess.run(["which", "iperf3"], check=True, capture_output=True)
    except:
        print("iperf3 не установлен. Установите: sudo apt install iperf3")
        sys.exit(1)
    
    # Тест 1: Upload, 1 поток
    run_iperf3_test(streams=1, reverse=False)
    
    # Тест 2: Upload, 2 потока
    run_iperf3_test(streams=2, reverse=False)
    
    # Тест 3: Download, 1 поток
    run_iperf3_test(streams=1, reverse=True)
    
    # Тест 4: Download, 2 потока
    run_iperf3_test(streams=2, reverse=True)