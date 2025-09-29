package model;

public class Weapon extends Item {
    private final int bonus;

    public Weapon(String name, int bonus) {
        super(name);
        this.bonus = bonus;
    }

    public int getBonus() {
        return bonus;
    }

    @Override
    public void apply(GameState ctx) {
        var p = ctx.getPlayer();
        int oldAttack = p.getAttack();
        p.setAttack(oldAttack + bonus);
        System.out.println(getName() + " экипирован!");
        System.out.println("Атака увеличена: " + oldAttack + " → " + p.getAttack());
        p.getInventory().remove(this);
    }
}
