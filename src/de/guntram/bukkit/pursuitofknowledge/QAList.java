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

    List<QA> entries;
    
    QAList(File file) {
        entries=new ArrayList<QA>();       
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
                    qa.answer=line;
                    qa.wasUsed=false;
                    entries.add(qa);
                    expectAnswer=false;
                } else {
                    qa=new QA();
                    qa.question=line;
                    expectAnswer=true;
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(QAList.class.getName()).log(Level.SEVERE, "Error reading "+file.getAbsolutePath(), ex);
        } finally {
            try {
                if (reader!=null)
                    reader.close();
            } catch (IOException ex) {
                Logger.getLogger(QAList.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
