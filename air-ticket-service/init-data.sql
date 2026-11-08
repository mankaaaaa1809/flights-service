-- Начальные данные для AirTicketService (PostgreSQL, база flights).
-- Запускать ПОСЛЕ первого старта сервиса (таблицы создаёт Hibernate).
-- Скрипт безопасен: если в flights уже есть строки, ничего не делает.
-- Чтобы заменить старые демо-данные, раскомментируйте следующую строку:
-- TRUNCATE bookings, flights RESTART IDENTITY CASCADE;

DO $$
DECLARE
  d timestamp := date_trunc('day', now());
BEGIN
  IF EXISTS (SELECT 1 FROM flights) THEN
    RAISE NOTICE 'В таблице flights уже есть данные - пропускаю';
    RETURN;
  END IF;

  INSERT INTO flights (flight_number, origin, destination, departure_time, arrival_time, price, total_seats) VALUES
    ('B2 701',  'Минск', 'Москва',          d + interval '1 day 07:30', d + interval '1 day 09:05', 129.00, 120),
    ('B2 702',  'Москва', 'Минск',          d + interval '1 day 10:00', d + interval '1 day 11:35', 135.00, 120),
    ('B2 715',  'Минск', 'Санкт-Петербург', d + interval '2 day 12:15', d + interval '2 day 13:50', 142.50, 100),
    ('LH 1431', 'Минск', 'Франкфурт',       d + interval '2 day 06:45', d + interval '2 day 09:30', 310.50, 150),
    ('TK 392',  'Минск', 'Стамбул',         d + interval '3 day 09:20', d + interval '3 day 13:10', 240.00, 180),
    ('B2 835',  'Минск', 'Тбилиси',         d + interval '4 day 05:50', d + interval '4 day 09:20', 199.99,  90),
    ('B2 825',  'Минск', 'Батуми',          d + interval '6 day 07:00', d + interval '6 day 10:30', 215.00,  90),
    ('FZ 712',  'Минск', 'Дубай',           d + interval '5 day 22:10', d + interval '6 day 03:40', 455.00, 200);

  INSERT INTO bookings (flight_id, passenger_name, passport_number, status, created_at)
  SELECT f.id, b.pname, b.passport, b.status, now()
  FROM (VALUES
    ('B2 701',  'Иванов Иван Иванович',   'MP1234567', 'CONFIRMED'),
    ('B2 701',  'Петрова Анна Сергеевна', 'MP7654321', 'CONFIRMED'),
    ('B2 715',  'Сидоров Пётр Андреевич', 'KH2345678', 'CONFIRMED'),
    ('LH 1431', 'Ковалёва Мария Игоревна','MC3456789', 'CONFIRMED'),
    ('LH 1431', 'Новиков Артём Олегович', 'AB4567890', 'CANCELLED'),
    ('TK 392',  'Жуков Дмитрий Павлович', 'HB5678901', 'CONFIRMED'),
    ('B2 835',  'Орлова Елена Викторовна','MP6789012', 'CONFIRMED'),
    ('FZ 712',  'Волков Сергей Ильич', 'KB7890123', 'CONFIRMED')
  ) AS b(flight_no, pname, passport, status)
  JOIN flights f ON f.flight_number = b.flight_no;
END $$;
