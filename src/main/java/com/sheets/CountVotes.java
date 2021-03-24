package com.sheets;

import java.util.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Collectors.flatMapping;
import static java.util.stream.Collectors.toMap;

public class CountVotes {

    public static void main(String[] args) throws IOException {

        // Read in the file

        Map<String, Map<String, Integer>> votingList = null;
        //Comparator<Map<String,Integer>> mapOrdering = Comparator.comparing(Map::values);
        //Map<String, Map<String,Integer>> votingList = new HashMap<>();  // iteration approach

        Map<Object, List<List<String>>> simpleList = null;

        try {
            Path file = new File("/Users/kennys/Dropbox/cobblers/cobblersVotes.txt").toPath();   // Use Heroku-supported key/value setup

            Stream<String> lineDetail = Files.lines(file);
            votingList  =
                    lineDetail
                            .filter(l -> !l.startsWith("#"))
                            .map((String l) -> {
                                return Arrays.asList(l.split("\\^",-2));    // 1.8 compatible (not using List.of)
                            })
                            // More elegant Collectors based approach
                            // Could use a Supplier to create a SortedMap to put things in order
                            .collect(groupingBy(l -> l.get(0),              // Use first String to groupBy
                                    flatMapping(inner -> inner.stream()     // flatMapping to create stream of Strings from the List of Lists
                                            .skip(1),                       // Skip first String 'cos it's the performer's name
                                            toMap(k -> k, v -> 1, (s,a) -> s + 1, () -> new TreeMap<String, Integer>())) )); // use toMap() with merge resolution to total the matches

//                            .forEach( l -> {    // TODO. Prob not ideal due to side-effects
//                                votingList.putIfAbsent(l.get(0), new HashMap<>());  // Add a new key, value to outer Map
//                                l.forEach(s -> {
//                                    if (!s.equalsIgnoreCase(l.get(0))) {
//                                        if (!votingList.get(l.get(0)).containsKey(s)) {
//                                            votingList.get(l.get(0)).put(s, 1);
//                                        } else {
//                                            votingList.get(l.get(0)).put(s,
//                                                    (votingList.get(l.get(0)).get(s) + 1)       // increment using boxing/unboxing
//                                            );
//                                        }
//                                    }
//                                });
//
//
//                            });

            // Way it was afore

            lineDetail = Files.lines(file);
            simpleList  =
                    lineDetail
                            .filter(l -> !l.startsWith("#"))
                            .map((String l) -> {
                                return Arrays.asList(l.split("\\^",-2));    // 1.8 compatible (not using List.of)
                            })
            .collect(groupingBy(l -> l.get(0)));

            // End of old way

        } catch (IOException ex) {
            throw new IOException("Problem opening data file");
        }

        System.out.println(simpleList + "\n");

        votingList.entrySet()
                .forEach(p -> System.out.println(p));







    }
}
