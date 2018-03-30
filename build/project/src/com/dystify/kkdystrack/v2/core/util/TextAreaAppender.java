package com.dystify.kkdystrack.v2.core.util;

import java.io.Serializable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import javafx.application.Platform;
import javafx.scene.control.TextArea;



/**
 * Allows for doing log output to a JavaFX TextArea from Log4j2
 * basically copied from: <a href="http://blog.pikodat.com/2015/10/11/frontend-logging-with-javafx/">this place</a>
 * @author Duemmer
 *
 */
@Plugin(name = "TextAreaAppender", category = "core", elementType = "appender", printObject = true)
public class TextAreaAppender extends AbstractAppender
{
	private TextArea textArea;
	private final Lock readLock = new ReentrantReadWriteLock().readLock();
	private static final int MAX_TEXTAREA_LENGTH = 50000;
	
	
	protected TextAreaAppender
	(
			String name, 
			Filter filter, 
			Layout<? extends Serializable> layout,
			boolean ignoreExceptions
	) 
	{ super(name, filter, layout, ignoreExceptions); }

	
	
	
	
	
	@Override
	public void append(LogEvent event) 
	{
		readLock.lock();
		final String msg = new String(getLayout().toByteArray(event));
		try {
			Platform.runLater(() -> {
				try {
					if(textArea != null) {
						// if it'll be too big, trim some characters off the back first
						if(textArea.getLength() + msg.length()> MAX_TEXTAREA_LENGTH)
							textArea.setText(textArea.getText(2000, textArea.getLength()-1));
						textArea.appendText(msg); // add the message
					}
					else
						LOGGER.error("Textarea for TextAreaAppender is null!");
				}
				 catch (Throwable t) {
					 System.err.println("Exception encountered in TextAreaAppender");
				}
			});
		} catch (IllegalStateException e) { e.printStackTrace(); } 
		finally { readLock.unlock(); }
	}
	
	
	
	
	/**
	   * Factory method. Log4j will parse the configuration and call this factory 
	   * method to construct the appender with
	   * the configured attributes.
	   *
	   * @param name   Name of appender
	   * @param layout Log layout of appender
	   * @param filter Filter for appender
	   * @return The TextAreaAppender
	   */
	@PluginFactory
	public static TextAreaAppender createAppender (
			@PluginAttribute("name") String name,
			@PluginElement("Layout") Layout<? extends Serializable> layout,
			@PluginElement("Filter") final Filter filter)
	{
		TextAreaAppender t = null;
		
		if(name == null)
			LOGGER.error("Name for TextAreaAppender was not set!");
		else {
			if(layout == null)
				layout = PatternLayout.createDefaultLayout();
			t = new TextAreaAppender(name, filter, layout, true);
		}
		
		return t;
	}






	public TextArea getTextArea() {
		return textArea;
	}






	public void setTextArea(TextArea textArea) {
		this.textArea = textArea;
	}
	
	
	
	
	
	

}
