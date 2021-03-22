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
import com.google.api.services.sheets.v4.model.*;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.joining;


// Since not that big, maybe first build a Grid of all tunes in a suitable way to use for all the stages?

// Javalin version hack. TODO refactor!!!


public class CreateSongFile {
    
    // Some changes

    private static final String APPLICATION_NAME = "Cobbler's Songs";
    private static Sheets sheetsService;
    private static String TUNES_SPREADSHEET_ID = "14MC3Q9_FAfUaLRWNQgcXMYQkHnjuUJKV3PbwQUpddi4";  // not null?

    private static Credential authorize() throws IOException, GeneralSecurityException {
        InputStream in = CreateSongFile.class.getClassLoader().getResourceAsStream("credentials.json");
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
        // Some HTML code for creating URLs
        String audioTagPrefix = "<audio controls><source src=\"";
        String googlePrefix = "http://docs.google.com/uc?export=open&id=";
        String closeTag = "\"/></audio><br>\n";




        sheetsService = getSheetsService();        
        Sheets.Spreadsheets.Get request = sheetsService.spreadsheets().get(TUNES_SPREADSHEET_ID);   // Get actual spreadsheet no range included
        request.setIncludeGridData(true);
        Spreadsheet spreadsheet = request.execute();            // Use this approach if needing more details for each cell or to iterate through all sheets


        //List<Object> headings = getHeadings();

        spreadsheet.getSheets().stream()
                .skip(1)
                .forEach(s -> getSongs(s, 250, 5,"wot"));   // Prob not a great way to do - side-effect issues (Collector here?)


            int numColumnsToGrab = 5;  // columns across for each person

             //PrintWriter pw = new PrintWriter("src/main/resources/public/songsdata.txt");
            //pw.close();
             //pw.println(headings.stream().limit(numColumnsToGrab).map(m -> (String)m).collect(joining("^")));

        
            //getSongs(spreadsheet, 7, 250, numColumnsToGrab,"wotever");


    
    }





    static String getSongs(Sheet currSheet, int rowsToGrab, int numColumnsToGrab, String prefix) {

        GridData theData = currSheet.getData().get(0);          // setStartXXX seems to have no effect, using skip instead
        // TODO - make this a method passing in skip and limit (plus details for recorded versions?)

        if (theData.getRowData() == null) return " -- Empty Sheet -- ";    // Hack to ignore sheets w null row data

        System.out.println("-------*** " + currSheet.getProperties().getTitle() + " ***---------");

        theData.getRowData().stream()
                .skip(2)
                .limit(rowsToGrab)
                .map( (RowData r) -> {
                    if (r.getValues() == null) return "*";      // Hack for empty content in a sheet
                    return r.getValues().stream().skip(1).limit(numColumnsToGrab)   // First column empty on most it seems
                            .map((CellData c) -> {
                                if (c.getHyperlink() != null) { // Prob not needed as checking for hyperlink
                                    if (c.getHyperlink().contains("view") && c.getHyperlink().contains("file/d/"))
                                        return prefix + c.getHyperlink().split("file/d/")[1].split("/view")[0];
                                    else return "";
                                }
                                // Replace newlines with # where value not null. TODO File.lines splits on \n, \r, or \n\r hence need to subst "#" as \n in data file
                                else return c.getFormattedValue()==null?null:c.getFormattedValue().replace('\n','#');
                            })
                            //.collect(toList());
                            .collect(joining("^"));
                })
                .forEach(System.out::println);
        //.forEach(l -> pw.println(l));




        return "hello";
    }

    // Temporary - maybe should just be a settings class?
    static List<Object> getHeadings() throws IOException, GeneralSecurityException{
        
        String range = "Favourites!A5:AV50"; // TODO rename to other than Sheet1
        
        ValueRange response = sheetsService.spreadsheets().values() // Use for Strings
            .get(TUNES_SPREADSHEET_ID, range)
            .execute();
        
        List<List<Object>> values = response.getValues();
        
        values.get(0).stream()
                .forEach(o -> System.out.println(o));
        
        return values.get(0);
         
    } 

}