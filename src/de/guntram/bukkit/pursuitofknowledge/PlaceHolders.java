/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.bukkit.pursuitofknowledge;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author gbl
 */
public class PlaceHolders {
    static String evaluate(String template, PlaceHolderProvider provider, Logger logger) {
        int start, end;
        boolean wantPluralS=false;
        logger.log(Level.FINE, "evaluating {0}", template);
        
        if (template==null)
            return null;
        
        //logger.log(Level.INFO, "start: {0}", (start=template.indexOf('%')));
        //logger.log(Level.INFO, "end:   {0}", (end=template.indexOf('%', start+1)));
        while ((start=template.indexOf('%'))>=0
           &&  (end  =template.indexOf('%', start+1))>=0) {
            String key=template.substring(start+1, end);
            wantPluralS=false;
            if (key.endsWith(".s")) {
                key=key.substring(0, key.length()-2);
                wantPluralS=true;
            }
            String value=provider.valueFor(key);
            logger.log(Level.FINE, "''{0}'' results in ''{1}''", new Object[]{key, value});
            if (wantPluralS) {
                value=(value.equals("1") ? "" : "s");
            }
            template=template.substring(0, start) + value + template.substring(end+1);
        }
        return template;
    }
}
