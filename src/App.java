import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Clipboard;

import javax.swing.*;
import java.awt.event.*;

import Misc.GetSystemInfo;
import FileUtil.FileUtil;

/**
 * Student ID Merger
 * This program will take input from a CSV containing student names and student IDs and merge them with yearbook photos in a folder that contains a text file with the student's name and photo location. This will be done by parcing the name and checking the about of matches, if there is to many matches or not enought it will prompt the user to input the student's ID.
 * 
 * Created for Bonham Independent School District
 * 
 * @author Walter Ozmore
 * @date 6/23/2022
 */
public class App {
    // UserInput though console, this is used by the console version of the program
    static Scanner userInput = new Scanner( System.in );

    // Students loaded in with photos, ie. from yearbooks
    static ArrayList<Student> students = new ArrayList<Student>();

    // Students that are loaded in via accender
    static ArrayList<Student> accenderStudents = new ArrayList<Student>();

    // Files to store, this is mostly used in the GUI version
    static File accenderFile = null;
    static File yearBookFile = null;
    static File outputFolder = null;
    static File lastDir = new File("H:\\Shared drives\\Technology\\Student IDs");

    // Determinds if the GUI should be enabled
    static boolean gui = true;

    public static void main(String[] args) throws Exception { run(); }


    /* ==================== Actual Program Stuff ==================== */

    /**
     * Loops though all students and atempts to match the students first and last name with the loaded student IDs in the map. If a match is not found get user input.
     * 
     * @param x The start of the loop, this is used to have the ability to pause and return to the matching progress
     * @author Walter Ozmore
     */
    static void match() { match(0); }
    static void match(int x) {
        for(; x < students.size(); x++) {
            Student student = students.get(x);

            ArrayList<Student> results = new ArrayList<Student>();

            for(int y=0;y<accenderStudents.size();y++) {
                Student accenderStudent = accenderStudents.get(y);

                /* Create a full name format not seprated by comma
                 * Example: YEAGER, KAMEY SHAWN => KAMEY SHAWN YEAGER
                 */
                String fullName = accenderStudent.fullName;
                int i = fullName.indexOf(",");
				if(i != -1) fullName = fullName.substring(i+2) + " " + fullName.substring(0, i);
                fullName = fullName.toUpperCase();

                // Check if fullName has the firstName and lastName, then add to the results
                if( fullName.contains( student.firstName.toUpperCase() ) && fullName.contains( student.lastName.toUpperCase() ) ) {
                    results.add(accenderStudent);
                }
            }

            if(results.size() == 0 || results.size() >= 2) {
                // if(gui) {
                //     gui_queryUser(student, results, x+1);
                //     return;
                // } else
                //     console_queryUser(student, results);
                continue;
            }

            // Only one result then accept the result as true
            Student result = results.get(0);

            // Copy the result student to the active student
            student.id = result.id;
            student.fullName = result.fullName;

            // Remove the result student to speed up the search for feature students
            accenderStudents.remove( result );
        }
    }

    
    /**
     * Reads the given file and creates students for each student in the file in the array accenderStudents
     * 
     * @param accenderFile The accenderFile to be read
     * @author Walter Ozmore
     */
    static void loadAccenderFile(String accenderFile) { loadAccenderFile( new File( accenderFile ) ); }
    static void loadAccenderFile(File accenderFile) {
        try {
            String csvRegex = ",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)";
            Scanner scanner = new Scanner( accenderFile );
            
            // Get header line
            String[] header = scanner.nextLine().split( csvRegex );

            while( scanner.hasNextLine() ) {
                Student student = new Student();
                String[] list = scanner.nextLine().split( csvRegex );
                for(int x = 0; x < list.length; x++) {
                    // If the place in the CVS corrosponseds with Name then set the name to the student, but the csvRegex does not remove quotes so trim the edges
                    if( header[x].startsWith("Name") ) student.fullName = list[x].substring(1, list[x].length() - 1 );
                    if( header[x].equals("Student ID") ) student.id = list[x];
                    try {
                        if( header[x].equals("Grade") ) student.gradeLevel = Integer.parseInt( list[x] );
                    } catch(Exception e) {}
                }
                accenderStudents.add( student );
            }
            scanner.close();
        } catch( Exception e ) {
            e.printStackTrace();
        }
    }

    /**
     * Loops though all students and copies their photos to the output directory with the name of the photo being the student's id.
     * 
     * @author Walter Ozmore
     */
    static void copyPictures(File outputDirectory) {
        for(Student s:students) {
			if(s.id == null || s.id.length() == 0) continue;
            // Grab the file extention
			String ext = s.yearbookImage.getName().substring(s.yearbookImage.getName().length()-4 );
            // Create output image in the output path with the name of the student's id
			File outputImage = new File(outputDirectory.getAbsolutePath()+"\\"+s.id+ext);
            System.out.println( outputImage.getAbsolutePath() );
            FileUtil.copy(s.yearbookImage, outputImage );
		}
        System.exit(0);
    }

    /**
     * Finds the TXT file in the given directory and reads its contents to a map of all file names to student IDs. Then loops though all files in the directory and creates a new object of Student for each picture and sets their firstname, lastname, and yearbook image location.
     * 
     * @param dir The directory that will be scanned
     * @author Walter Ozmore
     */
    static void load(String dir) { load(new File(dir)); }
    static void load(File dir) {
        /* Check file format */
        boolean valid = true;
        for(File file: dir.listFiles()) {
            if(file.isDirectory()) {
                load(file);
                valid = false;
            }
        }
        if( !valid ) return;
        
        /* Finds the text file and sets it to the variable */
        File textFile = null;
		
		for(File file:dir.listFiles())
			if( file.getName().toLowerCase().endsWith(".txt") )
				textFile = file;

        if(textFile == null) return;
        

        try {
            Scanner scanner = new Scanner( textFile );
            while(scanner.hasNextLine()) {
                String line = scanner.nextLine();
                line = line.replaceAll("\n", "");
                
                /* 
                * Example line: Yearbook 09 00257.JPG 09 Ramirez-Iniguez Manuel 
                * 0 - Yearbook
                * 1 - Grade Level
                * 2 - File Name
                * 3 - First Name
                * 4 - Last Name
                */
                String[] list = line.split("\t");
                Student student = new Student();
                student.firstName = list[5];
                student.lastName = list[4];
                student.yearbookImage = new File( dir.getAbsolutePath() + "\\" + list[2] );
                students.add(student);
            }
            scanner.close();
        } catch(IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }


    /* ==================== Sorting Calls ==================== */
    // These function will be called instead of the GUI or console functions then will call those functions biased on settings

    static void run() { if(gui) gui_run(); else console_run(); }


    /* ==================== GUI Functions ==================== */
    /**
     * Creates a GUI interface with three button and three labels for each file, the user may press the buttons to select the file. There is an aditional button at  the bottom that starts the program
     * 
     * @author Walter Ozmore
     */
    static void gui_run() {
        // Try and grab files
        accenderFile = new File( lastDir.getAbsolutePath() + "\\StudentList_June_2022.csv" );
        yearBookFile = new File( lastDir.getAbsolutePath() + "\\Yearbooks" );
        outputFolder = new File( lastDir.getAbsolutePath() + "\\Student Photo IDs" );

        JFrame frame = new JFrame("Student ID Merger");
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(4,2));


        JLabel label_accenderFile = new JLabel();
        label_accenderFile.setText( (accenderFile == null)? "No File Selected" : accenderFile.getAbsolutePath() );
        panel.add( label_accenderFile );

        JButton button_chooseAccenderFile = new JButton();
        button_chooseAccenderFile.setText("Choose accender file");
        button_chooseAccenderFile.addActionListener( 
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    File file = gui_chooseFile("Accender file");
                    if(file != null) App.accenderFile = file;
                    label_accenderFile.setText( (accenderFile == null)? "No File Selected" : accenderFile.getAbsolutePath() );
                }
            }
        );
        panel.add( button_chooseAccenderFile );


        JLabel label_yearBookFile = new JLabel();
        label_yearBookFile.setText( (yearBookFile == null)? "No File Selected" : yearBookFile.getAbsolutePath() );
        panel.add( label_yearBookFile );

        JButton button_chooseYearbookFile = new JButton();
        button_chooseYearbookFile.setText("Choose yearbook folder");
        button_chooseYearbookFile.addActionListener( 
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    File file = gui_chooseFile("Yearbook Folder");
                    if(file != null) App.yearBookFile = file;
                    label_yearBookFile.setText( (yearBookFile == null)? "No File Selected" : yearBookFile.getAbsolutePath() );
                }
            }
        );
        panel.add( button_chooseYearbookFile );


        JLabel label_outputFile = new JLabel();
        label_outputFile.setText( (outputFolder == null)? "No File Selected" : outputFolder.getAbsolutePath() );
        panel.add( label_outputFile );

        JButton button_chooseOutputFile = new JButton();
        button_chooseOutputFile.setText("Choose output folder");
        button_chooseOutputFile.addActionListener( 
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    File file = gui_chooseFile("Output Folder");
                    if(file != null) App.outputFolder = file;

                    label_outputFile.setText( (outputFolder == null)? "No File Selected" : outputFolder.getAbsolutePath() );
                }
            }
        );
        panel.add( button_chooseOutputFile );


        JButton button_go = new JButton();
        button_go.setText("Run");
        button_go.addActionListener( 
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    // Check if all files are selected
                    if( accenderFile == null ) { JOptionPane.showMessageDialog(null, "The accender file is not selected, please select a file", "Error", JOptionPane.INFORMATION_MESSAGE); return; }
                    if( yearBookFile == null ) { JOptionPane.showMessageDialog(null, "The yearBook file is not selected, please select a file", "Error", JOptionPane.INFORMATION_MESSAGE); return; }
                    if( outputFolder == null ) { JOptionPane.showMessageDialog(null, "The output folder is not selected, please select a file", "Error", JOptionPane.INFORMATION_MESSAGE); return; }

                    // Check if the files exist
                    if( !accenderFile.exists() ) { JOptionPane.showMessageDialog(null, "The accender file does not exist, please select a diffrent file", "Error", JOptionPane.INFORMATION_MESSAGE); return; }
                    if( !yearBookFile.exists() ) { JOptionPane.showMessageDialog(null, "The yearBook file does not exist, please select a diffrent file", "Error", JOptionPane.INFORMATION_MESSAGE); return; }
                    if( !outputFolder.exists() ) { JOptionPane.showMessageDialog(null, "The output folder does not exist, please select a diffrent file", "Error", JOptionPane.INFORMATION_MESSAGE); return; }

                    loadAccenderFile( accenderFile );
                    load( yearBookFile );
                    match();
                    frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                    copyPictures( outputFolder );
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                }
            }
        );
        panel.add( button_go );
        
        frame.add(panel);
        frame.setSize(700, 150);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    /**
     * Creates a new window with the student's name, photo and a list of the possible results. The user is prompted to enter the ID of the student displayed, when entered it will set the entered value to the student's ID.
     * 
     * @param student Student that needs a ID number
     * @param results Results from the search that could be useful for the user 
     * @param x Stored number from the search that will be put back in to the search function to continue the search
     */
    static void gui_queryUser(Student student, ArrayList<Student> results, int z) {
        JFrame frame = new JFrame("Selector");
        JPanel panel = new JPanel();
        panel.setLayout( null );
        
        // Draw image of student
        ImageIcon studentIcon = new ImageIcon( student.yearbookImage.getAbsolutePath() );
        JLabel studentImage = new JLabel( studentIcon );
        studentImage.setBounds(0, 0, 500, 500);
        panel.add( studentImage );

        // String column[] = { "ID", "First Name", "Last Name", "Full Name" };
        String column[] = { "ID", "Full Name", "Grade Level" };
        String data[][] = new String[results.size()][column.length];
        for(int x=0;x<results.size();x++) {
            data[x][1] = results.get(x).fullName;
            data[x][0] = results.get(x).id;
            data[x][2] = results.get(x).gradeLevel + "";
            // data[x][1] = results.get(x).firstName;
            // data[x][2] = results.get(x).lastName;
        }
        JTable jt = new JTable(data,column);
        JScrollPane sp=new JScrollPane(jt);
        sp.setBounds(500, 250, 500, 250);
        panel.add(sp);

        JLabel studentName = new JLabel(student.firstName + " " + student.lastName);
        studentName.setBounds(510, 0, 500, 20);
        panel.add( studentName );

        JLabel fileLocation = new JLabel(student.yearbookImage.getAbsolutePath());
        fileLocation.setBounds(510, 40, 500, 20);
        panel.add( fileLocation );

        // Copy name to clipboard
        StringSelection selection = new StringSelection( student.firstName + " " + student.lastName );
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);

        JLabel enterMessage = new JLabel("Enter the ID of the student:");
        enterMessage.setBounds(510, 20, 200, 20);
        panel.add( enterMessage );

        JTextField textField = new JTextField();
        textField.setBounds(720, 20, 100,20);
        textField.addActionListener( 
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    student.id = textField.getText();
                    System.out.println( student.id );
                    
                    frame.dispose();
                    match(z);
                }
            }
        );
        panel.add( textField );

        frame.add(panel);
        frame.setSize(1000, 500);
        frame.setVisible(true);
        frame.setResizable(false);
    }

    /**
     * Opens a file explorer and promps the user to select a file
     * 
     * @return The file that was selected, if no file was selected returns NULL
     */
    static File gui_chooseFile(String windowName) {
        JFrame jFrame = new JFrame();
        jFrame.setName(windowName);
        JFileChooser J_File_Chooser = new JFileChooser( new File("H:\\Shared drives\\Technology\\Student IDs") );

        J_File_Chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        int option = J_File_Chooser.showOpenDialog(jFrame);
        if(option == JFileChooser.APPROVE_OPTION) {
            File file = J_File_Chooser.getSelectedFile();
            System.out.println( "Selected: " + file.getAbsolutePath() );
            if(file.getParentFile() != null) lastDir = file.getParentFile();
            return file;
        } else {
            return null;
        }
    }


    /* ==================== Console Functions ==================== */

    /**
     * Runs the program using a console interface
     * 
     * @author Walter Ozmore
     */
    static void console_run() {
        // @TODO Create a way to open a file via commandline rather than opening a GUI
        System.out.println("Please choose the accender file");
        loadAccenderFile( gui_chooseFile("Accender File") );

        System.out.println("Please choose the yearbook file");
        load( gui_chooseFile("YearBook Folder") );
        
        match();
        System.out.println("Please choose the output folder");
        File outputDirectory = gui_chooseFile("Output Folder");
        copyPictures( outputDirectory );
    }

    /**
     * Prompts the user with the student's name, photo and a list of the possible results. The user is prompted to enter the ID of the student displayed, when entered it will set the entered value to the student's ID. The user can also enter '?' to bring up the student's photo in the default photo viewer on their system
     * 
     * @param student Student that needs a ID number
     * @param results Results from the search that could be useful for the user 
     */
    static void console_queryUser(Student student, ArrayList<Student> results) {
        do {
            // Print break line
            System.out.println();
            for(int z=0;z<50;z++) System.out.print("=");
            System.out.println();

            System.out.printf("The student %s %s could not be automaticly found, please enter the students ID or enter nothing to skip%n", student.firstName, student.lastName);
            
            // When there are results print out the results
            if(results.size() > 0) {
                System.out.printf("Type ? to open up the image of the student%n%n");
                printStudents( results );
            }
            
            System.out.printf("%nStudent ID: ");
            String line = userInput.nextLine();

            // If the line contains a ? then print out images of the students
            if(line.contains("?") && results.size() > 0) {
                String command = "cmd /c \"" + student.yearbookImage.getAbsolutePath() + "\"";
                System.out.println( command );
                GetSystemInfo.runCommand( command );
                continue;
            }
            
            // If blank skip
            if(line.length() == 0) break;
            
            student.id = line;
        } while(student.id == null);
    }

    /**
     * Prints the array students in a nice format for the user to see
     * @param students
     */
    static void printStudents(ArrayList<Student> students) {
        String format = "%-20s%-20s%-40s%-20s%s%n";
        System.out.printf(format, "First Name", "Last Name", "Full Name", "Student ID", "Photo File Path");
        for(int x=0;x<students.size();x++) {
            Student student = students.get(x);
            System.out.printf(format, 
                student.firstName,
                student.lastName,
                student.fullName,
                student.id,
                (student.yearbookImage != null && student.yearbookImage.exists())? student.yearbookImage.getAbsolutePath() : ""
            );
        }
    }
}

/**
 * A class created for the single purpose of storing data in a way I like
 * 
 * @author Walter Ozmore
 */
 class Student {
    File yearbookImage = null;
    String id;
    String firstName;
    String lastName;
    String fullName;
    int gradeLevel;
}