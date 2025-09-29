package core;

import model.GameState;
import java.util.List;

@FunctionalInterface
public interface Command { void execute(GameState ctx, List<String> args); }
