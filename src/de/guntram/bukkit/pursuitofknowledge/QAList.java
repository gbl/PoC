package de.guntram.bukkit.pursuitofknowledge;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class QA {
    private String question;
    private String answer;
    private boolean used;
    
    QA(String q) {
        question=q;
        answer="unknown";
        used=false;
    }
    QA(String q, String a) {
        question=q;
        answer=a;
        used=false;
    }
    
    void setQuestion(String s) { question=s; }
    void setAnswer(String s)   { answer=s;   }
    void setUsed()             { used=true; }
    
    String getQuestion() { return question; }
    String getAnswer()   { return answer; }
    boolean wasUsed()    { return used; }
    
    String getShowableAnswer() {
        int pos;
        if ((pos=answer.indexOf('|'))>0) {
            return answer.substring(0, pos);
        } else {
            return answer;
        }
    }
}

/**
 *
 * @author gbl
 */
public class QAList {

    private String inputFileName;
    private List<QA> entries;
    private int currentQuestion;
    private final Logger logger;
    private Pattern curPattern;
    
    /**
     *
     * @param file
     * @param logger
     */
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
                    qa.setAnswer(line);
                    entries.add(qa);
                    expectAnswer=false;
                } else {
                    logger.fine("got question "+line);
                    qa=new QA(line);
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
    
    /**
     *
     */
    public void randomize() {
        List<QA> newEntries=new ArrayList<QA>(entries.size());
        while (entries.size()>0) {
            int index=(int)(Math.random()*entries.size());
            newEntries.add(entries.get(index));
            entries.remove(index);
        }
        entries=newEntries;
    }
    
    /**
     *
     * @return
     */
    public QA nextQA() {
        curPattern=null;
        logger.info("nextQA: currentquestion="+currentQuestion+" and have "+entries.size()+"entries");
        currentQuestion++;
        if (currentQuestion>=entries.size()) {
            curPattern=null;
            return null;
        }
        entries.get(currentQuestion).setUsed();
        curPattern=Pattern.compile(entries.get(currentQuestion).getAnswer());
        return entries.get(currentQuestion);
    }
    
    /**
     *
     * @return
     */
    public QA currentQA() {
        return entries.get(currentQuestion);
    }
    
    /**
     *
     * @param answer
     * @return
     */
    public boolean checkAnswer(String answer) {
        if (curPattern==null)
            return false;
        Matcher m=curPattern.matcher(answer);
        // do not use matches() here, we want to match a part only, not the whole input.
        return m.find();
    }
    
    /**
     *
     * @return
     */
    public boolean hasMoreQuestions() {
        return currentQuestion < entries.size()-1;
    }
}
