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
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import static java.lang.System.out;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.joining;
import static spark.Spark.post;
import static spark.Spark.get;


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
        
        get("/tunes", (req, res) -> {
        
        sheetsService = getSheetsService();
        List<Object> headings = getHeadings();
        List<String> htmlLines = null;
        
        
        Sheets.Spreadsheets.Get request = sheetsService.spreadsheets().get(TUNES_SPREADSHEET_ID);   // Get actual spreadsheet no range included
        request.setIncludeGridData(true);
        Spreadsheet spreadsheet = request.execute();            // Use for links
        Sheet currSheet = spreadsheet.getSheets().get(0);       // Get the first spreadsheet
        GridData theData = currSheet.getData().get(0);          // setStartXXX seems to have no effect    First page ?
        
        String htmlOutput = theData.getRowData().stream()
                .skip(2)    // Skip the two headings rows
                //.filter(r -> r.getValues().get(headings.indexOf("Tonality")).getFormattedValue().equalsIgnoreCase("Dorian"))
                //.filter(f -> f.getValues().get(2) == null)
                .limit(3)
                .filter(o -> o.getValues().get(2).getEffectiveValue() != null)
                .map(o -> (
                        o.getValues().get(headings.indexOf("Name")).getFormattedValue() 
                        + " ... " 
                        + 
                        audioTagPrefix + googlePrefix + o.getValues().get(6).getHyperlink().split("file/d/")[1].split("/view")[0]))
                .collect(joining("\"/></audio><br>\n"));
                //.filter
        
//        System.out.println(theData.getRowData()     //List<RowData>
//                                    .get(2)         //RowData
//                                    .getValues()    //List<CellData>
//                                    .get(2)         //CellData
//                                    .getFormattedValue()
//        );       

//        List<List<Object>> values = response.getValues();
//        List<Object> headings = values.get(0);
//        List<String> htmlLines = null;
//        
//        if (values == null || values.isEmpty()) {
//            System.out.println("No data found");
//        } else {
//            htmlLines = values.stream()
//                    .filter(r -> r.get(headings.indexOf("Tonality")).toString().equalsIgnoreCase("Dorian"))    // Use recursion for multiple filters?
//                    //.forEach(row -> System.out.printf("Name: %s, tonality: %s, chords: %s\n", row.get(0), row.get(2), row.get(6)));
//                    //.forEach(row -> System.out.printf("Name: %s, tonality: %s, chords: %s\n", row.get(headings.indexOf("Name")), row.get(2), row.get(6)));
//                    //.forEach(row -> System.out.printf("%s : %s : %s\n", row.get(headings.indexOf("Name")), 
//                            //row.get(headings.indexOf("Tonality")), 
//                            //row.get(headings.indexOf("recorded voice/piano"))));
//                    .collect(mapping(r -> r.get(headings.indexOf("Name")).toString()
//                            + r.get(headings.indexOf("recorded voice/piano")).toString()
//                            , toList()));   // Need a joining here
//        }
        
                    //.collect(mapping(r -> r.stream().collect(joining()),toList()));   // Need a joining here?
                    //.collect(mapping(r -> r.get(1).toString() + ":" + r.get(5).toString(),toList()));   // Need a joining here?
                    //.collect(mapping(r -> r.get(1).toString(),toList()));   // Need a joining here?


        //String range = "Sheet1!A2:R52"; // TODO rename to other than Sheet1
//        sheetsService = getSheetsService();
//        Sheets.Spreadsheets.Get request = sheetsService.spreadsheets().get(TUNES_SPREADSHEET_ID);   // Get actual spreadsheet no range included
//        request.setIncludeGridData(true);
//        Spreadsheet spreadsheet = request.execute();
        
        //List<List<Object>> values = spreadsheet.;
        
        
        
        
//        System.out.println("here " + 
//        spreadsheet.getSheets().get(0)                      // Get the first spreadsheet
//                .getData().get(0)                           // First page ?
//                .getRowData().get(5).getValues()            // Row 5
//                .get(7).getHyperlink());                    // Column 7 and hyperlink, not String value
        
        
        // Prob best to use the range version of code for searching, then dataGrid version for getting hyperlinks.
        
        
        
            
//            StringBuilder htmlOutput = new StringBuilder();
//            htmlOutput.append("<br>")
//                    .append(htmlLines.get(2))
//                    .append("</br>");
//            System.out.println(htmlLines);
//            return htmlOutput.toString();

//            String htmlOutput = htmlLines.stream().collect(joining("</br>\n<br>"));
//            
            System.out.println ("" + htmlOutput + "\"/></audio>");
            
            
            
            
            return ("" + htmlOutput + "\"/></audio>");
        });
        
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
