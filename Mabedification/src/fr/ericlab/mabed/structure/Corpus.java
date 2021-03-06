////////////////////////////////////////////////////////////////////////////////
//  This file is part of MABED.                                               //
//                                                                            //
//  MABED is free software: you can redistribute it and/or modify             //
//  it under the terms of the GNU General Public License as published by      //
//  the Free Software Foundation, either version 3 of the License, or         //
//  (at your option) any later version.                                       //
//                                                                            //
//  MABED is distributed in the hope that it will be useful,                  //
//  but WITHOUT ANY WARRANTY; without even the implied warranty of            //
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             //
//  GNU General Public License for more details.                              //
//                                                                            //
//  You should have received a copy of the GNU General Public License         //
//  along with MABED.  If not, see <http://www.gnu.org/licenses/>.            //
////////////////////////////////////////////////////////////////////////////////
package fr.ericlab.mabed.structure;

import fr.ericlab.mabed.app.Configuration;
import fr.ericlab.util.Util;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import main.java.fr.ericlab.sondy.core.text.index.GlobalIndexer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

/**
 *
 * @author Adrien GUILLE, ERIC Lab, University of Lyon 2
 * @email adrien.guille@univ-lyon2.fr
 */
public class Corpus {

    public Configuration configuration;

    public String info;
    public int messageCount;
    public int nbTimeSlices;
    public boolean loaded = false;
    public Timestamp startTimestamp;
    public Timestamp endTimestamp;
    public int[] distribution;
    public String output;

    // Indexes
    short[][] frequencyMatrix;
    public ArrayList<String> vocabulary;
    short[][] mentionFrequencyMatrix;
    public ArrayList<String> mentionVocabulary;

    public Corpus(Configuration conf) {
        configuration = conf;
    }

    public void prepareCorpus() {
        System.out.println(Util.getDate() + " Preparing corpus...");
        String[] fileArray = new File("input/").list();
        nbTimeSlices = 0;
        NumberFormat formatter = new DecimalFormat("00000000");
        ArrayList<Integer> list = new ArrayList<>();
        for (String filename : fileArray) {
            if (filename.endsWith(".text")) {
                try {
                    list.add(formatter.parse(filename.substring(0, 8)).intValue());
                } catch (ParseException ex) {
                    Logger.getLogger(Corpus.class.getName()).log(Level.SEVERE, null, ex);
                }
                nbTimeSlices++;
            }
        }
        int a = Collections.min(list), b = Collections.max(list);
        LineIterator it = null;
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            it = FileUtils.lineIterator(new File("input/" + formatter.format(a) + ".time"), "UTF-8");
            if (it.hasNext()) {
                Date parsedDate = dateFormat.parse(it.nextLine());
                startTimestamp = new java.sql.Timestamp(parsedDate.getTime());
            }
            it = FileUtils.lineIterator(new File("input/" + formatter.format(b) + ".time"), "UTF-8");
            String lastLine = "";
            while (it.hasNext()) {
                lastLine = it.nextLine();
            }
            Date parsedDate = dateFormat.parse(lastLine);
            endTimestamp = new java.sql.Timestamp(parsedDate.getTime());
        } catch (IOException | ParseException ex) {
            Logger.getLogger(Corpus.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            LineIterator.closeQuietly(it);
        }
        System.out.print("   - Computing word frequencies");
        GlobalIndexer indexer = new GlobalIndexer(configuration.numberOfThreads, false);
        try {
            indexer.index("input/", configuration.stopwords);
        } catch (InterruptedException | IOException ex) {
            Logger.getLogger(Corpus.class.getName()).log(Level.SEVERE, null, ex);
        }
        indexer = new GlobalIndexer(configuration.numberOfThreads, true);
        try {
            indexer.index("input/", configuration.stopwords);
        } catch (InterruptedException | IOException ex) {
            Logger.getLogger(Corpus.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println(", 100% done.");
    }

    public void loadCorpus(boolean parallelized) {
        output = "";
        if (configuration.prepareCorpus) {
            prepareCorpus();
        }
        String[] fileArray = new File("input/").list();
        nbTimeSlices = 0;
        NumberFormat formatter = new DecimalFormat("00000000");
        ArrayList<Integer> list = new ArrayList<>();
        for (String filename : fileArray) {
            if (filename.endsWith(".text")) {
                try {
                    list.add(formatter.parse(filename.substring(0, 8)).intValue());
                } catch (ParseException ex) {
                    Logger.getLogger(Corpus.class.getName()).log(Level.SEVERE, null, ex);
                }
                nbTimeSlices++;
            }
        }
        int a = Collections.min(list), b = Collections.max(list);
        distribution = new int[nbTimeSlices];
        messageCount = 0;
        LineIterator it = null;
        try {
            it = FileUtils.lineIterator(new File("input/" + formatter.format(a) + ".time"), "UTF-8");
            if (it.hasNext()) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date parsedDate = dateFormat.parse(it.nextLine());
                startTimestamp = new java.sql.Timestamp(parsedDate.getTime());
            }
            it = FileUtils.lineIterator(new File("input/" + formatter.format(b) + ".time"), "UTF-8");
            String timestamp = "";
            while (it.hasNext()) {
                timestamp = it.nextLine();
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date parsedDate = dateFormat.parse(timestamp);
            endTimestamp = new java.sql.Timestamp(parsedDate.getTime());
        } catch (IOException | ParseException ex) {
            Logger.getLogger(Corpus.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            LineIterator.closeQuietly(it);
        }
        try {
            // Global index
            FileInputStream fisMatrix = new FileInputStream("input/indexes/frequencyMatrix.dat");
            ObjectInputStream oisMatrix = new ObjectInputStream(fisMatrix);
            frequencyMatrix = (short[][]) oisMatrix.readObject();
            FileInputStream fisVocabulary = new FileInputStream("input/indexes/vocabulary.dat");
            ObjectInputStream oisVocabulary = new ObjectInputStream(fisVocabulary);
            vocabulary = (ArrayList<String>) oisVocabulary.readObject();
            // Mention index
            FileInputStream fisMentionMatrix = new FileInputStream("input/indexes/mentionFrequencyMatrix.dat");
            ObjectInputStream oisMentionMatrix = new ObjectInputStream(fisMentionMatrix);
            mentionFrequencyMatrix = (short[][]) oisMentionMatrix.readObject();
            FileInputStream fisMentionVocabulary = new FileInputStream("input/indexes/mentionVocabulary.dat");
            ObjectInputStream oisMentionVocabulary = new ObjectInputStream(fisMentionVocabulary);
            mentionVocabulary = (ArrayList<String>) oisMentionVocabulary.readObject();
            // Message count
            String messageCountStr = FileUtils.readFileToString(new File("input/indexes/messageCount.txt"));
            messageCount = Integer.parseInt(messageCountStr);
            // Message count distribution
            FileInputStream fisDistribution = new FileInputStream("input/indexes/messageCountDistribution.dat");
            ObjectInputStream oisDistribution = new ObjectInputStream(fisDistribution);
            distribution = (int[]) oisDistribution.readObject();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Corpus.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(Corpus.class.getName()).log(Level.SEVERE, null, ex);
        }
        DecimalFormat df = new DecimalFormat("#,###");
        System.out.println(Util.getDate() + " Loaded corpus:");
        output += Util.getDate() + " Loaded corpus:\n";
        info = "   - time-slices: " + df.format(nbTimeSlices) + " time-slices of " + configuration.timeSliceLength + " minutes each\n";
        info += "   - first message: " + startTimestamp + "\n";
        double datasetLength = (nbTimeSlices * configuration.timeSliceLength) / 60 / 24;
        info += "   - last message: " + endTimestamp + " (" + datasetLength + " days)\n";
        info += "   - number of messages: " + df.format(messageCount);
        output += info;
        System.out.println(info);
    }

    public short[] getMentionFrequency(int i) {
        return mentionFrequencyMatrix[i];
    }

    public short[] getGlobalFrequency(String term) {
        int i = vocabulary.indexOf(term);
        if (i == -1) {
            return new short[nbTimeSlices];
        } else {
            return frequencyMatrix[i];
        }
    }

    public String getMessages(Event event) {
        String messages = "";
        NumberFormat formatter = new DecimalFormat("00000000");
        String mainTerm = event.mainTerm;
        int count = 0;
        for (int i = event.I.timeSliceA; i <= event.I.timeSliceB; i++) {
            try {
                String filename = "input/" + formatter.format(i) + ".text";
                List<String> lines = FileUtils.readLines(new File(filename));
                for (String line : lines) {
                    if (line.contains(" " + mainTerm + " ")) {
                        messages += line + "\n";
                        count++;
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(Corpus.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return messages;
    }

    public Timestamp toDate(int timeSlice) {
        Timestamp date = startTimestamp;
        long dateLong = date.getTime() + timeSlice * configuration.timeSliceLength * 60 * 1000L;
        return new Timestamp(dateLong);
    }
}
