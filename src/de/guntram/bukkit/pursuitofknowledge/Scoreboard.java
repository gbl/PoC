/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.bukkit.pursuitofknowledge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author gbl
 */
public class Scoreboard {
    private Map<String, Integer> data;
    private int entries;
    
    Scoreboard() {
        data=new HashMap();
        entries=0;
    }
    
    /**
     * Award a score point to a specific player.
     * @param playerName The player that gets the score point
     */
    public void awardPointTo(String playerName) {
        Integer current;
        if ((current=data.get(playerName))==null)
            data.put(playerName, 1);
        else
            data.put(playerName, current+1);
        entries++;
    }
    
    /**
     * Find out how many players with at least one score point there are
     * @return 
     * Number of players
     */
    public int getEntries() { return entries; }

    
 
    /**
     * Internal helper function to sort the player/score map by score
     * stolen from http://stackoverflow.com/questions/109383/
     * @param <K>
     * @param <V>
     * @param map the map to sort
     * @return
     * a sorted version of the map, containing the same key/value pairs
     */
        private static <K, V extends Comparable<? super V>> Map<K, V> 
        sortByValueDesc( Map<K, V> map )
    {
        List<Map.Entry<K, V>> list =
            new LinkedList<>( map.entrySet() );
        Collections.sort( list, new Comparator<Map.Entry<K, V>>()
        {
            @Override
            public int compare( Map.Entry<K, V> o1, Map.Entry<K, V> o2 )
            {
                return ( o2.getValue() ).compareTo( o1.getValue() );
            }
        } );

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list)
        {
            result.put( entry.getKey(), entry.getValue() );
        }
        return result;
    }

    /**
     * Find the best player(s) in the scoreboard, try to return the chosen
     * number of player names.
     * If there are too few players in the scoreboard, return those.
     * If some more players have the same amount of score points as the worst
     * one that was chosen, return them as well.
     * @param numWinners
     * The number of player names the caller wants to get.
     * @return
     * array of player names
     */
    public String[] bestPlayers(int numWinners) {
        if (data.isEmpty())
            return null;
        ArrayList<String> playerList=new ArrayList<>();
        Map<String, Integer> sorted=sortByValueDesc(data);
        int best=-1;
        int count=0;
        for (Map.Entry<String, Integer> entry: sorted.entrySet()) {
            if (count==numWinners)
                best=entry.getValue();
            if (count<numWinners || entry.getValue()==best)
                playerList.add(entry.getKey());
            else
                break;
        }
        return playerList.toArray(new String[playerList.size()]);
    }
    
    @Override
    public String toString() {
        StringBuilder temp=new StringBuilder();
        for (Map.Entry<String, Integer> entry: data.entrySet()) {
            temp.append(entry.getKey()).append(": ")
                .append(entry.getValue()).append("\n");
        }
        return temp.toString();
    }
}
