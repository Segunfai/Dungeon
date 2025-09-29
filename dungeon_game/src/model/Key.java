package model;

public class Key extends Item {
    public Key(String name) {
        super(name);
    }

    @Override
    public void apply(GameState ctx) {
        System.out.println("Ключ звенит. " + getName() + " Возможно, где-то есть дверь...");
    }
}
