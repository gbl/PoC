/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.bukkit.pursuitofknowledge;

/**
 *
 * @author gbl
 */
public class Prize {
    int probability;
    String unparsedItemStackList;
    // List<ItemStack> components;

    Prize(String prizeDescriptor) {
        int pos;
        
        for (pos=0; pos<prizeDescriptor.length() 
                    && Character.isDigit(prizeDescriptor.charAt(pos)); pos++)
            ;
        probability=Integer.parseInt(prizeDescriptor.substring(0, pos));
        unparsedItemStackList=prizeDescriptor.substring(pos);
    }
}
