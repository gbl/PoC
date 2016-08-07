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
public class Asker implements Runnable {
    
    private final PoK plugin;
    
    Asker(PoK plugin) {
        this.plugin=plugin;
    }

    @Override
    public void run() {
        // This should not happen as this is handled in SolutionGiver.
        // But to make sure no problems happen with empty QAlists, handle
        // it here as well.
        if (!plugin.qaList.hasMoreQuestions()) {
            plugin.nextGameMode();
            return;
        }
        QA qa=plugin.qaList.nextQA();
        if (qa==null) {
            Bukkit.broadcastMessage("nextQA returned null??");
        } else {
            // Bukkit.broadcast(qa.question, "poc.answer");
            Bukkit.broadcastMessage(plugin.getPrefix()+qa.question);
        }
        plugin.scheduleNextSolutionGiver();
    }
}
