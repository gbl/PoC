/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.bukkit.pursuitofknowledge;

import org.bukkit.Bukkit;

/**
 *
 * @author gbl
 */
public class SolutionGiver implements Runnable {

    private final PoK plugin;
    
    SolutionGiver(PoK plugin) {
        this.plugin=plugin;
    }

    @Override
    public void run() {
        QA qa=plugin.qaList.currentQA();
        // Bukkit.broadcast(qa.answer, "poc.answer");
        Bukkit.broadcastMessage(plugin.getPrefix()+qa.answer);
        if (!plugin.qaList.hasMoreQuestions()) {
            plugin.nextGameMode();
        } else {
            plugin.scheduleNextAsker();
        }
    }
}
