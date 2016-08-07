package de.guntram.bukkit.pursuitofknowledge;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

class QA {
    String question;
    String answer;
    boolean wasUsed;
}

public class QAList {

    String inputFileName;
    List<QA> entries;
    int currentQuestion;
    final Logger logger;
    
    public QAList(File file, Logger logger) {
        this.logger=logger;
        inputFileName=file.getName();
        entries=new ArrayList<QA>();
        currentQuestion=-1;
        QA qa=null;
        BufferedReader reader=null;
        try {
            reader=new BufferedReader(new FileReader(file));
            boolean expectAnswer=false;
            String line;
            while ((line=reader.readLine())!=null) {
                if (line.isEmpty() || line.charAt(0)=='#')
                    continue;
                if (expectAnswer) {
                    logger.fine("Saving answer "+line);
                    qa.answer=line;
                    qa.wasUsed=false;
                    entries.add(qa);
                    expectAnswer=false;
                } else {
                    logger.fine("got question "+line);
                    qa=new QA();
                    qa.question=line;
                    expectAnswer=true;
                }
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Error reading "+file.getAbsolutePath(), ex);
        } finally {
            try {
                if (reader!=null)
                    reader.close();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
        logger.info("Have "+entries.size()+" QA entries");
    }
    
    public void randomize() {
        List<QA> newEntries=new ArrayList<QA>(entries.size());
        while (entries.size()>0) {
            int index=(int)(Math.random()*entries.size());
            newEntries.add(entries.get(index));
            entries.remove(index);
        }
        entries=newEntries;
    }
    
    public QA nextQA() {
        logger.info("nextQA: currentquestion="+currentQuestion+" and have "+entries.size()+"entries");
        currentQuestion++;
        if (currentQuestion>=entries.size())
            return null;
        entries.get(currentQuestion).wasUsed=true;
        return entries.get(currentQuestion);
    }
    
    public QA currentQA() {
        return entries.get(currentQuestion);
    }
    
    public boolean hasMoreQuestions() {
        return currentQuestion < entries.size()-1;
    }
}
