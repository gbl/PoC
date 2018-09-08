/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.bukkit.pursuitofknowledge;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author gbl
 */
public class Prize {
    private int probability;
    private List<ItemStack> items;
    private static Pattern pattern;
    // List<ItemStack> components;

    Prize(String prizeDescriptor, Logger logger) {
        int pos;
        
        if (pattern==null) {
            pattern=Pattern.compile("(\\d+)(:(\\d+))?(\\((\\d+)\\))?");
        }
        String[] parts=prizeDescriptor.split(" ");
        items=new ArrayList<>(parts.length-1);
        try {
            probability=Integer.parseInt(parts[0]);
        } catch (NumberFormatException ex) {
            probability=1;
        }
        for (int i=1; i<parts.length; i++) {
            Matcher matcher=pattern.matcher(parts[i]);
            if (matcher.matches()) {
                String type=matcher.group(1);
                String subtype=matcher.group(3);
                String amount=matcher.group(5);
                
                Material material;
                try {
                    material=Material.getMaterial(type.toUpperCase());
                } catch (IllegalArgumentException ex) {
                    material=null;
                }
                if (material==null) {
                    logger.log(Level.WARNING, "Material {0} not recognized", type);
                    continue;
                }
                
                int damage=0;
                if (subtype!=null) try {
                    damage=Integer.parseInt(subtype);
                } catch (NumberFormatException ex) {
                }
                
                int quantity=1;
                if (amount!=null) try {
                    quantity=Integer.parseInt(amount);
                } catch (NumberFormatException ex) {
                }
                
                logger.log(Level.FINE, "creating stack from {0} subtype {1} amount {2}", new Object[]{material.name(), damage, quantity});
                ItemStack stack=new ItemStack(material, quantity, (short) damage);
                items.add(stack);
            }
        }
    }
    
    public int getProbability() {
        return probability;
    }
    
    public List<ItemStack> getItems() {
        return items;
    }
}
