package ru.fizteh.fivt.students.titov.shell;

import java.nio.file.Files;

public class MkdirCommand extends ru.fizteh.fivt.students.titov.shell.Command {
    public MkdirCommand() {
        name = "mkdir";
        numberOfArguments = 2;
    }

    @Override
    public boolean run(final String[] arguments) {
        if (arguments.length != numberOfArguments) {
            System.out.println("wrong number of arguments");
            return false;
        }
        try {
            Files.createDirectory(PathsFunction.toAbsolutePathString(arguments[1]));
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
