package com.dystify.kkdystrack.v2.core.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Scanner;
import java.util.TimeZone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.Callback;

/**
 * contains static utility methods and constants, which don't really have a specific place
 * @author Duemmer
 *
 */
public class Util 
{ 	
	private static Logger log = LogManager.getLogger(Util.class);

	/** if a potential song file does not have these extensions, discount it automatically */
	public static final String[] legalExtensions =
		{
				"mp3",
				"flac",
				"m4a",
				"wma",
				"ogg",
				"wav"
		};

	/**
	 * if any path element in a potential song directory contains this, reject that directory, as these dirs contain metadata
	 * which confuses the song loader
	 */
	public static final String[] illegalDirectoryContents = 
		{
				File.pathSeparator + "__MACOSX",
				File.pathSeparator + "."
		};

	public static final ExtensionFilter audioFileExtensionFilter = new ExtensionFilter("Valid Audio Files", legalExtensions);




	/**
	 * Initializes the application's logging infastructure. In particular, sets up
	 * the textAreaAppenders, which display log messages to the GUI, and creating the custom log levels
	 * that will be directed there
	 * @param info the textArea to use for the info box
	 * @param error the textArea to use for the info box
	 */
	public static void initLoggers(TextArea info, TextArea error) 
	{
		Map<String, Appender> appenders = ((org.apache.logging.log4j.core.Logger) LogManager.getLogger()).getAppenders();
		((TextAreaAppender)appenders.get("gui_appender_info")).setTextArea(info);
		((TextAreaAppender)appenders.get("gui_appender_error")).setTextArea(error);
	}


	/**
	 * Loads the path of a file in the resource directory. Unlike just using `new File(...)`, this
	 * will work both in editor as well as deployed
	 * @param path
	 * @return
	 * @throws FileNotFoundException 
	 */
	public static URL loadFile(String path) throws FileNotFoundException
	{
		File f;
		URL u;
		try {
			String jarPath = "";
			jarPath = Util.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
			path = new File(jarPath).getParent() + "/resource" +path;
			f = new File(path);
			u = f.toURI().toURL();
		} catch (MalformedURLException | URISyntaxException e) { throw new FileNotFoundException("failed to find resource at \"" +path +"\""); }

		if(u == null || !f.exists())
			throw new FileNotFoundException("failed to find resource at \"" +path +"\"");

		return u; // in a jar, the URL code sometimes gets confused when things aren't found. Only return a valid URL if we are sure nothing is amiss

	}



	/**
	 * Converts a fractional number of seconds to a nice textual
	 * representation: <code>%h:%mm:%ss</code>
	 * @param rawSecs
	 * @return
	 */
	public static String secondsToTimeString(double time) {
		int rawSecs = (int) Math.floor(time);
		double millis = time - rawSecs;
		int days = Math.floorDiv(rawSecs, 86400);
		int hours = Math.floorDiv((rawSecs % 86400), 3600);
		int minutes = Math.floorDiv((rawSecs % 3600), 60);	
		int seconds = rawSecs % 60;

		String ret;
		if(days > 0)
			ret = String.format("%d:%02d:%02d:%02d", days, hours, minutes, seconds);
		else if(hours > 0)
			ret = String.format("%d:%02d:%02d", hours, minutes, seconds);
		else
			ret = String.format("%d:%02d", minutes, seconds);

		if(millis != 0)
			ret += "."+ String.format("%03d", (int)(millis*1000));

		return ret;
	}


	/**
	 * Converts an integer number of seconds to a nice textual
	 * representation: <code>%h:%mm:%ss</code>
	 * @param rawSecs
	 * @return
	 */
	public static String intSecondsToTimeString(double secs) {
		return secondsToTimeString((int) secs);
	}




	public static String fmtDateTimeDisp(Date d) {
		if(d != null) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
			sdf.setTimeZone(TimeZone.getDefault());
			return sdf.format(d);
		}
		return "N/A";
	}


	/**
	 * returns true if the string is valid input for a textfield that should contain a floating 
	 * point value, as in: at least 1 contingent digit string, followed by a `.` and optionally
	 * more digits, with no other text following
	 * @param str
	 * @return
	 */
	public static boolean validNumericTextEntryStr(String str, boolean allowDouble)
	{
		if(allowDouble)
			return str.matches("^[0-9]+((\\.|,)[0-9]*|)$");
		else
			return str.matches("^[0-9]+$");
	}


	/**
	 * Creates and initializes a new instance of an controller class
	 * @return
	 */
	public static Object loadController(String url) throws IOException{
		FXMLLoader loader = null;
		loader = new FXMLLoader(loadFile(url));
		loader.load();
		return loader.getController();
	}



	/**
	 * Retrieves the contents of a URL and returns it as a String
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public static String getUrlContents(URL url) throws IOException {
		URLConnection cnx = url.openConnection();
		Scanner s = new Scanner(cnx.getInputStream());
		String ret = s.useDelimiter("\\A").next();
		s.close();
		return ret;
	}



	/**
	 * fully expands all the elements of the treeview
	 * @param t
	 */
	public static void expandTreeView(TreeItem<?> t) {
		if(t != null && !t.isLeaf()) {
			t.setExpanded(true);
			for(TreeItem<?> child : t.getChildren())
				expandTreeView(child);
		}
	}


	/**
	 * Convenience method to automatically create a new named daemon thread
	 * @param name
	 * @param r
	 * @return
	 */
	public static void runNewDaemon(String name, Runnable r) {
		Thread t = new Thread(r);
		if(!name.isEmpty()) {
			t.setName(name);
			log.debug("Launching Thread \"" +name+ "\"");
		}
		t.setDaemon(true);
		t.start();
	}




	public static void runNewDaemon(Runnable r) {
		runNewDaemon("", r);
	}




	/**
	 * Adds input validation to a textfield slated for use with numeric inputs, both doubles and integers.
	 * 
	 * @param tf the textfield to bind
	 * @param allowDouble if true, will accept floating point numbers, only integers if false
	 * @param onNewValidValue will get called whenever the numeric value of the control is changed, passed as an input to the callback
	 */
	public static void configTextFieldAsNumericInput(TextField tf, boolean allowDouble, Callback<Number, Void> onNewValidValue) {
		tf.setText("0");
		tf.textProperty().addListener((obs, oldVal, newVal) -> {
			if(!newVal.isEmpty()) {
				if(Util.validNumericTextEntryStr(newVal, allowDouble)) {// first see if the new one is valid, use it if it is
					if(allowDouble && newVal.matches("(-|^)[0-9]+(\\.|,)$"))
						newVal += "0"; // add a trailing 0 if needed
					if(allowDouble) // allocate the correct numeric type
						onNewValidValue.call(new Double(newVal));
					else
						onNewValidValue.call(new Integer(newVal));
				} else if(Util.validNumericTextEntryStr(oldVal, allowDouble)) { // if not, try the old one, and revert back if need be
					tf.setText(oldVal);
				} 

				else {// if that's no good just set it to 0
					tf.setText("0.0");
					onNewValidValue.call(0);
				}
			}
		});

		tf.focusedProperty().addListener((obs, oldVal, newVal) -> {
			if(tf.getText().isEmpty())
				tf.setText("0");
		});
	}



	public static void configTextFieldAsTimeIntervalInput(TextField tf, Callback<Number, Void> onNewValidValue) {
		tf.setText("00:00:00");
		tf.textProperty().addListener((obs, oldVal, newVal) -> {
			if(!newVal.isEmpty()) {
				String regex = "^[0-9]+(:$|:[0-9]{1,2}){0,3}($|(\\.|,)[0-9]*$)";
				if(newVal.matches(regex)) {
					if(newVal.trim().matches(".*[^0-9]$")) // ends in something other than a number, so appending a 0 will make it parseable because this should be a decimal point
						newVal += "0";
					onNewValidValue.call(parseTimeIntervalFromString(newVal));
				} else if(oldVal.matches(regex))
					tf.setText(oldVal);
				else {
					tf.setText("00:00:00");
					onNewValidValue.call(0.0);
				}
			}
		});

		tf.focusedProperty().addListener((obs, oldVal, newVal) -> {
			if(tf.getText().isEmpty())
				tf.setText("0");
		});
	}




	private static final int[] TIME_MULTIPLIERS = {1, 60, 3600, 86400};
	public static double parseTimeIntervalFromString(String timeIntervalStr) {
		String[] slots = timeIntervalStr.split(":");
		double seconds = 0;
		for(int i=0; i<slots.length; i++) {
			try { seconds += TIME_MULTIPLIERS[i]*Double.parseDouble(slots[(slots.length-1)-i]); }
			catch(NumberFormatException e) { log.debug("error parsing double from string \"" +slots[i]+ "\""); }
		}
		return seconds;
	}






	/**
	 * checks if the extension on the song file is a legal sound
	 * file extension. These are listed in {@link SongUtils#legalExtensions}
	 */
	public static boolean isLegalAudioFileExtension(File song)
	{
		if(song != null && song.isFile() && song.getName().contains(".")) { // don't even bother checking extensions if it isn't a file, or doesn't have an extension
			String ext = song.getName().substring(song.getName().lastIndexOf(".")+1);

			for(String foo : legalExtensions)
				if(foo.equalsIgnoreCase(ext))
					return true;
			System.out.println("rejected file \"" +song.getName()+ "\""); // don't log if its a dir either
		}
		return false;
	}


	/**
	 * Checks if the current directory is a legal song directory. It's legal if the File
	 * object isn't null, it's an existing directory, no parent directories start with an illegal
	 * directory name
	 * @param dir
	 * @return
	 */
	public static boolean isLegalSongDirectory(File dir) {
		if(dir != null && dir.isDirectory()) {
			for(String s : illegalDirectoryContents)
				if(dir.getAbsolutePath().contains(s))
					return false;
			return true;
		}
		return false;
	}





	/**
	 * Gets the extension of a file, returning it with the '.' excluded
	 */
	public static String getExt(File song)
	{
		return song.getName().substring(song.getName().lastIndexOf('.') + 1);
	}



	/**
	 * Does a simple formatting of a string to convert it to an SQL - safe identifier. In particular, 
	 * it replaces any continuous sequence of offending characters with underscores, and trims the 
	 * length down to 50 characters max. NOTE: will not check for minimum length!
	 * @param toClean the string to validate as an ide
	 * @return
	 */
	public static String fmtSqlIdentifier(String toClean) {
		String regex = "[^0-9a-zA-Z_]+";
		String ret = toClean.replaceAll(regex, "_");
		if(ret.length() > 50)
			ret = ret.substring(0, 50);
		return ret;
	}
}







