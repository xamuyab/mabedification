/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kerangalam.uyab;
/*
This class was originally came from fr.ericlab.mabed.app.Main and I moved the main method here
*/
import fr.ericlab.mabed.algo.MABED;
import fr.ericlab.mabed.app.Configuration;
import fr.ericlab.mabed.structure.Corpus;
import fr.ericlab.util.Util;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import main.java.fr.ericlab.sondy.core.app.Main;
import org.apache.commons.io.FileUtils;

public class Mabedification {

     /**
     *
     * @author Adrien GUILLE, ERIC Lab, University of Lyon 2
     * @email adrien.guille@univ-lyon2.fr
     */
    public static void main(String[] args) throws IOException {
        System.out.println("MABEDIFICATION -- bayu modified  ");
        Locale.setDefault(Locale.US);
        Configuration configuration = new Configuration();
        Corpus corpus = new Corpus(configuration);
        System.out.println("MABED: Mention-Anomaly-Based Event Detection");
        if (args.length == 0 || args[0].equals("-help")) {
            System.out.println("For more information on how to run MABED, see the README.txt file");
        } else {
            if (args[0].equals("-run")) {
                try {
                    if (configuration.numberOfThreads > 1) {
                        System.out.println("Running the parallelized implementation with " + configuration.numberOfThreads + " threads (this computer has " + Runtime.getRuntime().availableProcessors() + " available threads)");
                    } else {
                        System.out.println("Running the centralized implementation");
                    }
                    corpus.loadCorpus(configuration.numberOfThreads > 1);
                    String output = "MABED: Mention-Anomaly-Based Event Detection\n" + corpus.output + "\n";
                    System.out.println("-------------------------\n" + Util.getDate() + " MABED is running\n-------------------------");
                    output += "-------------------------\n" + Util.getDate() + " MABED is running\n-------------------------\n";
                    System.out.println(Util.getDate() + " Reading parameters:\n   - k = " + configuration.k + ", p = " + configuration.p + ", theta = " + configuration.theta + ", sigma = " + configuration.sigma);
                    MABED mabed = new MABED();
                    if (configuration.numberOfThreads > 1) {
                        output += mabed.applyParallelized(corpus, configuration);
                    } else {
                        output += mabed.applyCentralized(corpus, configuration);
                    }
                    System.out.println("--------------------\n" + Util.getDate() + " MABED ended\n--------------------");
                    output += "--------------------\n" + Util.getDate() + " MABED ended\n--------------------\n";
                    File outputDir = new File("output");
                    if (!outputDir.isDirectory()) {
                        outputDir.mkdir();
                    }
                    File textFile = new File("output/MABED.tex");
                    FileUtils.writeStringToFile(textFile, mabed.events.toLatex(corpus), false);
                    textFile = new File("output/MABED.log");
                    FileUtils.writeStringToFile(textFile, output, false);
                    mabed.events.printLatex(corpus);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                System.out.println("Unknown option '" + args[0] + "'\nType 'java -jar MABED.jar -help' for more information on how to run MABED");
            }
        }
    }
    
}
