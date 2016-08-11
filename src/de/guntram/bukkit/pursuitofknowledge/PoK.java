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
import java.util.HashSet;
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

class Pair {
    String part1;
    String part2;
    
    Pair(String s1, String s2) {
        part1=s1;
        part2=s2;
    }
}

public class PoK extends JavaPlugin implements Listener {
    
    private Map<String, GameMode> gameModes;
    private GameMode currentMode;
    private Map<String, PrizeList> prizeLists;
    private QAList qaList;
    private FileConfiguration config;
    private Logger logger;
    private int scheduledTask;
    private final int ticksPerMinute=20;    // correct: 20
    private boolean questionValid;
    private Scoreboard scores;
    private int numAnswers;                 // how many answers for the current question?
    private int numAsked;                   // how many questions where asked in the current scoreboard?
    private Set<String> correctAnswers;
    private List<Pair> wrongAnswers;

    @Override
    public void onEnable() {
        scheduledTask=-1;
        questionValid=false;

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
            
            if (args.length==1 && args[0].equalsIgnoreCase("stats")) {
                giveStats(sender);
                return true;
            }
            
            if (args.length==1 && (
                    args[0].equalsIgnoreCase("answer") || args[0].equalsIgnoreCase("lq"))
            ) {
                if (sender.hasPermission("pok.seeanswer")) {
                    QA qa=qaList.currentQA();
                    if (qa!=null) {
                        String a, sa;
                        sender.sendMessage("Last question was: "+qa.getQuestion());
                        sender.sendMessage("The answer is: "+(sa=qa.getShowableAnswer()));
                        if (!sa.equals(a=qa.getAnswer())) {
                            sender.sendMessage("Full answer expression is "+a);
                        }
                        for (Pair p: wrongAnswers) {
                            sender.sendMessage(p.part1+" wrongly answered '"+p.part2+"'");
                        }
                    } else {
                        sender.sendMessage("There is no current question");
                    }
                } else {
                    sender.sendMessage("You're lacking the pok.seeanswer permission");
                }
                return true;
            }
        }
        if (commandName.equalsIgnoreCase("ans")) {
            handleAnswer(sender, args);
            return true;
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
        if (currentMode.wantsRandomize())
            qaList.randomize();
        //Bukkit.broadcast(getPrefix()+currentMode.startMessage, "poc.answer."+modeName);
        Bukkit.broadcastMessage(getPrefix()+currentMode.startMessage);
        
        numAsked=0;
        scores=new Scoreboard();
        scheduleNextAsker();
        return new ReturnCode(true, "");
    }
    
    public ReturnCode nextGameMode() {
        return startGameMode(currentMode.nextGameMode);
    }
    
    public void scheduleNextAsker() {
        scheduledTask = getServer().getScheduler().scheduleSyncDelayedTask(this, new Asker(this), currentMode.delay*ticksPerMinute);
    }
    
    public void scheduleNextAnswerGiver() {
        scheduledTask=getServer().getScheduler().scheduleSyncDelayedTask(this, new Solver(this), currentMode.answerTime*ticksPerMinute);
    }
    
    public void ask(String s) {
        Bukkit.broadcastMessage(getPrefix()+currentMode.questionPrefix+s);
        questionValid=true;
        numAnswers=0;
        numAsked++;
        correctAnswers=new HashSet();
        wrongAnswers=new ArrayList();
    }
    
    public void solve(String s) {
        if (numAnswers==0) {
            Bukkit.broadcastMessage(getPrefix()+currentMode.expireMessage);
        } else {
            Bukkit.broadcastMessage(getPrefix()+currentMode.itemMessage);
        }
        questionValid=false;
        if (numAsked>=currentMode.threshold) {
            distributePrizes();
        }
    }
    
    public void distributePrizes() {
        if (scores.getEntries()>0) {
            String[] winners=scores.bestPlayers(currentMode.numWinners);
            reward(winners);
        } else {
            Bukkit.broadcastMessage(getPrefix()+"Noone won anything this round");
        }
        numAsked=0;
        scores=new Scoreboard();
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
    
    public QAList getQAList() {
        return qaList;
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
        // TODO: search jar for *.txt and copy all of them
        copySampleFile("default1.txt");
        copySampleFile("event-halloween.txt");
        copySampleFile("test.txt");
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
    
    private void reward(String[] players) {
        StringBuilder message=new StringBuilder(""+
                players.length+" winners: ");
        for (int i=0; i<players.length; i++) {
            message.append(players[i]);
            if (i==players.length-2)
                message.append(" and ");
            else if (i<players.length-2)
                message.append(", ");
        }
        if (players.length==1)
            message.append(" gets a "+currentMode.prizeList+" prize");
        else
            message.append(" get a "+currentMode.prizeList+" prize each");
        Bukkit.broadcastMessage(getPrefix()+message);
    }
    
    private void giveStats(CommandSender sender) {
        sender.sendMessage("Running mode "+currentMode.name);
        sender.sendMessage("Prize "+currentMode.prizeList+" given after "+currentMode.threshold+" questions, current: "+numAsked);
        sender.sendMessage("A maximum of "+currentMode.answerCount+" player(s) get a score point, current: "+numAnswers);
        sender.sendMessage(""+currentMode.numWinners+" player(s) will win a prize");
        sender.sendMessage("current scores: "+scores.toString());
    }
    
    // TODO: prevent a player from answering twice to the same question
    // maybe: allow them to correct themselves (gamemode option?)
    // but never allow more than 1 correct answer
    private void handleAnswer(CommandSender sender, String[] args) {
        String name=sender.getName();
        if (correctAnswers==null) {
            sender.sendMessage("There is no question pending!");
            return;
        }
        if (correctAnswers.contains(name)) {
            sender.sendMessage("You already answered this question.");
            return;
        }
        StringBuilder tempAnswer=new StringBuilder();
        for (int i=0; i<args.length; i++) {
            tempAnswer.append(args[i]);
            if (i!=args.length-1)
                tempAnswer.append(' ');
        }
        String answer=tempAnswer.toString();
        String message;
        message=getPrefix()+"'"+answer+"' is ";
        boolean correct=qaList.checkAnswer(answer);
        if (correct) {
            correctAnswers.add(name);
            message+="correct. ";
            if (questionValid && numAnswers<currentMode.answerCount) {
                message+=" You get a score point.";
                numAnswers++;
                scores.awardPointTo(name);
            } else if (questionValid) {
                message+=" But unfortunately, too many players were faster than you.";
            } else {
                message+=" But unfortunately, too late.";
            }
        } else {
            if (!currentMode.allowRetry())
                correctAnswers.add(name);
            wrongAnswers.add(new Pair(name, answer));
            message+="wrong. Sorry.";
            if (!questionValid) {
                message=message+" And too late as well.";
            }
        }
        sender.sendMessage(message);
    }
}
