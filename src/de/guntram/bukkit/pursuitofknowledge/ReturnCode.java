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
public class ReturnCode {
    boolean success;
    String message;
    
    ReturnCode(boolean s, String m) {
        success=s;
        message=m;
    }
}
