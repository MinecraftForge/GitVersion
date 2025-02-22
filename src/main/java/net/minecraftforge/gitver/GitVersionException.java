package net.minecraftforge.gitver;

// TODO [GitVersion] Document
public class GitVersionException extends RuntimeException {
    GitVersionException(String message) {
        super(message);
    }

    GitVersionException(String message, Throwable cause) {
        super(message, cause);
    }
}
