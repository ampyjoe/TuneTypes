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
import com.google.api.services.sheets.v4.model.GridData;
import com.google.api.services.sheets.v4.model.RowData;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
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
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;
import static spark.Spark.post;
import static spark.Spark.get;
import static spark.Spark.staticFiles;


// Since not that big, maybe first build a Grid of all tunes in a suitable way to use for all the stages?



public class ListTunes {
    
    // Some changes

    private static final String APPLICATION_NAME = "Google sheets tunes";
    private static Sheets sheetsService;
    private static String TUNES_SPREADSHEET_ID = "1iLZ7ViUJZn1yhYIR88_ohcXedqw4WCgZaKNOeiR85Ak";  // not null?

    private static Credential authorize() throws IOException, GeneralSecurityException {
        InputStream in = ListTunes.class.getClassLoader().getResourceAsStream("credentials.json");
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
    
    public static void main(String[] args) throws IOException, GeneralSecurityException {
        
        staticFiles.location("/public");
        
        
//        Spark.externalStaticFileLocation("/home/resources");
//get("/home", (req, res) -> {
//    return new ModelAndView("", "tunefilter.jsp");
//});
        
        
        
        String audioTagPrefix = "<audio controls><source src=\"";
        String googlePrefix = "http://docs.google.com/uc?export=open&id=";
        String closeTag = "\"/></audio><br>\n";

        sheetsService = getSheetsService();
        List<Object> headings = getHeadings();
        List<String> htmlLines = null;
        
           
        get("/tunes", (req, res) -> {
            

        Set<String> queryParams = req.queryParams();    // TODO Possibly try to remove any params with no values?
        
        System.out.println("The params:" + queryParams);

        Iterator<String> itr = queryParams.iterator();
        String[] values = null;

        // Build the filter
        Predicate<RowData> andFilter = o -> true;   // necessary to initialize outer attribute predicates (they'll all be AND'd)
        
        while(itr.hasNext()) 
        { 
             String key = itr.next();
             values = req.queryParamsValues(key);

             Predicate<RowData> orFilter = o -> false;   // necessary to initialize before each inner loop - each attribute value OR'd
             if (values.length > 0 && !values[0].equals("")) { // there are some values and the first isn't "" (so doesn't use Name if empty)
                 for (String value : values) {
                     System.out.println("key: " + key + " value: " + value);
                    orFilter = orFilter.or((RowData o) -> 
                            o.getValues().get(headings.indexOf(key)).getFormattedValue().equalsIgnoreCase(value));                  
                     
                 }
                 andFilter = andFilter.and(orFilter);
             }    

        } 

        
        Sheets.Spreadsheets.Get request = sheetsService.spreadsheets().get(TUNES_SPREADSHEET_ID);   // Get actual spreadsheet no range included
        request.setIncludeGridData(true);
        Spreadsheet spreadsheet = request.execute();            // Use for links
        Sheet currSheet = spreadsheet.getSheets().get(0);       // Get the first spreadsheet
        GridData theData = currSheet.getData().get(0);          // setStartXXX seems to have no effect    First page ?
        
        String htmlOutput = theData.getRowData().stream()   // A stream of RowData
                .skip(2)    // Skip the two headings rows

                .filter(o -> o.getValues().get(6).getHyperlink() != null)   // there must be a hyperlink for recorded voice/piano

                .filter(andFilter)
                
                .map(o -> (
                        o.getValues().get(headings.indexOf("Name")).getFormattedValue() 
                        + " ... " 
                        + 
                        audioTagPrefix + googlePrefix + o.getValues().get(6).getHyperlink().split("file/d/")[1].split("/view")[0]) + closeTag)
                //.collect(joining("\"/></audio><br>\n"));
                .collect(joining());
        
        if (htmlOutput.equals("")) htmlOutput = "No songs";
            
            return (
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
    

    
    

    
    
    
    
    
    // Temporary - maybe should just be a settings class?
    static List<Object> getHeadings() throws IOException, GeneralSecurityException{
        
        String range = "Sheet1!A2:R52"; // TODO rename to other than Sheet1
        
        ValueRange response = sheetsService.spreadsheets().values() // Use for Strings
            .get(TUNES_SPREADSHEET_ID, range)
            .execute();
        
        List<List<Object>> values = response.getValues();
        return values.get(0);
         
    }
    
    
    
    

}