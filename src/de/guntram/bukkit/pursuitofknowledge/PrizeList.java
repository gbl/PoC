/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.bukkit.pursuitofknowledge;

import java.util.Set;
import java.util.Vector;
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

    PrizeList(String name, ConfigurationSection alternatives) {
        this.init(name);
        Set<String> prizeNames=alternatives.getKeys(false);
        for (String prizeName:prizeNames) {
            String prizeDescriptor=alternatives.getString(prizeName);
            Prize prize=new Prize(prizeDescriptor);
            this.add(prize);
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
