package com.sheets;


import io.javalin.Javalin;
import io.javalin.http.Context;

import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.*;

import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.joining;

import java.util.stream.Stream;

// Since not that big, maybe first build a Grid of all tunes in a suitable way to use for all the stages?

// Javalin version hack. TODO refactor!!!



public class FilterTunes {
   
    
  private static int getHerokuAssignedPort() {
    String herokuPort = System.getenv("PORT");
    if (herokuPort != null) {
      return Integer.parseInt(herokuPort);
    }
    return 7000;    // If not on Heroku
  }
    
    
    
    
    public static void main(String[] args) {
        
        // Some HTML code for creating URLs
        String audioTagPrefix = "<audio id = \"tune\" controls><source src=\"";
        String googlePrefix = "http://docs.google.com/uc?export=open&id=";
        String closeTag = "\"/></audio>\n";

        // Set up Javalin incl a dir for static files
        Javalin app = Javalin.create(config -> config.addStaticFiles("public")).start(getHerokuAssignedPort());


//        app.post("/tunedetail", ctx -> {
//
//            ctx.result("hello");
//
//                });


        // Listen for POST to "/tunes"
        app.post("/tunes", ctx -> {

        List<List<String>> tuneDataList;
        try {
            Path file = new File("target/classes/public/tunesdata.txt").toPath();   // Use Heroku-supported key/value setup

            Stream<String> lineDetail = Files.lines(file);
            tuneDataList  =
                    lineDetail
                    .map((String l) -> {
                        return Arrays.asList(l.split("\\^",-2));    // 1.8 compatible (not using List.of)
                            })
                    .collect(toList());

        } catch (IOException ex) {
            throw new IOException("Problem opening data file");
        }

        List<String> headings = tuneDataList.get(0);
        System.out.println("tuneDataHeadings: " + headings);

        // Get the Predicate to use with the data
        Predicate<List<String>> aFilter = getStringPredicate(ctx, headings);

        int finalPt = Integer.parseInt(Objects.requireNonNull(ctx.formParam("Performance Type")));  // Or return "1" if null by catching null?

        // Now get the results and output as HTML links for the songs
        // As with other parameters, no selections means ALL performances of a song selected
        // TODO tidy up the filter lines
        
        String htmlOutput = tuneDataList.stream()
                .skip(1)                                                    // skip headings line
                .filter(t -> t.get(6 + finalPt) != null)                    // Check that a performance of appropriate performance type exists for this song
                .filter(t -> t.get(6 + finalPt).startsWith("http"))         // And that it looks like a URL (TODO actually check if valid URL??)
                .filter(aFilter)
               .map(o -> ("<div name ='" +
                        o.get(headings.indexOf("Name"))
                        //+ "' onClick = 'alert (\"hello\");'" +
                        + "'> "
                        + o.get(headings.indexOf("Name"))
                        + audioTagPrefix.replace("tune", o.get(17)) + o.get(6 + finalPt) + closeTag // TODO Fix this hack...Hard-coding 17 as lyrics
               + "</div><br>").replace("’", "'"))           // TODO little bit of a hack for replacing smart quotes - maybe fix in CreateTuneFile?

                                                                            // TODO Sort the output?
                .collect(joining());
        
            System.out.println("htmlOutput>>>>> " + htmlOutput);
        
        if (htmlOutput.equals("")) htmlOutput = "No songs";
            
            ctx.result (
                    " <form action=\"filtertunes.html\" method=\"get\">\n" +
"            <input type=\"submit\" value=\"Reload query page\" />\n" +
"        </form> <br> \n"
                     + htmlOutput);
        });


    }
    
    static Predicate<List<String>> getStringPredicate(Context ctx, List<String> headings)  {

        // Build the Filter to use with the data from sheets
        Map<String, List<String>> queryParamMap = ctx.formParamMap();    // TODO Possibly try to remove any params with no values?
        
        Set<Map.Entry<String, List<String>>> queryParams = queryParamMap.entrySet(); // Need a Set for iteration
        
        System.out.println("The params:" + queryParams);

        Iterator<Map.Entry<String, List<String>>> itr = queryParams.iterator();
        List<String> values;
        int pt = 0;

        // Build the predicate filter
        Predicate<List<String>> andFilter = o -> true;   // necessary to initialize outer attribute predicates (they'll all be AND'd)

        while(itr.hasNext()) 
        { 
             String key = itr.next().getKey();
            System.out.println("Key before conditional: " + key);
              values = queryParamMap.get(key);

             Predicate<List<String>> orFilter = o -> false;          // necessary to initialize before each inner loop - each attribute value OR'd

             // there are some values and the first value isn't "" (so don't create a predicate if nothing checked)
             if (values.size() > 0 && !values.get(0).equals("") && !key.equalsIgnoreCase("Performance Type")) {
                 for (String value : values) {
                     System.out.println("key: " + key + " value: " + value);

                     // A set of values for a single checkbox can be specified in HTML by using ":" to separate individual values
                     // TODO add special case for "name" key so it uses contains() not equals()
                     for (String singleValue: value.split(":")) {
                         orFilter = orFilter.or((List<String> o) ->
                                 !key.equalsIgnoreCase("Name")?o.get(headings.indexOf(key)).equalsIgnoreCase(singleValue):
                                         o.get(headings.indexOf(key)).toLowerCase().contains(singleValue.toLowerCase()));
                     }

                 }
                 andFilter = andFilter.and(orFilter);

            } 
            System.out.println("Perf type: " + pt);

        }
        return andFilter;
    }
}