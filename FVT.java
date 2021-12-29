import java.io.BufferedReader;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;

public class FVT {

    private static final Path FVT_PATH = Paths.get(".fvt");
    private static final Path LATEST_VERSION_PATH = Paths.get(".fvt/latestVersion.txt");
    private static final Path ADDED_FILES_PATH = Paths.get(".fvt/addedFiles.txt");
    private static final String SYSTEM_ERROR_STRING = "Underlying system problem. See ERR for details.";
    private static final int SYSTEM_ERROR_CODE = -3;

    private static List<String> addedFiles;

    public static void main(String... args) {
        if(args.length == 0) {
            System.out.print("Please specify command.");
            System.exit(1);
        }

        if(!(args[0].equals("init")) && !(args[0].equals("commit"))
                && !(args[0].equals("add")) && !(args[0].equals("detach"))
                && !(args[0].equals("checkout")) && !(args[0].equals("history"))
                && !(args[0].equals("version"))) {
            System.out.print("Unknown command " + args[0] + ".");
            System.exit(0);
        }

        if(!(args[0].equals("init")) && !Files.exists(FVT_PATH)) {
            System.out.println("Current directory is not initialized. Please use \"init\" command to initialize.");
            System.exit(-2);
        }

        if(!args[0].equals("init")) {
            addedFiles();
        }

        switch (args[0]) {
            case "init" -> init();
            case "add" -> add(args);
            case "detach" -> detach(args);
            case "checkout" -> checkout(args);
            case "commit" -> commit(args);
            case "history" -> history(args);
            case "version" -> version(args);
        }
    }

    static void init() {
        if(Files.exists(FVT_PATH)) {
            System.out.print("Current directory is already initialized.");
            System.exit(10);
        }

        try {
            //initialize fvt directory
            Files.createDirectory(FVT_PATH);
            System.out.println("Current directory initialized successfully.");

            // create files that store latest version, and files that were added to fvt
            Files.createFile(LATEST_VERSION_PATH);
            Files.createFile(ADDED_FILES_PATH);

            //new version directory (with message file)
            createNewVersionDir(0);

            addMessage("FVT initialized.");

        } catch (Exception e) {
            systemError(e);
        }
    }


    static void add(String... args) {
        if(args.length == 1) {
            System.out.print("Please specify file to add.");
            System.exit(20);
        }

        try {
            if (Files.exists(Paths.get(args[1])) && !addedFiles.contains(args[1])) {
                addedFiles.add(args[1]);
                writeToFile(ADDED_FILES_PATH, args[1] + "\n", true);

                createNewVersionDir(lastVersion() + 1);

                addMessage("Added file: " + args[1], args);

                System.out.println("File " + args[1] + " added successfully.");

            } else if (Files.notExists(Paths.get(args[1]))) {
                System.out.println("File " + args[1] + " not found.");
                System.exit(21);
            } else if (addedFiles.contains(args[1])) {
                System.out.println("File " + args[1] + " already added.");
            }
        } catch (Exception e) {
            System.out.println("File " + args[1] + " cannot be added, see ERR for details.");
            System.err.println(Arrays.toString(e.getStackTrace()));
            System.exit(22);
        }


    }

    static void detach(String... args) {
        if(args.length == 1) {
            System.out.println("Please specify file to detach.");
            System.exit(30);
        }

        try {

            if (Files.exists(Paths.get(args[1])) && addedFiles.contains(args[1])) {
                addedFiles.remove(args[1]);

                // updates added files
                try {
                    StringBuilder added = new StringBuilder();
                    for (String file : addedFiles) {
                        added.append(file);
                        added.append("\n");
                    }

                    writeToFile(ADDED_FILES_PATH, added.toString().trim(), false);

                } catch (Exception e) {
                    systemError(e);
                }

                createNewVersionDir(lastVersion() + 1);

                addMessage("Detached file: " + args[1], args);

                System.out.println("File " + args[1] + " detached successfully.");

            } else if (!addedFiles.contains(args[1])) {
                System.out.println("File " + args[1] + " is not added to fvt.");
            }

        }catch (Exception e) {
            System.out.println("File " + args[1] + " cannot be detached, see ERR for details.");
            System.err.println(Arrays.toString(e.getStackTrace()));
            System.exit(31);
        }
    }

    static void checkout(String... args) {

        long version = 0;

        try {
            version = Long.parseLong(args[1]);

            if(version > lastVersion()) {
                System.out.println("Invalid version number: " + args[1] + ".");
                System.exit(40);
            }

        } catch (Exception e) {
            System.out.println("Invalid version number: " + args[1] + ".");
            System.exit(40);
        }

        try {
            List<String> addedFilesInGivenVersion = Files.readAllLines(Paths.get(FVT_PATH +"/"+version+"/addedFiles.txt"));

            if(addedFilesInGivenVersion.size() > 0) {
                for (String file : addedFilesInGivenVersion) {
                    Path filePath = Paths.get(FVT_PATH + "/" + version + "/" + file.trim());
                    if (Files.exists(filePath)) {
                        Files.copy(filePath, Paths.get(file), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }

            System.out.println("Version " + version + " checked out successfully.");

        } catch (Exception e) {
            systemError(e);
        }

    }

    static void commit(String... args) {
        if(args.length == 1) {
            System.out.println("Please specify file to commit.");
            System.exit(50);
        }

        try {

            // the file is added and exists
            if (addedFiles.contains(args[1]) && Files.exists(Paths.get(args[1]))) {

                createNewVersionDir(lastVersion() + 1);

                System.out.println("File " + args[1] + " committed successfully.");
                addMessage("Committed file: " + args[1], args);

            } else if (Files.notExists(Paths.get(args[1]))) {
                System.out.println("File " + args[1] + " does not exist.");
                System.exit(51);
            } else if (Files.notExists(Paths.get(args[1])) && !addedFiles.contains(args[1])) {
                System.out.println("File " + args[1] + " is not added to fvt.");
            }
        } catch (Exception e) {
            System.out.println("File " + args[1] + " cannot be commited, see ERR for details.");
            System.err.println(Arrays.toString(e.getStackTrace()));
            System.exit(-52);
        }


    }

    static void history(String... args) {

        // no parameters
        if(args.length == 1) {
            for(long n = 0; n<= lastVersion(); n++) {

                System.out.println(n + ": " + readMessageFirstLine(n));
            }
            return;
        }

        // n last version details
        long numberOfVersions = 0;

        if(args.length == 3 && args[1].equals("-last")) {
            try {
                numberOfVersions = lastVersion() - Long.parseLong(args[2]);

                // because first version is 0
                numberOfVersions++;
            } catch (NumberFormatException ignored) {
                // if number is not correct we do nothing
            }
        }

        for(long n = lastVersion(); n - numberOfVersions >= 0; n--) {

            System.out.println(n + ": " + readMessageFirstLine(n));
        }
    }

    static void version(String... args) {
        if(args.length == 1) {
            System.out.println("Version: " + lastVersion());
            System.out.print(readMessageAllLines(lastVersion()).trim());
        } else if(args.length == 2) {

            long version=0;
            try {
                version = Long.parseLong(args[1]);
            } catch (Exception e) {
                System.out.println("Invalid version number: " + args[1] + ".");
                System.exit(60);
            }

            System.out.println("Version: " + version);
            System.out.print(readMessageAllLines(version).trim());

        }
    }

    public static String readMessageFirstLine(long version) {

        Path path = Paths.get(FVT_PATH + "/" + version + "/message.txt");
        BufferedReader reader;
        String message = "";
        try {
            reader = Files.newBufferedReader(path);
            message = reader.readLine();
        } catch(Exception e) {
            systemError(e);
        }

        return message;
    }

    public static String readMessageAllLines (long version) {
        Path path = Paths.get(FVT_PATH + "/" + version + "/message.txt");
        List<String> message;
        try {
            message = Files.readAllLines(path);

            StringBuilder result = new StringBuilder();

            for(int i=0; i<message.size(); i++) {
                result.append(message.get(i).trim());
                if(i != message.size()-1) {
                    result.append("\n");
                }
            }

            return result.toString().trim();
        } catch(Exception e) {
            systemError(e);
        }

        return "";
    }

    // read files that were added to fvt
    public static boolean addedFiles() {

        try {
            addedFiles = Files.readAllLines(ADDED_FILES_PATH);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static void systemError(Exception e) {
        System.out.print(SYSTEM_ERROR_STRING);
        System.out.print(Arrays.toString(e.getStackTrace()));
        System.exit(SYSTEM_ERROR_CODE);
    }

    // creates directory for new version and updates latest version
    public static void createNewVersionDir(long version) {

        // updates latest version
        try {
            writeToFile(LATEST_VERSION_PATH, Long.toString(version), false);
        } catch (Exception e) {
            systemError(e);
        }

        try {
            Files.createDirectory(Paths.get(FVT_PATH + "/" + version));
            Files.createFile(Paths.get(FVT_PATH + "/" + version + "/message.txt"));
            Files.copy(ADDED_FILES_PATH, Paths.get(FVT_PATH + "/" + version + "/addedFiles.txt"));


            try {
                List<String> addedFilesInGivenVersion = Files.readAllLines(Paths.get(FVT_PATH +"/"+version+"/addedFiles.txt"));

                if(addedFilesInGivenVersion.size() > 0) {
                    for (String file : addedFilesInGivenVersion) {
                        if (Files.exists(Paths.get(file))) {
                            Path src = Paths.get(file);
                            Path dst = Paths.get(FVT_PATH + "/" + version + "/" + file);
                            Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }

            } catch (Exception e) {
                systemError(e);
            }

        } catch(Exception e){
            systemError(e);
        }
    }

    public static long lastVersion() {

        long result = -1;
        try {
            result = Long.parseLong(Files.readString(LATEST_VERSION_PATH));
        } catch(Exception e) {
            systemError(e);
        }

        return result;
    }

    public static void writeToFile(Path path, String text, boolean append) {
        try {
            if(append)
                Files.write(path, text.getBytes(), StandardOpenOption.APPEND);
            else
                Files.write(path, text.getBytes());
        } catch(Exception e) {
            systemError(e);
        }
    }

    public static String additionalMessage(String... args) {
        if(args.length > 3 && args[2].equals("-m")) {
            return args[3];
        } else
            return "";
    }

    public static void addMessage(String text, String... args) {
        try {
            writeToFile(Paths.get(FVT_PATH + "/" + lastVersion() + "/message.txt"),(text + "\n" + additionalMessage(args)).trim(), false);
        } catch (Exception e) {
            systemError(e);
        }
    }
}
