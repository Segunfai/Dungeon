package core;

import model.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

public class Game {
    private final GameState state = new GameState();
    private final Map<String, Command> commands = new LinkedHashMap<>();

    static {
        WorldInfo.touch("Game");
    }

    public Game() {
        registerCommands();
        bootstrapWorld();
    }

    private void registerCommands() {
        commands.put("help", (ctx, a) -> System.out.println("Команды: " + String.join(", ", commands.keySet())));
        commands.put("gc-stats", (ctx, a) -> {
            Runtime rt = Runtime.getRuntime();
            long free = rt.freeMemory(), total = rt.totalMemory(), used = total - free;
            System.out.println("Память: used=" + used + " free=" + free + " total=" + total);
        });

        //Демонстрация реализации garbageCollector
        commands.put("alloc", (ctx, a) -> {
            // Демонстрация работы GC - создаем много объектов
            List<String> garbage = new ArrayList<>();
            for (int i = 0; i < 100000; i++) {
                garbage.add("String object " + i);
            }
            System.out.println("Создано 100000 объектов. GC должен их очистить.");
        });

        commands.put("demo-errors", (ctx, a) -> {
            System.out.println("=== Демонстрация ошибок ===");

            // Ошибка выполнения (Runtime Exception)
            System.out.println("1. Ошибка выполнения (ArithmeticException):");
            try {
                int result = 10 / 0; // Деление на ноль
            } catch (ArithmeticException e) {
                System.out.println("   Поймано: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            }

            // Ошибка компиляции (необходимо раскомментировать для проверок)
            /*
            System.out.println("2. Ошибка компиляции (пример):");
            System.out.println("   // String x = 123; // Не компилируется: несовместимые типы");
            System.out.println("   Эта ошибка обнаруживается на этапе компиляции");
             */
        });
        commands.put("look", (ctx, a) -> System.out.println(ctx.getCurrent().describe()));

        //Добавлена реализация команды move
        commands.put("move", (ctx, a) -> {
            if (a.isEmpty()) {
                throw new InvalidCommandException("Укажите направление: north, south, east, west");
            }

            String direction = a.getFirst().toLowerCase();
            Room current = ctx.getCurrent();
            Room nextRoom = current.getNeighbors().get(direction);

            if (nextRoom == null) {
                throw new InvalidCommandException("Нет пути в направлении: " + direction);
            }

            ctx.setCurrent(nextRoom);
            System.out.println("Вы перешли в: " + nextRoom.getName());
            System.out.println(nextRoom.describe());
        });

        //Реализация команды take
        commands.put("take", (ctx, a) -> {
            if (a.isEmpty()) {
                throw new InvalidCommandException("Укажите название предмета");
            }

            String itemName = String.join(" ", a);
            Room current = ctx.getCurrent();
            Player player = ctx.getPlayer();

            // Ищем предмет в комнате
            Optional<Item> foundItem = current.getItems().stream()
                    .filter(item -> item.getName().equalsIgnoreCase(itemName))
                    .findFirst();

            if (foundItem.isEmpty()) {
                throw new InvalidCommandException("Предмет не найден: " + itemName);
            }

            Item item = foundItem.get();
            current.getItems().remove(item);
            player.getInventory().add(item);

            System.out.println("Взято: " + item.getName());
        });

        //Реализация инвентаря
        commands.put("inventory", (ctx, a) -> {
            Player player = ctx.getPlayer();
            List<Item> inventory = player.getInventory();

            if (inventory.isEmpty()) {
                System.out.println("Инвентарь пуст");
                return;
            }

            // Группировка по типу предмета с использованием Stream API
            Map<String, List<Item>> groupedItems = inventory.stream()
                    .collect(Collectors.groupingBy(item -> {
                        if (item instanceof Potion) return "Potion";
                        if (item instanceof Weapon) return "Weapon";
                        if (item instanceof Key) return "Key";
                        return "Other";
                    }));

            // Сортировка по названию типа с помощью стримов
            groupedItems.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        String type = entry.getKey();
                        List<Item> items = entry.getValue();

                        // Группировка по имени и подсчет количества
                        Map<String, Long> itemCounts = items.stream()
                                .collect(Collectors.groupingBy(
                                        Item::getName,
                                        Collectors.counting()
                                ));

                        // Вывод отсортированный по имени предмета
                        itemCounts.entrySet().stream()
                                .sorted(Map.Entry.comparingByKey())
                                .forEach(itemEntry -> {
                                    System.out.println("- " + type + " (" + itemEntry.getValue() + "): " + itemEntry.getKey());
                                });
                    });
        });

        //Реализуем команду use
        commands.put("use", (ctx, a) -> {
            if (a.isEmpty()) {
                throw new InvalidCommandException("Укажите название предмета");
            }

            String itemName = String.join(" ", a);
            Player player = ctx.getPlayer();

            // Ищем предмет в инвентаре
            Optional<Item> foundItem = player.getInventory().stream()
                    .filter(item -> item.getName().equalsIgnoreCase(itemName))
                    .findFirst();

            if (foundItem.isEmpty()) {
                throw new InvalidCommandException("Предмет не найден в инвентаре: " + itemName);
            }

            Item item = foundItem.get();
            item.apply(ctx); // Полиморфизм - каждый предмет сам знает, что делать
        });

        //Реализация битвы
        commands.put("fight", (ctx, a) -> {
            Room current = ctx.getCurrent();
            Player player = ctx.getPlayer();
            Monster monster = current.getMonster();

            if (monster == null) {
                throw new InvalidCommandException("В этой комнате нет монстра");
            }

            System.out.println("Начинается бой с " + monster.getName());

            // Цикл боя
            while (player.getHp() > 0 && monster.getHp() > 0) {
                // Ход игрока
                System.out.println("Вы бьёте " + monster.getName() + " на " + player.getAttack() + ".");
                monster.setHp(monster.getHp() - player.getAttack());
                System.out.println("HP монстра: " + Math.max(monster.getHp(), 0));

                if (monster.getHp() <= 0) {
                    System.out.println("Монстр побежден!");
                    // Монстр выпадает лут
                    if (Math.random() > 0.5) {
                        Item loot = new Potion("Зелье из дропа", 3);
                        current.getItems().add(loot);
                        System.out.println("Монстр выронил: " + loot.getName());
                    }
                    current.setMonster(null);
                    ctx.addScore(10); // Бонус за победу
                    break;
                }

                // Ход монстра
                int monsterDamage = monster.getLevel();
                System.out.println("Монстр отвечает на " + monsterDamage + ".");
                player.setHp(player.getHp() - monsterDamage);
                System.out.println("Ваше HP: " + Math.max(player.getHp(), 0));

                if (player.getHp() <= 0) {
                    System.out.println("Вы погибли! Игра окончена.");
                    System.exit(0);
                }

                // Пауза между раундами
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        commands.put("save", (ctx, a) -> SaveLoad.save(ctx));
        commands.put("load", (ctx, a) -> SaveLoad.load(ctx));
        commands.put("scores", (ctx, a) -> SaveLoad.printScores());
        commands.put("exit", (ctx, a) -> {
            System.out.println("Пока!");
            System.exit(0);
        });
    }

    private void bootstrapWorld() {
        Player hero = new Player("Герой", 20, 5);
        state.setPlayer(hero);

        Room square = new Room("Площадь", "Каменная площадь с фонтаном.");
        Room forest = new Room("Лес", "Шелест листвы и птичий щебет.");
        Room cave = new Room("Пещера", "Темно и сыро.");
        square.getNeighbors().put("north", forest);
        forest.getNeighbors().put("south", square);
        forest.getNeighbors().put("east", cave);
        cave.getNeighbors().put("west", forest);

        forest.getItems().add(new Potion("Малое зелье", 5));
        forest.setMonster(new Monster("Волк", 1, 8));

        state.setCurrent(square);
    }

    public void run() {
        System.out.println("╔══════════════════════════════╗");
        System.out.println("║         DUNGEON MINI         ║");
        System.out.println("║    Подземные приключения     ║");
        System.out.println("╚══════════════════════════════╝");
        System.out.println("Введите 'help' для помощи");
        System.out.println();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                System.out.print("> ");
                String line = in.readLine();
                if (line == null) break;
                line = line.trim();
                if (line.isEmpty()) continue;
                List<String> parts = Arrays.asList(line.split("\s+"));
                String cmd = parts.getFirst().toLowerCase(Locale.ROOT);
                List<String> args = parts.subList(1, parts.size());
                Command c = commands.get(cmd);
                try {
                    if (c == null) throw new InvalidCommandException("Неизвестная команда: " + cmd);
                    c.execute(state, args);
                    state.addScore(1);
                } catch (InvalidCommandException e) {
                    System.out.println("Ошибка: " + e.getMessage());
                } catch (Exception e) {
                    System.out.println("Непредвиденная ошибка: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Ошибка ввода/вывода: " + e.getMessage());
        }
    }
}
