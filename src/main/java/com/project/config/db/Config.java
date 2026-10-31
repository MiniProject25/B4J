package com.project.config.db;

import lombok.Data;
import lombok.Getter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@Data
@Getter
public class Config {
    // structured DB folder
    private static final String ROOT = "b4j-data";

    @Getter
    private final String dbName;
    private final String filePath;

    public Config(String dbName) {
        this.dbName = dbName;
        this.filePath = ROOT + "/" + dbName;
    }

    public static Config init() {
        return init(new String[0]);
    }

    public static Config init(String[] args) {
        File root = new File(ROOT);
        if (!root.exists()) {
            root.mkdirs();
        }

        // If a DB name is passed via command line arguments, use it directly
        if (args != null && args.length > 0 && !args[0].trim().isEmpty()) {
            String dbname = args[0].trim();
            Config config = new Config(dbname);
            config.createDirectories();
            System.out.println("Connected to database: '" + dbname + "'");
            return config;
        }

        // Otherwise prompt interactively
        List<String> dbs = getExistingDatabases(root);

        System.out.println("---------- Choose or Create a DB ----------");
        for (int i = 0; i < dbs.size(); i++) {
            System.out.println((i + 1) + ". " + dbs.get(i));
        }
        System.out.println((dbs.size() + 1) + ". Create a new DB");
        System.out.print("Select Option: ");

        Scanner scanner = new Scanner(System.in);
        int choice;

        while (true) {
            if (!scanner.hasNextLine()) {
                // If standard input closed or piped without choice, default to 'default' db
                Config config = new Config("default");
                config.createDirectories();
                return config;
            }
            String line = scanner.nextLine().trim();
            try {
                choice = Integer.parseInt(line);
                if (choice >= 1 && choice <= dbs.size() + 1) {
                    break;
                }
                System.out.print("Please enter a number between 1 and " + (dbs.size() + 1) + ": ");
            } catch (NumberFormatException e) {
                System.out.print("Invalid input. Please enter a number: ");
            }
        }

        if (choice == dbs.size() + 1) {
            System.out.print("Enter a DB name: ");
            String dbname = "";
            while (dbname.isEmpty()) {
                if (!scanner.hasNextLine()) {
                    dbname = "default";
                    break;
                }
                dbname = scanner.nextLine().trim();
                if (dbname.isEmpty()) {
                    System.out.print("DB name cannot be empty. Enter a DB name: ");
                }
            }

            Config config = new Config(dbname);
            config.createDirectories();
            System.out.println("DB '" + dbname + "' created!");
            return config;
        }

        String existing = dbs.get(choice - 1);
        System.out.println("Connected to '" + existing + "'!");
        return new Config(existing);
    }

    public static List<String> getExistingDatabases(File root) {
        List<String> dbs = new ArrayList<>();
        File[] dirs = root.listFiles(File::isDirectory);
        if (dirs != null) {
            for (File file : dirs) {
                dbs.add(file.getName());
            }
        }
        return dbs;
    }

    public void createDirectories() {
        File file = new File(filePath);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    public String nodesPath() {
        return filePath + "/nodes.chk";
    }

    public String edgesPath() {
        return filePath + "/edges.chk";
    }

    public String walPath() {
        return filePath + "/wal.chk";
    }
}
