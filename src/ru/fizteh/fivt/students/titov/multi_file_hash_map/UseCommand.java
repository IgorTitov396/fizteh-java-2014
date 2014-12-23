package ru.fizteh.fivt.students.titov.multi_file_hash_map;

import ru.fizteh.fivt.students.titov.file_map.FileMap;

public class UseCommand extends MultiFileHashMapCommand {
    private static final String USING = "using ";
    public UseCommand() {
        initialize("use", 1);
    }

    @Override
    public boolean run(MFileHashMap myMap, String[] args) {
        FileMap newCurrentTable = (FileMap) myMap.getTable(args[1]);
        if (newCurrentTable != null) {
            FileMap currentTable = myMap.getCurrentTable();
            if (currentTable == null) {
                myMap.setCurrentTable(newCurrentTable);
                System.out.println(USING + args[1]);
            } else if (currentTable.getNumberOfUncommittedChanges() > 0) {
                System.out.println(currentTable.getNumberOfUncommittedChanges() + " unsaved changes");
            } else {
                myMap.setCurrentTable(newCurrentTable);
                System.out.println(USING + args[1]);
            }
        } else {
            System.out.println(args[1] + " not exists");
        }
        return true;
    }
}
