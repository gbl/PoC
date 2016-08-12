/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.bukkit.pursuitofknowledge;

import java.util.Map;
import java.util.logging.Logger;

/**
 *
 * @author gbl
 */
public class GameMode {
    
    /**
     *
     */
    public static final int allThreshold=99999;
    
    String name;
    int delay;
    int answerTime;
    int answerCount;
    String allow2ndTry;
    String filePattern;
    String prefix;
    String nextGameMode;
    int threshold;
    String prizeList;
    int numWinners;
    String randomize;
    String scramble;
    String startMessage;
    String endMessage;
    String questionMessage;
    String itemMessage;
    String timeoutMessage;
    String rewardMessage;
    String noRewardMessage;
    
    String copyFrom;
    
    private boolean isCopying;
    private boolean copyFromResolved;
    String disabledReason;
    
    GameMode(String name, Map config, Logger logger) {
        this.name           = name;
        delay               = cfInt(   config.get("delay"));
        answerTime          = cfInt(   config.get("answertime"));
        answerCount         = cfInt(   config.get("answercount"));
        allow2ndTry         = cfString(config.get("allow2ndtry"));
        filePattern         = cfString(config.get("filepattern"));
        prefix              = cfString(config.get("prefix"), true);
        nextGameMode        = cfString(config.get("nextgamemode"));
        threshold           = cfInt(   config.get("threshold"));
        prizeList           = cfString(config.get("prizelist"));
        numWinners          = cfInt(   config.get("numWinners"));
        randomize           = cfString(config.get("randomize"));
        scramble            = cfString(config.get("scramble"));
        startMessage        = cfString(config.get("startmessage"), true);
        endMessage          = cfString(config.get("endmessage"), true);
        questionMessage     = cfString(config.get("questionmessage"), true);
        itemMessage         = cfString(config.get("itemmessage"), true);
        timeoutMessage      = cfString(config.get("timeoutmessage"), true);
        rewardMessage       = cfString(config.get("rewardmessage"), true);
        noRewardMessage     = cfString(config.get("norewardmessage"), true);
        copyFrom            = cfString(config.get("copyfrom"));
        
        if (threshold==0 && "all".equals(config.get("threshold")))
            threshold=allThreshold;
    }
    
    private int cfInt(Object s) {
        try {
            return Integer.parseInt(s.toString());
        } catch (Exception ex) {
            return 0;
        }
    }
    
    private String cfString(Object s) {
        return cfString(s, false);
    }

    private String cfString(Object s, boolean translateColors) {
        if (s==null)
            return null;
        String result=s.toString();
        if (translateColors) {
            result=result.replace('&', '§');
        }
        return result;
    }
    
    /**
     *
     * @return
     */
    public boolean isDisabled() {
        return disabledReason!=null;
    }
    
    public boolean wantsRandomize() {
        return isTruthy(randomize);
    }
    
    public boolean allowRetry() {
        return isTruthy(allow2ndTry);
    }
    
    public boolean isLineScramble() {
        return scramble.equalsIgnoreCase("line") || scramble.equalsIgnoreCase("row");
    }
    
    public boolean isAllScramble() {
        return scramble.equalsIgnoreCase("all");
    }
    
    private boolean isTruthy(String s) {
        return
                !randomize.startsWith("n")
          &&    !randomize.startsWith("f")
          &&    !randomize.equals("0");
    }
    
    void resolveCopyFrom(GameMode template, Map<String, GameMode>gameModeList) {
        if (copyFrom==null
        ||  copyFromResolved) {
            return;
        }
        
        // set isCopying before testing template.isCopying to catch cases when
        // a mode tries to copy from itself
        isCopying=true;
        if (template.isCopying) {
            disabledReason="circular copy, detected on "+name+" copying in-copy "+template.name;
            isCopying=false;
            return;
        }
        if (template.isDisabled()) {
            disabledReason="trying to copy from disabled "+template.name;
            isCopying=false;
            return;
        }
        
        template.resolveCopyFrom(gameModeList.get(template.name), gameModeList);
        
        if (delay==0)               delay           = template.delay;
        if (answerTime==0)          answerTime      = template.answerTime;
        if (answerCount==0)         answerCount     = template.answerCount;
        if (allow2ndTry==null)      allow2ndTry     = template.allow2ndTry;
        if (filePattern==null)      filePattern     = template.filePattern;
        if (prefix==null)           prefix          = template.prefix;
        if (nextGameMode==null)     nextGameMode    = template.nextGameMode;
        if (threshold==0)           threshold       = template.threshold;
        if (prizeList==null)        prizeList       = template.prizeList;
        if (numWinners==0)          numWinners      = template.numWinners;
        if (randomize==null)        randomize       = template.randomize;
        if (scramble==null)         scramble        = template.scramble;
        if (startMessage==null)     startMessage    = template.startMessage;
        if (endMessage==null)       endMessage      = template.endMessage;
        if (questionMessage==null)  questionMessage = template.questionMessage;
        if (itemMessage==null)      itemMessage     = template.itemMessage;
        if (timeoutMessage==null)   timeoutMessage  = template.timeoutMessage;
        if (rewardMessage==null)    rewardMessage   = template.rewardMessage;
        if (noRewardMessage==null)  noRewardMessage = template.noRewardMessage;
        
        isCopying=false;
        copyFromResolved=true;
    }
    
    void sanitize(Map <String, PrizeList> prizes) {
        if (delay==0)               delay           = 1200;
        if (answerTime==0)          answerTime      = 60;
        if (answerCount==0)         answerCount     = 1;
        if (allow2ndTry==null)      allow2ndTry     = "yes";
        if (prefix==null)           prefix          = "";
        if (nextGameMode==null)     nextGameMode    = name;
        if (threshold==0)           threshold       = 1;
        if (numWinners==0)          numWinners      = 1;
        if (randomize==null)        randomize       = "yes";
        if (scramble==null)         scramble        = "none";
        if (startMessage==null)     startMessage    = "";
        if (endMessage==null)       endMessage      = "";
        if (questionMessage==null)  questionMessage = "%question%";
        if (itemMessage==null)      itemMessage     = "%answer%";
        if (timeoutMessage==null)   timeoutMessage  = "%answer%";
        if (rewardMessage==null)    rewardMessage   = "";
        if (noRewardMessage==null)  noRewardMessage = "";

        if (filePattern==null)      { disabledReason="no file pattern"; }
        if (prizeList==null)        { disabledReason="no prize list given"; }
        else if (prizes.get(prizeList)==null) {
                                    disabledReason="prize list "+prizeList+" nonexistent";
        }
    }
    
    @Override
    public String toString() {
        return "gamemode "          +name               +":"+
                "delay: "           +delay              +", "+
                "answertime: "      +answerTime         +", "+
                "answercount: "     +answerCount        +", "+
                "filepattern: "     +filePattern        +", "+
                "prefix: "          +prefix             +", "+
                "nextgamemode: "    +nextGameMode       +", "+
                "threshold: "       +threshold          +", "+
                "prizelist: "       +prizeList          +", "+
                "numwinners: "      +numWinners         +", "+
                "randomize: "       +randomize          +", "+
                "scramble: "        +scramble           +", "+
                "startmessage: "    +startMessage       +", "+
                "endmessage: "      +endMessage         +", "+
                "questionmessage: " +questionMessage    +", "+
                "itemmessage: "     +itemMessage        +", "+
                "timeoutmessage: "  +timeoutMessage     +", "+
                "rewardmessage: "   +rewardMessage      +", "+
                "norewardmessage: " +noRewardMessage    +".";
    }
}
