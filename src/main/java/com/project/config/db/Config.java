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
    // creating a structured DB folder.
    private static final String ROOT = "b4j-data/";

    @Getter
    private final String dbName;
    private final String filePath;

    private Config(String dbName) {
        this.dbName = dbName;
        filePath = ROOT + dbName + "/";
    }

    // initializing
    public static Config init() {
        // Read all dbs and prompt user to choose one or create one
        File root = new File(ROOT);
        if (!root.exists()) {
            root.mkdirs();
        }

        // get existing dbs
        List<String> dbs = getExistingDatabases(root);

        System.out.println("---------- Choose or Create a new DB ----------");
        for (int i = 0; i < dbs.size(); i++) {
            System.out.println((i+1) + ". " + dbs.get(i));
        }
        System.out.println((dbs.size()+1) + ". Create a new DB");
        System.out.println("Select Option: ");

        // Scanner class to take input
        Scanner scanner = new Scanner(System.in);
        int choice = scanner.nextInt();
        scanner.nextLine();

        // create a DB
        if (choice == dbs.size()+1) {
            System.out.print("Enter a DB name: ");
            String dbname = scanner.nextLine().trim();

            Config config = new Config(dbname);
            config.createDirectories();

            System.out.println("DB " + dbname + " created!");
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
