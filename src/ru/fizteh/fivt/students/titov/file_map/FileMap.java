package ru.fizteh.fivt.students.titov.file_map;

import ru.fizteh.fivt.storage.structured.Storeable;
import ru.fizteh.fivt.storage.structured.Table;
import ru.fizteh.fivt.storage.structured.TableProvider;
import ru.fizteh.fivt.students.titov.storeable.TypesUtils;
import ru.fizteh.fivt.students.titov.storeable.Serializator;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;

public class FileMap implements Table {
    private static final int MAX_NUMBER_OF_DIRS = 16;
    private static final int MAX_NUMBER_OF_FILES = 16;
    private static final String SUFFIX_OF_DIRECTORY = ".dir";
    private static final String SUFFIX_OF_FILE = ".dat";
    private static final String FILE_ENCODING = "UTF-8";
    private Map<String, Storeable> stableData;
    private Map<String, Storeable> addedData;
    private Map<String, Storeable> changedData;
    private Set<String> removedData;
    private List<Class<?>> typeList;
    private int numberOfColumns;
    private String directoryOfTable;
    private TableProvider parent;

    private int getNumberOfDirectory(int hash) {
        int result = hash % MAX_NUMBER_OF_DIRS;
        if (result < 0) {
            result += MAX_NUMBER_OF_DIRS;
        }
        return result;
    }

    private int getNumberOfFile(int hash) {
        int result = hash / MAX_NUMBER_OF_DIRS % MAX_NUMBER_OF_FILES;
        if (result < 0) {
            result += MAX_NUMBER_OF_FILES;
        }
        return result;
    }

    private void clearAll() {
        removedData.clear();
        addedData.clear();
        changedData.clear();
    }

    /**
     * Create empty Filemap
     *
     * @param newDirectory - directory of this file_map
     * @param newTypeList - list of types (signature of table)
     */
    public FileMap(String newDirectory, List<Class<?>> newTypeList, TableProvider newParent) {
        directoryOfTable = newDirectory;
        stableData = new HashMap<>();
        addedData = new HashMap<>();
        changedData = new HashMap<>();
        removedData = new HashSet<>();
        typeList = newTypeList;
        numberOfColumns = typeList.size();
        parent = newParent;
    }

    public TableProvider getTableProvider() {
        return parent;
    }

    @Override
    public String getName() {
        return Paths.get(directoryOfTable).getFileName().toString();
    }

    @Override
    public Storeable get(String key) throws IllegalArgumentException {
        if (key == null) {
            throw new IllegalArgumentException("null argument");
        }
        if (removedData.contains(key)) {
            return null;
        }
        if (changedData.containsKey(key)) {
            return changedData.get(key);
        }
        if (addedData.containsKey(key)) {
            return addedData.get(key);
        }
        return stableData.get(key);
    }

    @Override
    public Storeable remove(String key) throws IllegalArgumentException {
        if (key == null) {
            throw new IllegalArgumentException("null argument");
        }
        if (removedData.contains(key)) {
            return null;
        }
        if (addedData.containsKey(key)) {
            return addedData.remove(key);
        }
        if (changedData.containsKey(key)) {
            removedData.add(key);
            return changedData.remove(key);
        }
        if (stableData.containsKey(key)) {
            removedData.add(key);
        }
        return stableData.get(key);
    }

    @Override
    public Storeable put(String key, Storeable value) throws IllegalArgumentException {
        if (key == null || value == null) {
            throw new IllegalArgumentException("null argument");
        }
        TypesUtils.checkNewStorableValue(typeList, value);

        boolean wasDeleted = false;
        if (removedData.contains(key)) {
            removedData.remove(key);
            wasDeleted = true;
        }
        if (changedData.containsKey(key)) {
            return changedData.put(key, value);
        }
        if (addedData.containsKey(key)) {
            return addedData.put(key, value);
        }

        if (stableData.containsKey(key)) {
            changedData.put(key, value);
        } else {
            addedData.put(key, value);
        }

        if (wasDeleted) {
            return null;
        } else {
            return stableData.get(key);
        }
    }

    @Override
    public int size() {
        return stableData.size() + addedData.size() - removedData.size();
    }

    @Override
    public int getColumnsCount() {
        return numberOfColumns;
    }

    @Override
    public Class<?> getColumnType(int columnIndex) throws IndexOutOfBoundsException {
        return typeList.get(columnIndex);
    }

    @Override
    public int rollback() {
        int result = changedData.size() + removedData.size() + addedData.size();
        clearAll();
        return result;
    }

    @Override
    public int commit() throws IOException {
        int result = changedData.size() + removedData.size() + addedData.size();
        stableData.keySet().removeAll(removedData);
        stableData.putAll(changedData);
        stableData.putAll(addedData);
        boolean allRight = true;
        if (changedData.size() + removedData.size() > 0) {
            Set<String> resaveKeys = removedData;
            resaveKeys.addAll(changedData.keySet());
            for (String oneKey : resaveKeys) {
                if (!save(oneKey, false)) {
                    allRight = false;
                }
            }
        }
        for (String oneKey : addedData.keySet()) {
            if (!save(oneKey, true)) {
                allRight = false;
            }
        }

        clearAll();
        if (allRight) {
            return result;
        } else {
            throw new IOException();
        }
    }

    public List<String> list() {
        ArrayList<String> keyList = new ArrayList<>(stableData.keySet());
        keyList.removeAll(removedData);
        keyList.addAll(addedData.keySet());
        return keyList;
    }

    @Override
    public int getNumberOfUncommittedChanges() {
        return addedData.size() + changedData.size() + removedData.size();
    }

    public boolean init() throws BadFileException {
        String[] listOfDirectories = new File(directoryOfTable).list();
        if (listOfDirectories == null) {
            return true;
        }
        for (String directory: listOfDirectories) {
            String currentDirectory = directoryOfTable + File.separator
                    + directory;
            if (!Files.isDirectory(Paths.get(currentDirectory))) {
                continue;
            }
            String[] listOfFiles = new File(currentDirectory).list();
            for (String oneFile : listOfFiles) {
                String currentFile = currentDirectory + File.separator
                        + oneFile;
                int numberOfDirectory = directory.charAt(0) - '0';
                if (directory.charAt(1) != '.') {
                    numberOfDirectory = 10 * numberOfDirectory + directory.charAt(1) - '0';
                }
                int numberOfFile = oneFile.charAt(0) - '0';
                if (oneFile.charAt(1) != '.') {
                    numberOfFile = 10 * numberOfFile + oneFile.charAt(1) - '0';
                }
                try (FileInputStream inStream = new FileInputStream(currentFile)) {
                    FileChannel inputChannel;
                    inputChannel = inStream.getChannel();
                    ByteBuffer bufferFromDisk;
                    try {
                        bufferFromDisk =
                                inputChannel.map(MapMode.READ_ONLY, 0, inputChannel.size());
                    } catch (IOException e) {
                        throw new BadFileException(e);
                    }
                    try {
                        while (bufferFromDisk.hasRemaining()) {
                            byte[] key;
                            byte[] value;
                            int keySize;
                            int valueSize;
                            if (bufferFromDisk.remaining() >= 4) {
                                keySize = bufferFromDisk.getInt();
                                key = new byte[keySize];
                            } else {
                                throw new BadFileException();
                            }

                            if (keySize < 0) {
                                throw new BadFileException();
                            }

                            if (bufferFromDisk.remaining() >= keySize) {
                                bufferFromDisk.get(key, 0, key.length);
                            } else {
                                throw new BadFileException();
                            }

                            if (bufferFromDisk.remaining() >= 4) {
                                valueSize = bufferFromDisk.getInt();
                                if (valueSize < 0) {
                                    throw new BadFileException();
                                }
                                value = new byte[valueSize];
                            } else {
                                throw new BadFileException();
                            }
                            if (bufferFromDisk.remaining() >= valueSize) {
                                bufferFromDisk.get(value, 0, value.length);
                            } else {
                                throw new BadFileException();
                            }

                            String keyString = new String(key, FILE_ENCODING);
                            if (getNumberOfDirectory(keyString.hashCode()) != numberOfDirectory
                                    || getNumberOfFile(keyString.hashCode()) != numberOfFile) {
                                throw new BadFileException();
                            }

                            try {
                                stableData.put(new String(key, FILE_ENCODING),
                                        Serializator.deserialize(this, new String(value, "UTF-8")));
                            } catch (UnsupportedEncodingException e) {
                                throw new BadFileException(e);
                            } catch (ParseException e) {
                                throw new BadFileException(e);
                            }
                        }
                    } catch (NullPointerException e) {
                        throw new BadFileException(e);
                    }
                } catch (FileNotFoundException e) {
                    throw new BadFileException(e);
                } catch (BadFileException e) {
                    throw new BadFileException(e);
                } catch (IOException e) {
                    throw new BadFileException(e);
                }
            }
        }
        return true;
    }

    /**
     * Returns True if the record on the disc was successful and otherwise False.
     */
    public boolean save(String key, boolean appendFile) throws BadFileException {
        HashSet<String> keySet = new HashSet<>();
        ByteBuffer bufferForSize = ByteBuffer.allocate(4);

        int numberOfDirectory = getNumberOfDirectory(key.hashCode());
        int numberOfFile = getNumberOfFile(key.hashCode());
        if (appendFile) {
            keySet.clear();
            keySet.add(key);
        } else {
            Set<String> keySetFromDB = stableData.keySet();
            for (String oneKey : keySetFromDB) {
                if (numberOfDirectory == getNumberOfDirectory(oneKey.hashCode())
                        & numberOfFile == getNumberOfFile(oneKey.hashCode())) {
                    keySet.add(oneKey);
                }
            }
        }

        Path directoryForsave;
        Path fileForsave;
        directoryForsave = Paths.get(directoryOfTable, numberOfDirectory + SUFFIX_OF_DIRECTORY);
        if (!Files.exists(directoryForsave)) {
            try {
                Files.createDirectory(directoryForsave);
            } catch (IOException e) {
                System.err.println("error while creating directory for save");
                return false;
            }
        }

        fileForsave = Paths.get(directoryForsave.toString(), numberOfFile + SUFFIX_OF_FILE);
        if (!Files.exists(fileForsave)) {
            try {
                Files.createFile(fileForsave);
            } catch (IOException e) {
                System.err.println("error while creating file for save");
                return false;
            }
        }

        try (FileOutputStream outputStream
                     = new FileOutputStream(fileForsave.toString(), appendFile)) {
            for (String oneKey : keySet) {
                try {
                    byte[] keyByte = oneKey.getBytes("UTF-8");
                    byte[] valueByte = Serializator.serialize(this, stableData.get(oneKey)).getBytes("UTF-8");
                    outputStream.write(bufferForSize.putInt(0, keyByte.length).array());
                    outputStream.write(keyByte);
                    outputStream.write(bufferForSize.putInt(0, valueByte.length).array());
                    outputStream.write(valueByte);
                } catch (UnsupportedEncodingException e) {
                    System.err.println("unsupported encoding");
                    return false;
                } catch (IOException e) {
                    System.err.println("io exception");
                    return false;
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("file not found");
            return false;
        } catch (IOException e) {
            System.err.println("io exception");
            return false;
        }

        if (!appendFile) {
            deleteEmptyFiles(directoryForsave, fileForsave);
        }
        return true;
    }

    public boolean deleteEmptyFiles(Path directory, Path file) throws BadFileException {
        try {
            if (Files.size(file) == 0) {
                Files.delete(file);
            }
        } catch (IOException e) {
            throw new BadFileException(e);
        }
        String[] listOfFiles = new File(directory.toString()).list();
        if (listOfFiles.length == 0) {
            try {
                Files.delete(directory);
            } catch (IOException e) {
                throw new BadFileException(e);
            }
        }
        return true;
    }
}
