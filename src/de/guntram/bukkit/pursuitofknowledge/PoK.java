package de.guntram.bukkit.pursuitofknowledge;

import com.esotericsoftware.wildcard.Paths;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

class ReturnCode {
    boolean success;
    String message;
    
    ReturnCode(boolean s, String m) {
        success=s;
        message=m;
    }
}

public class PoK extends JavaPlugin implements Listener {
    
    Map<String, GameMode> gameModes;
    private GameMode currentMode;
    Map<String, PrizeList> prizeLists;
    QAList qaList;
    FileConfiguration config;
    Logger logger;
    private int scheduledTask;
    private final int ticksPerMinute=3;     // correct: 20

    @Override
    public void onEnable() {
        logger=getLogger();
        saveDefaultConfig();
        copySampleFiles();
        
        config=getConfig();
        Set<String> temp=config.getKeys(true);
        for (String s: temp) {
            logger.finer("config key: "+s+" issection? : "+config.isConfigurationSection(s));
        }

        loadPrizes();
        loadGameModes();
        scheduledTask=-1;
        startGameMode("default");
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String commandName=command.getName();
        if (commandName.equalsIgnoreCase("pok")) {
            if ((args.length==2 || args.length==3) && args[0].equalsIgnoreCase("mode")) {
               ReturnCode result=startGameMode(args[1], args.length==3 ? args[2] : null);
               if (!result.success) {
                   logger.warning(result.message);
                   sender.sendMessage(result.message);
               }
               return true;
            }
        }
        return false;
    }

    private void loadPrizes() {
        prizeLists=new HashMap();
        List<Map<?,?>> prizeMap=config.getMapList("prizelists");
        logger.log(Level.INFO, "prize map has {0} entries", prizeMap.size());
        
        for (Map<?,?> map:prizeMap) {
            logger.fine("parsing next prize map");
            for (Object listName:map.keySet()) {
                logger.fine("parsing prize: "+listName);
                logger.log(Level.FINE,"class is "+map.get(listName).getClass().getCanonicalName());
                logger.log(Level.FINE,"value is "+map.get(listName));
                ArrayList x=(ArrayList)map.get(listName);
                logger.log(Level.FINE,"element class is "+x.get(0).getClass().getCanonicalName());
                logger.log(Level.FINE,"element value is "+x.get(0));
                PrizeList toAdd=new PrizeList((String)listName, (List<Map>) map.get(listName), logger);
                prizeLists.put((String)listName, toAdd);
                logger.log(Level.INFO, "added prizelist {0} with {1} alternatives", new Object[]{listName, toAdd.alternativePrizes.size()});
            }
        }
    }
    
    private void loadGameModes() {
        gameModes=new HashMap();
        List<Map<?,?>> gameModeMap=config.getMapList("gamemodes");
        logger.log(Level.FINE, "game mode map has {0} entries", gameModeMap.size());
        for (Map<?,?> map:gameModeMap) {
            logger.fine("parsing game mode");
            for (Object gameModeName:map.keySet()) {
                logger.info("parsing gameMode "+gameModeName);
                logger.log(Level.FINE,"class is "+map.get(gameModeName).getClass().getCanonicalName());
                logger.log(Level.FINE,"value is "+map.get(gameModeName));
                GameMode toAdd=new GameMode((String)gameModeName, (Map)(map.get(gameModeName)), logger);
                gameModes.put((String) gameModeName, toAdd);
                logger.log(Level.INFO, "parsed gameMode: {0}", toAdd.toString());
            }
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
                logger.log(Level.WARNING, "{0} disabled: {1}", new Object[]{gameMode.name, gameMode.disabledReason});
        }
        for (GameMode gameMode:vals) {
            logger.log(Level.INFO, "finished gameMode: {0}", gameMode.toString());
        }
    }
    
    public ReturnCode startGameMode(String modeName) {
        return startGameMode(modeName, null);
    }
    
    public ReturnCode startGameMode(String modeName, String qaFileName) {
        cancelGame();
        currentMode=gameModes.get(modeName);
        if (currentMode==null || currentMode.isDisabled()) {
            return new ReturnCode(false, "cannot enter mode "+ modeName);
        }
        
        File inputFile;
        if (qaFileName==null) {
            inputFile=findMatchingFile(currentMode.filePattern);
            if (inputFile==null) {
                return new ReturnCode(false, "No files found matching "+ currentMode.filePattern);
            }
        } else {
            inputFile=new File(qaFileName);
        }

        if (!inputFile.exists()) {
            return new ReturnCode(false, "File "+inputFile.getName()+" not found");
        }
        logger.info("Using input file "+inputFile+" to start mode "+modeName);
        qaList=new QAList(inputFile, logger);
        qaList.randomize();
        //Bukkit.broadcast(getPrefix()+currentMode.startMessage, "poc.answer."+modeName);
        Bukkit.broadcastMessage(getPrefix()+currentMode.startMessage);
        
        scheduleNextAsker();
        return new ReturnCode(true, "");
    }
    
    public ReturnCode nextGameMode() {
        return startGameMode(currentMode.nextGameMode);
    }
    
    public void scheduleNextAsker() {
        scheduledTask = getServer().getScheduler().scheduleSyncDelayedTask(this, new Asker(this), currentMode.delay*ticksPerMinute);
    }
    
    public void scheduleNextSolutionGiver() {
        scheduledTask=getServer().getScheduler().scheduleSyncDelayedTask(this, new SolutionGiver(this), currentMode.answerTime*ticksPerMinute);
    }
    
    public void cancelGame() {
        if (scheduledTask!=-1) {
            getServer().getScheduler().cancelTask(scheduledTask);
        }
        scheduledTask=-1;
    }
    
    public String getPrefix() {
        return currentMode.prefix;
    }

    private File findMatchingFile(String filePattern) {
        File pluginDir=getDataFolder();
        Paths paths=new Paths(getDataFolder().getAbsolutePath(), filePattern);
        if (paths.count()==0) {
            return null;
        }
        File match=paths.getFiles().get((int) (Math.random()*paths.count()));
        return match;
    }
    
    private void copySampleFiles() {
        copySampleFile("default1.txt");
        copySampleFile("event-halloween.txt");
    }
    
    private void copySampleFile(String name) {
        File file;
        if ((file=new File(getDataFolder(), name)).exists())
            return;
        copy(getResource(name), file);
    }
    
    private void copy(InputStream in, File file) {
        byte[] buf = new byte[1024];
        int len;
        try (OutputStream out = new FileOutputStream(file)) {
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }      
}
