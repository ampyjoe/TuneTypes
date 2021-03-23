package com.sheets;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.groupingBy;

public class CountVotes {

    public static void main(String[] args) throws IOException {

        // Read in the file

        //Map<Object, List<List<String>>> votingList = null;
        Map<String, Map<String,Integer>> votingList = new HashMap<>();
        try {
            Path file = new File("/Users/kennys/Dropbox/cobblers/cobblersVotes.txt").toPath();   // Use Heroku-supported key/value setup

            Stream<String> lineDetail = Files.lines(file);
            //votingList  =
                    lineDetail
                            .filter(l -> !l.startsWith("#"))
                            .map((String l) -> {
                                return Arrays.asList(l.split("\\^",-2));    // 1.8 compatible (not using List.of)
                            })
//                            .collect(groupingBy((List l) -> l.get(0)));

                            .forEach( l -> {
                                votingList.putIfAbsent(l.get(0), new HashMap<>());  // Add a new key, value to outer Map
                                l.forEach(s -> {
                                    if (!s.equalsIgnoreCase(l.get(0))) {
                                        if (!votingList.get(l.get(0)).containsKey(s)) {
                                            votingList.get(l.get(0)).put(s, 1);
                                        } else {
                                            votingList.get(l.get(0)).put(s,
                                                    (votingList.get(l.get(0)).get(s) + 1)       // boxing/unboxing
                                            );
                                        }
                                    }
                                });


                            });




        } catch (IOException ex) {
            throw new IOException("Problem opening data file");
        }

        //System.out.println(votingList);

        votingList.entrySet().stream()
                .forEach(p -> System.out.println(p));







    }
}
