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
        //Изначальная реализация - команды в строку
        //commands.put("help", (ctx, a) -> System.out.println("Команды: " + String.join(", ", commands.keySet())));

        //Красивее и информативнее, но проще и более громоздко
            commands.put("help", (ctx, a) -> {
                System.out.println("===========================================");
                System.out.println("|           ДОСТУПНЫЕ КОМАНДЫ             |");
                System.out.println("|-----------------------------------------|");
                System.out.println("| about     - информация о игре           |");
                System.out.println("| look      - осмотреться в комнате       |");
                System.out.println("| move      - перемещение между комнатами |");
                System.out.println("| take      - взять предмет               |");
                System.out.println("| inventory - показать инвентарь          |");
                System.out.println("| use       - использовать предмет        |");
                System.out.println("| fight     - сразиться с монстром        |");
                System.out.println("| save      - сохранить игру              |");
                System.out.println("| load      - загрузить игру              |");
                System.out.println("| gc-stats  - статистика памяти           |");
                System.out.println("| gc-force  - очистка памяти              |");
                System.out.println("| scores    - таблица лидеров             |");
                System.out.println("| exit      - выход из игры               |");
                System.out.println("| help      - эта справка                 |");
                System.out.println("===========================================");
                System.out.println();
                System.out.println("  Примеры:");
                System.out.println("  move north, take Малое зелье, use Зелье");
                System.out.println();
            });

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

        commands.put("gc-force", (ctx, a) -> {
            System.out.println("Принудительный вызов Garbage Collector...");

            Runtime rt = Runtime.getRuntime();
            long beforeMemory = rt.totalMemory() - rt.freeMemory();
            System.out.println("Используемая память ДО очистки: " + beforeMemory / 1024 + " KB");

            // Принудительно вызываем GC
            System.gc();

            // Даем время на выполнение
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            long afterMemory = rt.totalMemory() - rt.freeMemory();
            System.out.println("Используемая память ПОСЛЕ очистки: " + afterMemory / 1024 + " KB");
            System.out.println("Очищено: " + (beforeMemory - afterMemory) / 1024 + " KB");
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

            // Ошибка компиляции

            System.out.println("2. Ошибка компиляции (пример):");
            System.out.println("   // String x = 123; // Не компилируется: несовместимые типы");
            System.out.println("   Эта ошибка обнаруживается на этапе компиляции");

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

            // Проверяем, закрыта ли дверь
            if (current.isDoorLocked(direction)) {
                throw new InvalidCommandException("Дверь в направлении " + direction + " закрыта. Нужен ключ!");
            }

            ctx.setCurrent(nextRoom);
            System.out.println("Вы перешли в: " + nextRoom.getName());
            System.out.println(nextRoom.describe());
        });

        //Реализация команды take
        commands.put("take", (ctx, a) -> {
            if (a.isEmpty()) {
                throw new InvalidCommandException("Укажите название предмета. Пример: take Малое зелье");
            }

            // Объединяем все аргументы в одну строку
            String itemName = String.join(" ", a);
            System.out.println("Поиск предмета: '" + itemName + "'");

            Room current = ctx.getCurrent();
            Player player = ctx.getPlayer();

            // Отладочная информация
            System.out.println("Предметы в комнате: " +
                    (current.getItems().isEmpty() ? "нет" :
                            current.getItems().stream().map(Item::getName).collect(Collectors.joining(", "))));

            // Ищем предмет в комнате (регистронезависимый поиск)
            Optional<Item> foundItem = current.getItems().stream()
                    .filter(item -> item.getName().equalsIgnoreCase(itemName))
                    .findFirst();

            if (foundItem.isEmpty()) {
                // Покажем какие предметы есть в комнате
                if (current.getItems().isEmpty()) {
                    throw new InvalidCommandException("В комнате нет предметов");
                } else {
                    String availableItems = current.getItems().stream()
                            .map(Item::getName)
                            .collect(Collectors.joining(", "));
                    throw new InvalidCommandException("Предмет '" + itemName + "' не найден. Доступные предметы: " + availableItems);
                }
            }

            Item item = foundItem.get();
            current.getItems().remove(item);
            player.getInventory().add(item);

            System.out.println("Взято: " + item.getName());
        });

        commands.put("debug", (ctx, a) -> {
            Room current = ctx.getCurrent();
            Player player = ctx.getPlayer();

            System.out.println("=== ОТЛАДОЧНАЯ ИНФОРМАЦИЯ ===");
            System.out.println("Комната: " + current.getName());
            System.out.println("Предметы в комнате: " + current.getItems().size());
            current.getItems().forEach(item ->
                    System.out.println("  - '" + item.getName() + "' (класс: " + item.getClass().getSimpleName() + ")")
            );
            System.out.println("Игрок: " + player.getName());
            System.out.println("Инвентарь: " + player.getInventory().size() + " предметов");
            System.out.println("============================");
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

            // Ищем предмет в инвентаре (регистронезависимый поиск)
            Optional<Item> foundItem = player.getInventory().stream()
                    .filter(item -> item.getName().equalsIgnoreCase(itemName))
                    .findFirst();

            if (foundItem.isEmpty()) {
                // Покажем что есть в инвентаре
                if (player.getInventory().isEmpty()) {
                    throw new InvalidCommandException("Инвентарь пуст");
                } else {
                    String inventoryItems = player.getInventory().stream()
                            .map(Item::getName)
                            .collect(Collectors.joining(", "));
                    throw new InvalidCommandException("Предмет не найден в инвентаре. Ваш инвентарь: " + inventoryItems);
                }
            }

            Item item = foundItem.get();
            System.out.println("Используется: " + item.getName());
            item.apply(ctx);
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

        //Реализация команды About
        commands.put("about", (ctx, a) -> {
            System.out.println("================================");
            System.out.println("|         DUNGEON MINI         |");
            System.out.println("|------------------------------|");
            System.out.println("| Версия игры: 1.0             |");
            System.out.println("| Java: " + System.getProperty("java.version") + "                 |");
            System.out.println("| " + System.getProperty("java.vendor") + "            |");
            System.out.println("|                              |");
            System.out.println("| Разработано для обучения     |");
            System.out.println("================================");
        });

        commands.put("save", (ctx, a) -> SaveLoad.save(ctx));
        commands.put("load", (ctx, a) -> SaveLoad.load(ctx));
        commands.put("scores", (ctx, a) -> SaveLoad.printScores());
        commands.put("exit", (ctx, a) -> {
            System.out.println("+-------------------------------+");
            System.out.println("|    До новых встреч, герой!   |");
            System.out.println("|  Подземелья ждут твоего      |");
            System.out.println("|      возвращения...          |");
            System.out.println("+-------------------------------+");
            System.exit(0);
        });
    }

    private void bootstrapWorld() {
        Player hero = new Player("Герой", 20, 5);
        state.setPlayer(hero);

        Room square = new Room("Площадь", "Каменная площадь с фонтаном.");
        Room forest = new Room("Лес", "Шелест листвы и птичий щебет.");
        Room cave = new Room("Пещера", "Темно и сыро.");
        //Добавление новой комнаты
        Room throneRoom = new Room("Зал великой славы",
                "Величественный зал с золотым троном. На троне лежит Меч легендарного героя!");
        square.getNeighbors().put("north", forest);
        forest.getNeighbors().put("south", square);
        forest.getNeighbors().put("east", cave);
        cave.getNeighbors().put("west", forest);
        cave.getNeighbors().put("north", throneRoom);
        throneRoom.getNeighbors().put("south", cave);

        // Закрываем дверь из пещеры в тронный зал
        cave.lockDoor("north");

        forest.getItems().add(new Potion("Малое зелье", 5));
        forest.setMonster(new Monster("Волк", 1, 8));
        cave.setMonster(new Monster("Гоблин", 2, 12));
        cave.getItems().add(new Key("Старый ключ"));
        throneRoom.getItems().add(new Weapon("Меч легендарного героя", 10));

        state.setCurrent(square);
    }

    private boolean isRussianLayoutMistake(String input) {
        // Если строка состоит из русских букв, но похожа на английскую команду
        return input.matches("[а-яё]+") && input.length() >= 2;
    }

    private String fixKeyboardLayout(String russianText) {
        Map<Character, Character> layoutMap = Map.ofEntries(
                Map.entry('й', 'q'), Map.entry('ц', 'w'), Map.entry('у', 'e'), Map.entry('к', 'r'),
                Map.entry('е', 't'), Map.entry('н', 'y'), Map.entry('г', 'u'), Map.entry('ш', 'i'),
                Map.entry('щ', 'o'), Map.entry('з', 'p'), Map.entry('х', '['), Map.entry('ъ', ']'),
                Map.entry('ф', 'a'), Map.entry('ы', 's'), Map.entry('в', 'd'), Map.entry('а', 'f'),
                Map.entry('п', 'g'), Map.entry('р', 'h'), Map.entry('о', 'j'), Map.entry('л', 'k'),
                Map.entry('д', 'l'), Map.entry('ж', ';'), Map.entry('э', '\''),
                Map.entry('я', 'z'), Map.entry('ч', 'x'), Map.entry('с', 'c'), Map.entry('м', 'v'),
                Map.entry('и', 'b'), Map.entry('т', 'n'), Map.entry('ь', 'm'), Map.entry('б', ','),
                Map.entry('ю', '.')
        );

        StringBuilder result = new StringBuilder();
        for (char c : russianText.toCharArray()) {
            result.append(layoutMap.getOrDefault(c, c));
        }
        return result.toString();
    }

    public void run() {
        System.out.println("=================================");
        System.out.println("|         DUNGEON MINI         |");
        System.out.println("|    Подземные приключения     |");
        System.out.println("=================================");
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
                    if (commands.get(cmd) == null && isRussianLayoutMistake(cmd)) {
                        String correctCmd = fixKeyboardLayout(cmd);
                        if (commands.get(correctCmd) != null) {
                            throw new InvalidCommandException(
                                    "Команда '" + cmd + "' не найдена. " +
                                            "Возможно, вы имели в виду '" + correctCmd + "'? " +
                                            "Проверьте раскладку клавиатуры!"
                            );
                        }
                    }
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
