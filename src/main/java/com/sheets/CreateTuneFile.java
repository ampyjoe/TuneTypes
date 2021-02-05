package com.sheets;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.GridData;
import com.google.api.services.sheets.v4.model.RowData;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;
import io.javalin.Javalin;
import io.javalin.http.Context;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import java.util.stream.Stream;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;
import static spark.Spark.post;
import static spark.Spark.get;
import static spark.Spark.staticFiles;


// Since not that big, maybe first build a Grid of all tunes in a suitable way to use for all the stages?

// Javalin version hack. TODO refactor!!!



public class CreateTuneFile {
    
    // Some changes

    private static final String APPLICATION_NAME = "Google sheets tunes";
    private static Sheets sheetsService;
    private static String TUNES_SPREADSHEET_ID = "1iLZ7ViUJZn1yhYIR88_ohcXedqw4WCgZaKNOeiR85Ak";  // not null?

    private static Credential authorize() throws IOException, GeneralSecurityException {
        InputStream in = CreateTuneFile.class.getClassLoader().getResourceAsStream("credentials.json");
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
            JacksonFactory.getDefaultInstance(), new InputStreamReader(in)
        );

        List<String> scopes = Collections.singletonList(SheetsScopes.SPREADSHEETS_READONLY);

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            JacksonFactory.getDefaultInstance(),
            clientSecrets,
            scopes)
            .setDataStoreFactory(new FileDataStoreFactory(new File("tokens")))
            .setAccessType("offline")
            .build();

        Credential credential = new AuthorizationCodeInstalledApp(
            flow, new LocalServerReceiver())
            .authorize("user");

        return credential;
    }


    public static Sheets getSheetsService() throws IOException, GeneralSecurityException {
        Credential credential = authorize();
        return new Sheets.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            JacksonFactory.getDefaultInstance(),
            credential)
            .setApplicationName(APPLICATION_NAME)
            .build();
    }
    
  private static int getHerokuAssignedPort() {
    String herokuPort = System.getenv("PORT");
    if (herokuPort != null) {
      return Integer.parseInt(herokuPort);
    }
    return 7000;
  }
    
    
    
    
    public static void main(String[] args) throws IOException, GeneralSecurityException {
        
        sheetsService = getSheetsService();        
        Sheets.Spreadsheets.Get request = sheetsService.spreadsheets().get(TUNES_SPREADSHEET_ID);   // Get actual spreadsheet no range included
        request.setIncludeGridData(true);
        Spreadsheet spreadsheet = request.execute();            // Use for links
        Sheet currSheet = spreadsheet.getSheets().get(0);       // Get the first spreadsheet
        GridData theData = currSheet.getData().get(0);          // setStartXXX seems to have no effect    First page ?
        
        // Some HTML code for creating URLs
        String audioTagPrefix = "<audio controls><source src=\"";
        String googlePrefix = "http://docs.google.com/uc?export=open&id=";
        String closeTag = "\"/></audio><br>\n";
        // Create a Stream where each tune is a List<CellData>
        
        
//                     // get the initial comments
//        Path file = new File("tunesData.txt".toPath();
//             Stream<String> comments = Files.lines(file)
//                     .takeWhile(f -> f.startsWith("#"))
//                     .collect(toList())
//                     .stream();
//
//             
             PrintWriter pw = new PrintWriter("tunesdata.txt");
//             
//             Stream<String> summat =  Arrays.stream(textString.split("\n"));
//             
//             // Combine Stream of comments and Stream of Jobs 
//             Stream.concat(comments, summat)
//                    //.peek(l -> System.out.println("here: " + l))
//                .forEach(l -> pw.println(l));
                              //pw.close();
        
        //List<List<String>> tuneDataList =        
        theData.getRowData().stream()
            .skip(2)
            .filter(r -> r.getValues().get(0).getFormattedValue()!=null)    // Make sure the tune name has a name!
            .map( (RowData r) -> {
                return r.getValues().stream().limit(10)
                        .map((CellData c) -> {
                            if (c.getHyperlink() != null) {
                                if (c.getHyperlink().contains("view") && c.getHyperlink().contains("file/d/"))
                                    return googlePrefix + c.getHyperlink().split("file/d/")[1].split("/view")[0];
                                else return "";
                            }
                            else return c.getFormattedValue();
                        })
                        //.collect(toList());
                        .collect(joining("^"));
            })
            //.collect(toList());
        
        // Write to file
//            streamOfLists
                //.map (r -> r.stream().collect(joining(",")))

                .forEach(l -> pw.println(l));
            pw.close();

            
            //streamOfLists.forEach(System.out::println);
            //System.exit(0);
        
        



        
        
        
        



        List<Object> headings = getHeadings();
    
    }


    // Temporary - maybe should just be a settings class?
    static List<Object> getHeadings() throws IOException, GeneralSecurityException{
        
        String range = "Sheet1!A2:R52"; // TODO rename to other than Sheet1
        
        ValueRange response = sheetsService.spreadsheets().values() // Use for Strings
            .get(TUNES_SPREADSHEET_ID, range)
            .execute();
        
        List<List<Object>> values = response.getValues();
        
        values.get(0).stream()
                .forEach(o -> System.out.println(o));
        
        return values.get(0);
         
    } 

}