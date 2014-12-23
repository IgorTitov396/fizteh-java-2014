package ru.fizteh.fivt.students.titov.multi_file_hash_map;

import ru.fizteh.fivt.students.titov.file_map.FileMap;
import ru.fizteh.fivt.students.titov.file_map.RemoveFileMapCommand;

public class RemoveDistributeCommand extends MultiFileHashMapCommand {
    public RemoveDistributeCommand() {
        initialize("remove", 1);
    }

    @Override
    public boolean run(MFileHashMap myMap, String[] args) {
        FileMap currentTable = myMap.getCurrentTable();
        if (myMap.getCurrentTable() == null) {
            isTable(currentTable);
            return true;
        }
        RemoveFileMapCommand removeCommand = new RemoveFileMapCommand();
        return removeCommand.run(currentTable, args);
    }
}
