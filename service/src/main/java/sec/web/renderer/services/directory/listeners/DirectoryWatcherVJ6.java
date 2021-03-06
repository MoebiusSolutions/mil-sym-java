package sec.web.renderer.services.directory.listeners;

import java.util.Calendar;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import sec.web.renderer.utils.IoUtilities;

@SuppressWarnings("unused")
public class DirectoryWatcherVJ6 implements ServletContextListener {
	public static final Logger LOGGER = Logger.getLogger(DirectoryWatcherVJ6.class.getName());
	public static DirectoryReaderTaskScheduler scheduler = new DirectoryReaderTaskScheduler();
	public final int INTERVAL = 60 * 1000;
	private IoUtilities utils = new IoUtilities();
	
	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		scheduler.stopTimer();		
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		//TODO: generate default properties file
		
		Properties props = IoUtilities.getProperties();//DirectoryReaderUtils.loadProperties("properties/prop.properties", DirectoryWatcherVJ6.class.getClassLoader());
		StringBuilder filePath = new StringBuilder();

		filePath.append(props.getProperty("directoryPath"));
//		filePath.append(File.separator);
//		filePath.append(props.getProperty("directoryName"));
//		filePath.append(File.separator);

		scheduler.startTimer(filePath.toString(), INTERVAL);
		LOGGER.log(Level.INFO, "Timer Initialized");
		
		System.err.println("\n\n\n" + new String("starting folder timer ").toUpperCase() + Calendar.getInstance().getTime().toString() + "\n\n\n");		
	}

	

}
