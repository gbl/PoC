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
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

class Pair {
    String part1;
    String part2;
    
    Pair(String s1, String s2) {
        part1=s1;
        part2=s2;
    }
}

public class PoK extends JavaPlugin implements Listener, PlaceHolderProvider {
    private Map<String, GameMode> gameModes;
    private GameMode currentMode;
    private Map<String, PrizeList> prizeLists;
    private QAList qaList;
    private FileConfiguration config;
    private Logger logger;
    private int scheduledTask;
    private final int ticksPerMinute=20;    // correct: 20
    private boolean questionValid;          // Can a question be answered rn, i.e. no timeout?
    private Scoreboard scores;
    private int numAnswers;                 // how many answers for the current question?
    private int numAsked;                   // how many questions where asked in the current scoreboard?
    private Set<String> correctAnswerers;
    private List<Pair> wrongAnswers;
    private String[] winners;
    private Economy economy;

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
            logger.log(Level.FINER, "config key: {0} issection? : {1}", new Object[]{s, config.isConfigurationSection(s)});
        }

        RegisteredServiceProvider<Economy> economyProvider;
        try {
            economyProvider=getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        } catch (NoClassDefFoundError err) {
            economyProvider=null;
        }
        if (economyProvider==null) {
            economy=null;
            logger.log(Level.WARNING, "Vault not found. Won't be able to give money to players.");
        } else {
            economy=economyProvider.getProvider();
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
        if (commandName.equalsIgnoreCase("answer")) {
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
                logger.log(Level.FINE, "parsing prize: {0}", listName);
                logger.log(Level.FINE, "class is {0}", map.get(listName).getClass().getCanonicalName());
                logger.log(Level.FINE, "value is {0}", map.get(listName));
                ArrayList x=(ArrayList)map.get(listName);
                logger.log(Level.FINE, "element class is {0}", x.get(0).getClass().getCanonicalName());
                logger.log(Level.FINE, "element value is {0}", x.get(0));
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
                logger.log(Level.FINE, "parsing gameMode {0}", gameModeName);
                logger.log(Level.FINE, "class is {0}", map.get(gameModeName).getClass().getCanonicalName());
                logger.log(Level.FINE, "value is {0}", map.get(gameModeName));
                GameMode toAdd=new GameMode((String)gameModeName, (Map)(map.get(gameModeName)), logger);
                gameModes.put((String) gameModeName, toAdd);
                logger.log(Level.FINE, "parsed gameMode: {0}", toAdd.toString());
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
            logger.log(Level.INFO, "Loaded GameMode: {0}", gameMode.toString());
        }
    }
    
    public ReturnCode startGameMode(String modeName) {
        return startGameMode(modeName, null);
    }
    
    public ReturnCode startGameMode(String modeName, String qaFileName) {
        cancelGame();
        currentMode=gameModes.get(modeName);
        if (currentMode==null) {
            return new ReturnCode(false, "cannot enter mode "+ modeName);
        }
        if (currentMode.isDisabled()) {
            return new ReturnCode(false, "Mode "+modeName+" is disabled: "+currentMode.disabledReason);
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
        logger.log(Level.INFO, "Using input file {0} to start mode {1}", new Object[]{inputFile, modeName});
        qaList=new QAList(inputFile, logger, currentMode);
        if (currentMode.wantsRandomize())
            qaList.randomize();
        broadcastToPlayers(currentMode.startMessage);
        
        questionValid=false;
        numAsked=0;
        scores=new Scoreboard();
        scheduleNextAsker();
        return new ReturnCode(true, "");
    }
    
    public ReturnCode nextGameMode() {
        broadcastToPlayers(currentMode.endMessage);
        return startGameMode(currentMode.nextGameMode);
    }
    
    public void scheduleNextAsker() {
        scheduledTask = getServer().getScheduler().scheduleSyncDelayedTask(this, new Asker(this), currentMode.delay*ticksPerMinute);
    }
    
    public void scheduleNextAnswerGiver() {
        scheduledTask=getServer().getScheduler().scheduleSyncDelayedTask(this, new Solver(this), currentMode.answerTime*ticksPerMinute);
    }
    
    public void ask() {
        broadcastToPlayers(currentMode.questionMessage);
        questionValid=true;
        numAnswers=0;
        numAsked++;
        correctAnswerers=new HashSet();
        wrongAnswers=new ArrayList();
    }
    
    public void solve() {
        String message;
        if (numAnswers==0) {
            message=PlaceHolders.evaluate(currentMode.timeoutMessage, this, logger);
        } else {
            message=PlaceHolders.evaluate(currentMode.itemMessage, this, logger);
        }
        broadcastToPlayers(message);
        questionValid=false;
        if (numAsked>=currentMode.threshold) {
            distributePrizes();
        }
    }
    
    public void distributePrizes() {
        if (numAsked>0) {   // This is needed as we are called at the end of
                            // one round AND at the end of the qa list
            String message;
            if (scores.getEntries()>0) {
                winners=scores.bestPlayers(currentMode.numWinners);
                PrizeList list=prizeLists.get(currentMode.prizeList);
                for (String winner:winners) {
                    Player player=Bukkit.getPlayer(winner);
                    Prize prize=list.getRandomPrize();
                    for (ItemStack stack:prize.getItems()) {
                        if (stack.getType() == Material.AIR) {
                            if (economy!=null) {
                                economy.depositPlayer(player, stack.getAmount());
                                player.sendMessage("You got paid "+stack.getAmount()+" money units.");
                            } else {
                                player.sendMessage("You would have gotten "+stack.getAmount()+" money units, but Vault isn't installed.");
                            }
                        } else {
                            // addItem may change stack.amount so save/restore it
                            int oldAmount=stack.getAmount();
                            player.getInventory().addItem(stack);
                            stack.setAmount(oldAmount);
                        }
                    }
                }
                message=PlaceHolders.evaluate(currentMode.rewardMessage, this, logger);
            } else {
                message=PlaceHolders.evaluate(currentMode.noRewardMessage, this, logger);
            }
            broadcastToPlayers(message);
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
        copySampleFile("scrambleall.txt");
        copySampleFile("scrambleline.txt");
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
    
    void broadcastToPlayers(String message) {
        if (message!=null && !message.equals("")) {
            message=PlaceHolders.evaluate(message, this, logger);
            Bukkit.broadcast(currentMode.prefix+message, "pok.answer");
        }
    }
    
    private String getPlayerList(String[] players) {
        StringBuilder playerList=new StringBuilder();
        for (int i=0; i<players.length; i++) {
            playerList.append(players[i]);
            if (i==players.length-2)
                playerList.append(" and ");
            else if (i<players.length-2)
                playerList.append(", ");
        }
        return playerList.toString();
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
        if (correctAnswerers==null) {
            sender.sendMessage("There is no question pending!");
            return;
        }
        if (correctAnswerers.contains(name)) {
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
            correctAnswerers.add(name);
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
                correctAnswerers.add(name);
            wrongAnswers.add(new Pair(name, answer));
            message+="wrong. Sorry.";
            if (!questionValid) {
                message=message+" And too late as well.";
            }
        }
        sender.sendMessage(message);
    }

    @Override
    public String valueFor(String placeholder) {
        if ("answer".equals(placeholder) && qaList.currentQA()!=null) {
            return qaList.currentQA().getShowableAnswer();
        }
        if ("answercount".equals(placeholder)) {
            return Integer.toString(currentMode.answerCount);
        }
        if ("answertime".equals(placeholder)) {
            return Integer.toString(currentMode.answerCount);
        }
        if ("delay".equals(placeholder)) {
            return Integer.toString(currentMode.delay);
        }
        if ("name".equals(placeholder)) {
            return currentMode.name;
        }
        if ("numwinners".equals(placeholder)) {
            return Integer.toString(currentMode.numWinners);
        }
        if ("playercount".equals(placeholder)) {
            return Integer.toString(correctAnswerers.size());
        }
        if ("players".equals(placeholder)) {
            return getPlayerList(correctAnswerers.toArray(new String[correctAnswerers.size()]));
        }
        if ("prizelist".equals(placeholder)) {
            return currentMode.prizeList;
        }
        if ("question".equals(placeholder) && qaList.currentQA()!=null) {
            return qaList.currentQA().getQuestion();
        }
        if ("threshold".equals(placeholder)) {
            if (currentMode.threshold==GameMode.allThreshold)
                return "all";
            return Integer.toString(currentMode.threshold);
        }
        if (placeholder!=null && placeholder.startsWith("threshold.")) {
            if (currentMode.threshold==GameMode.allThreshold)
                return placeholder.substring(10);
            return Integer.toString(currentMode.threshold);
        }
        if ("scoreboard".equals(placeholder)) {
            return scores.toString(currentMode.prefix);
        }
        if ("winnercount".equals(placeholder)) {
            return Integer.toString(winners.length);
        }
        if ("winners".equals(placeholder)) {
            return getPlayerList(winners);
        }
        return placeholder.toUpperCase();
    }
}
