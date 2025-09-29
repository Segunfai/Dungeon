package model;

import java.util.List;
import java.util.Map;

public class Key extends Item {
    public Key(String name) {
        super(name);
    }

    @Override
    public void apply(GameState ctx) {
        Room current = ctx.getCurrent();
        Player player = ctx.getPlayer();

        // Ищем закрытые двери в текущей комнате
        List<String> lockedDoors = current.getLockedDoors().entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .toList();

        if (lockedDoors.isEmpty()) {
            System.out.println("Здесь нет закрытых дверей для этого ключа");
            return;
        }

        // Открываем первую найденную закрытую дверь
        String doorToOpen = lockedDoors.getFirst();
        current.unlockDoor(doorToOpen);

        System.out.println("Ключ " + getName() + " открыл дверь на " + doorToOpen + "!");
        System.out.println("Теперь можно пройти в " + current.getNeighbors().get(doorToOpen).getName());

        // Убираем ключ из инвентаря после использования
        player.getInventory().remove(this);
    }
}
