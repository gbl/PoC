/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.bukkit.pursuitofknowledge;

import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;

/**
 *
 * @author gbl
 */
public class GameMode {
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
    String startMessage;
    String endMessage;
    String itemMessage;
    String expireMessage;
    
    String copyFrom;
    
    private boolean isCopying;
    private boolean copyFromResolved;
    String disabledReason;
    
    GameMode(ConfigurationSection section, String name) {
        this.name           = name;
        delay               = section.getInt   ("delay");
        answerTime          = section.getInt   ("answertime");
        answerCount         = section.getInt   ("answercount");
        filePattern         = section.getString("filepattern");
        prefix              = section.getString("prefix");
        nextGameMode        = section.getString("nextgamemode");
        threshold           = section.getInt   ("threshold");
        prizeList           = section.getString("prizelist");
        numWinners          = section.getInt   ("numWinners");
        startMessage        = section.getString("startmessage");
        endMessage          = section.getString("endmessage");
        itemMessage         = section.getString("itemmessage");
        expireMessage       = section.getString("expiremessage");
        copyFrom            = section.getString("copyfrom");
    }
    
    public boolean isDisabled() {
        return disabledReason!=null;
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
}
