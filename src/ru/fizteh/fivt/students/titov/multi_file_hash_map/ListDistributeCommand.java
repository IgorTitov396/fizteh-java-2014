package ru.fizteh.fivt.students.titov.multi_file_hash_map;

import ru.fizteh.fivt.students.titov.file_map.FileMap;
import ru.fizteh.fivt.students.titov.file_map.ListFileMapCommand;

public class ListDistributeCommand extends MultiFileHashMapCommand {
    public ListDistributeCommand() {
        initialize("list", 1);
    }

    @Override
    public boolean run(MFileHashMap myMap, String[] args) {
        FileMap currentTable = myMap.getCurrentTable();
        if (myMap.getCurrentTable() == null) {
            isTable(currentTable);
            return true;
        }
        ListFileMapCommand commandList = new ListFileMapCommand();
        return commandList.run(currentTable, args);
    }
}
