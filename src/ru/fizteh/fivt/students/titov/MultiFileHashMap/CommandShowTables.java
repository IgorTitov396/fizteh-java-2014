package ru.fizteh.fivt.students.titov.MultiFileHashMap;

public class CommandShowTables extends CommandMultiFileHashMap {
    public CommandShowTables() {
        name = "show";
        numberOfArguments = 2;
    }

    @Override
    public boolean run(MFileHashMap myMap, String[] args) {
        if (!args[1].equals("tables")) {
            System.out.println(name + ": wrong arguments");
            return false;
        }
        myMap.showTables();
        return true;
    }
}
