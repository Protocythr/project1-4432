import java.io.*;
import java.util.*;

public class Main {
    private static Frame[] frames;
    private static int cacheSize;
    private static int lastEvictedIndex = 0;

    public static void main(String[] args) throws Exception {
        // Your code here
        Main.cacheSize = args.length > 0 ? Integer.parseInt(args[0]) : -1;
        Main.frames = new Frame[cacheSize];
        for (int i = 0; i < cacheSize; i++) {
            Main.frames[i] = new Frame();
        }
        Scanner scanner = new Scanner(System.in);
        String[] startingCommands = {
            "SET 430 \"F05-Rec450, Jane Do, 10 Hill Rd, age020.\"",
            "GET 430",
            "GET 20",
            "SET 430 \"F05-Rec450, John Do, 23 Lake Ln, age056.\"",
            "PIN 5",
            "UNPIN 3",
            "GET 430",
            "PIN 5",
            "GET 646",
            "PIN 3",
            "SET 10 \"F01-Rec010, Tim Boe, 09 Deer Dr, age009.\"",
            "UNPIN 1",
            "GET 355",
            "PIN 2",
            "GET 156",
            "SET 10 \"F01-Rec010, No Work, 31 Hill St, age100.\""
        };
        int index = 0;
        while (true) {
            System.out.println("The program is ready for the next command");
            String[] commands;
            String command;
            if (index < startingCommands.length) {
                System.out.println("Executing command: " + startingCommands[index]);
                commands = startingCommands[index].split(" ");
                index++;
            }else{
                commands = scanner.nextLine().split(" ");
            }
            command = commands[0];
            int record = Integer.parseInt(commands[1]);
            if (commands.length > 2) {
                for (int i = 3; i < commands.length; i++) {
                    commands[2] += " " +commands[i];
                }
                commands = Arrays.copyOfRange(commands, 0, 3);
            }
            switch (command) {
                case "SET":
                    if (commands.length == 3) {
                        String newContent = commands[2].replace("\"", "");
                        Main.setRecord(record, newContent);
                    } else {
                        System.out.println("Invalid SET command format");
                    }
                    break;
                case "GET":
                    if (commands.length == 2) {
                        Main.getRecord(record);
                    } else {
                        System.out.println("Invalid GET command format");
                    }
                    break;
                case "PIN":
                    if (commands.length == 2) {
                        int blockId = record;
                        pinBlock(blockId);
                    } else {
                        System.out.println("Invalid PIN command format");
                    }
                    break;
                case "UNPIN":
                    if (commands.length == 2) {
                        int blockId = record;
                        unpinBlock(blockId);
                    } else {
                        System.out.println("Invalid PIN command format");
                    }
                    break;
                default:
                    System.out.println("Invalid command");
            }
            System.out.println();
        }
    }

    private static void pinBlock(int blockId) {
        int frame = findFrameInCache(blockId-1);
        if (frame == -1) {
            frame = allocateBlockFromDisk(blockId-1); // Load the block into cache if not present
            if (frame == -1) {
                System.out.print("The corresponding block " + blockId + " cannot be pinned because the memory buffers are full");
                return;
            }
            frames[frame].togglePinned();
            System.out.print("File " + blockId + " pinned in Frame " + (frame+1) + "; Not already pinned");
            if (frames[frame].getPreviousFileId() != -1) {
                System.out.print("; Evicted file " + (frames[frame].getPreviousFileId()+1) + " from Frame " + (frame+1));
            }
            return;
        }
        System.out.print("File " + blockId + " pinned in Frame " + (frame+1) + ";");
        if (frames[frame].getPinned()) {
            System.out.print(" Already pinned");
        } else {
            frames[frame].togglePinned();
            System.out.print(" Not already pinned");
        }
        return;
    }

    private static void unpinBlock(int blockId) {
        int frame = findFrameInCache(blockId-1);
        if (frame == -1) {
            System.out.print("The corresponding block " + blockId + " cannot be unpinned because it is not in memory.");
            return;
        }
        System.out.print("File " + blockId + " in frame " + (frame+1) + " is unpinned;");
        if (frames[frame].getPinned()) {
            frames[frame].togglePinned();
            System.out.print(" Frame " + (frame+1) + " was not already unpinned");
        } else {
            System.out.print(" Frame " + (frame+1) + " was already unpinned");
        }
        return;
    }

    private static int allocateBlockFromDisk(int blockId) {
        // file is not in cache, read from disk
        File fileToRead = new File("Project1/" + "F" + (blockId+1) + ".txt");
        String content;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileToRead));
            content = reader.readLine();
            reader.close();
        }catch (Exception e) {
            throw new IllegalStateException("File F" + (blockId+1) + ".txt does not exist in the Project1 directory");
        }
        return allocateFrame(blockId, content);
    }

    private static void setRecord(int record, String newContent) throws IOException {
        int file = record / 100;
        record = record % 100;
        if (frames == null) {
            throw new IllegalStateException("Cache not initialized");
        }
        if (record < 0 || record >= 100) {
            throw new IllegalArgumentException("Record number out of bounds");
        }
        int frameIndex = findFrameInCache(file);
        if (frameIndex != -1) {
            frames[frameIndex].setRecord(record, newContent);
            System.out.print("Write was successful;"+" File "+(file+1)+" already in memory;"+" Located in Frame "+(frameIndex+1));
            return;
        }
        // file is not in cache, read from disk
        int frame = allocateBlockFromDisk(file);
        if (frame != -1) {
            frames[frame].setRecord(record, newContent);
            System.out.print("Write was successful; Brought file " + (file+1) + " from disk; Placed in Frame " + (frame+1));
            if (frames[frame].getPreviousFileId() != -1) {
                System.out.print("; Evicted file " + (frames[frame].getPreviousFileId()+1) + " from Frame " + (frame+1));
            }
        }else{
            System.out.print("The corresponding block #"+(file+1)+" cannot be accessed from disk because the memory buffers are full; Write was unsuccessful ");
        }
        return;
    }

    private static void getRecord(int record) throws IOException {
        int file = record / 100;
        record = record % 100;
        if (frames == null) {
            throw new IllegalStateException("Cache not initialized");
        }
        if (record < 0 || record >= 100) {
            throw new IllegalArgumentException("Record number out of bounds");
        }
        int frameIndex = findFrameInCache(file);
        if (frameIndex != -1) {
            System.out.print(frames[frameIndex].returnRecord(record)+"; File " + (file+1) + " already in memory; Located in Frame " + (frameIndex+1));
            return;
        }
        // file is not in cache, read from disk
        int frame = allocateBlockFromDisk(file);
        if (frame != -1) {
            System.out.print(frames[frame].returnRecord(record)+"; Brought file " + (file+1) + " from disk; Placed in Frame " + (frame+1));
            if (frames[frame].getPreviousFileId() != -1) {
                System.out.print("; Evicted file " + (frames[frame].getPreviousFileId()+1) + " from Frame " + (frame+1));
            }
        }else{
            System.out.print("The corresponding block #"+(file+1)+" cannot be accessed from disk because the memory buffers are full");
        }
    }

    private static int findFrameInCache(int blockId) {
        for (int i = 0; i < cacheSize; i++) {
            if (frames[i].getBlockId() == blockId) {
                return i;
            }
        }
        return -1;
    }

    private static int allocateFrame(int blockId, String content) {
        int emptyFrameIndex = findEmptyFrame(blockId);
        if (emptyFrameIndex == -1) {
            return emptyFrameIndex;
        }
        frames[emptyFrameIndex].setContent(content, blockId);
        return emptyFrameIndex;
    }

    private static int findEmptyFrame(int blockId) {
        // Find an empty frame in the cache
        for (int i = 0; i < cacheSize; i++) {
            if (frames[i].getBlockId() == -1) {
                return i;
            }
        }
        for (int i = 0; i < cacheSize; i++) {
            if (lastEvictedIndex == cacheSize-1) {
                lastEvictedIndex = 0;
            }else {
                lastEvictedIndex++;
            }
            if (!frames[lastEvictedIndex].getPinned()) {
                return lastEvictedIndex;
            }
        }        
        return -1;
    }

    private static class Frame {
        private int blockId = -1;
        private String content;
        private Boolean dirty;
        private Boolean pinned;
        private int recordSize = 40;
        private int previousFileId = -1;


        public Frame() {
            this.dirty = false;
            this.pinned = false;
        }
        
        public int getBlockId() {
            return blockId;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content, int blockId) {
            this.writeBack();
            this.previousFileId = this.blockId;
            this.content = content;
            this.blockId = blockId;
            return;
        }

        public int getPreviousFileId() {
            return this.previousFileId;
        }

        public Boolean getDirty() {
            return this.dirty;
        }

        public void toggleDirty() {
            this.dirty = !this.dirty;
        }

        public Boolean getPinned() {
            return this.pinned;
        }

        public void togglePinned() {
            this.pinned = !this.pinned;
        }

        public String returnRecord(int record) {
            record = record - 1;
            return content.substring(record * recordSize, (record + 1) * recordSize);
        }

        public void setRecord(int record, String newRecord) {
            record = record - 1;
            if (newRecord.length() > recordSize) {
                throw new IllegalArgumentException("New record exceeds record size");
            }
            StringBuilder sb = new StringBuilder(content);
            sb.replace(record * recordSize, (record + 1) * recordSize, newRecord);
            content = sb.toString();
            dirty = true;
        }

        public void writeBack() {
            if (dirty) {
                File fileToWrite = new File("Project1/" + "F" + (blockId+1) + ".txt");
                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(fileToWrite));
                    writer.write(content);
                    writer.close();
                } catch (Exception e) {
                    throw new IllegalArgumentException("Error creating file: " + fileToWrite.getName(), e);
                }
                dirty = false;
            }else {
            }
        }
    }
}