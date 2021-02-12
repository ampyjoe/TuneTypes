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
    return 7000;
  }
    
    
    
    
    public static void main(String[] args) {
        
        // Some HTML code for creating URLs
        String audioTagPrefix = "<audio controls><source src=\"";
        String googlePrefix = "http://docs.google.com/uc?export=open&id=";
        String closeTag = "\"/></audio><br>\n";
        
        //.start(getHerokuAssignedPort())
        // Set up Javalin incl a dir for static files
        Javalin app = Javalin.create(config -> config.addStaticFiles("public")).start(getHerokuAssignedPort());
        //Javalin app = Javalin.create(config -> config.addStaticFiles("public")).start(7000);
        //Javalin app = Javalin.create().start(7000);
        



        // Start listening for requests
        app.post("/tunes", ctx -> {
            
        
        List<List<String>> tuneDataList;       // Error on line 271 if this declared as null????
        //List<String> tempList = new ArrayList<>();
        //FileUtil.
        
        String person = null;
        try {
            Path file = new File("target/classes/public/tunesdata.txt").toPath();

            Stream<String> lineDetail = Files.lines(file);
            tuneDataList  = 
                    lineDetail
                    .map((String l) -> {
                        return Arrays.asList(l.split("\\^",-2));    // 1.8 compatible
                            })                            
                    .collect(toList());

                    
        } catch (NoSuchElementException e) {
            //System.out.println("No such user " + e);
            throw new NoSuchElementException("No such user found");
        } catch (IOException ex) { 
            //System.out.println("Problem opening user data file.\n" + ex);
            throw new IOException("Problem opening user data file");
        }

        List<String> headings = tuneDataList.get(0);
        
        System.out.println("tuneDataList: " + headings);
        
        System.out.println("Index for Melodic Range: " + headings.indexOf("Melodic Range"));
        
        

            
        // Get the Predicate to use with the data from sheets
        
        Predicate<List<String>> aFilter = getStringPredicate(ctx, headings);
        
                
        int finalPt = Integer.parseInt(Objects.requireNonNull(ctx.formParam("Performance Type")));  // Or return "1" if null by catching null?

        
        // Now get the results and output as HTML links for the songs
            // As with other parameters, no selections means ALL performances of a song selected
            // TODO tidy up the filter lines
        

        
        String htmlOutput = tuneDataList.stream()
                .skip(1)                                                    // skip headings line
                .filter(t -> t.get(6 + finalPt) != null)                    // Check that a performance of appropriate type exists for this song
                .filter(t -> t.get(6 + finalPt).startsWith("http"))         // And that it looks like a URL (check if valid URL??)
                .filter(aFilter)
               .map(o -> (
                        o.get(headings.indexOf("Name")) 
                        + " ... "
                        + audioTagPrefix + o.get(6 + finalPt) + closeTag))
                .collect(joining());
        
            System.out.println("htmlOutput>>>>> " + htmlOutput);
        
        if (htmlOutput.equals("")) htmlOutput = "No songs";
            
            ctx.result (
                    " <form action=\"filtertunes.html\" method=\"get\">\n" +
"            <input type=\"submit\" value=\"Reload query page\" />\n" +
"        </form> <br> \n"
                     + htmlOutput);
        });
        
        
//        get("/tunefilter.jsp", (req, res) -> {
//        
////            System.out.println("hello: " + req.servletPath());
////        
////            res.redirect("http://localhost:4567/public/tunefilter.jsp");
//            return new ModelAndView("", "tunefilter.jsp");
//        
//        });

               
    }
    
    static Predicate<List<String>> getStringPredicate(Context ctx, List<String> headings)  {
        
        //List<Object> headings = getHeadings();
        
               // Build the Filter to use with the data from sheets
        Map<String, List<String>> queryParamMap = ctx.formParamMap();    // TODO Possibly try to remove any params with no values?
        
        Set<Map.Entry<String, List<String>>> queryParams = queryParamMap.entrySet();
        
        System.out.println("The params:" + queryParams);

        Iterator<Map.Entry<String, List<String>>> itr = queryParams.iterator();
        List<String> values;
        int pt = 0;

        // Build the predicate filter
        Predicate<List<String>> andFilter = o -> true;   // necessary to initialize outer attribute predicates (they'll all be AND'd)

        while(itr.hasNext()) 
        { 
             String key = itr.next().getKey();
              values = queryParamMap.get(key);

             Predicate<List<String>> orFilter = o -> false;          // necessary to initialize before each inner loop - each attribute value OR'd
             if (values.size() > 0 && !values.get(0).equals("") && !key.equalsIgnoreCase("Performance Type")) {  // there are some values and the first isn't "" (so don't create predicate if nothing checked)
                 for (String value : values) {
                     System.out.println("key: " + key + " value: " + value);

                     System.out.println("Other key: " + key + " value: " + value);
                     //if (key.equalsIgnoreCase("Melodic Range")) {
                             for (String singleValue: value.split(":")) {
                                 orFilter = orFilter.or((List<String> o) ->
                                         o.get(headings.indexOf(key)).equalsIgnoreCase(singleValue));
                             }

//                     } else orFilter = orFilter.or((List<String> o) ->              // This might not be necessary as it's a special case of the if stmt
//                             o.get(headings.indexOf(key)).equalsIgnoreCase(value));

                 }
                 andFilter = andFilter.and(orFilter);

            } 
            System.out.println("Perf type: " + pt);

        } 

        
        return andFilter;
        
        
    }
    
    
//    // Temporary - maybe should just be a settings class?
//    static List<Object> getHeadings() throws IOException, GeneralSecurityException{
//        
//
//        
//        return List.of("hello");
//         
//    } 

}