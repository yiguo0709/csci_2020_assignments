package sample;

import java.io.*;
import java.util.*;

public class WordCounter {
    public Map<String, Integer> wordCounts;

    public WordCounter() {
        wordCounts = new TreeMap<>();
    }

    public void processFile(File file) throws IOException {
        // for directories, recursively call
        if (file.isDirectory()) {
            File[] filesInDir = file.listFiles();
            for (int i=0; i<filesInDir.length; i++) {
                processFile(filesInDir[i]);
            }
        } else {
            // for single files, load the words and count
            Scanner scanner = new Scanner(file);
            while (scanner.hasNext()) {
                String word = scanner.next();
                if (isWord(word)) {
                    countWord(word);
                }
            }
        }
    }

    private void countWord(String word) {
        if (wordCounts.containsKey(word)) {
            // increment the countWord
            int oldCount = wordCounts.get(word);
            wordCounts.put(word, oldCount+1);
        } else {
            // add the word with count of 1
            wordCounts.put(word, 1);
        }
    }

    private boolean isWord(String token) {
        String pattern = "^[a-zA-Z]*$";
        if (token.matches(pattern)) return true;
        else return false;
    }

    public void printWordCounts(int minCount, File outFile) throws Exception {
        if (outFile.exists()) {
            System.err.println("Warning:  Overwriting file");
        }

        if (!(outFile.canWrite())) {
            System.err.println("Error:  Cannot write to file");
            System.exit(0);
        }

        PrintWriter fout = new PrintWriter(outFile);

        Set<String> keys = wordCounts.keySet();
        Iterator<String> keyIterator = keys.iterator();
        while (keyIterator.hasNext()) {
            String key = keyIterator.next();
            int count = wordCounts.get(key);

            if (count>=minCount) {
                fout.println("'"+key+"' -> '"+count+"'");
            }
        }

        fout.close();
    }

}

