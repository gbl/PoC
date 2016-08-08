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
    String filePattern;
    String prefix;
    String nextGameMode;
    int threshold;
    String prizeList;
    int numWinners;
    String randomize;
    String startMessage;
    String endMessage;
    String itemMessage;
    String expireMessage;
    
    String copyFrom;
    
    private boolean isCopying;
    private boolean copyFromResolved;
    String disabledReason;
    
    GameMode(String name, Map config, Logger logger) {
        this.name           = name;
        delay               = cfInt(   config.get("delay"));
        answerTime          = cfInt(   config.get("answertime"));
        answerCount         = cfInt(   config.get("answercount"));
        filePattern         = cfString(config.get("filepattern"));
        prefix              = cfString(config.get("prefix"), true);
        nextGameMode        = cfString(config.get("nextgamemode"));
        threshold           = cfInt(   config.get("threshold"));
        prizeList           = cfString(config.get("prizelist"));
        numWinners          = cfInt(   config.get("numWinners"));
        randomize           = cfString(config.get("randomize"));
        startMessage        = cfString(config.get("startmessage"), true);
        endMessage          = cfString(config.get("endmessage"), true);
        itemMessage         = cfString(config.get("itemmessage"), true);
        expireMessage       = cfString(config.get("expiremessage"), true);
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
        if (filePattern==null)      filePattern     = template.filePattern;
        if (prefix==null)           prefix          = template.prefix;
        if (nextGameMode==null)     nextGameMode    = template.nextGameMode;
        if (threshold==0)           threshold       = template.threshold;
        if (prizeList==null)        prizeList       = template.prizeList;
        if (numWinners==0)          numWinners      = template.numWinners;
        if (randomize==null)        randomize       = template.randomize;
        if (startMessage==null)     startMessage    = template.startMessage;
        if (endMessage==null)       endMessage      = template.endMessage;
        if (itemMessage==null)      itemMessage     = template.itemMessage;
        if (expireMessage==null)    expireMessage   = template.expireMessage;
        
        
        isCopying=false;
        copyFromResolved=true;
    }
    
    void sanitize(Map <String, PrizeList> prizes) {
        if (delay==0)               delay           = 1200;
        if (answerTime==0)          answerTime      = 60;
        if (answerCount==0)         answerCount     = 1;
        if (prefix==null)           prefix          = "";
        if (nextGameMode==null)     nextGameMode    = name;
        if (threshold==0)           threshold       = 1;
        if (numWinners==0)          numWinners      = 1;
        if (randomize==null)        randomize       = "yes";
        if (startMessage==null)     startMessage    = "";
        if (endMessage==null)       endMessage      = "";
        if (itemMessage==null)      itemMessage     = "";
        if (expireMessage==null)    expireMessage   = "";

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
                "startmessage: "    +startMessage       +", "+
                "endmessage: "      +endMessage         +", "+
                "itemmessage: "     +itemMessage        +", "+
                "expiremessage: "   +expireMessage      +".";
    }
}
