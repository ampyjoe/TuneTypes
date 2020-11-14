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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.joining;
import static spark.Spark.post;
import static spark.Spark.get;


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
        
        String googlePrefix = "http://docs.google.com/uc?export=open&id=";
        String audioTagPrefix = "<audio controls><source src=\"";
        sheetsService = getSheetsService();
        List<Object> headings = getHeadings();
        List<String> htmlLines = null;
        
           
        //get("/tunes", (req, res) -> {

//            String name = req.queryParams("name");
//            String phoneNumFrom = req.queryParams("From");


//        String name = "hokey";
//        String tonic = "D";
//        String tonality = "Dorian";
//        String melodic =
//        String harmonic =
//        String harmonization =

        Map<String,String> theParams //= req.params(); 
        = Map.of("Tonic", "G", "Tonality", "major");
        
        //Predicate theFilter = ( (RowData f) -> f.toString().equals("hello") ); // To start w a true Predicate. f will be the Object passed in
        
        Predicate<RowData> theFilter = o -> true;   // necessary to initialize
        
        // Can't really do it 'cos of lambda limitation on local variable in lambda expr
        //theParams.forEach( (k, v) -> theFilter = theFilter.and((RowData o) -> o.getValues().get(headings.indexOf(k)).getFormattedValue().equalsIgnoreCase(v)));
//        
//        
        Iterator<Map.Entry<String, String>> itr = theParams.entrySet().iterator(); 

        
        while(itr.hasNext()) 
        { 
             Map.Entry<String, String> entry = itr.next(); 
             theFilter = theFilter.and((RowData o) -> o.getValues().get(headings.indexOf(entry.getKey())).getFormattedValue().equalsIgnoreCase(entry.getValue()));
        } 

        
        Sheets.Spreadsheets.Get request = sheetsService.spreadsheets().get(TUNES_SPREADSHEET_ID);   // Get actual spreadsheet no range included
        request.setIncludeGridData(true);
        Spreadsheet spreadsheet = request.execute();            // Use for links
        Sheet currSheet = spreadsheet.getSheets().get(0);       // Get the first spreadsheet
        GridData theData = currSheet.getData().get(0);          // setStartXXX seems to have no effect    First page ?
        
        String htmlOutput = theData.getRowData().stream()
                .skip(2)    // Skip the two headings rows
                //.filter(r -> r.getValues().get(headings.indexOf("Tonality")).getFormattedValue().equalsIgnoreCase("Dorian"))
                //.filter(f -> f.getValues().get(2) == null)

                .filter(o -> o.getValues().get(6).getHyperlink() != null)   // there must be a hyperlink
                //.filter((RowData o) -> o.getValues().get(headings.indexOf("Tonality")).getFormattedValue().equalsIgnoreCase("Dorian"))
                .filter(theFilter)
                
                .map(o -> (
                        o.getValues().get(headings.indexOf("Name")).getFormattedValue() 
                        + " ... " 
                        + 
                        audioTagPrefix + googlePrefix + o.getValues().get(6).getHyperlink().split("file/d/")[1].split("/view")[0]))
                
                //.filter(o -> o.getValues().get(6).getHyperlink() != null)
                
                .collect(joining("\"/></audio><br>\n"));
                //.filter
        
//            
            System.out.println ("" + htmlOutput + "\"/></audio>");
            
            
            
            
//            return ("" + htmlOutput + "\"/></audio>");
//        });
        
        
//        get("/filter", (req, res) -> {
//        
//        
//        
//            return "";
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