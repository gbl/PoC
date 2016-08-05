package de.guntram.bukkit.pursuitofknowledge;

import com.esotericsoftware.wildcard.Paths;
import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class PoC extends JavaPlugin implements Listener {
    
    Map<String, GameMode> gameModes;
    Map<String, PrizeList> prizeLists;
    QAList currentList;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        loadPrizes();
        loadGameModes();
        startGameMode("default");
    }

    private void loadPrizes() {
        ConfigurationSection prizeListsSection=this.getConfig().getConfigurationSection("prizelists");
        Set<String> prizeListNames=prizeListsSection.getKeys(false);
        for (String listName:prizeListNames) {
            ConfigurationSection prizeListSection=prizeListsSection.getConfigurationSection(listName);
            PrizeList toAdd=new PrizeList(listName, prizeListSection);
            prizeLists.put(listName, toAdd);
        }
    }
    
    
    private void loadGameModes() {
        ConfigurationSection gameModesSection=this.getConfig().getConfigurationSection("gamemodes");
        Set<String> gameModeNames=gameModesSection.getKeys(false);
        for (String modeName:gameModeNames) {
            ConfigurationSection modeConfigSection=gameModesSection.getConfigurationSection(modeName);
            GameMode toAdd=new GameMode(modeConfigSection, modeName);
            gameModes.put(modeName, toAdd);
        }
        Collection<GameMode> vals=gameModes.values();
        for (GameMode gameMode:vals) {
            gameMode.resolveCopyFrom(gameModes.get(gameMode.copyFrom), gameModes);
        }
        for (GameMode gameMode:vals) {
            gameMode.sanitize(prizeLists);
        }
        for (GameMode gameMode:vals) {
            if (gameMode.isDisabled())
                this.getLogger().log(Level.WARNING, "{0} disabled: {1}", new Object[]{gameMode.name, gameMode.disabledReason});
        }
    }
    
    private void startGameMode(String modeName) {
        GameMode mode=gameModes.get(modeName);
        if (mode==null || mode.isDisabled()) {
            this.getLogger().log(Level.WARNING, "cannot enter mode {0}", modeName);
            return;
        }
        File inputFile=findMatchingFile(mode.filePattern);
        currentList=new QAList(inputFile);
        Bukkit.broadcast(mode.startMessage, "poc.answer."+modeName);
        
    }

    private File findMatchingFile(String filePattern) {
        File pluginDir=getDataFolder();
        Paths paths=new Paths(getDataFolder().getAbsolutePath(), filePattern);
        File match=paths.getFiles().get((int) (Math.random()*paths.count()));
        return match;
    }
}
