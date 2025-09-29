package core;

public class InvalidCommandException extends RuntimeException {
    public InvalidCommandException(String m) {
        super(m);
    }
}
