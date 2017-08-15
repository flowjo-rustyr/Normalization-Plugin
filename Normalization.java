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
import com.treestar.lib.FJPluginHelper;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Rusty Raymond on 6/30/2017.
 */
public class Normalization implements PopulationPluginInterface {
    private List<String> parameterNames = new ArrayList<String>();
    private static final String gVersion = "0.2";
    private static Icon gIcon = null;
    private static File gScriptFile = null;
    private static String Prefix = "";
    private static int parameterSelectionCount = 0;
    private List<String> outputFiles = new ArrayList<String>();
    private static String fileErrorName;
    private static String fileWarningName;

//****************************************************************************************      POPULATION PLUGIN FUNCTIONS     ****************************************************************************

    @Override
    public String getName() { return "Normalization"; }

    @Override
    public String getVersion() { return gVersion; }

    @Override
    public List<String> getParameters() { return parameterNames; }

    @Override
    public ExportFileTypes useExportFileType() { return (ExportFileTypes.CSV_PIR_SCALE); }


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

    @Override
    public ExternalAlgorithmResults invokeAlgorithm(SElement anSElement, File sampleFile, File outputFolder){

        ExternalAlgorithmResults results = new ExternalAlgorithmResults();
        if(!sampleFile.exists()){
            results.setErrorMessage("Sample does not exist!");
        }
        else{
            String trimmedFileName = StringUtil.rtrim(sampleFile.getName(), ".csv");
            long startTime = System.nanoTime();
            File normResults = this.performNormalization(sampleFile, trimmedFileName, this.parameterNames, outputFolder.getAbsolutePath());

            outputFiles.add(normResults.getAbsolutePath().toString());

            FJPluginHelper.loadSamplesIntoWorkspace(anSElement, new String[]
                    {outputFiles.get(0),});

            long estimatedTime = System.nanoTime() - startTime;

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

    @Override
    public boolean promptForOptions(SElement anSElement, List<String> pNames) {
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

        JLabel paramCount = new JLabel();
        paramCount.setText("The Current number of parameters selected is: " + parameterSelectionCount);
        guiObjects.add(paramCount);

        ParameterSelectionPanel pane = new ParameterSelectionPanel(mgr, eParameterSelectionMode.WithSetsAndParameters, true, false, false, true);
        Dimension dim = new Dimension(300, 500);
        pane.setMaximumSize(dim);
        pane.setMinimumSize(dim);
        pane.setPreferredSize(dim);
        pane.addParameterSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                parameterSelectionCount = pane.getParameterSelection().size();
                paramCount.setText("The Current number of parameters selected is: " + parameterSelectionCount);
            }

        });

        pane.setSelectedParameters(parameterNames);
        parameterNames.clear();

        guiObjects.add(pane);

        JPanel outputField = new JPanel();
        TitledBorder border = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK),"Output Field");
        border.setTitleJustification(TitledBorder.LEFT);
        outputField.setBorder(border);

        JLabel newCSVFileName = new JLabel();
        newCSVFileName.setText("CSV File Prefix:");
        newCSVFileName.setFont(new Font(outputField.getName(), Font.PLAIN, 16));
        outputField.add(newCSVFileName);
        outputField.setLayout(new FlowLayout(FlowLayout.LEFT));

        JTextField normCSVFile = new JTextField();
        normCSVFile.setText("NORM_");
        normCSVFile.setSize(800, 50);
        outputField.add(normCSVFile);

        guiObjects.add(outputField);

        int option = JOptionPane.showConfirmDialog(null, guiObjects.toArray(), "Normalization", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null);

        if (option == JOptionPane.OK_OPTION) {
            parameterNames.addAll(pane.getParameterSelection());
            Prefix = normCSVFile.getText();
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
        return gScriptFile;
    }

    /**
     * This Function should return a file created from the Normalization R script
     *
     */
    private File performNormalization(File fileName, String trimmedFileName, List<String> paramNames, String outputFilePath) {
        //If the output file path is empty or doesn't exist we are going to save it in the users temp folder.
        if (outputFilePath == null || outputFilePath.isEmpty()) {
            outputFilePath = (new HomeEnv()).getUserDocumentsFolder();
        }

        File tempOutputFile = new File(outputFilePath);
        StringWriter SWriter = new StringWriter();
        File normResult = this.composeRNormalizationStatements(fileName, trimmedFileName, paramNames, tempOutputFile, SWriter);

        if (normResult == null){
            System.out.println("Norm result is null");
            return null;
        }
        else {
            trimmedFileName = trimmedFileName.replaceAll("..ExtNode", "");
            String newFileName = "RScript.Normalization." + trimmedFileName + ".R";
            newFileName = newFileName.replaceAll(" ", "_");

            File runRScript = new File(outputFilePath, newFileName);

            try {
                FileUtil.write(runRScript, SWriter.toString());
                RFlowCalculator Batch = new RFlowCalculator();
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
        File scriptFile = this.getScriptFile(tempOutputFile);

        if (scriptFile == null){
            System.out.println("No Script File");
            return null;
        }
        else{
            BufferedReader BReader = null;
            if(trimmedFileName.endsWith(".csv")){
                trimmedFileName = trimmedFileName.substring(0, trimmedFileName.length() - ".csv".length());
            }
            if(trimmedFileName.endsWith("..ExtNode")){
                trimmedFileName = trimmedFileName.substring(0, trimmedFileName.length() - "..ExtNode".length());
            }

            String errOutputName = trimmedFileName;

            if(errOutputName.endsWith(".csv")){
                errOutputName = errOutputName.substring(0, errOutputName.length() - ".csv".length());
            }

            fileErrorName = errOutputName + "_outputScriptError.txt";
            fileWarningName = errOutputName + "_outputScriptWarning.txt";

            if(!trimmedFileName.endsWith(".csv"))
            {
                trimmedFileName = trimmedFileName + ".csv";
            }

            trimmedFileName = trimmedFileName.trim();

//          Uncomment to display the the filename of the R file
//          System.out.println("compose R Normalization fileName: ");
//          System.out.println(fileName);

            String newOutputFileName = Prefix + trimmedFileName;

            newOutputFileName = newOutputFileName.replaceAll(" ",  "_");

            if (tempOutputFile == null){
                tempOutputFile = fileName.getParentFile();
            }

            File fileResult = new File(tempOutputFile, newOutputFileName);
            newOutputFileName = fileResult.getAbsolutePath();

            if (EngineManager.isWindows()){
                newOutputFileName = newOutputFileName.replaceAll("\\\\", "/");
            }

            try {
                String reader = fileName.getAbsolutePath();

                if (EngineManager.isWindows()) {
                    reader = reader.replaceAll("\\\\", "/");
                }

                BReader = new BufferedReader(new FileReader(scriptFile));

                while (true) {
                    String lineReader;
                    while((lineReader = BReader.readLine()) != null) {

                        lineReader = lineReader.replace("SG_DATA_FILE_PATH", reader);
                        lineReader = lineReader.replace("SG_CSV_OUTPUT_FILE", newOutputFileName);
                        lineReader = lineReader.replace("SG_TXT_ERROR_FILE", errOutputName);

                        //uncomment lines to see the replaced lines in the script
//                      System.out.println("The replaced Line for this script is: ");
//                      System.out.println(lineReader);

                        SWriter.append(lineReader);
                        SWriter.append('\n');

                    }
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
            return fileResult;
        }
    }
}
