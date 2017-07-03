import com.flowjo.lib.parameters.ParameterSelectionPanel;
import com.flowjo.lib.parameters.ParameterSelectionPanel.eParameterSelectionMode;
import com.flowjo.lib.parameters.ParameterSetMgrInterface;
import com.treestar.lib.PluginHelper;
import com.treestar.lib.core.ExportFileTypes;
import com.treestar.lib.core.ExternalAlgorithmResults;
import com.treestar.lib.core.PopulationPluginInterface;
import com.treestar.lib.data.StringUtil;
import com.treestar.lib.file.FileUtil;
import com.treestar.lib.gui.panels.FJLabel;
import com.treestar.lib.xml.SElement;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Rusty Raymond on 6/30/2017.
 */
public class Normalization02 implements PopulationPluginInterface {
    private List<String> parameterNames = new ArrayList<String>();
    private static final String gVersion = "0.2";
    private ExportFileTypes fileType = ExportFileTypes.CSV_SCALE;
    private static Icon gIcon = null;
    private static File gScriptFile = null;


    @Override
    public String getName() {return "Normalization 0.2"; }

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
        //WRITE STUFF IN HERE
        ExternalAlgorithmResults results = new ExternalAlgorithmResults();
        if(!sampleFile.exists()){
            results.setErrorMessage("Sample does not exist!");
        }
        else{
//            this.checkUseExistingFiles(anSElement);
//            String trimmedFileName = StringUtil.rtrim(sampleFile.getName(), ".csv");
//            File normResults = this.performNormalization(sampleFile, trimmedFileName, this.preprocessCompParameterNames(), outputFolder.getAbsolutePath());
//            results.setCSVFile(normResults);
        }
        return  results;
    }

    @Override
   public SElement getElement(){
        //WRITE STUFF IN HERE
        SElement result = new SElement(getName());

        if (!parameterNames.isEmpty()){
            SElement elem = new SElement("Parameters");
            result.addContent(elem);
            for (String pName : parameterNames){
                SElement s = new SElement("P");
                elem.setString("name", pName);
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
    public boolean promptForOptions(SElement fcmlQueryElement, List<String> pNames) {

        // Use a helper method to get a ParameterSetMgrInterface, used by ParameterSelectionPanel
        ParameterSetMgrInterface mgr = PluginHelper.getParameterSetMgr(fcmlQueryElement);
        if (mgr == null)
            return false;

        List<Object> guiObjects = new ArrayList<Object>();
        FJLabel explainText = new FJLabel();
        guiObjects.add(explainText);
        String text = "<html><body>";
        text += "This plugin Normalizes a GENE SET and outputs it into a new CSV file";
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

        pane.setSelectedParameters(parameterNames);
        parameterNames.clear();

        String text2 = "This is a test string that i made to display something! The current num is: ";
        text2.toUpperCase();
        int numofParameters;
        numofParameters = pane.getParameterSelection().size();
        guiObjects.add(text2 + numofParameters);
        guiObjects.add(pane);
        //****************************************************************************************************************************************************************************************************************

        JPanel outputField = new JPanel();
        TitledBorder border = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK),"Output Field");
        border.setTitleJustification(TitledBorder.LEFT);
        outputField.setBorder(border);


        JLabel newCSVFileName = new JLabel();
        newCSVFileName.setText("New CSV File Name:");
        newCSVFileName.setFont(new Font(outputField.getName(), Font.PLAIN, 16)); //Fix the font size to fit the label correctly
        outputField.add(newCSVFileName);
        outputField.setLayout(new FlowLayout(FlowLayout.LEFT));

        JTextField normCSVFile = new JTextField();
        normCSVFile.setText("NORM_FileNameHere.csv");
        normCSVFile.setSize(800, 50);
        outputField.add(normCSVFile);
        guiObjects.add(outputField);

        int option = JOptionPane.showConfirmDialog(null, guiObjects.toArray(), "Normalization 0.2", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null);

        //Here is where i would need to enable and disable the okay button if if the user
       if (option == JOptionPane.OK_OPTION) {
            // user clicked ok, get all selected parameters
            parameterNames.addAll(pane.getParameterSelection());
            // make sure 'CellId' is included, so input data file will have it
            if (!parameterNames.contains("CellId"))
                parameterNames.add("CellId");
            return true;
        }
        return false;
    }

    //This function should get the R script
    private File getScriptFile(File absolutePath) {
        if(gScriptFile == null) {
            InputStream findScriptPath = this.getClass().getClassLoader().getResourceAsStream("scripts/RScript.Normalization.Template.R");
            if(findScriptPath != null) {
                try {
                    File scriptFile = new File(absolutePath, "RScript.Normalization.Template.R");
                    FileUtil.copyStreamToFile(findScriptPath, scriptFile);
                    gScriptFile = scriptFile;
                } catch (Exception exception) {
                    ;
                }
            }
        }
        return gScriptFile;
    }
}
