/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.bukkit.pursuitofknowledge;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;

/**
 *
 * @author gbl
 */
public class PrizeList {
    String name;
    Vector<Prize> alternativePrizes;
    int probabilitySum;
    
    private void init(String name) {
        this.name=name;
        alternativePrizes=new Vector();
        probabilitySum=0;
    }
    
    PrizeList(String name) {
        this.init(name);
    }

    PrizeList(String name, List<Map> entries, Logger logger) {
        this.init(name);
        
        for (Map entry:entries) {
            for (Object prizeDescriptor:entry.values()) {
                logger.log(Level.WARNING, "adding prize: {0}", new Object[]{prizeDescriptor});
                Prize prize=new Prize((String) prizeDescriptor);
                this.add(prize);
            }
        }
    }
    
    void add(Prize prize) {
        alternativePrizes.add(prize);
        probabilitySum+=prize.probability;
    }
    
    Prize getRandomPrize() {
        if (probabilitySum==0)
            return null;
        int selection=(int) (Math.random()*probabilitySum);
        for (Prize prize:alternativePrizes) {
            if (selection<prize.probability)
                return prize;
            selection-=prize.probability;
        }
        throw new UnsupportedOperationException("getRandomPrize failure");
    }
}
