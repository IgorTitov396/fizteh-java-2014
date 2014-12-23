package ru.fizteh.fivt.students.titov.multi_file_hash_map;

import ru.fizteh.fivt.students.titov.file_map.FileMap;

public class SizeCommand extends MultiFileHashMapCommand {
    public SizeCommand() {
        initialize("size", 1);
    }

    @Override
    public boolean run(MFileHashMap myMap, String[] args) {
        FileMap currentTable = myMap.getCurrentTable();
        if (currentTable == null) {
            isTable(currentTable);
        } else {
            System.out.println(currentTable.size());
        }
        return true;
    }
}
