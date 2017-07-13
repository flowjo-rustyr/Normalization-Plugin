import com.flowjo.lib.parameters.ParameterSelectionPanel;
import com.flowjo.lib.parameters.ParameterSelectionPanel.eParameterSelectionMode;
import com.flowjo.lib.parameters.ParameterSetMgrInterface;
import com.treestar.flowjo.engine.EngineManager;
import com.treestar.flowjo.engine.utility.RFlowCalculator;
import com.treestar.lib.PluginHelper;
import com.treestar.lib.core.ExportFileTypes;
import com.treestar.lib.core.ExternalAlgorithmResults;
import com.treestar.lib.core.PopulationPluginInterface;
import com.treestar.lib.data.StringUtil;
import com.treestar.lib.file.FileUtil;
import com.treestar.lib.gui.panels.FJLabel;
import com.treestar.lib.prefs.HomeEnv;
import com.treestar.lib.xml.SElement;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Rusty Raymond on 6/30/2017.
 */
public class Normalization implements PopulationPluginInterface {
    private List<String> parameterNames = new ArrayList<String>();
    private static final String gVersion = "0.2";
    private ExportFileTypes fileType = ExportFileTypes.CSV_SCALE;
    private static Icon gIcon = null;
    private static File gScriptFile = null;
    private static String Prefix = "";
    //private static int columnParsingStart = 5;

//****************************************************************************************      POPULATION PLUGIN FUNCTIONS     ****************************************************************************

    @Override
    public String getName() { return "Normalization"; }

    @Override
    public String getVersion() { return gVersion; }

    @Override
    public List<String> getParameters() { return parameterNames; }

    @Override
    public ExportFileTypes useExportFileType() { return (fileType); }

    //Used from the TopGenes Plugin
    @Override
    public Icon getIcon()
    {
        if (gIcon == null)
        {
            URL url = getClass().getClassLoader().getResource("images/plugin-icon.png");
            if (url != null)
                gIcon = new ImageIcon(url);
        }
        return gIcon;
    }

    //Used from the TopGenes Plugin
    @Override
    public void setElement(SElement element){
        SElement params = element.getChild("Parameters");
        if (params == null)
            return;
        parameterNames.clear();
        for (SElement elem : params.getChildren())
        {
            parameterNames.add(elem.getString("name"));
        }
    }

    //This is where i need to call the R script and output a CSV file
    @Override
    public ExternalAlgorithmResults invokeAlgorithm(SElement anSElement, File sampleFile, File outputFolder){

        ExternalAlgorithmResults results = new ExternalAlgorithmResults();

        //sampleFile.getName();
        String defaultDocFolder = (new HomeEnv()).getUserDocumentsFolder();
        File docFolder = new File(defaultDocFolder);
        if(!sampleFile.exists()){
            results.setErrorMessage("Sample does not exist!");
        }
        else{
            String trimmedFileName = StringUtil.rtrim(sampleFile.getName(), ".csv");
            long startTime = System.nanoTime();
            File normResults = this.performNormalization(sampleFile, trimmedFileName, this.parameterNames, docFolder.getAbsolutePath());
            long estimatedTime = System.nanoTime() - startTime;
//            System.out.println(outputFolder.getAbsoluteFile());
//            System.out.println(normResults);

            double seconds  = (double)estimatedTime/ 1000000000.0;

            System.out.println("The elapsed time it took to run normalization in seconds was: " + seconds);
            results.setCSVFile(normResults);
        }
        return  results;
    }

    @Override
    public SElement getElement(){
        SElement result = new SElement(getName());

        if (!parameterNames.isEmpty()){
            SElement elem = new SElement("Parameters");
            result.addContent(elem);
            for (String pName : parameterNames){
                SElement s = new SElement("P");
                s.setString("name", pName);
                elem.addContent(s);
            }
        }
        return result;
    }


    //This function takes in as parameters a samplefile (csv file, a trimmed file name for us to append to, the list of
    //parameters that a user selected, and the absolute path of the outputfile
//    private File performNormalization(File fileToUse, String trimmedFile, List<String> paramNames, String absOutputFilePath){
//
//    }
    @Override
    public boolean promptForOptions(SElement anSElement, List<String> pNames) {

        // Use a helper method to get a ParameterSetMgrInterface, used by ParameterSelectionPanel
        ParameterSetMgrInterface mgr = PluginHelper.getParameterSetMgr(anSElement);
        if (mgr == null)
            return false;

        List<Object> guiObjects = new ArrayList<Object>();
        FJLabel explainText = new FJLabel();
        guiObjects.add(explainText);
        String text = "<html><body>";
        text += "This plugin Normalizes a selection of genes and outputs the normalized data into a new CSV file";
        text += "<ul>";
        text += "</ul>";
        text += "</body></html>";
        explainText.setText(text);

        //this code here i need to figure out how to implement the amount of choices they made so that i can display them as a number
        //****************************************************************************************************************************************************************************************************************
        ParameterSelectionPanel pane = new ParameterSelectionPanel(mgr, eParameterSelectionMode.WithSetsAndParameters, true, false, false, true);
        Dimension dim = new Dimension(300, 500);
        pane.setMaximumSize(dim);
        pane.setMinimumSize(dim);
        pane.setPreferredSize(dim);

        pane.setSelectedParameters(parameterNames); //was pName names
        parameterNames.clear(); //pName names

        //String text2 = "This is a test string that i made to display something! The current num is: ";

        //*************************************************************************************************************************************8
        //Need to figure out how to make the number of parameters that a user selects dynamic so that they know
//        int numofParameters;
//        numofParameters = pane.getParameterSelection().size();
//        guiObjects.add(text2 + numofParameters);
//        //*************************************************************************************************************************************8
//
        guiObjects.add(pane);
        //****************************************************************************************************************************************************************************************************************

        JPanel outputField = new JPanel();
        TitledBorder border = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK),"Output Field");
        border.setTitleJustification(TitledBorder.LEFT);
        outputField.setBorder(border);


        JLabel newCSVFileName = new JLabel();
        newCSVFileName.setText("CSV File Prefix:");
        newCSVFileName.setFont(new Font(outputField.getName(), Font.PLAIN, 16)); //Fix the font size to fit the label correctly
        outputField.add(newCSVFileName);
        outputField.setLayout(new FlowLayout(FlowLayout.LEFT));

        JTextField normCSVFile = new JTextField();
        //This needs to be the samplefile name
        normCSVFile.setText("NORM_");//+ CSV_FILE_NAME);
        normCSVFile.setSize(800, 50);
        Prefix = normCSVFile.getText();
        outputField.add(normCSVFile);


        JLabel saveText = new JLabel();
        saveText.setText("(Saved in your Documents Folder)");
        saveText.setFont(new Font(outputField.getName(), Font.PLAIN, 16));
        outputField.add(saveText);

        guiObjects.add(outputField);

        //TOO BIG!!!
//        JFileChooser directory = new JFileChooser();
//        directory.setCurrentDirectory(new java.io.File("."));
//        directory.showSaveDialog(outputField);
//        outputField.add(directory);
//        //outputField.setLayout(new FlowLayout(FlowLayout.RIGHT));

        int option = JOptionPane.showConfirmDialog(null, guiObjects.toArray(), "Normalization", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null);

        //Here is where i would need to enable and disable the okay button if if the user
        if (option == JOptionPane.OK_OPTION) {
            // user clicked ok, get all selected parameters
            parameterNames.addAll(pane.getParameterSelection());

            //outputField.add(directory);

            // make sure 'CellId' is included, so input data file will have it
            if (!parameterNames.contains("CellId"))
                parameterNames.add("CellId");
            return true;
        }
        return false;
    }

//****************************************************************************************      NORMALIZATION FUNCTIONS     ****************************************************************************

    /**
     * This function should get the R script and return the path to the script
     */
    private File getScriptFile(File absolutePath) {
                if(gScriptFile == null) {
            InputStream findScriptPath = this.getClass().getClassLoader().getResourceAsStream("scripts/RScript.Normalization.Template.R");
            if(findScriptPath != null) {
                try {
                    File scriptFile = new File(absolutePath, "RScript.Normalization.Template.R");
                    FileUtil.copyStreamToFile(findScriptPath, scriptFile);
                    gScriptFile = scriptFile;
                } catch (Exception exception) {
                    System.out.println("Script not found");
                }
                System.out.println("Script found");
            }
        }
//        URL url = getClass().getClassLoader().getResource("scripts/RScript.Normalization.Template.R");
//        if (url != null)
//            try {
//
//                gScriptFile = new File(url.toURI());
//
//            }catch (URISyntaxException exception) {
//                System.out.println("URL Exception");
//            }
//
        return gScriptFile;
    }
    /**
     * This Function should return a file created from the Normalization R script
     *
     */
    private File performNormalization(File fileName, String trimmedFileName, List<String> paramNames, String outputFilePath) {
        //If the output file path is empty or doesn't exist we are going to save it in the users temp folder.
        if (outputFilePath == null || outputFilePath.isEmpty()) {
            outputFilePath = (new HomeEnv()).getUserDocumentsFolder(); //Was  .getUserTempFolder();
        }

        File tempOutputFile = new File(outputFilePath); //VAR 7
        StringWriter SWriter = new StringWriter(); //VAR8
        //VAR 9
        File normResult = this.composeRNormalizationStatements(fileName, trimmedFileName, paramNames, tempOutputFile, SWriter);

        if (normResult == null){
            System.out.println("Norm result is null");
            return null;
        }
        else {
            trimmedFileName = trimmedFileName.replaceAll("..ExtNode", "");
            String newFileName = "RScript.Normalization." + trimmedFileName + ".R"; //VAR 10
            newFileName = newFileName.replaceAll(" ", "_");

            File runRScript = new File(outputFilePath, newFileName);//VAR 11

            try {
                FileUtil.write(runRScript, SWriter.toString());
                RFlowCalculator Batch = new RFlowCalculator(); //VAR12
                Batch.executeRBatch(runRScript);
                if (runRScript != null && runRScript.exists() && runRScript.delete()) {
                    System.out.println("Deleted" + runRScript.getAbsolutePath());
                }
                Batch.deleteROutFile();
            }
            catch (IOException exception){
                System.out.println("IOexception in perform Normalization function");
                exception.printStackTrace();
            }

            return normResult;
        }
    }

    /**
     *
     * @param fileName
     * @param trimmedFileName
     * @param paramNames
     * @param tempOutputFile
     * @param SWriter
     * @return File
     */
    private File composeRNormalizationStatements(File fileName, String trimmedFileName, List<String> paramNames, File tempOutputFile, StringWriter SWriter ) {
        File scriptFile = this.getScriptFile(tempOutputFile); //VAR 7

        if (scriptFile == null){
            System.out.println("No Script File");
            return null;
        }
        else{
            BufferedReader BReader = null; //VAR 8
            //just to check and see if we missed a .csv somewhere in the filename
            if(trimmedFileName.endsWith(".csv")){
                trimmedFileName = trimmedFileName.substring(0, trimmedFileName.length() - ".csv".length());
            }
            if(trimmedFileName.endsWith("..ExtNode")){
                trimmedFileName = trimmedFileName.substring(0, trimmedFileName.length() - "..ExtNode".length());
            }

            //removes whitespace
            trimmedFileName = trimmedFileName.trim();

            System.out.println("compose R Normalization fileName: ");
            System.out.println(fileName);

            String newOutputFileName = Prefix + trimmedFileName; //+ ".csv"; //"Prefix" + trimmedFileName + ".csv"; VAR9

            newOutputFileName = newOutputFileName.replaceAll(" ",  "_");

            if (tempOutputFile == null){
                tempOutputFile = fileName.getParentFile();
            }

            File fileResult = new File(tempOutputFile, newOutputFileName); //VAR 10
            newOutputFileName = fileResult.getAbsolutePath();

            if (EngineManager.isWindows()){
                newOutputFileName = newOutputFileName.replaceAll("\\\\", "/");
            }

            try {
                String reader = fileName.getAbsolutePath(); //VAR 11

                if (EngineManager.isWindows()) {
                    reader = reader.replaceAll("\\\\", "/");
                }

                BReader = new BufferedReader(new FileReader(scriptFile));


                while (true) {
                    String lineReader;
                    while((lineReader = BReader.readLine()) != null) {

                        lineReader = lineReader.replace("SG_DATA_FILE_PATH", reader);
                        lineReader = lineReader.replace("SG_CSV_OUTPUT_FILE", newOutputFileName);


                        System.out.println("The replaced Line for this script is: ");
                        System.out.println(lineReader);

                        SWriter.append(lineReader);
                        SWriter.append('\n');

//                        System.out.println("WE ARE IN COMPOSE R STATEMENTS");
//                        System.out.println(reader);
//                        System.out.println(newOutputFileName);
                    }
                    //WE ARE EXITING HERE!!!!!!!!!!!
                    //System.out.println("Inside while (true) loop");
                    //this appends a new line but are we doing a csv?
                    SWriter.append(lineReader).append('\n');
                    return fileResult;
                }


            }
            catch (FileNotFoundException exception) {
                exception.printStackTrace();
            }
            catch (IOException exception) {
                exception.printStackTrace();
            }
            finally {
                if (BReader != null) {
                    try { BReader.close(); }
                    catch (IOException exception) {
                        exception.printStackTrace();
                    }
                }
            }

            System.out.println("Printing file Result");
            System.out.println(fileResult);
            return fileResult;
        }
    }


}
