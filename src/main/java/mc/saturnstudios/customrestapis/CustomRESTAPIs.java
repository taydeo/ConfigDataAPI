package mc.saturnstudios.customrestapis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

import static spark.Spark.*;

public final class CustomRESTAPIs extends JavaPlugin implements Listener {
    Logger LOG = Bukkit.getLogger();
    boolean debug; // TODO incorporate debugging sys outputs
    Hashtable<String, String> localFileKeysAndPath = new Hashtable<>();
    String serviceName, pathToDir;
    FileConfiguration config = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "config.yml"));
    Plugin plugin = this;
    String LOG_PREFIX = ChatColor.BLUE + "[CustomAPIs] ";

    @Override
    public void onEnable() {
        createFileIfDoesntExist();

        LOG.info(LOG_PREFIX + ChatColor.YELLOW + "Loading.");

        if (config.getInt("configVersion") == 1) {
            LOG.info(LOG_PREFIX + "Config file has loaded");
        } else LOG.severe(LOG_PREFIX + ChatColor.RED + "didnt load lol");

        for (String localFile : Objects.requireNonNull(config.getConfigurationSection("filesToSend")).getKeys(false)) {

            serviceName = config.getString(localFile + ".statName");
            pathToDir = config.getString(localFile + ".pathToDir");

            System.out.println(serviceName + " " + pathToDir);

            localFileKeysAndPath.put(serviceName, pathToDir);
        } LOG.info(LOG_PREFIX + ChatColor.GREEN + "Files in the config are loaded.");
        startInternalServer();
    }

    public void createFileIfDoesntExist() {
        String CONFIG_NAME = "config.yml";
        File fileConfig = new File(getDataFolder(), CONFIG_NAME);
        if (!fileConfig.exists()) {
            saveResource(CONFIG_NAME, false);
            LOG.info(LOG_PREFIX + ChatColor.YELLOW + "Created new config file.");
        } else LOG.info(LOG_PREFIX + "Found config file - " + fileConfig);
    }

    public void startInternalServer() {
        ipAddress(config.getString("Server Options.ipAddress"));
        port(config.getInt("Server Options.portNumber"));
        init(); // sets the parameters for the server, then starts it.

        get("/datatofetch/:serviceName", ((request, response) -> {
            String UUID = request.queryParams("UUID");
            String filePathToCheck;

            if (UUID == null) {
                filePathToCheck = localFileKeysAndPath.get(request.params(":serviceName"));
            } else {
                filePathToCheck = localFileKeysAndPath.get(request.params(":serviceName") + UUID) + ".json";
            } // setups the checking process

            if (isJSONValid(filePathToCheck)) {
                return new File(filePathToCheck);
            } else {
                String convertedToJSON = convertYamlToJson(filePathToCheck); // always assumes that if the file isnt a
                assert convertedToJSON != null;                              // JSON file, its a YAML file
                return new File(convertedToJSON);
            }
        }));
        LOG.info(LOG_PREFIX + ChatColor.GREEN + "The server is live and ready to receive requests!");
    }

    private static String convertYamlToJson(String yaml) {
        try {
            ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
            Object obj = yamlReader.readValue(yaml, Object.class);
            ObjectMapper jsonWriter = new ObjectMapper();

            return jsonWriter.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException ex) {
            ex.printStackTrace();
            System.out.println("Make sure the file you are trying to request is a VALID json file.");
        }
        return null;
    }

    public static boolean isJSONValid(String jsonInString) {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.readTree(jsonInString);
            return true;
        } catch (Exception e) {
            System.out.println("Could not find the file " + jsonInString);
            return false;
        }
    }

    @Override
    public void onDisable() {
        LOG.info(LOG_PREFIX + ChatColor.RED + "Disabled.");
    }
}