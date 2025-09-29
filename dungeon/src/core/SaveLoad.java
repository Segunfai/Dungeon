package core;

import model.*;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class SaveLoad {
    private static final Path SAVE = Paths.get("save.txt");
    private static final Path SCORES = Paths.get("scores.csv");

    public static void save(GameState s) {
        try (BufferedWriter w = Files.newBufferedWriter(SAVE)) {
            Player p = s.getPlayer();
            w.write("player;" + p.getName() + ";" + p.getHp() + ";" + p.getAttack());
            w.newLine();

            String inv = p.getInventory().stream()
                    .map(i -> i.getClass().getSimpleName() + ":" + i.getName())
                    .collect(Collectors.joining(","));
            w.write("inventory;" + inv);
            w.newLine();

            // СОХРАНЯЕМ ТЕКУЩУЮ КОМНАТУ
            w.write("room;" + s.getCurrent().getName());
            w.newLine();

            // СОХРАНЯЕМ СЧЕТ
            w.write("score;" + s.getScore());
            w.newLine();

            System.out.println("Игра сохранена в " + SAVE.toAbsolutePath());
            writeScore(p.getName(), s.getScore());

        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось сохранить игру", e);
        }
    }

    private static void restoreCurrentRoom(GameState s, String roomName) {
        // Простой способ - создаем комнаты как в bootstrapWorld()
        Room square = new Room("Площадь", "Каменная площадь с фонтаном.");
        Room forest = new Room("Лес", "Шелест листвы и птичий щебет.");
        Room cave = new Room("Пещера", "Темно и сыро.");

        square.getNeighbors().put("north", forest);
        forest.getNeighbors().put("south", square);
        forest.getNeighbors().put("east", cave);
        cave.getNeighbors().put("west", forest);

        // Восстанавливаем предметы и монстров
        forest.getItems().add(new Potion("Малое зелье", 5));
        forest.setMonster(new Monster("Волк", 1, 8));

        // Устанавливаем текущую комнату
        switch (roomName) {
            case "Лес" -> s.setCurrent(forest);
            case "Пещера" -> s.setCurrent(cave);
            default -> s.setCurrent(square); // По умолчанию площадь
        }
    }

    public static void load(GameState s) {
        if (!Files.exists(SAVE)) {
            System.out.println("Сохранение не найдено.");
            return;
        }

        try (BufferedReader r = Files.newBufferedReader(SAVE)) {
            Map<String, String> map = new HashMap<>();
            for (String line; (line = r.readLine()) != null; ) {
                String[] parts = line.split(";", 2);
                if (parts.length == 2) {
                    map.put(parts[0], parts[1]);
                }
            }

            Player p = s.getPlayer();

            // Загружаем данные игрока
            String[] playerData = map.getOrDefault("player", "Hero;10;3").split(";");
            p.setName(playerData[0]);
            p.setHp(Integer.parseInt(playerData[1]));
            p.setAttack(Integer.parseInt(playerData[2]));

            // Очищаем и загружаем инвентарь
            p.getInventory().clear();
            String inv = map.getOrDefault("inventory", "");
            if (!inv.isBlank()) {
                for (String tok : inv.split(",")) {
                    String[] itemData = tok.split(":", 2);
                    if (itemData.length < 2) continue;

                    switch (itemData[0]) {
                        case "Potion" -> p.getInventory().add(new Potion(itemData[1], 5));
                        case "Key" -> p.getInventory().add(new Key(itemData[1]));
                        case "Weapon" -> p.getInventory().add(new Weapon(itemData[1], 3));
                        default -> {}
                    }
                }
            }

            // ВОССТАНАВЛИВАЕМ ТЕКУЩУЮ КОМНАТУ
            String roomName = map.getOrDefault("room", "Площадь");
            restoreCurrentRoom(s, roomName);

            // ВОССТАНАВЛИВАЕМ СЧЕТ
            String scoreStr = map.getOrDefault("score", "0");
            s.addScore(Integer.parseInt(scoreStr) - s.getScore()); // Устанавливаем точное значение

            System.out.println("Игра загружена! Комната: " + roomName);

        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось загрузить игру", e);
        }
    }

    public static void printScores() {
        if (!Files.exists(SCORES)) {
            System.out.println("Пока нет результатов.");
            return;
        }
        try (BufferedReader r = Files.newBufferedReader(SCORES)) {
            System.out.println("Таблица лидеров (топ-10):");
            r.lines().skip(1).map(l -> l.split(",")).map(a -> new Score(a[1], Integer.parseInt(a[2])))
                    .sorted(Comparator.comparingInt(Score::score).reversed()).limit(10)
                    .forEach(s -> System.out.println(s.player() + " — " + s.score()));
        } catch (IOException e) {
            System.err.println("Ошибка чтения результатов: " + e.getMessage());
        }
    }

    private static void writeScore(String player, int score) {
        try {
            boolean header = !Files.exists(SCORES);
            try (BufferedWriter w = Files.newBufferedWriter(SCORES, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                if (header) {
                    w.write("ts,player,score");
                    w.newLine();
                }
                w.write(LocalDateTime.now() + "," + player + "," + score);
                w.newLine();
            }
        } catch (IOException e) {
            System.err.println("Не удалось записать очки: " + e.getMessage());
        }
    }

    private record Score(String player, int score) {
    }
}
